package org.lin.campusitem.controller;

import org.lin.common.jwt.JwtAuth;
import org.lin.common.result.Result;
import org.lin.common.threadlocal.UserThreadLocal;
import org.lin.common.entity.Category;
import org.lin.common.entity.Product;
import org.lin.campusitem.dto.ProductRequest;
import org.lin.campusitem.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/item")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/list")
    public Result<?> getProductList(@RequestParam Long current, @RequestParam Long size) {
        return productService.getProductList(current, size);
    }

    @GetMapping("/detail")
    public Result<?> getProductDetail(@RequestParam Long productId) {
        return productService.getProductDetail(productId);
    }

    @GetMapping("/categories")
    public Result<List<Category>> getCategories() {
        return productService.getCategories();
    }

    @GetMapping("/user/{userId}")
    public Result<?> getProductsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size) {
        return productService.getProductsByUserId(userId, current, size);
    }

    @GetMapping("/search")
    public Result<?> searchProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam Long current,
            @RequestParam Long size) {
        return productService.searchProducts(categoryId, keyword, current, size);
    }

    @JwtAuth
    @PostMapping("/publish")
    public Result<?> publishProduct(@RequestBody ProductRequest productRequest) {
        Long userId = UserThreadLocal.get().getId();
        productRequest.getProduct().setPublishUserId(userId);
        return productService.publishProduct(productRequest.getProduct(), productRequest.getImages());
    }

    @JwtAuth
    @PostMapping(value = "/publish/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> publishProductWithFiles(
            @RequestPart("product") Product product,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        Long userId = UserThreadLocal.get().getId();
        return productService.publishProductWithImages(product, files, userId);
    }

    @JwtAuth
    @PutMapping("/edit")
    public Result<?> editProduct(@RequestBody ProductRequest productRequest) {
        Long userId = UserThreadLocal.get().getId();
        return productService.editProduct(productRequest.getProduct(), productRequest.getImages(), userId);
    }

    @JwtAuth
    @PutMapping("/off-shelf")
    public Result<?> offShelfProduct(@RequestParam Long productId) {
        Long userId = UserThreadLocal.get().getId();
        return productService.offShelfProduct(productId, userId);
    }

    @JwtAuth
    @DeleteMapping("/delete")
    public Result<?> deleteProduct(@RequestParam Long productId) {
        Long userId = UserThreadLocal.get().getId();
        return productService.deleteProduct(productId, userId);
    }

    @JwtAuth
    @GetMapping("/my")
    public Result<?> getMyProducts(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        Long userId = UserThreadLocal.get().getId();
        return productService.getMyProducts(userId, status, current, size);
    }

    @JwtAuth
    @PutMapping("/on-shelf")
    public Result<?> onShelfProduct(@RequestParam Long productId) {
        Long userId = UserThreadLocal.get().getId();
        return productService.onShelfProduct(productId, userId);
    }

    @PostMapping("/view")
    public Result<?> incrementViewCount(@RequestParam Long productId, HttpServletRequest request) {
        // 优先使用网关传递的 X-User-Id（已登录用户），否则用客户端 IP 作为访客标识
        String userId = request.getHeader("X-User-Id");
        String visitorId = (userId != null && !userId.isEmpty()) ? userId : getClientIp(request);
        return productService.incrementViewCount(productId, visitorId);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个（即客户端真实 IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @GetMapping("/hot")
    public Result<?> getHotProducts() {
        return productService.getHotProducts();
    }

    @GetMapping("/batch")
    public Result<?> getProductsByIds(@RequestParam String ids) {
        List<Long> idList = java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
        return productService.getProductsByIds(idList);
    }

    /**
     * 内部统计接口（供 campus-admin 调用）
     */
    @GetMapping("/internal/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("productCount", productService.countAll());
        stats.put("categoryCount", productService.countCategories());
        stats.put("onSaleCount", productService.countOnSale());
        return Result.success(stats);
    }
}