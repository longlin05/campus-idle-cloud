package org.lin.campusorder.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.entity.OrderInfo;
import org.lin.campusorder.mapper.OrderMapper;
import org.lin.campusorder.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单超时自动取消定时任务。
 * <p>
 * 每分钟扫描一次，将创建超过 30 分钟仍未支付的订单（status=0）自动取消并恢复库存。
 */
@Slf4j
@Component
public class OrderTimeoutScheduler {

    /** 未支付订单保留时长（分钟） */
    private static final int TIMEOUT_MINUTES = 30;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    @Scheduled(fixedRate = 60_000)
    public void cancelTimeoutOrders() {
        List<OrderInfo> timeoutOrders;
        try {
            timeoutOrders = orderMapper.selectTimeoutUnpaidOrders(TIMEOUT_MINUTES);
        } catch (Exception e) {
            log.error("[超时取消] 查询超时未支付订单失败", e);
            return;
        }

        if (timeoutOrders == null || timeoutOrders.isEmpty()) {
            return;
        }

        log.info("[超时取消] 发现 {} 笔超时未支付订单，开始自动取消", timeoutOrders.size());

        int success = 0;
        int fail = 0;
        for (OrderInfo order : timeoutOrders) {
            try {
                orderService.autoCancelOrder(order.getOrderId());
                success++;
            } catch (Exception e) {
                fail++;
                log.error("[超时取消] 自动取消失败 orderId={}", order.getOrderId(), e);
            }
        }

        log.info("[超时取消] 本轮完成，成功 {} 笔，失败 {} 笔", success, fail);
    }
}
