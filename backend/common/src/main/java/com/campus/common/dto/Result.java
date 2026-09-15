package com.campus.common.dto;

import com.campus.common.exception.ErrorCode;
import org.slf4j.MDC;

import java.time.Instant;

/**
 * 统一返回结构（《系统设计》§3.5.1）。
 *
 * <p>成功码 {@link #SUCCESS_CODE} 为 W0 阶段补充的项目约定（上游文档只定义了错误码分段规则），
 * 需在 W1 前后端联调时与前端约定确认（W0 报告 H-02）。
 */
public record Result<T>(String code, String msg, T data, String traceId, Instant timestamp) {

    public static final String SUCCESS_CODE = "000000";
    public static final String SUCCESS_MSG = "success";

    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MSG, data, currentTraceId(), Instant.now());
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getUserMsg(), null, currentTraceId(), Instant.now());
    }

    public static <T> Result<T> fail(String code, String msg) {
        return new Result<>(code, msg, null, currentTraceId(), Instant.now());
    }

    public boolean success() {
        return SUCCESS_CODE.equals(code);
    }

    private static String currentTraceId() {
        return MDC.get("traceId");
    }
}
