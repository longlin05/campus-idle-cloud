package org.lin.common.threadlocal;

import org.lin.campusidle.vo.UserV0;

public class UserThreadLocal {
    private static final ThreadLocal<UserV0> userThreadLocal = new ThreadLocal<>();

    /**
     * 设置用户信息到ThreadLocal
     * @param user 用户信息
     */
    public static void set(UserV0 user) {
        userThreadLocal.set(user);
    }

    /**
     * 从ThreadLocal获取用户信息
     * @return 用户信息
     */
    public static UserV0 get() {
        return userThreadLocal.get();
    }

    /**
     * 清理ThreadLocal中的用户信息
     */
    public static void remove() {
        userThreadLocal.remove();
    }
}