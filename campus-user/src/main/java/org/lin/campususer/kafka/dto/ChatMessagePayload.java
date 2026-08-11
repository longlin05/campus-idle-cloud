package org.lin.campususer.kafka.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天消息旁路事件负载。
 * <p>
 * 聊天消息采用"同步落库 + Kafka 旁路"策略：
 * ChatController.sendMessage 同步写入 user_notification 表（保证实时性与不丢消息），
 * 同时投递本事件到 Kafka，供未来扩展场景消费：
 * <ul>
 *   <li>WebSocket 实时推送给在线接收方</li>
 *   <li>消息内容审计 / 敏感词检测</li>
 *   <li>消息搜索引擎索引构建</li>
 * </ul>
 * <p>
 * 当前 ChatMessageConsumer 仅记录日志，作为扩展预留点。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessagePayload implements Serializable {

    /** 消息记录 ID（同步落库后已分配，消费者可直接使用） */
    private Long notificationId;

    /** 发送者用户 ID */
    private Long senderId;

    /** 接收者用户 ID */
    private Long receiverId;

    /** 消息内容 */
    private String content;

    /** 关联商品 ID（咨询某商品时携带，可为 null） */
    private Long productId;

    /** 发送时间 */
    private Date createTime;
}
