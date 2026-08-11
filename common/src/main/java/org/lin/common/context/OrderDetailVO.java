package org.lin.common.context;

import lombok.Data;
import org.lin.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class OrderDetailVO {
    private Long orderId;
    private String orderNo;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal orderAmount;
    private Integer status;
    private String statusName;
    private String remark;
    private Date createTime;
    private Date payTime;
    private Date shipTime;
    private Date confirmTime;
    private Date updateTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private UserInfo buyer;
    private UserInfo seller;
    private ProductInfo product;
    private Integer quantity;

    /**
     * @deprecated 使用 {@link OrderStatus#getNameByCode(Integer)} 替代
     */
    @Deprecated
    public static String getStatusName(Integer status) {
        return OrderStatus.getNameByCode(status);
    }
}