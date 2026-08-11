package org.lin.common.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.lin.common.context.UserInfo;
import org.lin.common.threadlocal.UserThreadLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT认证拦截器
 * 处理 @JwtAuth 注解的方法，从请求头或网关传递的用户信息中获取用户ID
 * 
 * 优先使用网关注入的 X-User-Id 请求头（网关已验证token并传递用户信息），
 * 如果没有则尝试从 Authorization 请求头解析token（直接访问微服务时使用）。
 * 
 * 只在有 spring-webmvc (Servlet 环境) 的微服务中生效，网关(WebFlux)自动跳过。
 */
@Slf4j
@Component
@ConditionalOnClass(HandlerInterceptor.class)
public class JwtAuthInterceptor implements HandlerInterceptor {

    // JwtUtils 可选注入，某些微服务可能不需要直接解析token
    @Autowired(required = false)
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是Controller方法，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 检查方法上的 @JwtAuth 注解
        JwtAuth jwtAuth = handlerMethod.getMethodAnnotation(JwtAuth.class);
        
        // 如果方法上没有，检查类上的 @JwtAuth 注解
        if (jwtAuth == null) {
            jwtAuth = handlerMethod.getBeanType().getAnnotation(JwtAuth.class);
        }
        
        // 如果没有 @JwtAuth 注解，直接放行
        if (jwtAuth == null) {
            return true;
        }

        // 优先使用网关注入的请求头（网关已验证token并传递用户信息）
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            UserInfo userInfo = new UserInfo();
            userInfo.setId(Long.valueOf(userIdHeader));
            String nickname = request.getHeader("X-User-Nickname");
            if (nickname != null && !nickname.isEmpty()) {
                userInfo.setNickname(nickname);
            }
            String phone = request.getHeader("X-User-Phone");
            if (phone != null && !phone.isEmpty()) {
                userInfo.setPhone(phone);
            }
            String role = request.getHeader("X-User-Role");
            if (role != null && !role.isEmpty()) {
                userInfo.setRole(Integer.valueOf(role));
            }
            String status = request.getHeader("X-User-Status");
            if (status != null && !status.isEmpty()) {
                userInfo.setStatus(Integer.valueOf(status));
            }
            String avatar = request.getHeader("X-User-Avatar");
            if (avatar != null && !avatar.isEmpty()) {
                userInfo.setAvatar(avatar);
            }
            UserThreadLocal.set(userInfo);
            log.debug("从网关请求头获取用户ID: {}", userIdHeader);

            // 管理员权限校验：role=0为管理员，非管理员禁止访问admin接口
            if (jwtAuth.admin() && (userInfo.getRole() == null || userInfo.getRole() != 0)) {
                log.warn("非管理员用户尝试访问管理员接口, userId={}, role={}", userInfo.getId(), userInfo.getRole());
                writeForbidden(response, "无权限访问");
                return false;
            }
            return true;
        }

        // 如果没有 JwtUtils，无法直接解析token
        if (jwtUtils == null) {
            log.warn("未配置JwtUtils且无X-User-Id请求头，鉴权失败");
            writeUnauthorized(response, "未登录或token已过期");
            return false;
        }

        // 兼容直接访问微服务的情况（从Authorization中解析）
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("未提供有效的Authorization token");
            writeUnauthorized(response, "未登录或token已过期");
            return false;
        }

        String jwt = token.substring(7);
        try {
            Claims claims = jwtUtils.parseToken(jwt);
            UserInfo userInfo = new UserInfo();
            userInfo.setId(Long.valueOf(claims.getSubject()));
            userInfo.setNickname(claims.get("nickname", String.class));
            userInfo.setPhone(claims.get("phone", String.class));
            userInfo.setRole(claims.get("role", Integer.class));
            userInfo.setStatus(claims.get("status", Integer.class));
            userInfo.setAvatar(claims.get("avatar", String.class));
            UserThreadLocal.set(userInfo);
            log.debug("从Authorization解析用户ID: {}", userInfo.getId());

            // 管理员权限校验：role=0为管理员，非管理员禁止访问admin接口
            if (jwtAuth.admin() && (userInfo.getRole() == null || userInfo.getRole() != 0)) {
                log.warn("非管理员用户尝试访问管理员接口, userId={}, role={}", userInfo.getId(), userInfo.getRole());
                writeForbidden(response, "无权限访问");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("token解析失败: {}", e.getMessage());
            writeUnauthorized(response, "token无效或已过期");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理ThreadLocal，防止内存泄漏
        UserThreadLocal.remove();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"" + message + "\",\"data\":null}");
    }
}
