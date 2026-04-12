package org.lin.common.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.lin.campusidle.common.exception.BusinessException;
import org.lin.campusidle.common.threadlocal.UserThreadLocal;
import org.lin.campusidle.vo.UserV0;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtValidator jwtValidator;

    @Autowired
    public JwtAuthInterceptor(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 检查是否为方法处理器
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        JwtAuth jwtAuth = handlerMethod.getMethodAnnotation(JwtAuth.class);

        // 如果方法上没有JwtAuth注解，检查类上是否有
        if (jwtAuth == null) {
            jwtAuth = handlerMethod.getBeanType().getAnnotation(JwtAuth.class);
        }

        // 如果没有JwtAuth注解，直接通过
        if (jwtAuth == null) {
            return true;
        }

        // 验证令牌
        Claims claims = jwtValidator.validateToken(request);

        // 检查是否需要管理员权限
        if (jwtAuth.admin()) {
            Integer role = (Integer) claims.get("role");
            if (role == null || role != 0) {
                throw new BusinessException("权限不足，需要管理员权限");
            }
        }

        // 检查用户状态
        Integer status = (Integer) claims.get("status");
        if (status == null || status != 1) {
            throw new BusinessException("用户账号已被禁用");
        }

        // 将用户信息存储到ThreadLocal
        UserV0 userV0 = new UserV0();
        userV0.setId((Long) claims.get("id"));
        userV0.setUsername((String) claims.get("username"));
        userV0.setPhone((String) claims.get("phone"));
        userV0.setNickname((String) claims.get("nickname"));
        userV0.setRole((Integer) claims.get("role"));
        userV0.setStatus(status);
        UserThreadLocal.set(userV0);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理ThreadLocal中的用户信息，避免内存泄漏
        UserThreadLocal.remove();
    }
}