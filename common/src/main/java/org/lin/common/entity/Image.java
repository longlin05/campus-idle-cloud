package org.lin.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("idle_image")
public class Image {
    public static final Integer TYPE_AVATAR = 1;
    public static final Integer TYPE_PRODUCT = 2;
    public static final Integer TYPE_SYSTEM = 3;

    @TableId(type = IdType.AUTO)
    private Long imageId;
    private Integer type;
    private Long relationId;
    private String imageUrl;
    private Integer sortOrder;
    private Date createTime;

    @TableField(exist = false)
    private String description;
}
