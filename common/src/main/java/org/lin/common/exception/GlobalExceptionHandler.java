package org.lin.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.lin.campusidle.common.result.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
@ResponseBody      //适配前后端分离返回JSON
public class GlobalExceptionHandler {

    //处理自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        //打印业务异常日志报告（warn）区别于系统错误
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }


    //处理请求参数校验异常（@Requestbody JSON参数校验
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        //提取校验失败的第一条提示信息
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验异常:{}", message);
        return Result.error(400, message);
    }

}
