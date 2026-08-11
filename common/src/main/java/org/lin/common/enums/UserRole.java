package org.lin.common.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {
    ADMIN(0, "管理员"),
    USER(1, "普通用户");

    private final int code;
    private final String name;

    UserRole(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        if (code == null) return "未知";
        for (UserRole role : values()) {
            if (role.code == code) {
                return role.name;
            }
        }
        return "未知";
    }

    public boolean matches(Integer code) {
        return code != null && code == this.code;
    }

    public static boolean isAdmin(Integer code) {
        return ADMIN.matches(code);
    }
}
