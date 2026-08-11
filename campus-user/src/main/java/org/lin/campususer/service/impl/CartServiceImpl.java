package org.lin.campususer.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.context.CartItemInfo;
import org.lin.common.result.Result;
import org.lin.common.util.RedisUtils;
import org.lin.common.entity.Cart;
import org.lin.common.entity.CartItem;
import org.lin.campususer.mapper.CartItemMapper;
import org.lin.campususer.mapper.CartMapper;
import org.lin.campususer.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private static final String CART_CACHE_KEY_PREFIX = "cart:user:";
    private static final String CART_CACHE_KEY_SUFFIX = ":items";
    private static final long CART_CACHE_EXPIRE_MINUTES = 30;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private CartItemMapper cartItemMapper;

    @Autowired
    private RedisUtils redisUtils;

    @Override
    public Result<List<CartItemInfo>> getCartList(Long userId) {
        String cacheKey = getCacheKey(userId);
        List<CartItemInfo> cachedList = (List<CartItemInfo>) redisUtils.get(cacheKey);
        if (cachedList != null) {
            return Result.success(cachedList);
        }

        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemMapper.findItemsByCartId(cart.getCartId());
        List<CartItemInfo> result = new ArrayList<>();
        for (CartItem item : items) {
            result.add(convertToCartItemInfo(item));
        }

        redisUtils.set(cacheKey, result, CART_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return Result.success(result);
    }

    @Override
    public Result<?> addToCart(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > 99) {
            return Result.businessError("商品数量必须在1-99之间");
        }

        Cart cart = getOrCreateCart(userId);
        // 优先原子累加：数据库层 quantity = quantity + delta，避免并发添加互相覆盖
        int rows = cartItemMapper.incrementQuantity(cart.getCartId(), productId, quantity);

        if (rows == 0) {
            // 购物车中尚无该商品 → 插入新行
            CartItem newItem = new CartItem();
            newItem.setCartId(cart.getCartId());
            newItem.setProductId(productId);
            newItem.setQuantity(Math.min(quantity, 99));
            newItem.setSelected(1);
            try {
                cartItemMapper.insert(newItem);
            } catch (DuplicateKeyException e) {
                // 并发下两个请求同时发现"无该商品"并插入，唯一键 uk_cart_product 保证只有一个成功，
                // 另一个转为原子累加，避免丢失一次添加
                log.info("[购物车] 并发插入冲突，转为原子累加 cartId={} productId={}", cart.getCartId(), productId);
                cartItemMapper.incrementQuantity(cart.getCartId(), productId, quantity);
            }
        }

        clearCartCache(userId);
        return Result.success();
    }

    @Override
    public Result<?> updateQuantity(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > 99) {
            return Result.businessError("商品数量必须在1-99之间");
        }

        Cart cart = cartMapper.findByUserId(userId);
        if (cart == null) {
            return Result.businessError("购物车不存在");
        }

        CartItem item = cartItemMapper.findByCartIdAndProductId(cart.getCartId(), productId);
        if (item == null) {
            return Result.businessError("商品不在购物车中");
        }

        item.setQuantity(quantity);
        cartItemMapper.updateById(item);

        clearCartCache(userId);
        return Result.success();
    }

    @Override
    public Result<?> removeItem(Long userId, Long productId) {
        Cart cart = cartMapper.findByUserId(userId);
        if (cart == null) {
            return Result.success();
        }

        CartItem item = cartItemMapper.findByCartIdAndProductId(cart.getCartId(), productId);
        if (item != null) {
            cartItemMapper.deleteByItemId(item.getItemId());
        }

        clearCartCache(userId);
        return Result.success();
    }

    @Override
    public Result<?> removeItems(Long userId, List<Long> productIds) {
        Cart cart = cartMapper.findByUserId(userId);
        if (cart == null) {
            return Result.success();
        }

        if (productIds != null) {
            for (Long productId : productIds) {
                CartItem item = cartItemMapper.findByCartIdAndProductId(cart.getCartId(), productId);
                if (item != null) {
                    cartItemMapper.deleteByItemId(item.getItemId());
                }
            }
        }

        clearCartCache(userId);
        return Result.success();
    }

    @Override
    public Result<?> clearCart(Long userId) {
        Cart cart = cartMapper.findByUserId(userId);
        if (cart != null) {
            cartItemMapper.deleteAllItemsByCartId(cart.getCartId());
        }

        clearCartCache(userId);
        return Result.success();
    }

    private Cart getOrCreateCart(Long userId) {
        Cart cart = cartMapper.findByUserId(userId);
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cartMapper.insert(cart);
        }
        return cart;
    }

    private String getCacheKey(Long userId) {
        return CART_CACHE_KEY_PREFIX + userId + CART_CACHE_KEY_SUFFIX;
    }

    private void clearCartCache(Long userId) {
        redisUtils.delete(getCacheKey(userId));
    }

    private CartItemInfo convertToCartItemInfo(CartItem item) {
        CartItemInfo info = new CartItemInfo();
        info.setItemId(item.getItemId());
        info.setProductId(item.getProductId());
        info.setName("商品#" + item.getProductId());
        info.setPrice(0.0);
        info.setQuantity(item.getQuantity());
        info.setSelected(item.getSelected() != null && item.getSelected() == 1);
        info.setImages(new ArrayList<>());
        return info;
    }
}
