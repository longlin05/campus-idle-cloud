package org.lin.campusgateway.filter;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

//限流功能之后学习了sentinel后再换掉redisson
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
    
    private final RedissonClient redissonClient;
    
    @Autowired
    public RateLimitFilter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取客户端IP
        String clientIp = "127.0.0.1"; // 默认IP
        if (exchange.getRequest().getRemoteAddress() != null && exchange.getRequest().getRemoteAddress().getAddress() != null) {
            clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        // 2. 构建限流key
        String key = "rate_limit:" + clientIp;

        // 3. 使用Redisson的原子计数器
        RAtomicLong counter = redissonClient.getAtomicLong(key);

        // 4. 增加计数
        long count = counter.incrementAndGet();

        // 5. 第一次请求，设置过期时间
        if (count == 1) {
            counter.expire(1, TimeUnit.MINUTES);
        }

        // 6. 判断是否超过限流阈值
        if (count > 100) { // 每分钟100次请求
            return tooManyRequests(exchange);
        }

        // 7. 继续处理请求
        return chain.filter(exchange);
    }
    
    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }
    
    @Override
    public int getOrder() {
        return -998;
    }
}