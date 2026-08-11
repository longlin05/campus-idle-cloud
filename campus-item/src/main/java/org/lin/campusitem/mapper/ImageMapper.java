package org.lin.campusitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lin.common.entity.Image;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ImageMapper extends BaseMapper<Image> {
    @Select("SELECT * FROM idle_image WHERE type = #{type} AND relation_id = #{relationId} ORDER BY sort_order ASC")
    List<Image> findByTypeAndRelationId(Integer type, Long relationId);

    @Select("SELECT * FROM idle_image WHERE type = #{type} ORDER BY sort_order ASC")
    List<Image> findByType(Integer type);

    @Select({
        "<script>",
        "SELECT * FROM idle_image WHERE type = #{type} AND relation_id IN",
        "<foreach collection='relationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        " ORDER BY sort_order ASC",
        "</script>"
    })
    List<Image> findByTypeAndRelationIds(@Param("type") Integer type, @Param("relationIds") Collection<Long> relationIds);

    @Delete("DELETE FROM idle_image WHERE relation_id = #{relationId}")
    void deleteByRelationId(Long relationId);

    @Delete("DELETE FROM idle_image WHERE type = #{type} AND relation_id = #{relationId}")
    void deleteByTypeAndRelationId(Integer type, Long relationId);
}