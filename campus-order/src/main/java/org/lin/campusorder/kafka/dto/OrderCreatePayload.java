package org.lin.campusorder.kafka.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单创建事件负载。
 * <p>
 * 用户下单时，OrderServiceImpl 先在 Redis 中预扣库存（保证并发安全），
 * 然后构造本对象投递到 Kafka。消费者据此异步创建订单落库。
 * <p>
 * 支持两种下单方式：
 * <ul>
 *   <li>直接购买：{@link #productIds} 仅 1 个元素，{@link #quantities} 对应 1 个数量</li>
 *   <li>购物车结算：{@link #productIds} 多个元素，每个数量默认 1</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatePayload implements Serializable {

    /** 临时订单号（Redis 预扣时生成，用于前端轮询查询订单状态） */
    private String orderNo;

    /** 买家用户 ID */
    private Long buyerId;

    /** 商品 ID 列表（直接购买为单元素，购物车结算为多元素） */
    private List<Long> productIds;

    /** 每个商品对应的购买数量（与 productIds 一一对应） */
    private List<Integer> quantities;

    /** 收件人姓名 */
    private String receiverName;

    /** 收件人电话 */
    private String receiverPhone;

    /** 收件人地址 */
    private String receiverAddress;

    /** 订单总金额（消费者落库时用于校验，由预扣阶段从商品价格计算） */
    private BigDecimal totalAmount;
}
