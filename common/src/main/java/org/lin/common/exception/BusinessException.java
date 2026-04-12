package org.lin.common.exception;

import lombok.Getter;
import org.lin.campusidle.common.result.ResultCodeEnum;

@Getter
public class BusinessException extends RuntimeException {
    private Integer code;
    private String message;


    //业务异常-自定义提示
    public BusinessException(String message) {
        super(message);
        this.message = message;
        this.code = ResultCodeEnum.BUSINESS_ERROR.getCode();
    }


    //业务异常-自定义状态码+提示
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    //业务异常-使用枚举定义
    public BusinessException(ResultCodeEnum codeEnum) {
        super(codeEnum.getMessage());
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMessage();
    }
}
