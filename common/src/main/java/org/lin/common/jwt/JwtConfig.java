package org.lin.common.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    // 密钥
    private String secret;
    // 过期时间（毫秒）
    private Long expireTime;
    // 令牌前缀
    private String tokenPrefix = "Bearer ";
}