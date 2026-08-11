package org.lin.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("idle_category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Integer categoryId;
    private String categoryName;
    private String categoryDesc;
    private Integer sortOrder;
    private Integer status;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDeleted;
}
