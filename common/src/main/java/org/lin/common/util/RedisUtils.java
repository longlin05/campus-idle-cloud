package org.lin.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** 释放锁的 Lua 脚本：仅当持有者令牌匹配时才删除，get + del 原子执行 */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(UNLOCK_LUA, Long.class);

    // 基本操作
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void delete(Set<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    // 哈希操作
    public void hset(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object hget(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    public Long hincrBy(String key, String field, long delta) {
        return redisTemplate.opsForHash().increment(key, field, delta);
    }

    public void hdelete(String key, Object... fields) {
        redisTemplate.opsForHash().delete(key, fields);
    }

    // 有序集合操作
    public void zadd(String key, double score, Object value) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    public Double zincrby(String key, double increment, Object value) {
        return redisTemplate.opsForZSet().incrementScore(key, value, increment);
    }

    public Set<Object> zrevrange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    // ==================== 分布式锁 ====================

    /**
     * 尝试获取分布式锁（SETNX 语义）。
     * <p>
     * 适用于「首次写入者获胜」的幂等标记场景（如浏览去重、Kafka 消费幂等），
     * 锁只靠 TTL 自动过期、业务内不手动解锁，无需校验持有者。
     *
     * @return true 获取成功（首次），false 已被占用
     */
    public boolean tryLock(String key, long expireSeconds) {
        return tryLockWithToken(key, expireSeconds) != null;
    }

    /**
     * 尝试获取分布式锁，返回持有者令牌。
     * <p>
     * 适用于需要显式解锁的互斥场景（如定时任务的分布式选主、跨实例临界区）：
     * 解锁时须传入本方法返回的令牌，避免误删他人持有的锁。
     *
     * @return 持有者令牌（UUID），获取失败返回 null
     */
    public String tryLockWithToken(String key, long expireSeconds) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(key, token, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /**
     * 释放分布式锁（Lua 原子校验持有者令牌后删除）。
     * <p>
     * 解决「先 get 后 del」非原子导致的误删问题：若持有者 A 的锁已过期、B 抢到同一 key 的锁，
     * A 解锁时因令牌不匹配不会删掉 B 的锁，从而避免临界区被第三方并发进入。
     *
     * @return true 释放成功；false 令牌不匹配（锁已被他人持有或已过期）
     */
    public boolean unlock(String key, String token) {
        if (token == null) {
            return false;
        }
        Long result = redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
        return Long.valueOf(1).equals(result);
    }

    /**
     * 直接删除锁。
     * <p>
     * 仅适用于值恒定、且 key 生命周期内不会被换主持有的幂等标记（如浏览去重 key）。
     * 互斥锁场景请勿使用——它不校验持有者，可能误删他人刚获取的锁。
     *
     * @deprecated 互斥锁请使用 {@link #unlock(String, String)}，这里仅保留给幂等标记场景
     */
    @Deprecated
    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    // 工具方法
    public void setWithRandomExpire(String key, Object value, int baseMinutes) {
        int randomMinutes = baseMinutes + new Random().nextInt(6);
        set(key, value, randomMinutes, TimeUnit.MINUTES);
    }

    // 增加计数
    public void incrementCount(String key) {
        redisTemplate.opsForValue().increment(key);
    }

    // 减少计数
    public void decrementCount(String key) {
        redisTemplate.opsForValue().decrement(key);
    }

    // 设置过期时间
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    // 获取剩余过期时间（秒），-2 表示 key 不存在，-1 表示未设置过期
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}
