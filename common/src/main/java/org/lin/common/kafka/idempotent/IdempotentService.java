package org.lin.common.kafka.idempotent;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.util.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka 消费幂等服务。
 * <p>
 * 基于 Redis SETNX 实现，防止 Kafka 重试或重平衡导致的重复消费。
 * <p>
 * 使用方式（消费者侧）：
 * <pre>
 * if (!idempotentService.tryConsume(eventId)) {
 *     log.warn("事件已处理，跳过 eventId={}", eventId);
 *     return;
 * }
 * // 执行业务逻辑
 * </pre>
 * <p>
 * 幂等记录保留 24 小时后自动过期，覆盖 Kafka 重试窗口。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "lin.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class IdempotentService {

    @Autowired
    private RedisUtils redisUtils;

    /** 幂等 Key 前缀 */
    private static final String IDEMPOTENT_KEY_PREFIX = "kafka:idempotent:";

    /** 幂等记录保留时长（小时） */
    private static final long EXPIRE_HOURS = 24;

    /**
     * 尝试标记事件为「已消费」。
     * <p>
     * 使用 SETNX 语义：仅当 Key 不存在时设置成功。
     *
     * @param eventId 事件唯一 ID
     * @return true 表示首次消费，可继续执行业务；false 表示已消费过，应跳过
     */
    public boolean tryConsume(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            log.warn("[幂等校验] eventId 为空，跳过幂等检查直接放行");
            return true;
        }
        String key = IDEMPOTENT_KEY_PREFIX + eventId;
        boolean acquired = redisUtils.tryLock(key, EXPIRE_HOURS * 3600);
        if (acquired) {
            log.debug("[幂等校验] 首次消费 eventId={}", eventId);
        } else {
            log.warn("[幂等校验] 重复消费已拦截 eventId={}", eventId);
        }
        return acquired;
    }

    /**
     * 手动释放幂等锁（仅在业务执行失败需要允许重试时调用）。
     *
     * @param eventId 事件唯一 ID
     */
    public void release(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return;
        }
        String key = IDEMPOTENT_KEY_PREFIX + eventId;
        redisUtils.delete(key);
        log.debug("[幂等校验] 释放幂等锁 eventId={}", eventId);
    }
}
