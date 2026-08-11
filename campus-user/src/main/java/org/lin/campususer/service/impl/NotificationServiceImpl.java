package org.lin.campususer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.lin.common.context.PageResult;
import org.lin.common.kafka.KafkaProducerService;
import org.lin.common.kafka.KafkaTopicConstants;
import org.lin.common.kafka.dto.NotificationSendPayload;
import org.lin.common.result.Result;
import org.lin.common.util.RedisUtils;
import org.lin.common.entity.Notification;
import org.lin.common.enums.NotificationType;
import org.lin.campususer.mapper.NotificationMapper;
import org.lin.campususer.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    private static final String UNREAD_COUNT_KEY = "notification:unread:count:";
    private static final String UNREAD_COUNT_TYPE_KEY = "notification:unread:count:type:";
    private static final String NOTIFICATION_KEY = "notification:";
    private static final long EXPIRE_SECONDS = 7 * 24 * 60 * 60L;

    @Override
    public Result<PageResult<Notification>> getNotificationList(Long receiverId, Long current, Long size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getIsDeleted, 0)
                .ne(Notification::getType, NotificationType.CHAT.getCode())
                .orderByDesc(Notification::getCreateTime);
        Page<Notification> page = notificationMapper.selectPage(new Page<>(current, size), wrapper);
        PageResult<Notification> pageResult = new PageResult<>();
        pageResult.setRecords(page.getRecords());
        pageResult.setTotal(page.getTotal());
        pageResult.setCurrent(page.getCurrent());
        pageResult.setSize(page.getSize());
        pageResult.setPages(page.getPages());
        return Result.success(pageResult);
    }

    @Override
    public Result<PageResult<Notification>> getAdminList(String keyword, Integer type, Long current, Long size) {
        // 缺省只查系统通知，避免把订单通知（senderId=0, type=ORDER）混入管理员消息列表
        Integer filterType = type != null ? type : NotificationType.SYSTEM.getCode();
        String kw = keyword != null ? keyword.trim() : null;

        // 按发送批次去重：一次"全体/批量"发送只显示一条，并统计该批次接收人数
        long total = notificationMapper.countAdminGroups(filterType, kw);
        long offset = (current - 1) * size;
        List<Notification> records = notificationMapper.selectAdminGroups(filterType, kw, offset, size);

        PageResult<Notification> pageResult = new PageResult<>();
        pageResult.setRecords(records);
        pageResult.setTotal(total);
        pageResult.setCurrent(current);
        pageResult.setSize(size);
        pageResult.setPages(total == 0 ? 0 : (total + size - 1) / size);
        return Result.success(pageResult);
    }

    @Override
    public Result<?> deleteAdminBatch(Long notificationId) {
        Notification notification = notificationMapper.findByNotificationId(notificationId);
        if (notification == null) {
            return Result.error(404, "消息不存在");
        }
        List<Long> receiverIds;
        if (notification.getBatchNo() != null && !notification.getBatchNo().isEmpty()) {
            receiverIds = notificationMapper.findReceiverIdsByBatchNo(notification.getBatchNo());
            notificationMapper.deleteByBatchNo(notification.getBatchNo());
        } else {
            // 旧数据 batch_no 为 NULL：按内容匹配整批删除
            receiverIds = notificationMapper.findReceiverIdsByTitleContent(
                    notification.getType(), notification.getTitle(), notification.getContent());
            notificationMapper.deleteByTitleContent(
                    notification.getType(), notification.getTitle(), notification.getContent());
        }
        // 清理受影响用户的未读计数缓存，避免删除后未读数虚高
        for (Long receiverId : receiverIds) {
            if (receiverId != null) {
                deleteAllRedisCache(receiverId);
            }
        }
        log.info("[系统消息] 管理员删除整批 notificationId={} 影响接收人 {} 个", notificationId, receiverIds.size());
        return Result.success("删除成功");
    }

    @Override
    public Result<PageResult<Notification>> getNotificationListByType(Long receiverId, Integer type, Long current, Long size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, receiverId)
                .eq(Notification::getType, type)
                .eq(Notification::getIsDeleted, 0)
                .orderByDesc(Notification::getCreateTime);
        Page<Notification> page = notificationMapper.selectPage(new Page<>(current, size), wrapper);
        PageResult<Notification> pageResult = new PageResult<>();
        pageResult.setRecords(page.getRecords());
        pageResult.setTotal(page.getTotal());
        pageResult.setCurrent(page.getCurrent());
        pageResult.setSize(page.getSize());
        pageResult.setPages(page.getPages());
        return Result.success(pageResult);
    }

    @Override
    public Result<Notification> getNotificationById(Long notificationId) {
        String cacheKey = NOTIFICATION_KEY + notificationId;
        Notification notification = (Notification) redisUtils.get(cacheKey);
        if (notification == null) {
            notification = notificationMapper.findByNotificationId(notificationId);
            if (notification != null) {
                redisUtils.set(cacheKey, notification, EXPIRE_SECONDS, TimeUnit.SECONDS);
            }
        }
        if (notification == null) {
            return Result.error(404, "通知不存在");
        }
        if (notification.getIsRead() != null && notification.getIsRead() == 0) {
            notification.setIsRead(1);
            notification.setUpdateTime(new Date());
            notificationMapper.updateById(notification);
            deleteRedisCache(notification);
        }
        return Result.success(notification);
    }

    @Override
    public Result<Integer> getUnreadCount(Long receiverId) {
        String cacheKey = UNREAD_COUNT_KEY + receiverId;
        Integer count = (Integer) redisUtils.get(cacheKey);
        if (count == null) {
            count = notificationMapper.countUnread(receiverId);
            if (count == null) {
                count = 0;
            }
            redisUtils.set(cacheKey, count, 30 * 60, TimeUnit.SECONDS);
        }
        return Result.success(count);
    }

    @Override
    public Result<Map<String, Integer>> getUnreadCountByType(Long receiverId) {
        String cacheKey = UNREAD_COUNT_TYPE_KEY + receiverId;
        Map<String, Integer> result = (Map<String, Integer>) redisUtils.get(cacheKey);
        if (result == null) {
            result = new HashMap<>();
            Integer count0 = notificationMapper.countUnreadByType(receiverId, 0);
            Integer count1 = notificationMapper.countUnreadByType(receiverId, NotificationType.ORDER.getCode());
            Integer count2 = notificationMapper.countUnreadByType(receiverId, NotificationType.SYSTEM.getCode());
            result.put("0", count0 != null ? count0 : 0);
            result.put("1", count1 != null ? count1 : 0);
            result.put("2", count2 != null ? count2 : 0);
            redisUtils.set(cacheKey, result, 30 * 60, TimeUnit.SECONDS);
        }
        return Result.success(result);
    }

    @Override
    public Result<?> markAllAsRead(Long receiverId) {
        notificationMapper.batchMarkAsRead(receiverId);
        deleteAllRedisCache(receiverId);
        return Result.success("全部标记已读");
    }

    @Override
    public Result<?> markAsReadByType(Long receiverId, Integer type) {
        if (type == null) {
            return markAllAsRead(receiverId);
        }
        notificationMapper.batchMarkAsReadByType(receiverId, type);
        deleteAllRedisCache(receiverId);
        return Result.success("该类型已标记已读");
    }

    @Override
    public Result<?> deleteNotification(Long notificationId) {
        Notification notification = notificationMapper.findByNotificationIdIncludeDeleted(notificationId);
        if (notification == null) {
            return Result.error(404, "通知不存在");
        }
        notificationMapper.deleteById(notificationId);
        deleteRedisCache(notification);
        deleteAllRedisCache(notification.getReceiverId());
        return Result.success("删除成功");
    }

    @Override
    public Result<?> sendNotification(Notification notification) {
        Date now = new Date();
        if (notification.getIsRead() == null) {
            notification.setIsRead(0);
        }
        if (notification.getIsDeleted() == null) {
            notification.setIsDeleted(0);
        }
        notification.setCreateTime(now);
        notification.setUpdateTime(now);

        // 未读计数统一在消费侧（NotificationSendConsumer）累加：
        // 1. 保证跨服务发送（如 campus-order）也能正确 +1
        // 2. 避免发送方与消费方 double count
        // 查询未读数时 Redis miss 会 fallback 到 DB 统计，无需担心最终一致性

        // 投递 Kafka 事件，消费者异步落库 + 累加未读计数
        NotificationSendPayload payload = NotificationSendPayload.builder()
                .receiverId(notification.getReceiverId())
                .senderId(notification.getSenderId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .productId(notification.getProductId())
                .createTime(now)
                .build();
        kafkaProducerService.sendAsync(KafkaTopicConstants.NOTIFICATION_SEND, "send", payload);

        log.info("[通知发送] 异步投递成功 receiverId={} type={}",
                notification.getReceiverId(), notification.getType());
        return Result.success(notification);
    }

    private void deleteRedisCache(Notification notification) {
        redisUtils.delete(NOTIFICATION_KEY + notification.getNotificationId());
    }

    private void deleteAllRedisCache(Long receiverId) {
        redisUtils.delete(UNREAD_COUNT_KEY + receiverId);
        redisUtils.delete(UNREAD_COUNT_TYPE_KEY + receiverId);
    }
}
