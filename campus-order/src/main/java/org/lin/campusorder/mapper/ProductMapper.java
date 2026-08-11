package org.lin.campusorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.lin.common.entity.Product;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT * FROM idle_product WHERE product_id = #{productId} AND status = 1 AND is_deleted = 0")
    Product findAvailableProduct(Long productId);

    @Update("UPDATE idle_product SET quantity = quantity - #{quantity}, status = CASE WHEN quantity - #{quantity} <= 0 THEN 0 ELSE status END WHERE product_id = #{productId} AND quantity >= #{quantity} AND is_deleted = 0")
    int reduceStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE idle_product SET quantity = quantity + #{quantity} WHERE product_id = #{productId} AND is_deleted = 0")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}