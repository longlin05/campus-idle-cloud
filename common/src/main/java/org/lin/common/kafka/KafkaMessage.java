package org.lin.common.kafka;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.UUID;

/**
 * Kafka 统一消息封装。
 * <p>
 * 所有业务事件投递到 Kafka 时统一使用该载体，便于消费者做幂等校验、路由分发与日志追踪。
 * <ul>
 *   <li>{@link #eventId} 作为唯一事件标识，消费者基于它做幂等去重</li>
 *   <li>{@link #eventType} 用于同 Topic 下区分子事件类型（如 product.published / product.updated）</li>
 *   <li>{@link #source} 标识生产者服务，便于排查</li>
 *   <li>{@link #data} 承载业务负载，由生产者/消费者约定具体类型</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaMessage implements Serializable {

    /** 事件唯一 ID，默认自动生成，消费者用于幂等去重 */
    private String eventId;

    /** 事件类型，如 product.published / order.created / chat.message */
    private String eventType;

    /** 事件来源服务名，如 campus-item / campus-order */
    private String source;

    /** 事件发生时间戳（毫秒） */
    private Long timestamp;

    /** 业务负载数据（JSON 反序列化为具体类型） */
    private Object data;

    /**
     * 快速构建消息的工厂方法，自动填充 eventId 与 timestamp。
     *
     * @param eventType 事件类型
     * @param source    来源服务
     * @param data      业务负载
     * @return 已就绪的 KafkaMessage
     */
    public static KafkaMessage of(String eventType, String source, Object data) {
        return KafkaMessage.builder()
                .eventId(UUID.randomUUID().toString().replace("-", ""))
                .eventType(eventType)
                .source(source)
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
    }
}
