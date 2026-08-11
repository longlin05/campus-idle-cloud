package org.lin.campususer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lin.common.entity.Cart;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    @Select("SELECT * FROM idle_cart WHERE user_id = #{userId}")
    Cart findByUserId(@Param("userId") Long userId);
}
