package org.lin.campusitem.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.kafka.KafkaMessage;
import org.lin.common.kafka.KafkaTopicConstants;
import org.lin.common.kafka.consumer.AbstractKafkaConsumer;
import org.lin.common.entity.Image;
import org.lin.campusitem.kafka.dto.ProductPublishPayload;
import org.lin.campusitem.mapper.ImageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 商品发布事件消费者。
 * <p>
 * 异步处理两类副作用：
 * <ol>
 *   <li>将图片 URL 列表落库到 Image 表（与商品主表通过 relationId 关联）</li>
 *   <li>粉丝通知：本服务只负责发通知事件到 Kafka，由 campus-user 服务的
 *       NotificationConsumer 实际写通知表（跨服务通过 Kafka 解耦）</li>
 * </ol>
 * <p>
 * 幂等保证：继承 {@link AbstractKafkaConsumer}，基于 eventId 去重；
 * 图片落库前再次校验避免重复插入（同一 productId + imageUrl 不重复落）。
 */
@Slf4j
@Component
public class ProductPublishedConsumer extends AbstractKafkaConsumer<ProductPublishPayload> {

    @Autowired
    private ImageMapper imageMapper;

    /**
     * 监听商品发布 Topic。
     * <p>
     * groupId 单独命名为 campus-item-product-group，便于按业务域隔离消费进度。
     */
    @KafkaListener(
            topics = KafkaTopicConstants.PRODUCT_PUBLISHED,
            groupId = "campus-item-product-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(KafkaMessage message) {
        super.handle(message, ProductPublishPayload.class);
    }

    @Override
    protected void doConsume(String eventId, ProductPublishPayload payload) {
        if (payload == null || payload.getProductId() == null) {
            log.warn("[商品发布消费] payload 为空或缺少 productId，跳过 eventId={}", eventId);
            return;
        }
        log.info("[商品发布消费] 开始处理 productId={} hasImages={}",
                payload.getProductId(), payload.isHasImages());

        // 1. 异步落库图片记录
        if (payload.isHasImages() && payload.getImageUrls() != null) {
            persistImages(payload);
        }

        // 2. 粉丝通知：本服务不发通知事件，由 campus-user 监听同 Topic 自行处理
        //    （避免跨服务直接调用，遵循"事件驱动 + 各服务自治"原则）
        //    如需在此触发，可投递 NOTIFICATION_SEND 事件，由 campus-user 消费
    }

    /**
     * 将图片 URL 列表批量落库到 Image 表。
     * <p>
     * 幂等策略：先查是否已存在同 productId 的图片记录，存在则跳过。
     * 防止 Kafka 重试导致重复插入。
     */
    private void persistImages(ProductPublishPayload payload) {
        Long productId = payload.getProductId();
        // 幂等校验：已有图片记录则跳过（重试场景下保护）
        var existing = imageMapper.findByTypeAndRelationId(Image.TYPE_PRODUCT, productId);
        if (existing != null && !existing.isEmpty()) {
            log.info("[商品发布消费] productId={} 图片已存在，跳过落库（幂等保护）", productId);
            return;
        }

        int sortOrder = 0;
        for (String imageUrl : payload.getImageUrls()) {
            if (imageUrl == null || imageUrl.isEmpty()) {
                continue;
            }
            Image image = new Image();
            image.setType(Image.TYPE_PRODUCT);
            image.setRelationId(productId);
            image.setImageUrl(imageUrl);
            image.setSortOrder(sortOrder++);
            image.setCreateTime(new Date());
            imageMapper.insert(image);
        }
        log.info("[商品发布消费] 图片落库完成 productId={} count={}", productId, sortOrder);
    }
}
