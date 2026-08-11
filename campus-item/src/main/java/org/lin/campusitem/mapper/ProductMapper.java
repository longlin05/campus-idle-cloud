package org.lin.campusitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.lin.common.entity.Product;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT * FROM idle_product WHERE product_id=#{productId}")
    Product findByProductId(Long productId);

    @Select("SELECT * FROM idle_product WHERE publish_user_id=#{userId} ORDER BY create_time DESC")
    IPage<Product> selectPageByUserId(Page<Product> page, @Param("userId") Long userId);

    @Update("UPDATE idle_product SET quantity = quantity - #{quantity}, status = CASE WHEN quantity - #{quantity} <= 0 THEN 0 ELSE status END WHERE product_id = #{productId} AND quantity >= #{quantity} AND is_deleted = 0")
    int reduceStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("UPDATE idle_product SET quantity = quantity + #{quantity} WHERE product_id = #{productId} AND is_deleted = 0")
    int restoreStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Select("SELECT COUNT(*) FROM idle_product WHERE publish_user_id = #{userId} AND is_deleted = 0")
    int countByPublishUserId(@Param("userId") Long userId);

    /**
     * 批量统计多个卖家的在售商品数，供列表页一次性填充 sellerProductCount，避免逐商品 COUNT 的 N+1。
     *
     * @return 每行 publish_user_id -> 商品数
     */
    @Select({
        "<script>",
        "SELECT publish_user_id, COUNT(*) AS cnt FROM idle_product",
        "WHERE publish_user_id IN",
        "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        " AND is_deleted = 0 GROUP BY publish_user_id",
        "</script>"
    })
    List<Map<String, Object>> countGroupByPublishUserId(@Param("userIds") Collection<Long> userIds);

    @Update("UPDATE idle_product SET view_count = view_count + 1 WHERE product_id = #{productId}")
    int incrementViewCount(@Param("productId") Long productId);

    /**
     * 按指定增量累加浏览量（供 Kafka 消费者批量落库使用）。
     *
     * @param productId 商品 ID
     * @param delta     增量（必须 > 0）
     * @return 受影响行数
     */
    @Update("UPDATE idle_product SET view_count = view_count + #{delta} WHERE product_id = #{productId}")
    int incrementViewCountByDelta(@Param("productId") Long productId, @Param("delta") int delta);

    @Select("SELECT * FROM idle_product WHERE status = 1 AND is_deleted = 0 ORDER BY view_count DESC LIMIT 10")
    List<Product> findHotProducts();

    @Select("SELECT COUNT(*) FROM idle_product WHERE is_deleted = 0")
    long countAll();

    @Select("SELECT COUNT(*) FROM idle_product WHERE status = 1 AND is_deleted = 0")
    long countOnSale();
}