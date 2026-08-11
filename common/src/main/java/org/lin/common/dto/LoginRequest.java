package org.lin.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
    private String phone;

    @Size(min = 6, message = "密码至少6位")
    private String password;

    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    private String code;
}