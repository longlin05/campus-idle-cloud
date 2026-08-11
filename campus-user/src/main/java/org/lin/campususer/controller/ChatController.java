package org.lin.campususer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.lin.common.context.ConversationVO;
import org.lin.common.context.PageResult;
import org.lin.common.dto.MarkReadRequest;
import org.lin.common.dto.SendChatRequest;
import org.lin.common.jwt.JwtAuth;
import org.lin.common.kafka.KafkaProducerService;
import org.lin.common.kafka.KafkaTopicConstants;
import org.lin.common.result.Result;
import org.lin.common.threadlocal.UserThreadLocal;
import org.lin.common.entity.Notification;
import org.lin.common.entity.User;
import org.lin.common.enums.NotificationType;
import org.lin.campususer.kafka.dto.ChatMessagePayload;
import org.lin.campususer.mapper.NotificationMapper;
import org.lin.campususer.mapper.UserMapper;
import org.lin.campususer.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/chat")
public class ChatController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @JwtAuth
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getConversations() {
        Long userId = UserThreadLocal.get().getId();
        List<Long> partnerIds = notificationMapper.findChatPartnerIds(userId);
        List<ConversationVO> list = new ArrayList<>();
        if (partnerIds.isEmpty()) {
            return Result.success(list);
        }

        // 批量查询用户、最后一条消息、未读数，消除逐会话的 3N 条 N+1 查询
        Map<Long, User> userMap = new HashMap<>();
        for (User u : userMapper.selectBatchIds(partnerIds)) {
            userMap.put(u.getUserId(), u);
        }
        Map<Long, Notification> lastMsgMap = new HashMap<>();
        for (Notification n : notificationMapper.findLastChatMessageOfEachPartner(userId)) {
            Long partnerId = n.getSenderId().equals(userId) ? n.getReceiverId() : n.getSenderId();
            lastMsgMap.put(partnerId, n);
        }
        Map<Long, Integer> unreadMap = new HashMap<>();
        for (Map<String, Object> row : notificationMapper.countUnreadChatFromUsers(userId)) {
            Number pid = (Number) row.get("partner_id");
            Number cnt = (Number) row.get("cnt");
            if (pid != null && cnt != null) {
                unreadMap.put(pid.longValue(), cnt.intValue());
            }
        }

        for (Long partnerId : partnerIds) {
            ConversationVO vo = new ConversationVO();
            vo.setUserId(partnerId);
            User partner = userMap.get(partnerId);
            if (partner != null) {
                vo.setNickname(partner.getNickname());
                vo.setAvatar(partner.getAvatar());
            }
            Notification last = lastMsgMap.get(partnerId);
            if (last != null) {
                vo.setLastMessage(last.getContent());
                vo.setLastMessageTime(last.getCreateTime());
            }
            vo.setUnreadCount(unreadMap.getOrDefault(partnerId, 0));
            list.add(vo);
        }
        list.sort((a, b) -> {
            if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) {
                return 0;
            }
            if (a.getLastMessageTime() == null) {
                return 1;
            }
            if (b.getLastMessageTime() == null) {
                return -1;
            }
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });
        return Result.success(list);
    }

    @JwtAuth
    @GetMapping("/messages")
    public Result<PageResult<Notification>> getMessages(
            @RequestParam Long userId,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size) {
        Long myId = UserThreadLocal.get().getId();
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getType, NotificationType.CHAT.getCode())
                .eq(Notification::getIsDeleted, 0)
                .and(w -> w.and(ww -> ww.eq(Notification::getSenderId, myId)
                        .eq(Notification::getReceiverId, userId))
                        .or(ww -> ww.eq(Notification::getSenderId, userId)
                                .eq(Notification::getReceiverId, myId)));
        if (productId != null && productId > 0) {
            wrapper.eq(Notification::getProductId, productId);
        }
        wrapper.orderByDesc(Notification::getCreateTime);
        Page<Notification> page = notificationMapper.selectPage(new Page<>(current, size), wrapper);
        List<Notification> records = page.getRecords();
        Collections.reverse(records);
        PageResult<Notification> pageResult = new PageResult<>();
        pageResult.setRecords(records);
        pageResult.setTotal(page.getTotal());
        pageResult.setCurrent(page.getCurrent());
        pageResult.setSize(page.getSize());
        pageResult.setPages(page.getPages());
        return Result.success(pageResult);
    }

    @JwtAuth
    @PostMapping("/send")
    public Result<?> sendMessage(@RequestBody SendChatRequest req) {
        Long senderId = UserThreadLocal.get().getId();
        Long receiverId = req.getReceiverId();
        String content = req.getContent();
        Long productId = req.getProductId();
        if (senderId == null || receiverId == null || content == null || content.trim().isEmpty()) {
            return Result.businessError("发送者、接收者和内容不能为空");
        }
        if (senderId.equals(receiverId)) {
            return Result.businessError("不能给自己发送消息");
        }
        Notification n = new Notification();
        n.setSenderId(senderId);
        n.setReceiverId(receiverId);
        n.setContent(content.trim());
        n.setTitle("私信");
        n.setType(NotificationType.CHAT.getCode());
        if (productId != null && productId > 0) {
            n.setProductId(productId);
        } else {
            n.setProductId(null);
        }
        n.setIsRead(0);
        n.setIsDeleted(0);
        Date now = new Date();
        n.setCreateTime(now);
        n.setUpdateTime(now);
        // 聊天消息同步落库：保证实时性与不丢消息（发完即可查）
        notificationMapper.insert(n);

        // 投递 Kafka 旁路事件：供未来扩展（WebSocket 实时推送 / 消息审计 / 敏感词检测）
        // 当前 ChatMessageConsumer 仅记录日志，不影响主流程
        ChatMessagePayload payload = ChatMessagePayload.builder()
                .notificationId(n.getNotificationId())
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content.trim())
                .productId(n.getProductId())
                .createTime(now)
                .build();
        kafkaProducerService.sendAsync(KafkaTopicConstants.CHAT_MESSAGE, "chat", payload);

        Map<String, Object> r = new HashMap<>();
        r.put("notificationId", n.getNotificationId());
        r.put("createTime", now);
        r.put("productId", n.getProductId());
        return Result.success(r);
    }

    @JwtAuth
    @GetMapping("/unread")
    public Result<Integer> getUnreadCount(@RequestParam(required = false) Long userId) {
        Long myId = UserThreadLocal.get().getId();
        // 统计当前用户收到的全部未读聊天消息（receiver=当前用户，任意 sender）。
        // 注意：前端传的 userId 是当前用户自己的 id，不能当作对方 sender 去查，
        // 否则 countUnreadChatFromUser(myId, myId) 会永远返回 0。
        Integer c = notificationMapper.countUnreadByType(myId, NotificationType.CHAT.getCode());
        return Result.success(c != null ? c : 0);
    }

    @JwtAuth
    @PostMapping("/mark-read")
    public Result<?> markAsRead(@RequestBody MarkReadRequest req) {
        Long myId = UserThreadLocal.get().getId();
        Long otherId = req.getUserId();
        int row = notificationMapper.markChatAsRead(myId, otherId);
        return Result.success("已更新" + row + "条");
    }

    @JwtAuth
    @GetMapping("/open/{userId}")
    public Result<Map<String, Object>> openChat(@PathVariable Long userId) {
        Long myId = UserThreadLocal.get().getId();
        Map<String, Object> res = new HashMap<>();
        User u = userMapper.findByUserId(userId);
        if (u == null) {
            return Result.error(404, "对方不存在");
        }
        Map<String, Object> ui = new HashMap<>();
        ui.put("userId", u.getUserId());
        ui.put("nickname", u.getNickname());
        ui.put("avatar", u.getAvatar());
        res.put("otherUser", ui);
        res.put("messages", notificationMapper.findChatMessages(myId, userId));
        Integer un = notificationMapper.countUnreadChatFromUser(myId, userId);
        res.put("unreadCount", un != null ? un : 0);
        if (un != null && un > 0) {
            notificationMapper.markChatAsRead(myId, userId);
        }
        return Result.success(res);
    }
}
