package org.lin.common.context;

import lombok.Data;

@Data
public class LoginResult {
    private UserInfo user;
    private String token;
}