package org.lin.campususer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.lin.common.entity.Notification;

import java.util.List;
import java.util.Map;

/** 管理端分组列表的分组键：优先 batch_no，旧数据（NULL）按 type#title#content 归组 */
final class AdminGroupKeys {
    static final String KEY = "COALESCE(batch_no, CONCAT(type,'#',COALESCE(title,''),'#',COALESCE(content,'')))";
    private AdminGroupKeys() {}
}

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("SELECT * FROM user_notification WHERE receiver_id=#{receiverId} AND is_deleted=0 ORDER BY create_time DESC")
    List<Notification> findByReceiverId(Long receiverId);

    @Select("SELECT * FROM user_notification WHERE notification_id=#{id} AND is_deleted=0")
    Notification findByNotificationId(Long id);

    @Select("SELECT * FROM user_notification WHERE notification_id=#{id}")
    Notification findByNotificationIdIncludeDeleted(Long id);

    @Select("SELECT COUNT(*) FROM user_notification WHERE receiver_id=#{receiverId} AND is_read=0 AND is_deleted=0")
    Integer countUnread(Long receiverId);

    @Update("UPDATE user_notification SET is_read=1,update_time=now() WHERE receiver_id=#{receiverId} AND is_read=0 AND is_deleted=0")
    int batchMarkAsRead(Long receiverId);

    @Select("SELECT * FROM user_notification WHERE type=3 AND is_deleted=0 AND ((sender_id=#{userId} AND receiver_id=#{otherUserId}) OR (sender_id=#{otherUserId} AND receiver_id=#{userId})) ORDER BY create_time ASC")
    List<Notification> findChatMessages(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    @Select("SELECT DISTINCT CASE WHEN sender_id=#{userId} THEN receiver_id ELSE sender_id END AS otherUserId " +
            "FROM user_notification " +
            "WHERE type=3 AND is_deleted=0 AND (sender_id=#{userId} OR receiver_id=#{userId})")
    List<Long> findChatPartnerIds(Long userId);

    @Select("SELECT * FROM user_notification WHERE type=3 AND is_deleted=0 AND ((sender_id=#{userId} AND receiver_id=#{otherUserId}) OR (sender_id=#{otherUserId} AND receiver_id=#{userId})) ORDER BY create_time DESC LIMIT 1")
    Notification findLastChatMessage(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    /**
     * 批量查询当前用户每个会话伙伴的最后一条聊天消息（替代逐个 findLastChatMessage 的 N+1）。
     * 返回记录中 partner 通过 sender/receiver 与 userId 比较得出。
     */
    @Select("SELECT t2.* FROM user_notification t2 INNER JOIN ( " +
            "SELECT CASE WHEN sender_id=#{userId} THEN receiver_id ELSE sender_id END AS partner_id, " +
            "MAX(notification_id) AS max_id " +
            "FROM user_notification WHERE type=3 AND is_deleted=0 AND (sender_id=#{userId} OR receiver_id=#{userId}) " +
            "GROUP BY partner_id) t1 ON t2.notification_id=t1.max_id")
    List<Notification> findLastChatMessageOfEachPartner(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_notification WHERE type=3 AND is_deleted=0 AND receiver_id=#{userId} AND sender_id=#{otherUserId} AND is_read=0")
    Integer countUnreadChatFromUser(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    /**
     * 批量统计当前用户各伙伴（sender）的未读聊天数（替代逐个 countUnreadChatFromUser 的 N+1）。
     * 只统计本用户收到的私信，sender 集合必为会话伙伴的子集，无需额外 IN 过滤。
     */
    @Select("SELECT sender_id AS partner_id, COUNT(*) AS cnt FROM user_notification " +
            "WHERE type=3 AND is_deleted=0 AND receiver_id=#{userId} AND is_read=0 " +
            "GROUP BY sender_id")
    List<Map<String, Object>> countUnreadChatFromUsers(@Param("userId") Long userId);

    @Update("UPDATE user_notification SET is_read=1,update_time=NOW() WHERE type=3 AND is_deleted=0 AND receiver_id=#{userId} AND sender_id=#{otherUserId} AND is_read=0")
    int markChatAsRead(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    @Select("SELECT COUNT(*) FROM user_notification WHERE receiver_id=#{receiverId} AND type=#{type} AND is_read=0 AND is_deleted=0")
    Integer countUnreadByType(@Param("receiverId") Long receiverId, @Param("type") Integer type);

    @Update("UPDATE user_notification SET is_read=1,update_time=NOW() WHERE receiver_id=#{receiverId} AND type=#{type} AND is_read=0 AND is_deleted=0")
    int batchMarkAsReadByType(@Param("receiverId") Long receiverId, @Param("type") Integer type);

    // ===== 管理员系统消息列表（按发送批次去重，一次发送显示一条） =====

    /** 按批次统计去重后的记录数（分页总数） */
    @Select("<script>" +
            "SELECT COUNT(*) FROM (SELECT 1 FROM user_notification " +
            "WHERE sender_id=0 AND type=#{type} AND is_deleted=0 " +
            "<if test='keyword != null and keyword != \"\"'> AND (title LIKE CONCAT('%',#{keyword},'%') OR content LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "GROUP BY " + AdminGroupKeys.KEY + ") t" +
            "</script>")
    long countAdminGroups(@Param("type") Integer type, @Param("keyword") String keyword);

    /** 每个批次取一条代表记录（MAX(notification_id)），并统计该批次接收人数 batch_size */
    @Select("<script>" +
            "SELECT MAX(notification_id) AS notification_id, MAX(receiver_id) AS receiver_id, MAX(sender_id) AS sender_id, " +
            "MAX(batch_no) AS batch_no, MAX(title) AS title, MAX(content) AS content, MAX(type) AS type, " +
            "MAX(product_id) AS product_id, MAX(is_read) AS is_read, MAX(create_time) AS create_time, " +
            "MAX(update_time) AS update_time, MAX(is_deleted) AS is_deleted, COUNT(*) AS batch_size " +
            "FROM user_notification " +
            "WHERE sender_id=0 AND type=#{type} AND is_deleted=0 " +
            "<if test='keyword != null and keyword != \"\"'> AND (title LIKE CONCAT('%',#{keyword},'%') OR content LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "GROUP BY " + AdminGroupKeys.KEY + " " +
            "ORDER BY MAX(create_time) DESC LIMIT #{offset}, #{size}" +
            "</script>")
    List<Notification> selectAdminGroups(@Param("type") Integer type, @Param("keyword") String keyword,
                                         @Param("offset") long offset, @Param("size") long size);

    /** 查询某批次涉及的所有接收人（用于删除时清未读缓存） */
    @Select("SELECT DISTINCT receiver_id FROM user_notification WHERE sender_id=0 AND is_deleted=0 AND batch_no=#{batchNo}")
    List<Long> findReceiverIdsByBatchNo(@Param("batchNo") String batchNo);

    /** 查询某旧批次（batch_no 为 NULL）涉及的所有接收人 */
    @Select("SELECT DISTINCT receiver_id FROM user_notification WHERE sender_id=0 AND is_deleted=0 " +
            "AND batch_no IS NULL AND type=#{type} AND title=#{title} AND content=#{content}")
    List<Long> findReceiverIdsByTitleContent(@Param("type") Integer type, @Param("title") String title,
                                             @Param("content") String content);

    /** 管理员删除整批（按批次号逻辑删除） */
    @Update("UPDATE user_notification SET is_deleted=1, update_time=NOW() WHERE sender_id=0 AND is_deleted=0 AND batch_no=#{batchNo}")
    int deleteByBatchNo(@Param("batchNo") String batchNo);

    /** 管理员删除旧批次（batch_no 为 NULL，按内容匹配逻辑删除） */
    @Update("UPDATE user_notification SET is_deleted=1, update_time=NOW() WHERE sender_id=0 AND is_deleted=0 " +
            "AND batch_no IS NULL AND type=#{type} AND title=#{title} AND content=#{content}")
    int deleteByTitleContent(@Param("type") Integer type, @Param("title") String title, @Param("content") String content);
}
