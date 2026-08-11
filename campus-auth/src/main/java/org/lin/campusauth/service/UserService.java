package org.lin.campusauth.service;

import org.lin.common.context.UserInfo;
import org.lin.common.entity.User;

public interface UserService {
    String sendCode(String phone);
    boolean verifyCode(String phone, String code);
    default boolean isCodeInvalid(String phone, String code) {
        return !verifyCode(phone, code);
    }
    UserInfo verifyPhonePassword(String phone, String password);
    UserInfo register(String phone, String code, String password, String nickname);
    UserInfo loginByCode(String phone, String code);
    void resetPassword(String phone, String code, String password);
    User getUserById(Long userId);

    /**
     * 由 campus-user 调用，同步头像/昵称/邮箱到 campus_auth 库
     */
    void syncUserProfile(Long userId, String avatar, String nickname, String email);

    long countAll();

    long countTodayUsers();
}