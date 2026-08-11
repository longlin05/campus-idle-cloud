package org.lin.campususer.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.kafka.KafkaMessage;
import org.lin.common.kafka.KafkaTopicConstants;
import org.lin.common.kafka.consumer.AbstractKafkaConsumer;
import org.lin.campususer.kafka.dto.ChatMessagePayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 聊天消息旁路消费者。
 * <p>
 * 聊天消息采用"同步落库 + Kafka 旁路"策略：ChatController.sendMessage 已同步写入 DB，
 * 本消费者只处理旁路事件，当前仅记录日志，为未来扩展预留：
 * <ul>
 *   <li>WebSocket 实时推送给在线接收方（需配合 WebSocket 网关）</li>
 *   <li>消息内容审计 / 敏感词检测</li>
 *   <li>消息搜索引擎索引构建</li>
 * </ul>
 * <p>
 * 注意：本消费者不做落库（聊天消息已同步落库），仅做旁路处理。
 */
@Slf4j
@Component
public class ChatMessageConsumer extends AbstractKafkaConsumer<ChatMessagePayload> {

    @KafkaListener(
            topics = KafkaTopicConstants.CHAT_MESSAGE,
            groupId = "campus-user-chat-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(KafkaMessage message) {
        super.handle(message, ChatMessagePayload.class);
    }

    @Override
    protected void doConsume(String eventId, ChatMessagePayload payload) {
        if (payload == null) {
            log.warn("[聊天旁路消费] payload 为空，跳过 eventId={}", eventId);
            return;
        }
        // 当前仅记录日志，未来可扩展为 WebSocket 推送 / 审计 / 索引
        log.info("[聊天旁路消费] 消息已记录 notificationId={} {} -> {}",
                payload.getNotificationId(), payload.getSenderId(), payload.getReceiverId());

        // TODO 未来扩展点：
        // 1. if (webSocketSessionManager.isOnline(payload.getReceiverId())) {
        //        webSocketSessionManager.push(payload.getReceiverId(), payload);
        //    }
        // 2. sensitiveWordFilter.check(payload.getContent());
    }
}
