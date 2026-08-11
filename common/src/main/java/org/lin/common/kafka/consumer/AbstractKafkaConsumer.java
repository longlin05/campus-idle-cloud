package org.lin.common.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.kafka.KafkaMessage;
import org.lin.common.kafka.idempotent.IdempotentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Kafka 消费者抽象基类。
 * <p>
 * 统一封装日志记录、幂等校验与异常处理，子类只需实现 {@link #doConsume} 业务逻辑。
 * <p>
 * 使用示例：
 * <pre>
 * &#64;Component
 * &#64;Slf4j
 * public class ProductPublishedConsumer extends AbstractKafkaConsumer&lt;ProductPublishPayload&gt; {
 *     &#64;Autowired
 *     private ImageMapper imageMapper;
 *
 *     &#64;KafkaListener(topics = KafkaTopicConstants.PRODUCT_PUBLISHED,
 *                    groupId = "campus-item-image-group",
 *                    containerFactory = "kafkaListenerContainerFactory")
 *     public void onMessage(KafkaMessage message) {
 *         super.handle(message, ProductPublishPayload.class);
 *     }
 *
 *     &#64;Override
 *     protected void doConsume(String eventId, ProductPublishPayload payload) {
 *         // 落库图片记录、发送粉丝通知等
 *     }
 * }
 * </pre>
 * <p>
 * 加 {@code @Component} 仅用于消除 IDE 对 @Autowired 字段的静态检查告警，
 * 实际参与 Spring 管理的是各业务子类。
 *
 * @param <T> 业务负载具体类型
 */
@Slf4j
@Component
public abstract class AbstractKafkaConsumer<T> {

    @Autowired
    protected IdempotentService idempotentService;

    /**
     * 统一入口：反序列化、幂等校验、分发业务处理。
     * <p>
     * 子类的 @KafkaListener 方法直接调用本方法即可。
     *
     * @param message      Kafka 原始消息（统一封装格式）
     * @param payloadClass 业务负载的具体 Class，用于 JSON 转换
     */
    @SuppressWarnings("unchecked")
    public void handle(KafkaMessage message, Class<T> payloadClass) {
        if (message == null) {
            log.warn("[Kafka消费] 收到空消息，跳过");
            return;
        }
        String eventId = message.getEventId();
        String eventType = message.getEventType();
        log.info("[Kafka消费] 开始处理 eventId={} eventType={} source={}",
                eventId, eventType, message.getSource());

        // 幂等校验：已处理过的事件直接跳过
        if (!idempotentService.tryConsume(eventId)) {
            return;
        }

        try {
            T payload = convertPayload(message.getData(), payloadClass);
            doConsume(eventId, payload);
            log.info("[Kafka消费] 处理完成 eventId={} eventType={}", eventId, eventType);
        } catch (Exception e) {
            // 业务处理失败：释放幂等锁，允许 Kafka 重试
            idempotentService.release(eventId);
            log.error("[Kafka消费] 处理失败 eventId={} eventType={}", eventId, eventType, e);
            // 抛出异常触发 Spring Kafka 的重试机制（重试耗尽后进入死信队列）
            throw new RuntimeException("Kafka 消费失败: " + eventType, e);
        }
    }

    /**
     * 子类实现的具体业务处理逻辑。
     *
     * @param eventId 事件 ID
     * @param payload 反序列化后的业务负载
     */
    protected abstract void doConsume(String eventId, T payload) throws Exception;

    /**
     * 将 Object 类型的 payload 转换为目标类型。
     * <p>
     * Kafka 反序列化后 data 通常是 LinkedHashMap，借助 Jackson 转换。
     * 子类可重写以使用自定义 ObjectMapper。
     *
     * @param data         原始 payload
     * @param payloadClass 目标类型
     * @return 转换后的对象
     */
    @SuppressWarnings("unchecked")
    protected T convertPayload(Object data, Class<T> payloadClass) {
        if (data == null) {
            return null;
        }
        if (payloadClass.isInstance(data)) {
            return (T) data;
        }
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return mapper.convertValue(data, payloadClass);
    }
}
