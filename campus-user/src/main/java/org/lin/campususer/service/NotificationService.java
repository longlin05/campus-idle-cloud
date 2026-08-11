package org.lin.campususer.service;

import org.lin.common.context.PageResult;
import org.lin.common.result.Result;
import org.lin.common.entity.Notification;

import java.util.Map;

public interface NotificationService {

    Result<PageResult<Notification>> getNotificationList(Long receiverId, Long current, Long size);

    /**
     * 管理员端系统消息列表（senderId=0 的站方通知）。
     *
     * @param keyword 标题/内容模糊
     * @param type    通知类型（缺省只查系统通知 SYSTEM，避免混入订单通知）
     */
    Result<PageResult<Notification>> getAdminList(String keyword, Integer type, Long current, Long size);

    Result<PageResult<Notification>> getNotificationListByType(Long receiverId, Integer type, Long current, Long size);

    Result<Notification> getNotificationById(Long notificationId);

    Result<Integer> getUnreadCount(Long receiverId);

    Result<Map<String, Integer>> getUnreadCountByType(Long receiverId);

    Result<?> markAllAsRead(Long receiverId);

    Result<?> markAsReadByType(Long receiverId, Integer type);

    Result<?> deleteNotification(Long notificationId);

    /**
     * 管理员删除整批系统消息：按代表记录的批次号删除该批次全部接收行（batch_no 为 NULL 的旧数据按内容匹配），
     * 并清理受影响用户的通知未读缓存。
     */
    Result<?> deleteAdminBatch(Long notificationId);

    Result<?> sendNotification(Notification notification);
}
