package org.lin.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_favorite")
public class Favorite {
    @TableId(type = IdType.AUTO)
    private Long favoriteId;
    private Long userId;
    private Long productId;
    private Date createTime;
    @TableLogic
    private Integer isDeleted;
}
