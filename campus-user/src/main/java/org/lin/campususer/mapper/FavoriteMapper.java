package org.lin.campususer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.lin.common.entity.Favorite;

import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
    @Select("SELECT * FROM sys_favorite WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Favorite> findByUserId(Long userId);

    @Select("SELECT * FROM sys_favorite WHERE user_id = #{userId} AND product_id = #{productId} AND is_deleted = 0")
    Favorite findByUserIdAndProductId(Long userId, Long productId);

    @Select("SELECT * FROM sys_favorite WHERE user_id = #{userId} AND product_id = #{productId}")
    Favorite findByUserIdAndProductIdIncludingDeleted(@Param("userId") Long userId, @Param("productId") Long productId);

    @Update("UPDATE sys_favorite SET is_deleted = 0, create_time = #{createTime} WHERE favorite_id = #{favoriteId}")
    int restoreById(@Param("favoriteId") Long favoriteId, @Param("createTime") java.util.Date createTime);
}