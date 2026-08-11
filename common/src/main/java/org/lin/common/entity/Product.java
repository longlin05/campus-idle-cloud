package org.lin.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("idle_product")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    @TableId(type = IdType.AUTO)
    private Long productId;
    private Long publishUserId;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer quantity;
    private Integer tradeType;
    private Integer categoryId;
    private Integer status;
    private Integer viewCount;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDeleted;
}
