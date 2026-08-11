package org.lin.campusitem.kafka.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

/**
 * 商品发布事件负载。
 * <p>
 * 当商品主信息同步落库成功后，由 {@code ProductServiceImpl} 构造本对象投递到 Kafka，
 * 消费者据此异步处理：图片记录落 Image 表、向粉丝推送通知。
 * <p>
 * 设计原则：只携带消费者所需的必要字段，避免传输完整实体造成耦合。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPublishPayload implements Serializable {

    /** 商品 ID（已落库，消费者据此关联图片） */
    private Long productId;

    /** 发布者用户 ID（用于查询粉丝列表） */
    private Long publishUserId;

    /** 商品标题（用于通知内容渲染） */
    private String title;

    /** 商品价格（用于通知内容渲染） */
    private Double price;

    /** 图片 URL 列表（publishWithImages 已上传到 OSS，此处异步落 Image 表） */
    private List<String> imageUrls;

    /** 是否包含图片（用于消费者判断是否需要处理图片） */
    private boolean hasImages;
}
