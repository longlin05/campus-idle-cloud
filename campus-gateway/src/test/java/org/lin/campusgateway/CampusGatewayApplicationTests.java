package org.lin.campusgateway;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CampusGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testAuthFilter() {
        // 模拟JwtUtils
        org.lin.common.jwt.JwtUtils jwtUtils = Mockito.mock(org.lin.common.jwt.JwtUtils.class);
        io.jsonwebtoken.Claims claims = Mockito.mock(io.jsonwebtoken.Claims.class);
        Mockito.when(claims.getSubject()).thenReturn("1");
        Mockito.when(jwtUtils.parseToken(Mockito.anyString())).thenReturn(claims);

        // 创建AuthFilter
        org.lin.campusgateway.filter.AuthFilter authFilter = new org.lin.campusgateway.filter.AuthFilter(jwtUtils);
        GatewayFilterChain chain = Mockito.mock(GatewayFilterChain.class);
        Mockito.when(chain.filter(Mockito.any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // 创建带有有效token的请求
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // 执行过滤器
        authFilter.filter(exchange, chain).block();

        // 验证请求被正常处理
        Mockito.verify(chain).filter(exchange);
    }

    @Test
    void testRateLimitFilter() {
        // 模拟RedissonClient
        org.redisson.api.RedissonClient redissonClient = Mockito.mock(org.redisson.api.RedissonClient.class);
        org.redisson.api.RAtomicLong counter = Mockito.mock(org.redisson.api.RAtomicLong.class);
        Mockito.when(redissonClient.getAtomicLong(Mockito.anyString())).thenReturn(counter);
        Mockito.when(counter.incrementAndGet()).thenReturn(1L);

        // 创建RateLimitFilter
        org.lin.campusgateway.filter.RateLimitFilter rateLimitFilter = new org.lin.campusgateway.filter.RateLimitFilter(redissonClient);
        GatewayFilterChain chain = Mockito.mock(GatewayFilterChain.class);
        Mockito.when(chain.filter(Mockito.any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // 创建请求
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // 执行过滤器
        rateLimitFilter.filter(exchange, chain).block();

        // 验证请求被正常处理
        Mockito.verify(chain).filter(exchange);
    }

    @Test
    void testLogFilter() {
        // 创建LogFilter
        org.lin.campusgateway.filter.LogFilter logFilter = new org.lin.campusgateway.filter.LogFilter();
        GatewayFilterChain chain = Mockito.mock(GatewayFilterChain.class);
        Mockito.when(chain.filter(Mockito.any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // 创建请求
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // 执行过滤器
        logFilter.filter(exchange, chain).block();

        // 验证请求被正常处理
        Mockito.verify(chain).filter(exchange);
    }

    @Test
    void testExceptionFilter() {
        // 创建ExceptionFilter
        org.lin.campusgateway.filter.ExceptionFilter exceptionFilter = new org.lin.campusgateway.filter.ExceptionFilter();
        GatewayFilterChain chain = Mockito.mock(GatewayFilterChain.class);
        // 模拟chain.filter抛出异常
        Mockito.when(chain.filter(Mockito.any(ServerWebExchange.class))).thenThrow(new RuntimeException("Test exception"));

        // 创建请求
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // 执行过滤器
        exceptionFilter.filter(exchange, chain).block();

        // 验证返回500状态
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
    }
}
