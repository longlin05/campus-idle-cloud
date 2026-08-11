package org.lin.campususer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.lin.common.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT * FROM sys_user WHERE phone=#{phone} AND is_deleted = 0")
    User findByPhone(String phone);

    @Select("SELECT * FROM sys_user WHERE user_id=#{userId} AND is_deleted = 0")
    User findByUserId(Long userId);

    @Update("UPDATE sys_user SET avatar = #{avatar}, update_time = NOW() WHERE user_id = #{userId}")
    int updateAvatar(@Param("userId") Long userId, @Param("avatar") String avatar);
}