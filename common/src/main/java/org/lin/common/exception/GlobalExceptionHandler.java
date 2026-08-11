package org.lin.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.lin.common.result.Result;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //处理自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("[业务异常] code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    //处理请求参数校验异常（@RequestBody JSON参数校验）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("[参数校验异常] {}", message);
        return Result.error(400, message);
    }

    //处理文件上传超限异常
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("[上传超限] {}", e.getMessage());
        return Result.error(400, "图片大小超过限制（最大10MB），请压缩后重试");
    }

    //处理事务异常（库存扣减失败、事务回滚等）
    @ExceptionHandler(TransactionSystemException.class)
    public Result<?> handleTransactionException(TransactionSystemException e) {
        log.error("[事务异常] {}", e.getMessage(), e);
        Throwable c = e.getCause();
        int depth = 0;
        while (c != null && depth < 5) {
            log.error("[事务异常-根因链{}] {}: {}", depth, c.getClass().getSimpleName(), c.getMessage());
            c = c.getCause();
            depth++;
        }
        return Result.error(500, "事务执行失败，请重试");
    }

    //兜底：捕获所有运行时异常（NPE、IllegalArgumentException等）
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error("[运行时异常] {}", e.getMessage(), e);
        Throwable c = e.getCause();
        int depth = 0;
        while (c != null && depth < 5) {
            log.error("[运行时异常-根因链{}] {}: {}", depth, c.getClass().getSimpleName(), c.getMessage());
            c = c.getCause();
            depth++;
        }
        return Result.error(500, "服务器内部错误: " + e.getMessage());
    }

    //最终兜底：捕获所有异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("[未知异常] {}", e.getMessage(), e);
        Throwable c = e.getCause();
        int depth = 0;
        while (c != null && depth < 5) {
            log.error("[未知异常-根因链{}] {}: {}", depth, c.getClass().getSimpleName(), c.getMessage());
            c = c.getCause();
            depth++;
        }
        return Result.error(500, "服务器内部错误");
    }

}
