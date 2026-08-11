package org.lin.campusitem.kafka.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;

/**
 * 商品浏览量计数事件负载。
 * <p>
 * 每次商品详情页被访问，先在 Redis 中 INCR 累计浏览量（保证实时性），
 * 同时投递本事件到 Kafka，消费者按周期或数量阈值批量落库到 DB，
 * 避免高并发下每条浏览都直接打 DB。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewCountPayload implements Serializable {

    /** 商品 ID */
    private Long productId;

    /** 本次累计的浏览增量（消费者聚合后一次性 UPDATE 到 DB） */
    private Integer count;
}
