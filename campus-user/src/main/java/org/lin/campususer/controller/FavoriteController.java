package org.lin.campususer.controller;

import org.lin.common.jwt.JwtAuth;
import org.lin.common.result.Result;
import org.lin.common.threadlocal.UserThreadLocal;
import org.lin.common.entity.Favorite;
import org.lin.campususer.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @JwtAuth
    @GetMapping
    public Result<List<Favorite>> getFavorites() {
        Long userId = UserThreadLocal.get().getId();
        return Result.success(favoriteService.getFavoritesByUserId(userId));
    }

    @JwtAuth
    @PostMapping("/{productId}")
    public Result<Void> addFavorite(@PathVariable Long productId) {
        Long userId = UserThreadLocal.get().getId();
        favoriteService.addFavorite(userId, productId);
        return Result.success();
    }

    @JwtAuth
    @DeleteMapping("/{productId}")
    public Result<Void> removeFavorite(@PathVariable Long productId) {
        Long userId = UserThreadLocal.get().getId();
        favoriteService.removeFavorite(userId, productId);
        return Result.success();
    }

    @JwtAuth
    @GetMapping("/{productId}/check")
    public Result<Boolean> checkFavorite(@PathVariable Long productId) {
        Long userId = UserThreadLocal.get().getId();
        return Result.success(favoriteService.isFavorite(userId, productId));
    }
}