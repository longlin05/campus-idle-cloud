package org.lin.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.Random;

@Component
public class RedisUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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

    // 分布式锁
    public boolean tryLock(String key, long expireSeconds) {
        return redisTemplate.opsForValue().setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS);
    }

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
}
