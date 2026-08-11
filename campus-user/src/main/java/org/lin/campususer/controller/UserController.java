package org.lin.campususer.controller;

import org.lin.common.context.UserHomeVO;
import org.lin.common.context.UserInfo;
import org.lin.common.jwt.JwtAuth;
import org.lin.common.result.Result;
import org.lin.common.threadlocal.UserThreadLocal;
import org.lin.campususer.dto.AvatarRequest;
import org.lin.campususer.dto.UserInfoRequest;
import org.lin.campususer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @JwtAuth
    @GetMapping("/info")
    public Result<UserInfo> getUserInfo() {
        Long userId = UserThreadLocal.get().getId();
        return Result.success(userService.getUserInfoById(userId));
    }

    @GetMapping("/info/{userId}")
    public Result<UserInfo> getUserInfoById(@PathVariable Long userId) {
        return Result.success(userService.getUserInfoById(userId));
    }

    /**
     * 用户主页信息：公开接口（无需登录），
     * 登录状态下会额外返回 followedByMe（当前用户是否关注该用户）
     */
    @GetMapping("/home/{userId}")
    public Result<UserHomeVO> getUserHome(@PathVariable Long userId) {
        Long currentUserId = null;
        try {
            if (UserThreadLocal.get() != null) {
                currentUserId = UserThreadLocal.get().getId();
            }
        } catch (Exception ignored) {
            // 未登录，忽略
        }
        return Result.success(userService.getUserHome(userId, currentUserId));
    }

    @JwtAuth
    @PutMapping("/avatar")
    public Result<UserInfo> updateAvatar(@RequestBody AvatarRequest request) {
        Long userId = UserThreadLocal.get().getId();
        return Result.success(userService.updateUserAvatar(userId, request.getAvatar()));
    }

    @JwtAuth
    @PutMapping("/info")
    public Result<UserInfo> updateUserInfo(@RequestBody UserInfoRequest request) {
        Long userId = UserThreadLocal.get().getId();
        return Result.success(userService.updateUserInfo(
                userId,
                request.getNickname(),
                request.getEmail(),
                request.getAvatar()));
    }
}