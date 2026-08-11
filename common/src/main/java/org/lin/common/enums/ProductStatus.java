package org.lin.common.enums;

import lombok.Getter;

/**
 * 商品状态枚举
 */
@Getter
public enum ProductStatus {
    OFF_SHELF(0, "已下架"),
    ON_SALE(1, "在售"),
    ADMIN_OFF_SHELF(2, "管理员下架");

    private final int code;
    private final String name;

    ProductStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        if (code == null) return "未知";
        for (ProductStatus status : values()) {
            if (status.code == code) {
                return status.name;
            }
        }
        return "未知";
    }

    public boolean matches(Integer code) {
        return code != null && code == this.code;
    }

    /**
     * 是否为可购买状态（仅"在售"可购买）
     */
    public static boolean isPurchasable(Integer code) {
        return ON_SALE.matches(code);
    }
}
