package org.lin.campusauth.service.impl;

import org.lin.common.context.UserInfo;
import org.lin.common.exception.BusinessException;
import org.lin.common.util.CodeUtil;
import org.lin.common.util.Md5Util;
import org.lin.common.entity.User;
import org.lin.campusauth.mapper.UserMapper;
import org.lin.campusauth.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CodeUtil codeUtil;

    @Override
    public String sendCode(String phone) {
        return codeUtil.sendCode(phone);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        return codeUtil.verifyCode(phone, code);
    }

    @Override
    public UserInfo verifyPhonePassword(String phone, String password) {
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            return null;
        }
        if (Md5Util.verify(password, user.getPassword())) {
            return convertToUserInfo(user);
        }
        return null;
    }

    @Override
    public UserInfo register(String phone, String code, String password, String nickname) {
        if (this.isCodeInvalid(phone, code)) {
            throw new BusinessException("验证码错误或已过期");
        }

        User existingPhoneUser = userMapper.findByPhone(phone);
        if (existingPhoneUser != null) {
            throw new BusinessException("手机号已被注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword(Md5Util.encrypt(password));
        user.setNickname(nickname);
        user.setEmail(null);

        userMapper.insert(user);

        return convertToUserInfo(user);
    }

    @Override
    public UserInfo loginByCode(String phone, String code) {
        if (this.isCodeInvalid(phone, code)) {
            return null;
        }

        User user = userMapper.findByPhone(phone);
        if (user == null) {
            return null;
        }

        return convertToUserInfo(user);
    }

    @Override
    public void resetPassword(String phone, String code, String password) {
        if (this.isCodeInvalid(phone, code)) {
            throw new BusinessException("验证码错误或已过期");
        }

        User user = userMapper.findByPhone(phone);
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }

        user.setPassword(Md5Util.encrypt(password));
        userMapper.updateById(user);
    }

    @Override
    public User getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.findByUserId(userId);
    }

    @Override
    public void syncUserProfile(Long userId, String avatar, String nickname, String email) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        userMapper.syncProfile(userId, avatar, nickname, email);
    }

    @Override
    public long countAll() {
        return userMapper.countAll();
    }

    @Override
    public long countTodayUsers() {
        String today = java.time.LocalDate.now().toString();
        return userMapper.countTodayUsers(today);
    }

    private UserInfo convertToUserInfo(User user) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getUserId());
        userInfo.setNickname(user.getNickname());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRole(user.getRole());
        userInfo.setStatus(user.getStatus());
        userInfo.setCreateTime(user.getCreateTime());
        userInfo.setLastLoginTime(user.getLastLoginTime());
        return userInfo;
    }
}