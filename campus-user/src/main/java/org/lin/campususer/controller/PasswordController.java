package org.lin.campususer.controller;

import org.lin.common.dto.ChangePasswordRequest;
import org.lin.common.jwt.JwtAuth;
import org.lin.common.jwt.JwtUtils;
import org.lin.common.result.Result;
import org.lin.common.threadlocal.UserThreadLocal;
import org.lin.common.util.Md5Util;
import org.lin.common.util.RedisUtils;
import org.lin.common.entity.User;
import org.lin.campususer.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class PasswordController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisUtils redisUtils;

    @JwtAuth
    @PutMapping("/password")
    public Result<?> changePassword(@RequestBody ChangePasswordRequest req) {
        Long userId = UserThreadLocal.get().getId();
        User user = userMapper.findByUserId(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        if (req.getOldPassword() == null || req.getNewPassword() == null) {
            return Result.businessError("密码不能为空");
        }
        if (!Md5Util.verify(req.getOldPassword(), user.getPassword())) {
            return Result.businessError("原密码错误");
        }
        if (req.getNewPassword().length() < 6 || req.getNewPassword().length() > 20) {
            return Result.businessError("新密码长度需6-20位");
        }
        user.setPassword(Md5Util.encrypt(req.getNewPassword()));
        user.setUpdateTime(new java.util.Date());
        userMapper.updateById(user);
        return Result.success("修改成功");
    }
}
