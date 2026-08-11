package org.lin.campusauth.mapper;

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

    /**
     * 同步更新用户资料（由 campus-user 调用，保证两个库数据一致）
     */
    @Update("UPDATE sys_user SET avatar = #{avatar}, nickname = #{nickname}, email = #{email}, update_time = NOW() WHERE user_id = #{userId}")
    int syncProfile(@Param("userId") Long userId,
                    @Param("avatar") String avatar,
                    @Param("nickname") String nickname,
                    @Param("email") String email);

    @Select("SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0")
    long countAll();

    @Select("SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0 AND DATE(create_time) = #{date}")
    long countTodayUsers(@Param("date") String date);
}