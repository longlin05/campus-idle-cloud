package org.lin.campusitem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.lin.common.entity.Category;
import org.lin.common.result.Result;
import org.lin.campusitem.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 分类内部接口（供 campus-admin 服务调用，不经过网关JWT鉴权）。
 * <p>
 * 分类以 campus_item 库为准（前台首页 /item/categories 即读此库），
 * 管理端的增删改也走这里，保证前后台数据一致。
 * 路径前缀 /item/internal/，网关已配置白名单放行。
 */
@Slf4j
@RestController
@RequestMapping("/item/internal")
public class InternalCategoryController {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 全部分类（含禁用，按排序展示）。
     */
    @GetMapping("/categories")
    public Result<List<Category>> list() {
        List<Category> list = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getCategoryId));
        return Result.success(list);
    }

    /**
     * 新增分类。
     */
    @PostMapping("/category")
    @Transactional
    public Result<?> create(@RequestBody Category category) {
        if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            return Result.businessError("分类名称不能为空");
        }
        category.setCategoryId(null);
        if (category.getStatus() == null) category.setStatus(1);
        if (category.getSortOrder() == null) category.setSortOrder(0);
        Date now = new Date();
        category.setCreateTime(now);
        category.setUpdateTime(now);
        category.setIsDeleted(0);
        categoryMapper.insert(category);
        log.info("[分类] 新增 categoryId={} name={}", category.getCategoryId(), category.getCategoryName());
        return Result.success(category.getCategoryId());
    }

    /**
     * 修改分类。
     */
    @PutMapping("/category/{categoryId}")
    @Transactional
    public Result<?> update(@PathVariable Integer categoryId, @RequestBody Category category) {
        Category existing = categoryMapper.selectById(categoryId);
        if (existing == null) {
            return Result.error(404, "分类不存在");
        }
        if (category.getCategoryName() != null) {
            existing.setCategoryName(category.getCategoryName());
        }
        existing.setCategoryDesc(category.getCategoryDesc());
        existing.setSortOrder(category.getSortOrder());
        if (category.getStatus() != null) {
            existing.setStatus(category.getStatus());
        }
        existing.setUpdateTime(new Date());
        categoryMapper.updateById(existing);
        return Result.success();
    }

    /**
     * 删除分类（逻辑删除，@TableLogic）。
     */
    @DeleteMapping("/category/{categoryId}")
    @Transactional
    public Result<?> delete(@PathVariable Integer categoryId) {
        Category existing = categoryMapper.selectById(categoryId);
        if (existing == null) {
            return Result.error(404, "分类不存在");
        }
        categoryMapper.deleteById(categoryId);
        log.info("[分类] 删除 categoryId={} name={}", categoryId, existing.getCategoryName());
        return Result.success();
    }
}
