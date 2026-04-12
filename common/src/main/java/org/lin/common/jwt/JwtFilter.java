package org.lin.common.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JwtConfig jwtConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        // 获取请求头中的Authorization
        String authorization = request.getHeader("Authorization");

        // 检查Authorization头是否存在且格式正确
        if (authorization != null && authorization.startsWith(jwtConfig.getTokenPrefix())) {
            // 提取令牌
            String token = authorization.substring(jwtConfig.getTokenPrefix().length());
            try {
                // 验证令牌
                if (!jwtUtils.isTokenExpired(token)) {
                    // 可以在这里将用户信息存储到请求中，供后续处理使用
                    // 例如：request.setAttribute("userId", jwtUtils.getSubject(token));
                }
            } catch (Exception e) {
                // 令牌验证失败，这里可以根据需要处理
                // 例如：返回401错误
            }
        }

        // 继续执行过滤器链
        chain.doFilter(request, response);
    }
}