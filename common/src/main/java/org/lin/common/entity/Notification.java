package org.lin.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long notificationId;
    private Long receiverId;
    private Long senderId;
    /** 发送批次号：同一批"全体/批量"发送的所有行共享，用于管理端列表去重；单条发送可为空 */
    private String batchNo;
    private String title;
    private String content;
    private Integer type;
    private Long productId;
    private Integer isRead;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDeleted;

    /** 管理端列表用：该批次实际接收人数（非数据库列） */
    @TableField(exist = false)
    private Integer batchSize;
}
