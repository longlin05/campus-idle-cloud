package org.lin.common.enums;

import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
public enum NotificationType {
    ORDER(1, "订单通知"),
    SYSTEM(2, "系统通知"),
    CHAT(3, "聊天消息");

    private final int code;
    private final String name;

    NotificationType(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        if (code == null) return "未知";
        for (NotificationType type : values()) {
            if (type.code == code) {
                return type.name;
            }
        }
        return "未知";
    }

    public boolean matches(Integer code) {
        return code != null && code == this.code;
    }
}
