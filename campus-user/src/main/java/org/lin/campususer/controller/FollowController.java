package org.lin.campususer.controller;

import org.lin.common.jwt.JwtAuth;
import org.lin.common.result.Result;
import org.lin.common.threadlocal.UserThreadLocal;
import org.lin.common.entity.Follow;
import org.lin.campususer.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/follow")
public class FollowController {

    @Autowired
    private FollowService followService;

    @JwtAuth
    @GetMapping
    public Result<List<Follow>> getFollows() {
        Long userId = UserThreadLocal.get().getId();
        return Result.success(followService.getFollowsByUserId(userId));
    }

    @JwtAuth
    @PostMapping("/{followUserId}")
    public Result<Void> follow(@PathVariable Long followUserId) {
        Long userId = UserThreadLocal.get().getId();
        followService.follow(userId, followUserId);
        return Result.success();
    }

    @JwtAuth
    @DeleteMapping("/{followUserId}")
    public Result<Void> unfollow(@PathVariable Long followUserId) {
        Long userId = UserThreadLocal.get().getId();
        followService.unfollow(userId, followUserId);
        return Result.success();
    }

    @JwtAuth
    @GetMapping("/{followUserId}/check")
    public Result<Boolean> checkFollow(@PathVariable Long followUserId) {
        Long userId = UserThreadLocal.get().getId();
        return Result.success(followService.isFollowing(userId, followUserId));
    }
}