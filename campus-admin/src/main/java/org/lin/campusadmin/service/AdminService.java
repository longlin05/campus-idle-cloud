package org.lin.campusadmin.service;

import org.lin.common.context.AdminProductVO;
import org.lin.common.context.PageResult;
import org.lin.common.result.Result;
import org.lin.common.entity.Category;
import org.lin.common.entity.Product;
import org.lin.common.entity.User;

import java.util.List;
import java.util.Map;

public interface AdminService {
    Result<List<Category>> getCategoryList();
    Result<PageResult<User>> getUserList(String keyword, Long current, Long size);
    Result<?> updateUserStatus(Long userId, Integer status);
    Result<?> updateProduct(Long productId, Product product);
    Result<?> updateCategory(Integer categoryId, Category category);
    Result<?> addCategory(Category category);
    Result<?> deleteCategory(Integer categoryId);
    Result<Map<String, Object>> getStats();
    Result<?> updateUserRole(Long userId, Integer role);
    Result<PageResult<AdminProductVO>> getProductList(String keyword, Long current, Long size, Integer status);
    Result<?> deleteProduct(Long productId);
}