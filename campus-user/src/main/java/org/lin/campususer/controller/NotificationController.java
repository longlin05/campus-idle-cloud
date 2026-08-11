package org.lin.campususer.controller;

import org.lin.common.context.PageResult;
import org.lin.common.jwt.JwtAuth;
import org.lin.common.result.Result;
import org.lin.common.entity.Notification;
import org.lin.campususer.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 管理员端系统消息列表（内部接口，senderId=0 站方通知）。
     */
    @GetMapping("/internal/admin-list")
    public Result<PageResult<Notification>> getAdminList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam Long current,
            @RequestParam Long size) {
        return notificationService.getAdminList(keyword, type, current, size);
    }

    @GetMapping("/list")
    public Result<PageResult<Notification>> getNotificationList(
            @RequestParam Long receiverId,
            @RequestParam(required = false) Integer type,
            @RequestParam Long current,
            @RequestParam Long size) {
        if (type == null) {
            return notificationService.getNotificationList(receiverId, current, size);
        } else {
            return notificationService.getNotificationListByType(receiverId, type, current, size);
        }
    }

    @GetMapping("/unread/count")
    public Result<Integer> getUnreadCount(@RequestParam Long receiverId) {
        return notificationService.getUnreadCount(receiverId);
    }

    @GetMapping("/unread/count-by-type")
    public Result<Map<String, Integer>> getUnreadCountByType(@RequestParam Long receiverId) {
        return notificationService.getUnreadCountByType(receiverId);
    }

    @GetMapping("/detail/{notificationId}")
    public Result<Notification> getNotificationById(@PathVariable Long notificationId) {
        return notificationService.getNotificationById(notificationId);
    }

    @PostMapping("/mark-all-read")
    public Result<?> markAllAsRead(@RequestParam Long receiverId) {
        return notificationService.markAllAsRead(receiverId);
    }

    @PostMapping("/mark-read-by-type")
    public Result<?> markAsReadByType(
            @RequestParam Long receiverId,
            @RequestParam(required = false) Integer type) {
        return notificationService.markAsReadByType(receiverId, type);
    }

    @DeleteMapping("/{notificationId}")
    public Result<?> deleteNotification(@PathVariable Long notificationId) {
        return notificationService.deleteNotification(notificationId);
    }

    /**
     * 管理员删除整批系统消息（内部接口）：按代表记录的批次号删除该批次全部接收行。
     */
    @DeleteMapping("/internal/admin-batch/{notificationId}")
    public Result<?> deleteAdminBatch(@PathVariable Long notificationId) {
        return notificationService.deleteAdminBatch(notificationId);
    }

    @JwtAuth
    @PostMapping("/send")
    public Result<?> sendNotification(@RequestBody Notification notification) {
        return notificationService.sendNotification(notification);
    }
}
