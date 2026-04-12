package org.lin.common.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtConfig jwtConfig;

    /**
     * 生成JWT令牌
     * @param subject 主题（一般存用户ID）
     * @param claims 自定义载荷
     * @return JWT令牌
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        // 将字符串密钥转为规范的SecretKey对象
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());

        return Jwts.builder()
                // 自定义载荷
                .claims(claims)
                // 主题（用户ID）
                .subject(subject)
                // 签发时间
                .issuedAt(new Date())
                // 过期时间
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpireTime()))
                // 签名算法+密钥
                .signWith(secretKey)
                // 生成token
                .compact();
    }

    /**
     * 重载：简化生成token（只传用户ID）
     * @param subject 用户ID
     * @return JWT令牌
     */
    public String generateToken(String subject) {
        return generateToken(subject, Map.of());
    }

    /**
     * 解析JWT令牌，获取全部载荷
     * @param token JWT令牌
     * @return 令牌中的载荷Claims
     */
    public Claims parseToken(String token) {
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("token已过期");
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("token非法");
        }
    }

    /**
     * 从令牌中获取主题（用户ID）
     * @param token JWT令牌
     * @return 用户ID
     */
    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 判断令牌是否过期
     * @param token JWT令牌
     * @return true=过期，false=有效
     */
    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}