package org.lin.campusorder.service;

import org.lin.common.context.OrderDetailVO;
import org.lin.common.context.OrderListItemVO;
import org.lin.common.context.PageResult;
import org.lin.common.result.Result;
import org.lin.common.entity.OrderInfo;
import org.lin.campusorder.kafka.dto.OrderStatusVO;

import java.util.List;

public interface OrderService {
    Result<?> createOrderDirect(Long userId, Long productId, Integer quantity, String receiverName, String receiverPhone, String receiverAddress);
    Result<?> createOrderFromCart(Long userId, List<Long> productIds, List<Integer> quantities, String receiverName, String receiverPhone, String receiverAddress);
    boolean validateProductStatus(Long productId);
    Result<OrderInfo> getOrderById(Long orderId);
    Result<OrderDetailVO> getOrderDetail(Long orderId, Long userId);
    Result<?> payOrder(Long orderId, Long userId);
    Result<?> shipOrder(Long orderId, Long sellerId);
    Result<?> confirmReceipt(Long orderId, Long userId);
    Result<?> cancelOrder(Long orderId, Long userId);
    /**
     * 系统自动取消超时未支付订单（定时任务调用，无需身份校验）。
     */
    Result<?> autoCancelOrder(Long orderId);
    Result<PageResult<OrderInfo>> getOrderListByUserId(Long userId, Long current, Long size);
    Result<PageResult<OrderInfo>> getOrderListByUserIdAndStatus(Long userId, Integer status, Long current, Long size);
    Result<PageResult<OrderListItemVO>> getOrderListByUserIdEnhanced(Long userId, Long current, Long size);
    Result<PageResult<OrderListItemVO>> getOrderListByUserIdAndStatusEnhanced(Long userId, Integer status, Long current, Long size);
    Result<PageResult<OrderListItemVO>> getBuyOrderList(Long buyerId, Integer status, Long current, Long size);
    Result<PageResult<OrderListItemVO>> getSellOrderList(Long sellerId, Integer status, Long current, Long size);

    /**
     * 管理员全量订单列表（内部接口，campus-admin 调用）。
     *
     * @param keyword 订单号 / 商品名模糊
     * @param status  订单状态筛选（可空）
     */
    Result<PageResult<OrderListItemVO>> adminGetOrderList(String keyword, Integer status, Long current, Long size);
    Result<?> applyRefund(Long orderId, Long userId);
    Result<?> processRefund(Long orderId, Long userId);

    /**
     * 查询订单创建状态（前端轮询用）。
     * <p>
     * 异步下单后，前端凭 orderNo 轮询此接口获取订单最终状态。
     *
     * @param orderNo 临时订单号
     * @return 订单状态 VO（processing/success/failed）
     */
    Result<OrderStatusVO> getOrderStatusByOrderNo(String orderNo);

    long countAll();
    long countTodayOrders();
    java.math.BigDecimal sumTodayAmount();
    java.math.BigDecimal sumTotalAmount();
}