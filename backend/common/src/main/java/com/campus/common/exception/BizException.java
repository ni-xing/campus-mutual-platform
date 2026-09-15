package com.campus.common.exception;

import lombok.Getter;

/** 业务异常：携带 {@link ErrorCode}，由全局异常处理器统一转 Result（《系统设计》§3.5.1）。 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getDesc());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String extraMsg) {
        super(errorCode.getDesc() + ": " + extraMsg);
        this.errorCode = errorCode;
    }

    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode);
    }
}
