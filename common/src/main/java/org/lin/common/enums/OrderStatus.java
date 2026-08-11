package org.lin.common.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatus {
    PENDING_PAYMENT(0, "待付款"),
    PENDING_SHIPMENT(1, "待发货"),
    PENDING_RECEIPT(2, "待收货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    REFUNDING(5, "退款中"),
    REFUNDED(6, "已退款");

    private final int code;
    private final String name;

    OrderStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据状态码获取状态名称
     */
    public static String getNameByCode(Integer code) {
        if (code == null) return "未知";
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status.name;
            }
        }
        return "未知";
    }

    /**
     * 判断状态码是否匹配
     */
    public boolean matches(Integer code) {
        return code != null && code == this.code;
    }

    /**
     * 是否为终态（不可再变更）
     */
    public static boolean isTerminal(Integer code) {
        return COMPLETED.matches(code) || CANCELLED.matches(code) || REFUNDED.matches(code);
    }

    /**
     * 是否为已成交状态（已完成/待收货/待发货，用于统计金额）
     */
    public static boolean isPaid(Integer code) {
        return PENDING_SHIPMENT.matches(code) || PENDING_RECEIPT.matches(code) || COMPLETED.matches(code);
    }
}
