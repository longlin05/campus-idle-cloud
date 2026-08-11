package org.lin.campusgateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LogFilter implements GlobalFilter, Ordered {
    
    private static final Logger logger = LoggerFactory.getLogger(LogFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 记录请求开始时间
        long start = System.currentTimeMillis();
        
        // 从ServerWebExchange中获取用户信息
        String userId = (String) exchange.getAttributes().get("userId");
        if (userId != null) {
            logger.info("用户 {} 请求: {} {}", userId, 
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath());
        } else {
            logger.info("匿名用户请求: {} {}", 
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath());
        }
        
        // 继续处理请求
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // 记录请求结束时间和处理时间
            long end = System.currentTimeMillis();
            logger.info("{} {} - {}ms", 
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                end - start);
        }));
    }
    
    @Override
    public int getOrder() {
        return -1000;
    }
}