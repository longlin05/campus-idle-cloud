package org.lin.campususer.controller;

import org.lin.common.context.CartItemInfo;
import org.lin.common.jwt.JwtAuth;
import org.lin.common.result.Result;
import org.lin.common.threadlocal.UserThreadLocal;
import org.lin.campususer.dto.AddToCartRequest;
import org.lin.campususer.dto.BatchDeleteRequest;
import org.lin.campususer.dto.UpdateQuantityRequest;
import org.lin.campususer.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @JwtAuth
    @GetMapping("/list")
    public Result<List<CartItemInfo>> getCartList() {
        Long userId = UserThreadLocal.get().getId();
        return cartService.getCartList(userId);
    }

    @JwtAuth
    @PostMapping("/add")
    public Result<?> addToCart(@RequestBody AddToCartRequest req) {
        Long userId = UserThreadLocal.get().getId();
        return cartService.addToCart(userId, req.getProductId(), req.getQuantity());
    }

    @JwtAuth
    @PutMapping("/update")
    public Result<?> updateQuantity(@RequestBody UpdateQuantityRequest req) {
        Long userId = UserThreadLocal.get().getId();
        return cartService.updateQuantity(userId, req.getProductId(), req.getQuantity());
    }

    @JwtAuth
    @DeleteMapping("/{productId}")
    public Result<?> removeItem(@PathVariable Long productId) {
        Long userId = UserThreadLocal.get().getId();
        return cartService.removeItem(userId, productId);
    }

    @JwtAuth
    @DeleteMapping("/batch")
    public Result<?> removeItems(@RequestBody BatchDeleteRequest req) {
        Long userId = UserThreadLocal.get().getId();
        return cartService.removeItems(userId, req.getProductIds());
    }

    @JwtAuth
    @DeleteMapping("/clear")
    public Result<?> clearCart() {
        Long userId = UserThreadLocal.get().getId();
        return cartService.clearCart(userId);
    }
}
