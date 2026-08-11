package org.lin.campususer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.lin.common.entity.Address;

import java.util.List;

@Mapper
public interface AddressMapper extends BaseMapper<Address> {
    @Select("SELECT * FROM sys_address WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY is_default DESC, create_time DESC")
    List<Address> findByUserId(Long userId);

    @Select("SELECT * FROM sys_address WHERE user_id = #{userId} AND is_default = 1 AND is_deleted = 0")
    Address findDefaultByUserId(Long userId);
}