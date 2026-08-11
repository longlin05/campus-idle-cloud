package org.lin.campususer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.lin.common.entity.Follow;

import java.util.List;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
    @Select("SELECT * FROM sys_follow WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Follow> findByUserId(Long userId);

    @Select("SELECT * FROM sys_follow WHERE user_id = #{userId} AND follow_user_id = #{followUserId} AND is_deleted = 0")
    Follow findByUserIdAndFollowUserId(Long userId, Long followUserId);

    /** 关注数：我关注了多少人 */
    @Select("SELECT COUNT(*) FROM sys_follow WHERE user_id = #{userId} AND is_deleted = 0")
    int countFollowing(Long userId);

    /** 粉丝数：有多少人关注了我 */
    @Select("SELECT COUNT(*) FROM sys_follow WHERE follow_user_id = #{userId} AND is_deleted = 0")
    int countFollowers(Long userId);
}