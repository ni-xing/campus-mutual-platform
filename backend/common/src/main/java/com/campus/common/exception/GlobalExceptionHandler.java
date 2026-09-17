package com.campus.common.exception;

import com.campus.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理（《系统设计》§3.5.1 / 安全设计 §4.2）：
 * 禁止向前端泄露堆栈、SQL、路径与版本信息。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("biz error code={} desc={}", e.getErrorCode().getCode(), e.getMessage());
        // extraMsg 为业务方给定的更具体文案；缺省回落错误码注册表 userMsg
        String msg = e.getExtraMsg() != null ? e.getExtraMsg() : e.getErrorCode().getUserMsg();
        return Result.fail(e.getErrorCode().getCode(), msg);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidation(BindException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("param invalid: {}", detail);
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(),
                ErrorCode.PARAM_INVALID.getUserMsg() + (detail.isEmpty() ? "" : "（" + detail + "）"));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public Result<Void> handleBadRequest(Exception e) {
        log.warn("bad request: {}", e.getMessage());
        return Result.fail(ErrorCode.PARAM_INVALID);
    }

    /**
     * 保留框架语义状态码（404/405/503 等）。W1 实测教训：无此处理器时，
     * 网关无路由匹配的 404 会被下方兜底处理器吞成 200+B000001。
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public Result<Void> handleResponseStatus(org.springframework.web.server.ResponseStatusException e) {
        log.warn("status exception: {}", e.getMessage());
        return Result.fail(String.valueOf(e.getStatusCode().value()), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        // 堆栈只落服务端日志，不返回前端
        log.error("unexpected error", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
