package org.lin.common.config;

import org.lin.common.jwt.InternalAuthInterceptor;
import org.lin.common.jwt.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 注册 JWT 认证拦截器 + 内部接口鉴权拦截器
 * 只在有 spring-webmvc (Servlet 环境) 的微服务中生效，网关(WebFlux)自动跳过
 */
@Configuration
@ConditionalOnClass(WebMvcConfigurer.class)
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Autowired
    private InternalAuthInterceptor internalAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 JWT 认证拦截器（对所有路径）
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                .order(-100);

        // 注册内部接口鉴权拦截器（仅 /internal/** 路径），校验 X-Internal-Token
        registry.addInterceptor(internalAuthInterceptor)
                .addPathPatterns("/**/internal/**")
                .order(-90);
    }
}
