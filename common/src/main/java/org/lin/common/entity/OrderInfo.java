package org.lin.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("order_info")
public class OrderInfo {
    @TableId(type = IdType.AUTO)
    private Long orderId;
    private String orderNo;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    private Long buyerId;
    private Long sellerId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal orderAmount;
    private Integer quantity;
    private Integer status;
    private Date payTime;
    private Date shipTime;
    private Date confirmTime;
    private Date cancelTime;
    private String remark;
    private Date createTime;
    private Date updateTime;
    @TableLogic
    private Integer isDeleted;
}
