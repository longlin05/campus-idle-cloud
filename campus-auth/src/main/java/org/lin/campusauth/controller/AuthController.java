package org.lin.campusauth.controller;

import jakarta.validation.Valid;
import org.lin.common.dto.LoginRequest;
import org.lin.common.dto.RegisterRequest;
import org.lin.common.dto.SendCodeRequest;
import org.lin.common.dto.ResetPasswordRequest;
import org.lin.common.context.LoginResult;
import org.lin.common.context.UserInfo;
import org.lin.common.result.Result;
import org.lin.common.jwt.JwtUtils;
import org.lin.common.entity.User;
import org.lin.campusauth.dto.SyncProfileRequest;
import org.lin.campusauth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    public Result<LoginResult> register(@Valid @RequestBody RegisterRequest request) {
        UserInfo user = userService.register(request.getPhone(), request.getCode(),
                                              request.getPassword(), request.getNickname());

        String token = generateToken(user);

        LoginResult result = new LoginResult();
        result.setUser(user);
        result.setToken(token);

        return Result.success(result);
    }

    @PostMapping("/login/password")
    public Result<LoginResult> loginByPassword(@Valid @RequestBody LoginRequest request) {
        return doLogin(request.getPhone(), request.getPassword());
    }

    @PostMapping("/login/password/query")
    public Result<LoginResult> loginByPasswordQuery(
            @RequestParam String phone,
            @RequestParam String password) {
        return doLogin(phone, password);
    }

    private Result<LoginResult> doLogin(String phone, String password) {
        UserInfo user = userService.verifyPhonePassword(phone, password);
        if (user == null) {
            return Result.businessError("手机号或密码错误");
        }

        String token = generateToken(user);

        LoginResult result = new LoginResult();
        result.setUser(user);
        result.setToken(token);

        return Result.success(result);
    }

    @PostMapping("/login/sms")
    public Result<LoginResult> loginByCode(@Valid @RequestBody LoginRequest request) {
        UserInfo user = userService.loginByCode(request.getPhone(), request.getCode());
        if (user == null) {
            return Result.businessError("手机号或验证码错误");
        }

        String token = generateToken(user);

        LoginResult result = new LoginResult();
        result.setUser(user);
        result.setToken(token);

        return Result.success(result);
    }

    @PostMapping("/send-code")
    public Result<String> sendCode(@Valid @RequestBody SendCodeRequest request) {
        userService.sendCode(request.getPhone());
        return Result.success("验证码发送成功");
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getPhone(), request.getCode(), request.getPassword());
        return Result.success();
    }

    @GetMapping("/user/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 内部同步接口：由 campus-user 在更新用户资料后调用，
     * 将 avatar/nickname/email 同步回 campus_auth 库，保证登录时能拿到最新头像。
     */
    @PostMapping("/internal/sync-profile")
    public Result<Void> syncUserProfile(@RequestBody SyncProfileRequest request) {
        userService.syncUserProfile(request.getUserId(), request.getAvatar(),
                request.getNickname(), request.getEmail());
        return Result.success();
    }

    /**
     * 内部统计接口（供 campus-admin 调用）
     */
    @GetMapping("/internal/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userService.countAll());
        stats.put("todayUserCount", userService.countTodayUsers());
        return Result.success(stats);
    }

    private String generateToken(UserInfo user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("phone", user.getPhone());
        claims.put("nickname", user.getNickname());
        claims.put("avatar", user.getAvatar());
        claims.put("role", user.getRole());
        claims.put("status", user.getStatus());
        return jwtUtils.generateToken(String.valueOf(user.getId()), claims);
    }
}