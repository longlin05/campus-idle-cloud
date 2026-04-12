package org.lin.common.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.lin.campusidle.common.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class JwtValidator {

    private final JwtUtils jwtUtils;

    public JwtValidator(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    /**
     * 从请求头中提取并验证JWT令牌
     * @param request HttpServletRequest
     * @return Claims 令牌中的声明
     * @throws BusinessException 令牌无效时抛出异常
     */
    public Claims validateToken(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            throw new BusinessException("未提供认证令牌");
        }
        
        try {
            return jwtUtils.parseToken(token);
        } catch (Exception e) {
            throw new BusinessException("认证令牌无效或已过期");
        }
    }

    /**
     * 从请求头中提取令牌
     * @param request HttpServletRequest
     * @return 令牌字符串，若不存在则返回null
     */
    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * 从令牌中获取用户ID
     * @param request HttpServletRequest
     * @return 用户ID
     */
    public Long getUserId(HttpServletRequest request) {
        Claims claims = validateToken(request);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从令牌中获取用户名
     * @param request HttpServletRequest
     * @return 用户名
     */
    public String getUsername(HttpServletRequest request) {
        Claims claims = validateToken(request);
        return (String) claims.get("username");
    }

    /**
     * 从令牌中获取用户角色
     * @param request HttpServletRequest
     * @return 用户角色
     */
    public Integer getRole(HttpServletRequest request) {
        Claims claims = validateToken(request);
        return (Integer) claims.get("role");
    }

    /**
     * 检查用户是否为管理员
     * @param request HttpServletRequest
     * @return 是否为管理员
     */
    public boolean isAdmin(HttpServletRequest request) {
        Integer role = getRole(request);
        return role != null && role == 0; // 0表示管理员
    }
}