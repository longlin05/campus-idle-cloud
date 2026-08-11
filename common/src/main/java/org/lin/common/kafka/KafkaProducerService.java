package org.lin.common.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka 统一生产者服务。
 * <p>
 * 业务层通过本服务投递消息，屏蔽 KafkaTemplate 细节。
 * 提供「同步等待发送结果」与「异步回调」两种发送方式：
 * <ul>
 *   <li>{@link #sendSync}：阻塞等待 Broker ACK，用于必须确认投递成功的关键事件</li>
 *   <li>{@link #sendAsync}：异步发送 + 回调，用于追求吞吐的场景（推荐）</li>
 * </ul>
 * 发送失败仅记录日志与告警，不回滚业务事务（业务主流程已同步落库，消息用于异步副作用）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "lin.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    /**
     * 异步发送消息（推荐）。
     * <p>
     * 立即返回，发送结果通过回调处理。业务主流程无需等待 Broker ACK。
     *
     * @param topic     目标 Topic
     * @param eventType 事件类型（用于消费者路由）
     * @param data      业务负载
     */
    public void sendAsync(String topic, String eventType, Object data) {
        KafkaMessage message = KafkaMessage.of(eventType, applicationName, data);
        String eventId = message.getEventId();
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, eventId, message);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka发送失败] topic={} eventId={} eventType={}", topic, eventId, eventType, ex);
            } else {
                log.debug("[Kafka发送成功] topic={} eventId={} partition={} offset={}",
                        topic, eventId,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * 同步发送消息，阻塞等待 Broker ACK。
     * <p>
     * 仅用于必须确认投递成功的关键事件。注意：会降低业务接口响应速度。
     *
     * @param topic     目标 Topic
     * @param eventType 事件类型
     * @param data      业务负载
     * @return true 表示发送成功
     */
    public boolean sendSync(String topic, String eventType, Object data) {
        KafkaMessage message = KafkaMessage.of(eventType, applicationName, data);
        String eventId = message.getEventId();
        try {
            SendResult<String, Object> result = kafkaTemplate.send(topic, eventId, message).get();
            log.debug("[Kafka同步发送成功] topic={} eventId={} partition={} offset={}",
                    topic, eventId,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return true;
        } catch (Exception e) {
            log.error("[Kafka同步发送失败] topic={} eventId={} eventType={}", topic, eventId, eventType, e);
            return false;
        }
    }
}
