package org.lin.common.threadlocal;

import org.lin.common.context.UserInfo;

public class UserThreadLocal {
    private static final ThreadLocal<UserInfo> userThreadLocal = new ThreadLocal<>();

    public static void set(UserInfo user) {
        userThreadLocal.set(user);
    }

    public static UserInfo get() {
        return userThreadLocal.get();
    }

    public static void remove() {
        userThreadLocal.remove();
    }
}