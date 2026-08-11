package org.lin.campusapi.vo;

import lombok.Data;

@Data
public class LoginResult {
    // 用户信息
    private UserV0 user;
    // JWT令牌
    private String token;
}