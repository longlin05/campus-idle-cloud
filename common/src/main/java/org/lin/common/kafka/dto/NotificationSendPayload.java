package org.lin.common.kafka.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知发送事件负载（通用）。
 * <p>
 * 当任一服务需要发送通知（订单状态变更、商品下架提醒、关注提醒等）时，
 * 构造本对象投递到 {@link org.lin.common.kafka.KafkaTopicConstants#NOTIFICATION_SEND} Topic，
 * campus-user 服务的 NotificationSendConsumer 会异步落库并累加 Redis 未读计数。
 * <p>
 * 通知类型约定（与 campus-user 前端分类一致）：
 * <ul>
 *   <li>0 = system：系统公告/全局通知</li>
 *   <li>1 = order：订单相关（创建、支付、发货、确认、取消、退款）</li>
 *   <li>2 = interaction：互动消息（关注、点赞等）</li>
 *   <li>3 = chat：聊天私信（走 ChatMessagePayload 专用通道即可）</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSendPayload implements Serializable {

    /** 接收者用户 ID */
    private Long receiverId;

    /** 发送者用户 ID（系统通知可为 null 或 0） */
    private Long senderId;

    /** 发送批次号：同一批"全体/批量"发送共享同一值，管理端列表据此去重（可为 null） */
    private String batchNo;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知类型：0=系统 1=订单 2=互动 3=聊天 */
    private Integer type;

    /** 关联商品 ID（可为 null） */
    private Long productId;

    /** 关联订单 ID（可为 null，订单类通知填写） */
    private Long orderId;

    /** 创建时间（由发送方生成，消费者落库时使用，保证时间一致） */
    private Date createTime;
}
