package org.lin.common.context;

import lombok.Data;
import org.lin.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class OrderListItemVO {
    private Long orderId;
    private String orderNo;
    private BigDecimal orderAmount;
    private Integer status;
    private String statusName;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private Date createTime;
    private Date payTime;
    private Date shipTime;
    private Long buyerId;
    private Long sellerId;

    /**
     * @deprecated 使用 {@link OrderStatus#getNameByCode(Integer)} 替代
     */
    @Deprecated
    public static String getStatusName(Integer status) {
        return OrderStatus.getNameByCode(status);
    }
}