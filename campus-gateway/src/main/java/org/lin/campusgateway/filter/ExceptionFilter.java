package org.lin.campusgateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class ExceptionFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(throwable -> {
                    logger.error("网关处理异常 path={}", exchange.getRequest().getURI().getPath(), throwable);

                    ServerHttpResponse response = exchange.getResponse();
                    Map<String, Object> body = new HashMap<>();

                    if (throwable instanceof NotFoundException) {
                        // 目标服务未注册到 Nacos / 已下线
                        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                        body.put("code", 503);
                        body.put("message", "后端服务未启动，请检查服务是否已注册到Nacos");
                    } else if (throwable instanceof ConnectException) {
                        // 目标服务拒绝连接
                        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                        body.put("code", 503);
                        body.put("message", "后端服务连接失败，请检查服务是否正常运行");
                    } else if (throwable instanceof TimeoutException) {
                        // 调用超时
                        response.setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
                        body.put("code", 504);
                        body.put("message", "后端服务响应超时，请稍后重试");
                    } else {
                        // 其他异常
                        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        body.put("code", 500);
                        body.put("message", "网关处理异常: " + throwable.getMessage());
                    }

                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    try {
                        byte[] bytes = objectMapper.writeValueAsBytes(body);
                        DataBuffer buffer = response.bufferFactory().wrap(bytes);
                        return response.writeWith(Mono.just(buffer));
                    } catch (Exception e) {
                        logger.error("序列化错误响应失败", e);
                        return response.setComplete();
                    }
                });
    }

    @Override
    public int getOrder() {
        return -997;
    }
}
