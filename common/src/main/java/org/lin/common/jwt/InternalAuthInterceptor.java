package org.lin.common.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 内部接口鉴权拦截器。
 * <p>
 * 微服务间通过 /internal/** 直连调用（绕过网关），此前这些端点完全无鉴权，
 * 任何直连服务端口的客户端都能调用（如扣减/恢复库存、读取管理统计数据）。
 * 本拦截器要求所有 /internal/** 请求携带与配置一致的 {@code X-Internal-Token} 头，
 * 否则返回 403。调用方通过 {@link InternalAuthRequestInterceptor} 在 RestTemplate 出站请求中自动携带该头。
 * 仅在 Servlet(spring-webmvc) 环境生效，网关(WebFlux)自动跳过。
 */
@Slf4j
@Component
@ConditionalOnClass(HandlerInterceptor.class)
public class InternalAuthInterceptor implements HandlerInterceptor {

    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Value("${campus.internal-token:campus-internal-token}")
    private String internalToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (internalToken.equals(token)) {
            return true;
        }
        log.warn("[内部鉴权] 拒绝无令牌的内部调用 path={} from={}", request.getRequestURI(), request.getRemoteAddr());
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"禁止访问内部接口\",\"data\":null}");
        return false;
    }
}
