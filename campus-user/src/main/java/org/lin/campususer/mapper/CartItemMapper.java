package org.lin.campususer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.lin.common.entity.CartItem;

import java.util.List;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {

    @Select("SELECT * FROM idle_cart_item WHERE cart_id = #{cartId}")
    List<CartItem> findItemsByCartId(@Param("cartId") Long cartId);

    @Select("SELECT * FROM idle_cart_item WHERE cart_id = #{cartId} AND product_id = #{productId}")
    CartItem findByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);

    /**
     * 原子累加购物车商品数量（上限 99）。
     * <p>
     * 直接在数据库层执行 quantity = quantity + delta，避免「读-改-写」竞态下并发添加
     * 互相覆盖（如两个并发请求都读到 quantity=2，写回 3，丢失一次 +1）。
     * 返回受影响行数：0 表示该商品不在购物车中。
     */
    @Update("UPDATE idle_cart_item SET quantity = LEAST(quantity + #{delta}, 99), update_time = NOW() " +
            "WHERE cart_id = #{cartId} AND product_id = #{productId}")
    int incrementQuantity(@Param("cartId") Long cartId, @Param("productId") Long productId, @Param("delta") Integer delta);

    @Delete("DELETE FROM idle_cart_item WHERE item_id = #{itemId}")
    void deleteByItemId(@Param("itemId") Long itemId);

    @Delete("DELETE FROM idle_cart_item WHERE cart_id = #{cartId}")
    void deleteAllItemsByCartId(@Param("cartId") Long cartId);
}
