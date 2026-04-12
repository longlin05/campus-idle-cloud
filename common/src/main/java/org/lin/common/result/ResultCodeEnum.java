package org.lin.common.result;

import lombok.Getter;

@Getter
public enum ResultCodeEnum {
    //通用成功
    SUCCESS(200,"操作成功"),
    //客户端错误
    PARAMETER_ERROR(400,"参数校验失败"),
    UNAUTHORIZED(401,"未授权，请先登录"),
    FORBIDDEN(403,"权限不足，禁止访问"),
    NOT_FOUND(404,"请求资源不存在"),
    CONFLICT(409,"冲突错误"),
    //服务端错误
    SYSTEM_ERROR(500,"系统内部错误，请稍后重试"),
    //业务异常
    BUSINESS_ERROR(600,"业务处理失败"),
    ;
    private final Integer code;
    private final String message;
    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
