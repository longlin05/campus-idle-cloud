package org.lin.campusadmin.controller;

import org.lin.common.context.AdminProductVO;
import org.lin.common.jwt.JwtAuth;
import org.lin.common.result.Result;
import org.lin.common.context.PageResult;
import org.lin.common.entity.Category;
import org.lin.common.entity.Product;
import org.lin.common.entity.User;
import org.lin.campusadmin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @JwtAuth(admin = true)
    @GetMapping("/categories")
    public Result<List<Category>> getCategoryList() {
        return adminService.getCategoryList();
    }

    @JwtAuth(admin = true)
    @GetMapping("/users")
    public Result<PageResult<User>> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam Long current,
            @RequestParam Long size) {
        return adminService.getUserList(keyword, current, size);
    }

    @JwtAuth(admin = true)
    @PutMapping("/users/{userId}/status")
    public Result<?> updateUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        return adminService.updateUserStatus(userId, status);
    }

    @JwtAuth(admin = true)
    @PutMapping("/products/{productId}")
    public Result<?> updateProduct(@PathVariable Long productId, @RequestBody Product product) {
        return adminService.updateProduct(productId, product);
    }

    @JwtAuth(admin = true)
    @PutMapping("/categories/{categoryId}")
    public Result<?> updateCategory(@PathVariable Integer categoryId, @RequestBody Category category) {
        return adminService.updateCategory(categoryId, category);
    }

    @JwtAuth(admin = true)
    @PostMapping("/categories")
    public Result<?> addCategory(@RequestBody Category category) {
        return adminService.addCategory(category);
    }

    @JwtAuth(admin = true)
    @DeleteMapping("/categories/{categoryId}")
    public Result<?> deleteCategory(@PathVariable Integer categoryId) {
        return adminService.deleteCategory(categoryId);
    }

    @JwtAuth(admin = true)
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return adminService.getStats();
    }

    @JwtAuth(admin = true)
    @PutMapping("/users/{userId}/role")
    public Result<?> updateUserRole(@PathVariable Long userId, @RequestParam Integer role) {
        return adminService.updateUserRole(userId, role);
    }

    @JwtAuth(admin = true)
    @GetMapping("/products")
    public Result<PageResult<AdminProductVO>> getProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam Long current,
            @RequestParam Long size,
            @RequestParam(required = false) Integer status) {
        return adminService.getProductList(keyword, current, size, status);
    }

    @JwtAuth(admin = true)
    @DeleteMapping("/products/{productId}")
    public Result<?> deleteProduct(@PathVariable Long productId) {
        return adminService.deleteProduct(productId);
    }
}