package org.lin.campusitem.service;

import org.lin.common.context.PageResult;
import org.lin.common.context.ProductInfo;
import org.lin.common.result.Result;
import org.lin.common.entity.Category;
import org.lin.common.entity.Product;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    Result<PageResult<ProductInfo>> getProductList(Long current, Long size);
    Result<ProductInfo> getProductDetail(Long productId);
    Result<ProductInfo> getProductForInternal(Long productId);
    Result<List<Category>> getCategories();
    Result<PageResult<ProductInfo>> searchProducts(Integer categoryId, String keyword, Long current, Long size);
    Result<?> publishProduct(Product product, List<String> images);
    Result<?> publishProductWithImages(Product product, List<MultipartFile> files, Long userId);
    Result<?> editProduct(Product product, List<String> images, Long userId);
    Result<?> offShelfProduct(Long productId, Long userId);
    Result<?> deleteProduct(Long productId, Long userId);
    Result<PageResult<ProductInfo>> getMyProducts(Long userId, Integer status, Long current, Long size);
    Result<?> onShelfProduct(Long productId, Long userId);
    Result<PageResult<ProductInfo>> getProductsByUserId(Long userId, Long current, Long size);
    Result<?> incrementViewCount(Long productId, String visitorId);
    Result<List<ProductInfo>> getHotProducts();
    Result<List<ProductInfo>> getProductsByIds(List<Long> ids);
    int reduceStock(Long productId, Integer quantity);
    int restoreStock(Long productId, Integer quantity);

    long countAll();
    long countCategories();
    long countOnSale();

    Result<PageResult<ProductInfo>> adminGetProductList(String keyword, Long current, Long size, Integer status);
}