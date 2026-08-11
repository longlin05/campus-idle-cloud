package org.lin.campusorder.kafka.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;

/**
 * 订单创建状态查询结果。
 * <p>
 * 前端下单后立即拿到 {@code orderNo}，通过轮询接口查询订单最终状态：
 * <ul>
 *   <li>{@code processing}：订单正在异步创建中（Redis 已预扣库存，DB 尚未落库）</li>
 *   <li>{@code success}：订单创建成功，{@code orderId} 已填充，可跳转订单详情</li>
 *   <li>{@code failed}：订单创建失败（库存不足/商品下架等），{@code reason} 给出原因</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusVO implements Serializable {

    /** 临时订单号（前端轮询凭证） */
    private String orderNo;

    /** 订单状态：processing / success / failed */
    private String status;

    /** 订单 ID（status=success 时填充） */
    private Long orderId;

    /** 失败原因（status=failed 时填充） */
    private String reason;

    /** 订单总金额（status=success 时填充） */
    private Double totalAmount;

    public static OrderStatusVO processing(String orderNo) {
        return OrderStatusVO.builder()
                .orderNo(orderNo)
                .status("processing")
                .build();
    }

    public static OrderStatusVO success(String orderNo, Long orderId, Double totalAmount) {
        return OrderStatusVO.builder()
                .orderNo(orderNo)
                .status("success")
                .orderId(orderId)
                .totalAmount(totalAmount)
                .build();
    }

    public static OrderStatusVO failed(String orderNo, String reason) {
        return OrderStatusVO.builder()
                .orderNo(orderNo)
                .status("failed")
                .reason(reason)
                .build();
    }
}
