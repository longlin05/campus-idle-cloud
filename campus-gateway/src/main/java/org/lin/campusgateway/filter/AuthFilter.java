package org.lin.campusgateway.filter;

import io.jsonwebtoken.Claims;
import org.lin.common.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    // 白名单：无需鉴权的路径前缀（登录、注册、验证码、重置密码、公开商品浏览、公开用户主页）
    // 注意：AuthFilter 在 RewritePath 之前执行（order=-999），此处 path 为浏览器请求的"原始路径"（带 /api 前缀）。
    // 因此白名单必须使用原始路径（/api/auth/**、/api/product/** 等），同时保留重写后路径（/auth/**、/item/**）以兼容直接访问。
    // 路径末尾带 "/" 表示前缀匹配，否则精确匹配
    private static final List<String> WHITE_LIST = List.of(
            // ===== 认证模块（前端 /api/auth/** → 重写为 /auth/**）=====
            "/api/auth/send-code",
            "/api/auth/login/password",
            "/api/auth/login/password/query",
            "/api/auth/login/sms",
            "/api/auth/register",
            "/api/auth/reset-password",
            "/api/auth/user/",
            "/api/auth/internal/sync-profile",
            "/auth/send-code",
            "/auth/login/password",
            "/auth/login/password/query",
            "/auth/login/sms",
            "/auth/register",
            "/auth/reset-password",
            "/auth/user/",
            "/auth/internal/sync-profile",
            // ===== 商品模块（前端 /api/product/** → 重写为 /item/**）=====
            "/api/product/list",
            "/api/product/detail",
            "/api/product/categories",
            "/api/product/search",
            "/api/product/hot",
            "/api/product/batch",
            "/api/product/view",
            "/api/product/user/",
            "/api/product/internal/",
            "/api/item/list",
            "/api/item/detail",
            "/api/item/categories",
            "/api/item/search",
            "/api/item/hot",
            "/api/item/batch",
            "/api/item/view",
            "/api/item/user/",
            "/api/item/internal/",
            "/item/list",
            "/item/detail",
            "/item/categories",
            "/item/search",
            "/item/hot",
            "/item/batch",
            "/item/view",
            "/item/user/",
            "/item/internal/",
            // ===== 用户公开信息 =====
            "/api/user/info/",
            "/user/info/",
            // ===== 管理员公开系统图片/平台配置 =====
            "/api/admin/system-images/public/"
    );

    private final JwtUtils jwtUtils;

    /** 网关向下游传递的用户身份头。客户端可在入站请求中伪造这些头，因此转发前必须剥离并覆盖。 */
    private static final List<String> USER_HEADERS = List.of(
            "X-User-Id", "X-User-Nickname", "X-User-Phone", "X-User-Role", "X-User-Status", "X-User-Avatar");

    @Autowired
    public AuthFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 白名单放行（但剥离客户端伪造的 X-User-* 头，防止下游把公开接口当登录态）
        if (isWhiteListed(path)) {
            return chain.filter(stripUserHeaders(exchange));
        }

        // 2. 从请求头获取token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");

        // 3. 验证token
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        // 4. 解析token
        String jwt = token.substring(7);
        try {
            Claims claims = jwtUtils.parseToken(jwt);
            String userId = claims.getSubject();
            // 5. 将用户信息存储到ServerWebExchange的属性中
            exchange.getAttributes().put("userId", userId);

            // 6. 先剥离入站 X-User-* 头，再用 JWT 解析结果 set 覆盖（set 是替换而非追加），
            //    避免客户端预置的伪造头被下游 getHeader 读到（getHeader 返回首个值）。
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(request -> request.headers(headers -> {
                        USER_HEADERS.forEach(headers::remove);
                        headers.set("X-User-Id", userId);
                        if (claims.get("nickname") != null) {
                            headers.set("X-User-Nickname", claims.get("nickname").toString());
                        }
                        if (claims.get("phone") != null) {
                            headers.set("X-User-Phone", claims.get("phone").toString());
                        }
                        if (claims.get("role") != null) {
                            headers.set("X-User-Role", claims.get("role").toString());
                        }
                        if (claims.get("status") != null) {
                            headers.set("X-User-Status", claims.get("status").toString());
                        }
                        if (claims.get("avatar") != null) {
                            headers.set("X-User-Avatar", claims.get("avatar").toString());
                        }
                    }))
                    .build();

            return chain.filter(mutatedExchange);
        } catch (Exception e) {
            return unauthorized(exchange);
        }
    }

    /**
     * 剥离入站请求中的 X-User-* 头（仅适用于无需鉴权的公开接口）。
     */
    private ServerWebExchange stripUserHeaders(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> USER_HEADERS.forEach(headers::remove)))
                .build();
    }

    private boolean isWhiteListed(String path) {
        for (String prefix : WHITE_LIST) {
            if (prefix.endsWith("/")) {
                if (path.startsWith(prefix)) return true;
            } else if (path.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -999;
    }
}