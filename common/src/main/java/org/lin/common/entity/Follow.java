package org.lin.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_follow")
public class Follow {
    @TableId(type = IdType.AUTO)
    private Long followId;
    private Long userId;
    private Long followUserId;
    private Date createTime;
    @TableLogic
    private Integer isDeleted;
}
