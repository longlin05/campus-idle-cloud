package org.lin.campusorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.lin.common.context.OrderDetailVO;
import org.lin.common.context.OrderListItemVO;
import org.lin.common.context.PageResult;
import org.lin.common.context.ProductInfo;
import org.lin.common.context.UserInfo;
import org.lin.common.enums.NotificationType;
import org.lin.common.enums.OrderStatus;
import org.lin.common.enums.ProductStatus;
import org.lin.common.exception.BusinessException;
import org.lin.common.kafka.KafkaProducerService;
import org.lin.common.kafka.KafkaTopicConstants;
import org.lin.common.kafka.dto.NotificationSendPayload;
import org.lin.common.result.Result;
import org.lin.common.util.RedisUtils;
import org.lin.campusorder.client.ProductClient;
import org.lin.common.entity.OrderInfo;
import org.lin.common.entity.Product;
import org.lin.common.entity.User;
import org.lin.campusorder.kafka.dto.OrderStatusVO;
import org.lin.campusorder.mapper.OrderMapper;
import org.lin.campusorder.mapper.UserMapper;
import org.lin.campusorder.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private RedisUtils redisUtils;

    private static final String ORDER_STATUS_KEY = "order:status:";

    // 订单通知专用共享线程池（daemon，不阻塞 JVM 退出）：
    // Kafka 发送在 broker 不可达 / producer 缓冲满时可能阻塞（默认 max.block.ms=60s），
    // 因此通知发送放在独立线程池执行，避免阻塞下单主流程。
    // 外层扇出池：仅负责构造 payload 并提交内层发送任务，容量 2，避免单线程下慢通知排队阻塞后续订单。
    private static final ExecutorService NOTIFY_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "order-notify-thread");
        t.setDaemon(true);
        return t;
    });
    // 内层带超时发送池：真正执行 Kafka send，外层对每个发送任务等待 ≤2s 后放弃。
    // 容量 4 > 外层最大并发 2，保证内层任务总能获得线程，不会与外层互相饿死。
    private static final ExecutorService NOTIFY_SEND_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "order-notify-send");
        t.setDaemon(true);
        return t;
    });

    // ==================== 下单（同步落库，直接返回 orderId） ====================

    @Override
    @Transactional
    public Result<?> createOrderDirect(Long userId, Long productId, Integer quantity,
                                       String receiverName, String receiverPhone, String receiverAddress) {
        log.info("[直接下单] ===== 开始创建订单 buyerId={} productId={} quantity={} receiver={} phone={}",
                userId, productId, quantity, receiverName, receiverPhone);
        try {
            String orderNo = generateOrderNo();
            // 校验商品状态/卖家/库存 → 扣减库存 → 落库，与购物车下单共用同一套逻辑
            OrderInfo order = createOrderForProduct(orderNo, userId, productId, quantity,
                    receiverName, receiverPhone, receiverAddress);
            log.info("[直接下单] ===== 订单创建成功 orderId={} buyerId={} productId={} quantity={}",
                    order.getOrderId(), userId, productId, order.getQuantity());
            notifyOrderCreated(order, order.getProductName());
            return Result.success(buildCreateResult(order));
        } catch (BusinessException e) {
            log.warn("[直接下单] 业务失败 buyerId={} productId={} code={} msg={}",
                    userId, productId, e.getCode(), e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Throwable t) {
            log.error("[直接下单] ===== 创建订单发生异常 buyerId={} productId={} err={}",
                    userId, productId, t.getMessage(), t);
            // 打印根因链
            Throwable c = t.getCause();
            int depth = 0;
            while (c != null && depth < 5) {
                log.error("[直接下单] 根因链[{}]: {}", depth++, c.toString(), c);
                c = c.getCause();
            }
            // 让事务回滚
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    @Override
    @Transactional
    public Result<?> createOrderFromCart(Long userId, List<Long> productIds, List<Integer> quantities,
                                         String receiverName, String receiverPhone, String receiverAddress) {
        if (productIds == null || productIds.isEmpty()) {
            return Result.error(400, "请选择商品");
        }

        String orderNo = generateOrderNo();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderInfo> createdOrders = new ArrayList<>();
        List<Long> reducedProductIds = new ArrayList<>();
        List<Integer> reducedQuantities = new ArrayList<>();

        try {
            for (int i = 0; i < productIds.size(); i++) {
                Long productId = productIds.get(i);
                // 获取该商品的购买数量，若quantities为null或对应值为null，则默认为1
                int qty = 1;
                if (quantities != null && i < quantities.size() && quantities.get(i) != null) {
                    qty = quantities.get(i);
                }
                if (qty < 1) {
                    qty = 1;
                }
                // 单个商品统一走 createOrderForProduct：校验商品状态/卖家/库存 → 扣减库存 → 落库
                OrderInfo order = createOrderForProduct(orderNo, userId, productId, qty,
                        receiverName, receiverPhone, receiverAddress);
                // 记录本次成功扣减的库存与已创建的订单，供后续商品失败时整体回滚
                reducedProductIds.add(productId);
                reducedQuantities.add(qty);
                createdOrders.add(order);
                totalAmount = totalAmount.add(order.getOrderAmount());
            }
        } catch (BusinessException e) {
            log.warn("[购物车下单] 业务失败，回滚已扣库存 orderNo={} code={} msg={}",
                    orderNo, e.getCode(), e.getMessage());
            rollbackStock(reducedProductIds, reducedQuantities);
            for (OrderInfo o : createdOrders) {
                try {
                    orderMapper.deleteById(o.getOrderId());
                } catch (Exception ignore) {}
            }
            return Result.error(e.getCode(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("[购物车下单] 创建订单异常，已回滚库存", e);
            rollbackStock(reducedProductIds, reducedQuantities);
            for (OrderInfo o : createdOrders) {
                try {
                    orderMapper.deleteById(o.getOrderId());
                } catch (Exception ignore) {}
            }
            return Result.error(500, "创建订单失败，请稍后重试");
        }

        Long firstOrderId = createdOrders.isEmpty() ? null : createdOrders.get(0).getOrderId();

        log.info("[购物车下单] 订单创建完成 orderNo={} count={} firstOrderId={} total={}",
                orderNo, createdOrders.size(), firstOrderId, totalAmount);

        for (OrderInfo order : createdOrders) {
            notifyOrderCreated(order, order.getProductName());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", firstOrderId);
        result.put("orderNo", orderNo);
        result.put("totalAmount", totalAmount.doubleValue());
        result.put("count", createdOrders.size());
        return Result.success(result);
    }

    /**
     * 单个商品的统一购买逻辑：直接下单与购物车下单共用。
     * <p>统一在这里校验商品状态、卖家、库存，然后扣减库存并落库，保证两种购买方式的校验逻辑完全一致。
     * 校验/扣减失败抛出 {@link BusinessException}，由调用方决定回滚策略（购物车批量场景会回滚已扣的库存）。
     *
     * @param orderNo 订单号（购物车批量场景下多个订单共享同一个 orderNo）
     */
    private OrderInfo createOrderForProduct(String orderNo, Long buyerId, Long productId, Integer quantity,
                                            String receiverName, String receiverPhone, String receiverAddress) {
        int qty = quantity == null || quantity <= 0 ? 1 : quantity;

        // 1. 通过跨服务调用获取商品信息（从 campus-item 服务获取）
        ProductInfo productInfo = productClient.getProduct(productId);
        if (productInfo == null || productInfo.getStatus() == null || !ProductStatus.ON_SALE.matches(productInfo.getStatus())) {
            log.warn("[统一下单] 商品不存在或已下架 productId={} info={}", productId, productInfo);
            throw new BusinessException(404, "商品不存在或已下架");
        }
        if (productInfo.getSellerId() == null) {
            log.warn("[统一下单] 商品sellerId为空 productId={}", productId);
            throw new BusinessException(500, "商品卖家信息异常，请联系管理员");
        }
        if (productInfo.getSellerId().equals(buyerId)) {
            throw new BusinessException(400, "不能购买自己的商品");
        }
        int stock = productInfo.getStock() != null ? productInfo.getStock()
                : (productInfo.getQuantity() != null ? productInfo.getQuantity() : 0);
        if (stock < qty) {
            log.warn("[统一下单] 库存不足 productId={} 需求={} 库存={}", productId, qty, stock);
            throw new BusinessException(400, "商品「" + productInfo.getTitle() + "」库存不足，当前库存" + stock + "件");
        }

        // 2. 扣减库存（调用 campus-item 服务，DB 层 update 带 quantity >= #{quantity} 兜底）
        boolean reduced = productClient.reduceStock(productId, qty);
        if (!reduced) {
            log.warn("[统一下单] 扣减库存返回false productId={} quantity={}", productId, qty);
            throw new BusinessException(400, "库存扣减失败，请重试");
        }

        // 3. 写入订单表
        Product product = convertToProduct(productInfo);
        String productImage = productInfo.getImageUrl();
        if (productImage == null && productInfo.getImages() != null && !productInfo.getImages().isEmpty()) {
            productImage = productInfo.getImages().get(0);
        }
        OrderInfo order = buildOrder(orderNo, product, productImage, buyerId, qty,
                receiverName, receiverPhone, receiverAddress);
        try {
            orderMapper.insert(order);
        } catch (RuntimeException e) {
            // 库存已扣但订单落库失败（跨服务无分布式事务）：尽力补偿恢复库存，避免"幽灵扣减"
            log.error("[统一下单] 订单落库失败，补偿恢复库存 productId={} qty={}", productId, qty, e);
            try {
                productClient.restoreStock(productId, qty);
            } catch (Exception ex) {
                // 补偿也失败：库存与订单已不一致，需人工对账（商品ID + 数量）
                log.error("[统一下单] 恢复库存失败，请人工对账 productId={} qty={}", productId, qty, ex);
            }
            throw e;
        }
        log.info("[统一下单] 订单落库成功 orderId={} orderNo={} buyerId={} productId={} qty={}",
                order.getOrderId(), orderNo, buyerId, productId, qty);
        return order;
    }

    /**
     * 下单成功通知（买家、卖家各一条），直接下单与购物车下单共用。
     */
    private void notifyOrderCreated(OrderInfo order, String productTitle) {
        safeSendOrderNotification(order, productTitle, "订单创建成功",
                String.format("您的订单已创建，商品「%s」x%d，金额 ¥%.2f，请尽快支付",
                        productTitle, order.getQuantity(), order.getOrderAmount().doubleValue()),
                String.format("买家已下单购买您的商品「%s」x%d，金额 ¥%.2f，请等待支付",
                        productTitle, order.getQuantity(), order.getOrderAmount().doubleValue()));
    }

    // ==================== 查询 ====================

    @Override
    public boolean validateProductStatus(Long productId) {
        ProductInfo info = productClient.getProduct(productId);
        return info != null && info.getStatus() != null && ProductStatus.ON_SALE.matches(info.getStatus());
    }

    @Override
    public Result<OrderInfo> getOrderById(Long orderId) {
        OrderInfo order = orderMapper.findByOrderId(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        return Result.success(order);
    }

    @Override
    public Result<OrderDetailVO> getOrderDetail(Long orderId, Long userId) {
        OrderInfo order = orderMapper.findByOrderId(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return Result.error(403, "无权查看此订单");
        }

        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(order, vo);
        vo.setStatusName(OrderStatus.getNameByCode(order.getStatus()));

        // 商品信息直接用订单快照，不再跨服务调用
        ProductInfo productInfoVO = new ProductInfo();
        productInfoVO.setId(order.getProductId());
        productInfoVO.setTitle(order.getProductName());
        productInfoVO.setName(order.getProductName());
        productInfoVO.setPrice(order.getProductPrice() != null ? order.getProductPrice().doubleValue() : null);
        if (order.getProductImage() != null) {
            productInfoVO.setImageUrl(order.getProductImage());
            productInfoVO.setImages(List.of(order.getProductImage()));
        }
        vo.setProduct(productInfoVO);

        // 买家/卖家信息并行查询（仅本地 DB，无跨服务调用）
        CompletableFuture<User> buyerFuture = CompletableFuture.supplyAsync(() -> userMapper.findByUserId(order.getBuyerId()));
        CompletableFuture<User> sellerFuture = CompletableFuture.supplyAsync(() -> userMapper.findByUserId(order.getSellerId()));

        try {
            User buyer = buyerFuture.get(3, TimeUnit.SECONDS);
            if (buyer != null) {
                UserInfo buyerInfo = new UserInfo();
                buyerInfo.setId(buyer.getUserId());
                buyerInfo.setNickname(buyer.getNickname());
                buyerInfo.setAvatar(buyer.getAvatar());
                buyerInfo.setPhone(buyer.getPhone());
                vo.setBuyer(buyerInfo);
            }

            User seller = sellerFuture.get(3, TimeUnit.SECONDS);
            if (seller != null) {
                UserInfo sellerInfo = new UserInfo();
                sellerInfo.setId(seller.getUserId());
                sellerInfo.setNickname(seller.getNickname());
                sellerInfo.setAvatar(seller.getAvatar());
                sellerInfo.setPhone(seller.getPhone());
                vo.setSeller(sellerInfo);
            }
        } catch (Exception e) {
            log.warn("[订单详情] 查询买家/卖家信息超时 orderId={}", orderId, e);
        }

        return Result.success(vo);
    }

    // ==================== 状态流转（都附带发通知） ====================

    @Override
    @Transactional
    public Result<?> payOrder(Long orderId, Long userId) {
        OrderInfo order = orderMapper.findByOrderId(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            return Result.error(403, "无权操作此订单");
        }
        if (!OrderStatus.PENDING_PAYMENT.matches(order.getStatus())) {
            return Result.error(400, "订单状态不正确");
        }
        // 原子状态流转：仅当仍为待付款时置为待发货，防止与「超时自动取消」并发导致已支付订单被误取消
        int rows = orderMapper.compareAndSetStatus(order.getOrderId(),
                OrderStatus.PENDING_PAYMENT.getCode(), OrderStatus.PENDING_SHIPMENT.getCode());
        if (rows == 0) {
            log.info("[支付] 订单状态已被并发变更，支付失败 orderId={}", orderId);
            return Result.error(400, "订单状态不正确");
        }
        order.setStatus(OrderStatus.PENDING_SHIPMENT.getCode());
        order.setPayTime(new Date());

        safeSendOrderNotification(order, order.getProductName(),
                "订单支付成功",
                String.format("您已成功支付订单，金额 ¥%.2f，请等待卖家发货", order.getOrderAmount().doubleValue()),
                String.format("买家已完成支付，金额 ¥%.2f，请尽快发货", order.getOrderAmount().doubleValue()));

        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> shipOrder(Long orderId, Long sellerId) {
        OrderInfo order = orderMapper.findByOrderId(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!order.getSellerId().equals(sellerId)) {
            return Result.error(403, "无权操作此订单");
        }
        if (!OrderStatus.PENDING_SHIPMENT.matches(order.getStatus())) {
            return Result.error(400, "订单状态不正确");
        }
        // 原子状态流转，防止并发重复发货
        int rows = orderMapper.compareAndSetStatus(order.getOrderId(),
                OrderStatus.PENDING_SHIPMENT.getCode(), OrderStatus.PENDING_RECEIPT.getCode());
        if (rows == 0) {
            return Result.error(400, "订单状态不正确");
        }
        order.setStatus(OrderStatus.PENDING_RECEIPT.getCode());
        order.setShipTime(new Date());

        safeSendOrderNotification(order, order.getProductName(),
                "卖家已发货",
                String.format("卖家已发货，请您留意查收，及时确认收货"),
                String.format("订单已标记发货，等待买家确认收货"));

        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> confirmReceipt(Long orderId, Long userId) {
        OrderInfo order = orderMapper.findByOrderId(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            return Result.error(403, "无权操作此订单");
        }
        if (!OrderStatus.PENDING_RECEIPT.matches(order.getStatus())) {
            return Result.error(400, "订单状态不正确");
        }
        // 原子状态流转，防止并发重复确认收货
        int rows = orderMapper.compareAndSetStatus(order.getOrderId(),
                OrderStatus.PENDING_RECEIPT.getCode(), OrderStatus.COMPLETED.getCode());
        if (rows == 0) {
            return Result.error(400, "订单状态不正确");
        }
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setConfirmTime(new Date());

        safeSendOrderNotification(order, order.getProductName(),
                "交易完成",
                String.format("您已确认收货，交易完成，感谢您的支持"),
                String.format("买家已确认收货，交易完成"));

        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> cancelOrder(Long orderId, Long userId) {
        OrderInfo order = orderMapper.findByOrderId(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            return Result.error(403, "无权操作此订单");
        }
        if (OrderStatus.isTerminal(order.getStatus())) {
            return Result.error(400, "订单状态不正确");
        }
        // 记录原状态，用于判断是否需要恢复库存
        int originalStatus = order.getStatus();
        // 原子状态流转：以读到的状态为期望状态，防止与「超时自动取消/重复取消」并发导致库存被重复恢复
        int rows = orderMapper.compareAndSetStatus(order.getOrderId(),
                originalStatus, OrderStatus.CANCELLED.getCode());
        if (rows == 0) {
            log.info("[取消] 订单状态已被并发变更，取消失败 orderId={} originalStatus={}", orderId, originalStatus);
            return Result.error(400, "订单状态不正确");
        }
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelTime(new Date());

        // 仅待支付状态取消才恢复库存（已支付走退款流程恢复库存）
        if (OrderStatus.PENDING_PAYMENT.matches(originalStatus)) {
            productClient.restoreStock(order.getProductId(), order.getQuantity());
        }

        safeSendOrderNotification(order, order.getProductName(),
                "订单已取消",
                String.format("订单已取消，如有疑问请联系卖家"),
                String.format("订单被买家取消，商品库存已恢复"));

        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> autoCancelOrder(Long orderId) {
        OrderInfo order = orderMapper.findByOrderId(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!OrderStatus.PENDING_PAYMENT.matches(order.getStatus())) {
            // 状态已变更（可能用户刚支付），跳过自动取消
            return Result.error(400, "订单状态不正确");
        }
        // 原子状态流转：仅当仍为待付款时才取消，防止与用户支付并发导致已支付订单被误取消、
        // 库存被双重恢复（支付成功提交后，这里 WHERE status=0 匹配不到行，返回 0 直接跳过）
        int rows = orderMapper.compareAndSetStatus(order.getOrderId(),
                OrderStatus.PENDING_PAYMENT.getCode(), OrderStatus.CANCELLED.getCode());
        if (rows == 0) {
            log.info("[超时取消] 订单状态已被并发变更（可能刚完成支付），跳过自动取消 orderId={}", orderId);
            return Result.error(400, "订单状态不正确");
        }

        // 待支付订单超时取消，恢复库存
        productClient.restoreStock(order.getProductId(), order.getQuantity());

        safeSendOrderNotification(order, order.getProductName(),
                "订单已超时取消",
                String.format("订单超时未支付已自动取消，商品库存已恢复"),
                String.format("买家订单超时未支付已自动取消，商品库存已恢复"));

        log.info("[超时取消] 订单自动取消成功 orderId={} productId={}", orderId, order.getProductId());
        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> applyRefund(Long orderId, Long userId) {
        OrderInfo order = orderMapper.findByOrderId(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            return Result.error(403, "无权操作此订单");
        }
        if (!OrderStatus.PENDING_SHIPMENT.matches(order.getStatus()) && !OrderStatus.PENDING_RECEIPT.matches(order.getStatus())) {
            return Result.error(400, "订单状态不正确");
        }
        // 原子状态流转，防止并发重复申请退款
        int rows = orderMapper.compareAndSetStatus(order.getOrderId(),
                order.getStatus(), OrderStatus.REFUNDING.getCode());
        if (rows == 0) {
            return Result.error(400, "订单状态不正确");
        }
        order.setStatus(OrderStatus.REFUNDING.getCode());

        safeSendOrderNotification(order, order.getProductName(),
                "退款申请",
                String.format("您已提交退款申请，请等待卖家处理"),
                String.format("买家申请退款，金额 ¥%.2f，请及时处理", order.getOrderAmount().doubleValue()));

        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> processRefund(Long orderId, Long userId) {
        OrderInfo order = orderMapper.findByOrderId(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (!order.getSellerId().equals(userId)) {
            return Result.error(403, "无权操作此订单");
        }
        if (!OrderStatus.REFUNDING.matches(order.getStatus())) {
            return Result.error(400, "订单状态不正确");
        }
        // 原子状态流转，防止并发重复处理退款导致库存被重复恢复
        int rows = orderMapper.compareAndSetStatus(order.getOrderId(),
                OrderStatus.REFUNDING.getCode(), OrderStatus.REFUNDED.getCode());
        if (rows == 0) {
            return Result.error(400, "订单状态不正确");
        }
        order.setStatus(OrderStatus.REFUNDED.getCode());

        productClient.restoreStock(order.getProductId(), order.getQuantity());

        safeSendOrderNotification(order, order.getProductName(),
                "退款完成",
                String.format("卖家已同意退款，金额 ¥%.2f，请注意查收", order.getOrderAmount().doubleValue()),
                String.format("已同意退款，商品库存已恢复"));

        return Result.success();
    }

    // ==================== 列表查询 ====================

    @Override
    public Result<PageResult<OrderInfo>> getOrderListByUserId(Long userId, Long current, Long size) {
        Page<OrderInfo> page = new Page<>(current, size);
        IPage<OrderInfo> resultPage = orderMapper.selectPageByBuyerId(page, userId);
        PageResult<OrderInfo> result = new PageResult<>();
        BeanUtils.copyProperties(resultPage, result);
        return Result.success(result);
    }

    @Override
    public Result<PageResult<OrderInfo>> getOrderListByUserIdAndStatus(Long userId, Integer status, Long current, Long size) {
        Page<OrderInfo> page = new Page<>(current, size);
        IPage<OrderInfo> resultPage = orderMapper.selectPageByBuyerIdAndStatus(page, userId, status);
        PageResult<OrderInfo> result = new PageResult<>();
        BeanUtils.copyProperties(resultPage, result);
        return Result.success(result);
    }

    @Override
    public Result<PageResult<OrderListItemVO>> getOrderListByUserIdEnhanced(Long userId, Long current, Long size) {
        Page<OrderInfo> page = new Page<>(current, size);
        IPage<OrderInfo> resultPage = orderMapper.selectPageByBuyerId(page, userId);
        return Result.success(convertToOrderListItemPage(resultPage));
    }

    @Override
    public Result<PageResult<OrderListItemVO>> getOrderListByUserIdAndStatusEnhanced(Long userId, Integer status, Long current, Long size) {
        Page<OrderInfo> page = new Page<>(current, size);
        IPage<OrderInfo> resultPage = orderMapper.selectPageByBuyerIdAndStatus(page, userId, status);
        return Result.success(convertToOrderListItemPage(resultPage));
    }

    @Override
    public Result<PageResult<OrderListItemVO>> getBuyOrderList(Long buyerId, Integer status, Long current, Long size) {
        Page<OrderInfo> page = new Page<>(current, size);
        IPage<OrderInfo> resultPage;
        if (status != null) {
            resultPage = orderMapper.selectPageByBuyerIdAndStatus(page, buyerId, status);
        } else {
            resultPage = orderMapper.selectPageByBuyerId(page, buyerId);
        }
        return Result.success(convertToOrderListItemPage(resultPage));
    }

    @Override
    public Result<PageResult<OrderListItemVO>> getSellOrderList(Long sellerId, Integer status, Long current, Long size) {
        Page<OrderInfo> page = new Page<>(current, size);
        IPage<OrderInfo> resultPage;
        if (status != null) {
            resultPage = orderMapper.selectPageBySellerIdAndStatus(page, sellerId, status);
        } else {
            resultPage = orderMapper.selectPageBySellerId(page, sellerId);
        }
        return Result.success(convertToOrderListItemPage(resultPage));
    }

    @Override
    public Result<PageResult<OrderListItemVO>> adminGetOrderList(String keyword, Integer status, Long current, Long size) {
        LambdaQueryWrapper<OrderInfo> wrapper = Wrappers.lambdaQuery(OrderInfo.class)
                .eq(status != null, OrderInfo::getStatus, status)
                .and(keyword != null && !keyword.isEmpty(), w -> w
                        .like(OrderInfo::getOrderNo, keyword)
                        .or()
                        .like(OrderInfo::getProductName, keyword))
                .orderByDesc(OrderInfo::getCreateTime);
        Page<OrderInfo> page = new Page<>(current, size);
        IPage<OrderInfo> resultPage = orderMapper.selectPage(page, wrapper);
        return Result.success(convertToOrderListItemPage(resultPage));
    }

    // ==================== 订单状态查询 ====================

    @Override
    public Result<OrderStatusVO> getOrderStatusByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            return Result.error(400, "订单号不能为空");
        }

        // 1. 先查 Redis（Kafka 消费者异步更新的状态）
        try {
            Object cached = redisUtils.get(ORDER_STATUS_KEY + orderNo);
            if (cached instanceof OrderStatusVO) {
                OrderStatusVO vo = (OrderStatusVO) cached;
                log.info("[订单状态查询] Redis命中 orderNo={} status={}", orderNo, vo.getStatus());
                return Result.success(vo);
            }
        } catch (Exception e) {
            log.warn("[订单状态查询] Redis查询失败，继续查DB orderNo={}", orderNo, e);
        }

        // 2. 查数据库（同步下单或Kafka已完成落库）
        OrderInfo order = orderMapper.findByOrderNo(orderNo);
        if (order != null) {
            log.info("[订单状态查询] DB命中 orderNo={} orderId={}", orderNo, order.getOrderId());
            return Result.success(OrderStatusVO.success(orderNo, order.getOrderId(),
                    order.getOrderAmount().doubleValue()));
        }

        // 3. 都没找到，返回 processing（给异步消费者时间）
        log.info("[订单状态查询] 订单未找到，返回processing orderNo={}", orderNo);
        return Result.success(OrderStatusVO.processing(orderNo));
    }

    // ==================== 私有工具方法 ====================

    private OrderInfo buildOrder(String orderNo, Product product, String productImage, Long buyerId, Integer quantity,
                                 String receiverName, String receiverPhone, String receiverAddress) {
        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setProductId(product.getProductId());
        order.setProductName(product.getTitle());
        order.setProductImage(productImage);
        order.setProductPrice(product.getPrice());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getPublishUserId());
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setOrderAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setQuantity(quantity);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        order.setIsDeleted(0);
        return order;
    }

    private Product convertToProduct(ProductInfo info) {
        Product p = new Product();
        p.setProductId(info.getId());
        p.setTitle(info.getTitle() != null ? info.getTitle() : info.getName());
        p.setDescription(info.getDescription());
        p.setPrice(info.getPrice() != null ? BigDecimal.valueOf(info.getPrice()) : BigDecimal.ZERO);
        p.setQuantity(info.getStock() != null ? info.getStock() : (info.getQuantity() != null ? info.getQuantity() : 0));
        p.setStatus(info.getStatus() != null ? info.getStatus() : ProductStatus.ON_SALE.getCode());
        p.setPublishUserId(info.getSellerId());
        return p;
    }

    private Map<String, Object> buildCreateResult(OrderInfo order) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getOrderId());
        result.put("orderNo", order.getOrderNo());
        result.put("totalAmount", order.getOrderAmount().doubleValue());
        result.put("count", 1);
        return result;
    }

    private void rollbackStock(List<Long> productIds, List<Integer> quantities) {
        for (int i = 0; i < quantities.size(); i++) {
            try {
                productClient.restoreStock(productIds.get(i), quantities.get(i));
            } catch (Exception e) {
                log.error("[库存回滚] 失败 productId={} quantity={}", productIds.get(i), quantities.get(i), e);
            }
        }
    }

    private PageResult<OrderListItemVO> convertToOrderListItemPage(IPage<OrderInfo> page) {
        PageResult<OrderListItemVO> result = new PageResult<>();
        BeanUtils.copyProperties(page, result);
        List<OrderListItemVO> records = new ArrayList<>();
        for (OrderInfo order : page.getRecords()) {
            OrderListItemVO vo = new OrderListItemVO();
            BeanUtils.copyProperties(order, vo);
            vo.setStatusName(OrderStatus.getNameByCode(order.getStatus()));
            vo.setQuantity(order.getQuantity());
            // 使用订单快照，不再跨服务查商品
            vo.setProductName(order.getProductName());
            vo.setProductImage(order.getProductImage());
            records.add(vo);
        }
        result.setRecords(records);
        return result;
    }

    /**
     * 给买家、卖家分别发送一条订单类系统通知。
     * 使用独立线程池异步发送，Kafka 不可达时不阻塞下单主流程。
     */
    private void safeSendOrderNotification(OrderInfo order, String productTitle,
                                           String title, String buyerContent, String sellerContent) {
        if (kafkaProducerService == null) {
            log.info("[订单通知-降级] Kafka未启用，跳过发送 orderId={}", order.getOrderId());
            return;
        }
        final Date now = new Date();
        final Long orderId = order.getOrderId();
        final Long buyerId = order.getBuyerId();
        final Long sellerId = order.getSellerId();
        final Long productId = order.getProductId();
        NOTIFY_EXECUTOR.submit(() -> {
            try {
                NotificationSendPayload buyerPayload = NotificationSendPayload.builder()
                        .receiverId(buyerId)
                        .senderId(0L)
                        .title(title)
                        .content(buyerContent)
                        .type(NotificationType.ORDER.getCode())
                        .productId(productId)
                        .orderId(orderId)
                        .createTime(now)
                        .build();
                sendNotificationWithTimeout(buyerPayload, "买家", orderId);
            } catch (Exception e) {
                log.error("[订单通知-买家] 发送失败 orderId={} buyerId={}", orderId, buyerId, e);
            }
            try {
                NotificationSendPayload sellerPayload = NotificationSendPayload.builder()
                        .receiverId(sellerId)
                        .senderId(0L)
                        .title(title)
                        .content(sellerContent)
                        .type(NotificationType.ORDER.getCode())
                        .productId(productId)
                        .orderId(orderId)
                        .createTime(now)
                        .build();
                sendNotificationWithTimeout(sellerPayload, "卖家", orderId);
            } catch (Exception e) {
                log.error("[订单通知-卖家] 发送失败 orderId={} sellerId={}", orderId, sellerId, e);
            }
        });
    }

    /**
     * 在共享发送线程池中执行 Kafka 发送，最多等待 2 秒。
     * <p>
     * 原实现每次调用 newSingleThreadExecutor()（非 daemon 线程，Kafka 阻塞时不响应中断会泄漏、阻塞 JVM 退出），
     * 这里改为复用共享池 + Future 超时取消，既不新建线程，也保留「broker 不可达 / producer 缓冲满」时的超时保护。
     */
    private void sendNotificationWithTimeout(NotificationSendPayload payload, String who, Long orderId) {
        try {
            Future<?> future = NOTIFY_SEND_EXECUTOR.submit(() -> kafkaProducerService.sendAsync(
                    KafkaTopicConstants.NOTIFICATION_SEND, "order-notify", payload));
            try {
                future.get(2, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                // 2 秒未完成即放弃，防止 Kafka metadata 拉取 / producer 缓冲阻塞导致线程被长期占用
                future.cancel(true);
                log.warn("[订单通知-{}] 超时放弃 orderId={}", who, orderId);
            }
        } catch (Exception e) {
            log.error("[订单通知-{}] 发送失败 orderId={} err={}", who, orderId, e.getMessage(), e);
        }
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public long countAll() {
        return orderMapper.countAll();
    }

    @Override
    public long countTodayOrders() {
        String today = java.time.LocalDate.now().toString();
        return orderMapper.countTodayOrders(today);
    }

    @Override
    public java.math.BigDecimal sumTodayAmount() {
        String today = java.time.LocalDate.now().toString();
        return orderMapper.sumTodayAmount(today);
    }

    @Override
    public java.math.BigDecimal sumTotalAmount() {
        return orderMapper.sumTotalAmount();
    }
}
