package org.lin.common.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 消费者公共配置。
 * <p>
 * 所有需要消费 Kafka 消息的微服务引入 common 后自动装配。
 * 通过 {@code lin.kafka.enabled=true} 开关控制，未启用的服务不装配消费者基础设施。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>关闭自动提交，由容器 AckMode 控制 offset 提交，保证消费成功才提交</li>
 *   <li>使用 JsonDeserializer 的信任所有包策略，反序列化 {@link KafkaMessage}</li>
 *   <li>并发消费数默认 3，可按服务负载调整</li>
 * </ul>
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "lin.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:campus-idle-group}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:latest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.max-poll-records:500}")
    private int maxPollRecords;

    @Value("${lin.kafka.consumer.concurrency:3}")
    private int concurrency;

    @Value("${lin.kafka.consumer.retry-max-attempts:3}")
    private long retryMaxAttempts;

    @Value("${lin.kafka.consumer.retry-interval:2000}")
    private long retryInterval;

    /**
     * 消费者工厂。
     * <p>
     * 关闭自动提交（enable.auto.commit=false），改用容器层手动提交，
     * 确保业务处理成功后再提交 offset，避免消息丢失。
     */
    @Bean
    public ConsumerFactory<String, Object> kafkaConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        // 信任所有包，允许反序列化 common 模块下的 KafkaMessage 及业务 DTO
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * 通用监听容器工厂。
     * <p>
     * 使用 RECORD 模式：每条消息处理成功后立即提交 offset，保证消费可靠性。
     * 消费失败时由 {@code DefaultErrorHandler} 触发重试，重试耗尽后投递死信。
     */
    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        // 错误处理：重试 retryMaxAttempts 次（间隔 retryInterval 毫秒），耗尽后投递死信队列
        // 死信 Topic 命名规则：原 Topic 名 + ".dlq"（由 DeadLetterPublishingRecoverer 默认行为决定）
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        // FixedBackOff(interval, maxAttempts)：maxAttempts 不含首次，0 表示不重试直接进死信
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(retryInterval, retryMaxAttempts));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
