package org.lin.campusadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lin.common.entity.Product;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT p.* FROM idle_product p WHERE p.is_deleted = 0 AND (p.title LIKE CONCAT('%', #{keyword}, '%') OR p.description LIKE CONCAT('%', #{keyword}, '%'))")
    IPage<Product> selectPageByKeyword(Page<Product> page, @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM idle_product WHERE is_deleted = 0")
    long countAll();
}