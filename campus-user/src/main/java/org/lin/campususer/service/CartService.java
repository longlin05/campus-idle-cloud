package org.lin.campususer.service;

import org.lin.common.context.CartItemInfo;
import org.lin.common.result.Result;

import java.util.List;

public interface CartService {

    Result<List<CartItemInfo>> getCartList(Long userId);

    Result<?> addToCart(Long userId, Long productId, Integer quantity);

    Result<?> updateQuantity(Long userId, Long productId, Integer quantity);

    Result<?> removeItem(Long userId, Long productId);

    Result<?> removeItems(Long userId, List<Long> productIds);

    Result<?> clearCart(Long userId);
}
