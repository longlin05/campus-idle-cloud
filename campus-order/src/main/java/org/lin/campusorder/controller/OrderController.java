package org.lin.campusorder.controller;

import org.lin.common.jwt.JwtAuth;
import org.lin.common.result.Result;
import org.lin.common.threadlocal.UserThreadLocal;
import org.lin.campusorder.dto.CartOrderRequest;
import org.lin.campusorder.dto.DirectOrderRequest;
import org.lin.campusorder.kafka.dto.OrderStatusVO;
import org.lin.campusorder.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 获取订单列表（按 buyer 维度，支持状态筛选和分页）。
     */
    @JwtAuth
    @GetMapping
    public Result<?> getOrderList(
            @RequestParam(required = false) Integer status,
            @RequestParam Long current,
            @RequestParam Long size) {
        Long userId = UserThreadLocal.get().getId();
        if (status != null) {
            return orderService.getOrderListByUserIdAndStatusEnhanced(userId, status, current, size);
        }
        return orderService.getOrderListByUserIdEnhanced(userId, current, size);
    }

    /**
     * 直接下单（商品详情页直接购买单个商品）。
     * <p>
     * 同步落库并返回 orderId，前端直接跳订单详情页。
     */
    @JwtAuth
    @PostMapping("/direct")
    public Result<?> createOrderDirect(@RequestBody DirectOrderRequest request) {
        Long userId = UserThreadLocal.get().getId();
        return orderService.createOrderDirect(userId, request.getProductId(), request.getQuantity(),
                request.getReceiverName(), request.getReceiverPhone(), request.getReceiverAddress());
    }

    /**
     * 从购物车下单（批量购买多个商品）。
     */
    @JwtAuth
    @PostMapping("/from-cart")
    public Result<?> createOrderFromCart(@RequestBody CartOrderRequest request) {
        Long userId = UserThreadLocal.get().getId();
        return orderService.createOrderFromCart(userId, request.getProductIds(),
                request.getQuantities(),
                request.getReceiverName(), request.getReceiverPhone(), request.getReceiverAddress());
    }

    /**
     * 获取买入订单列表（买家视角）。
     */
    @JwtAuth
    @GetMapping("/buy")
    public Result<?> getBuyOrderList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        Long userId = UserThreadLocal.get().getId();
        return orderService.getBuyOrderList(userId, status, current, size);
    }

    /**
     * 获取售卖订单列表（卖家视角）。
     */
    @JwtAuth
    @GetMapping("/sell")
    public Result<?> getSellOrderList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        Long userId = UserThreadLocal.get().getId();
        return orderService.getSellOrderList(userId, status, current, size);
    }

    /**
     * 获取订单详情（增强版，含买家/卖家/商品信息）。仅买家或卖家可查看。
     */
    @JwtAuth
    @GetMapping("/{orderId}")
    public Result<?> getOrderDetail(@PathVariable Long orderId) {
        Long userId = UserThreadLocal.get().getId();
        return orderService.getOrderDetail(orderId, userId);
    }

    /**
     * 订单支付（仅买家可操作）。
     */
    @JwtAuth
    @PutMapping("/{orderId}/pay")
    public Result<?> payOrder(@PathVariable Long orderId) {
        Long userId = UserThreadLocal.get().getId();
        return orderService.payOrder(orderId, userId);
    }

    /**
     * 卖家发货。
     */
    @JwtAuth
    @PutMapping("/{orderId}/ship")
    public Result<?> shipOrder(@PathVariable Long orderId) {
        Long sellerId = UserThreadLocal.get().getId();
        return orderService.shipOrder(orderId, sellerId);
    }

    /**
     * 买家确认收货（仅买家可操作）。
     */
    @JwtAuth
    @PutMapping("/{orderId}/confirm")
    public Result<?> confirmReceipt(@PathVariable Long orderId) {
        Long userId = UserThreadLocal.get().getId();
        return orderService.confirmReceipt(orderId, userId);
    }

    /**
     * 取消订单（仅买家可操作）。
     */
    @JwtAuth
    @PutMapping("/{orderId}/cancel")
    public Result<?> cancelOrder(@PathVariable Long orderId) {
        Long userId = UserThreadLocal.get().getId();
        return orderService.cancelOrder(orderId, userId);
    }

    /**
     * 买家申请退款（仅买家可操作）。
     */
    @JwtAuth
    @PostMapping("/{orderId}/refund/apply")
    public Result<?> applyRefund(@PathVariable Long orderId) {
        Long userId = UserThreadLocal.get().getId();
        return orderService.applyRefund(orderId, userId);
    }

    /**
     * 卖家处理退款（仅卖家可操作）。
     */
    @JwtAuth
    @PostMapping("/{orderId}/refund/process")
    public Result<?> processRefund(@PathVariable Long orderId) {
        Long userId = UserThreadLocal.get().getId();
        return orderService.processRefund(orderId, userId);
    }

    /**
     * 根据订单号查询订单状态（供前端异步下单轮询使用）。
     * <p>
     * 可能返回三种状态：
     * <ul>
     *   <li>processing：订单创建中（尚未落库，继续轮询）</li>
     *   <li>success：订单创建成功，orderId 已填充</li>
     *   <li>failed：订单创建失败，reason 给出原因</li>
     * </ul>
     */
    @GetMapping("/status")
    public Result<OrderStatusVO> getOrderStatus(@RequestParam String orderNo) {
        return orderService.getOrderStatusByOrderNo(orderNo);
    }

    /**
     * 内部统计接口（供 campus-admin 调用）
     */
    @GetMapping("/internal/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("orderCount", orderService.countAll());
        stats.put("todayOrderCount", orderService.countTodayOrders());
        stats.put("todayAmount", orderService.sumTodayAmount());
        stats.put("totalAmount", orderService.sumTotalAmount());
        return Result.success(stats);
    }

    /**
     * 内部订单列表（供 campus-admin 调用，无鉴权）。
     */
    @GetMapping("/internal/list")
    public Result<?> adminGetOrderList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        return orderService.adminGetOrderList(keyword, status, current, size);
    }
}
