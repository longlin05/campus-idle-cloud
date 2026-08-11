package org.lin.campusorder.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.kafka.KafkaMessage;
import org.lin.common.kafka.KafkaTopicConstants;
import org.lin.common.kafka.consumer.AbstractKafkaConsumer;
import org.lin.common.util.RedisUtils;
import org.lin.common.entity.OrderInfo;
import org.lin.common.entity.Product;
import org.lin.common.enums.OrderStatus;
import org.lin.campusorder.kafka.dto.OrderCreatePayload;
import org.lin.campusorder.kafka.dto.OrderStatusVO;
import org.lin.campusorder.mapper.OrderMapper;
import org.lin.campusorder.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 订单创建事件消费者。
 * <p>
 * 接收 {@link OrderCreatePayload} 事件，异步创建订单落库到 DB。
 * <p>
 * 处理流程：
 * <ol>
 *   <li>遍历 productIds，查询商品信息（卖家 ID、价格）</li>
 *   <li>构造 OrderInfo 记录并插入 DB</li>
 *   <li>更新 Redis 中的订单状态为 success（含 orderId + totalAmount）</li>
 *   <li>若处理失败，更新 Redis 状态为 failed 并回滚库存</li>
 * </ol>
 * <p>
 * 幂等保证：继承 {@link AbstractKafkaConsumer}，基于 eventId 去重；
 * 另外在落库前再次查询 orderNo 是否已存在，防止重复消费创建重复订单。
 */
@Slf4j
@Component
public class OrderCreateConsumer extends AbstractKafkaConsumer<OrderCreatePayload> {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisUtils redisUtils;

    private static final String ORDER_STATUS_KEY = "order:status:";
    private static final long ORDER_STATUS_TTL = 10 * 60L;

    @KafkaListener(
            topics = KafkaTopicConstants.ORDER_CREATED,
            groupId = "campus-order-create-group",
            // 订单创建需保证顺序（同一 orderNo 不能并发处理），concurrency=1
            concurrency = "1",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(KafkaMessage message) {
        super.handle(message, OrderCreatePayload.class);
    }

    @Override
    protected void doConsume(String eventId, OrderCreatePayload payload) throws Exception {
        if (payload == null || payload.getOrderNo() == null) {
            log.warn("[订单创建消费] payload 为空或缺少 orderNo，跳过 eventId={}", eventId);
            return;
        }

        String orderNo = payload.getOrderNo();
        log.info("[订单创建消费] 开始处理 orderNo={} buyerId={} productCount={}",
                orderNo, payload.getBuyerId(),
                payload.getProductIds() != null ? payload.getProductIds().size() : 0);

        // 幂等校验：DB 中已存在同 orderNo 的订单则跳过
        OrderInfo existing = orderMapper.findByOrderNo(orderNo);
        if (existing != null) {
            log.info("[订单创建消费] orderNo={} 订单已存在，跳过（幂等保护）", orderNo);
            // 确保 Redis 状态是 success
            updateRedisSuccess(orderNo, existing);
            return;
        }

        // 遍历商品创建订单（购物车场景多个商品 → 多条订单记录共享同一 orderNo）
        Long firstOrderId = null;
        for (int i = 0; i < payload.getProductIds().size(); i++) {
            Long productId = payload.getProductIds().get(i);
            Integer quantity = payload.getQuantities().get(i);

            Product product = productMapper.findAvailableProduct(productId);
            if (product == null) {
                // 商品已下架：回滚已扣减库存，更新状态为 failed
                log.error("[订单创建消费] 商品不存在或已下架 productId={}", productId);
                rollbackAndFail(orderNo, payload, "商品不存在或已下架");
                return;
            }

            OrderInfo order = buildOrder(orderNo, payload, product, quantity);
            orderMapper.insert(order);
            if (firstOrderId == null) {
                firstOrderId = order.getOrderId();
            }
        }

        // 更新 Redis 状态为 success
        OrderStatusVO success = OrderStatusVO.success(orderNo, firstOrderId,
                payload.getTotalAmount().doubleValue());
        redisUtils.set(ORDER_STATUS_KEY + orderNo, success, ORDER_STATUS_TTL, TimeUnit.SECONDS);
        log.info("[订单创建消费] 订单创建成功 orderNo={} firstOrderId={}", orderNo, firstOrderId);
    }

    /**
     * 构造 OrderInfo 记录。
     * <p>
     * 注意：库存已在 OrderServiceImpl 同步扣减，此处不重复扣减。
     */
    private OrderInfo buildOrder(String orderNo, OrderCreatePayload payload, Product product, Integer quantity) {
        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setProductId(product.getProductId());
        order.setBuyerId(payload.getBuyerId());
        order.setSellerId(product.getPublishUserId());
        order.setReceiverName(payload.getReceiverName());
        order.setReceiverPhone(payload.getReceiverPhone());
        order.setReceiverAddress(payload.getReceiverAddress());
        order.setOrderAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setQuantity(quantity);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        order.setIsDeleted(0);
        return order;
    }

    /**
     * 处理失败：回滚库存 + 更新 Redis 状态为 failed。
     */
    private void rollbackAndFail(String orderNo, OrderCreatePayload payload, String reason) {
        // 回滚库存（消费者侧补救）
        for (int i = 0; i < payload.getProductIds().size(); i++) {
            try {
                productMapper.restoreStock(payload.getProductIds().get(i),
                        payload.getQuantities().get(i));
                log.warn("[订单创建消费] 库存回滚成功 productId={} quantity={}",
                        payload.getProductIds().get(i), payload.getQuantities().get(i));
            } catch (Exception e) {
                log.error("[订单创建消费] 库存回滚失败 productId={}",
                        payload.getProductIds().get(i), e);
            }
        }
        // 更新 Redis 状态为 failed
        OrderStatusVO failed = OrderStatusVO.failed(orderNo, reason);
        redisUtils.set(ORDER_STATUS_KEY + orderNo, failed, ORDER_STATUS_TTL, TimeUnit.SECONDS);
    }

    private void updateRedisSuccess(String orderNo, OrderInfo order) {
        OrderStatusVO success = OrderStatusVO.success(orderNo, order.getOrderId(),
                order.getOrderAmount().doubleValue());
        redisUtils.set(ORDER_STATUS_KEY + orderNo, success, ORDER_STATUS_TTL, TimeUnit.SECONDS);
    }
}
