package org.lin.campususer.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.kafka.KafkaMessage;
import org.lin.common.kafka.KafkaTopicConstants;
import org.lin.common.kafka.consumer.AbstractKafkaConsumer;
import org.lin.common.kafka.dto.NotificationSendPayload;
import org.lin.common.util.RedisUtils;
import org.lin.common.entity.Notification;
import org.lin.campususer.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 通知发送事件消费者。
 * <p>
 * 接收 {@link NotificationSendPayload} 事件，异步落库到 user_notification 表，
 * 并在消费侧（即 user 服务自身，可访问 user Redis db）累加未读计数，
 * 保证发送方（如 campus-order）无需关心 user 服务的 Redis key 规则。
 * <p>
 * 处理流程：
 * <ol>
 *   <li>校验 payload，缺 receiverId 直接跳过</li>
 *   <li>构造 Notification 实体并 insert 到 DB</li>
 *   <li>Redis 累加总未读 + 对应类型未读计数（7 天 TTL）</li>
 * </ol>
 * 幂等保证：继承 {@link AbstractKafkaConsumer}，基于 eventId 去重。
 */
@Slf4j
@Component
public class NotificationSendConsumer extends AbstractKafkaConsumer<NotificationSendPayload> {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private RedisUtils redisUtils;

    private static final String UNREAD_COUNT_KEY = "notification:unread:count:";
    private static final String UNREAD_COUNT_TYPE_KEY = "notification:unread:count:type:";
    private static final long EXPIRE_SECONDS = 7 * 24 * 60 * 60L;

    @KafkaListener(
            topics = KafkaTopicConstants.NOTIFICATION_SEND,
            groupId = "campus-user-notification-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(KafkaMessage message) {
        super.handle(message, NotificationSendPayload.class);
    }

    @Override
    protected void doConsume(String eventId, NotificationSendPayload payload) {
        if (payload == null || payload.getReceiverId() == null) {
            log.warn("[通知发送消费] payload 为空或缺少 receiverId，跳过 eventId={}", eventId);
            return;
        }

        log.info("[通知发送消费] 开始处理 receiverId={} type={} title={}",
                payload.getReceiverId(), payload.getType(), payload.getTitle());

        Date createTime = payload.getCreateTime() != null ? payload.getCreateTime() : new Date();

        Notification notification = new Notification();
        notification.setReceiverId(payload.getReceiverId());
        notification.setSenderId(payload.getSenderId() != null ? payload.getSenderId() : 0L);
        notification.setBatchNo(payload.getBatchNo());
        notification.setTitle(payload.getTitle());
        notification.setContent(payload.getContent());
        notification.setType(payload.getType() != null ? payload.getType() : 0);
        notification.setProductId(payload.getProductId());
        notification.setIsRead(0);
        notification.setIsDeleted(0);
        notification.setCreateTime(createTime);
        notification.setUpdateTime(createTime);

        notificationMapper.insert(notification);
        log.info("[通知发送消费] 落库完成 notificationId={} receiverId={}",
                notification.getNotificationId(), notification.getReceiverId());

        // 消费侧累加未读计数：发送方（如订单服务）在独立 Redis DB，无法直接写 user 的计数
        incrUnread(payload.getReceiverId());
    }

    private void incrUnread(Long receiverId) {
        try {
            redisUtils.incrementCount(UNREAD_COUNT_KEY + receiverId);
            // 保证 TTL（incrementCount 不一定会设过期，首次设一下）
            applyTtlIfNeeded(UNREAD_COUNT_KEY + receiverId);
        } catch (Exception e) {
            log.error("[通知未读计数] 累加总数失败 receiverId={}", receiverId, e);
        }
        // 按类型未读在读取侧（NotificationServiceImpl.getUnreadCountByType）是"整包 Map 缓存"：
        // key = notification:unread:count:type:{receiverId}。新通知到达必须删除该缓存，
        // 让读取侧回源 DB 拿到最新值；不能在消费侧按类型累加独立 key（...:type:{receiverId}:{type}），
        // 那与读取侧的 Map key 不是同一个 key，会导致订单/系统未读数 30 分钟不刷新。
        try {
            redisUtils.delete(UNREAD_COUNT_TYPE_KEY + receiverId);
        } catch (Exception e) {
            log.error("[通知未读计数] 删除类型计数缓存失败 receiverId={}", receiverId, e);
        }
    }

    private void applyTtlIfNeeded(String key) {
        try {
            Long ttl = redisUtils.getExpire(key);
            if (ttl == null || ttl <= 0) {
                redisUtils.expire(key, EXPIRE_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception ignore) {
            // 单条失败不影响主流程
        }
    }
}
