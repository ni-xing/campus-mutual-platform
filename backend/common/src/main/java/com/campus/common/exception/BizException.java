package com.campus.common.exception;

import lombok.Getter;

/**
 * 业务异常：携带 {@link ErrorCode}，由全局异常处理器统一转 Result（《系统设计》§3.5.1）。
 *
 * <p>{@code extraMsg} 为可选的更具体用户文案（如"请使用校园邮箱"），
 * 全局异常处理器优先取它，缺省回落错误码注册表的 userMsg。
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String extraMsg;

    public BizException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public BizException(ErrorCode errorCode, String extraMsg) {
        super(errorCode.getDesc() + (extraMsg == null ? "" : ": " + extraMsg));
        this.errorCode = errorCode;
        this.extraMsg = extraMsg;
    }

    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode);
    }
}
