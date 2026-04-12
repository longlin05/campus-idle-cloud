package org.lin.common.result;

import lombok.Data;

@Data
public class Result<T> {
    //响应状态码
    private Integer code;
    //响应提示信息
    private String message;
    //响应数据
    private T data;


    //private 禁止外部new
    private Result(Integer code, String  message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }


    //=========成功响应方法==========
    //成功无数据返回型
    public static <T> Result<T> success() {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(),ResultCodeEnum.SUCCESS.getMessage(),null);
    }

    //成功-带数据返回
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), ResultCodeEnum.SUCCESS.getMessage(), data);
    }

    //成功-自定义提示+数据
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(),message,data);
    }


    //=========失败响应方法==========

    //失败使用枚举定义
    public static <T> Result<T> error(ResultCodeEnum codeEnum) {
        return new Result<>(codeEnum.getCode(), codeEnum.getMessage(), null);
    }
    //失败-自定义状态码+提示
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code,message,null);
    }
    //失败-业务异常专用
    public static <T> Result<T> businessError(String message) {
        return new Result<>(ResultCodeEnum.BUSINESS_ERROR.getCode(), message,null);
    }
    
    //判断响应是否成功
    public boolean isSuccess() {
        return ResultCodeEnum.SUCCESS.getCode().equals(this.code);
    }
}
