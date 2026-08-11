package org.lin.campusadmin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.lin.common.entity.Image;

import java.util.List;

@Mapper
public interface ImageMapper extends BaseMapper<Image> {
    @Select("SELECT * FROM idle_image WHERE type = #{type} AND relation_id = #{relationId} ORDER BY sort_order ASC LIMIT 1")
    Image findFirstByTypeAndRelationId(Integer type, Long relationId);

    @Delete("DELETE FROM idle_image WHERE type = #{type} AND relation_id = #{relationId}")
    void deleteByTypeAndRelationId(Integer type, Long relationId);
}