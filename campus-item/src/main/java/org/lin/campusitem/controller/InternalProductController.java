package org.lin.campusitem.controller;

import org.lin.common.context.PageResult;
import org.lin.common.context.ProductInfo;
import org.lin.common.result.Result;
import org.lin.common.entity.Product;
import org.lin.campusitem.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品内部接口（供其他微服务调用，不经过网关JWT鉴权）。
 * 路径前缀 /item/internal/，网关已配置白名单放行。
 */
@RestController
@RequestMapping("/item/internal")
public class InternalProductController {

    @Autowired
    private ProductService productService;

    /**
     * 获取商品信息（供订单服务下单时校验使用）。
     * 内部接口不查卖家信息，避免嵌套跨服务调用。
     */
    @GetMapping("/product/{productId}")
    public Result<ProductInfo> getProduct(@PathVariable Long productId) {
        return productService.getProductForInternal(productId);
    }

    /**
     * 扣减库存（下单成功后调用）。
     */
    @PutMapping("/product/{productId}/stock/reduce")
    public Result<?> reduceStock(@PathVariable Long productId, @RequestParam Integer quantity) {
        int affected = productService.reduceStock(productId, quantity);
        if (affected == 0) {
            return Result.error(400, "库存不足或商品不存在");
        }
        return Result.success("库存扣减成功");
    }

    /**
     * 恢复库存（订单取消/退款时调用）。
     */
    @PutMapping("/product/{productId}/stock/restore")
    public Result<?> restoreStock(@PathVariable Long productId, @RequestParam Integer quantity) {
        int affected = productService.restoreStock(productId, quantity);
        if (affected == 0) {
            return Result.error(400, "恢复库存失败");
        }
        return Result.success("库存恢复成功");
    }

    /**
     * 批量获取商品简要信息（供订单列表展示使用）。
     * 使用批量查询，消除 N+1 问题。
     */
    @GetMapping("/products")
    public Result<Map<Long, ProductInfo>> getProductsByIds(@RequestParam String ids) {
        Map<Long, ProductInfo> result = new HashMap<>();
        try {
            List<Long> idList = new java.util.ArrayList<>();
            for (String idStr : ids.split(",")) {
                idList.add(Long.parseLong(idStr.trim()));
            }
            Result<List<ProductInfo>> r = productService.getProductsByIds(idList);
            if (r.getCode() == 200 && r.getData() != null) {
                for (ProductInfo info : r.getData()) {
                    result.put(info.getId(), info);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return Result.success(result);
    }

    /**
     * 管理员获取商品列表（供 campus-admin 服务调用）。
     */
    @GetMapping("/list")
    public Result<PageResult<ProductInfo>> getProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        return productService.adminGetProductList(keyword, current, size, status);
    }
}
