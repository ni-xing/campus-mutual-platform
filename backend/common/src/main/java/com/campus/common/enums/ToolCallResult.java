package com.campus.common.enums;

/**
 * AI 工具调用结果（取值与《系统设计》§4.1 状态枚举、§3.3 核心业务对象清单一致）。
 */
public enum ToolCallResult {
    /** success */
    SUCCESS,
    /** failed */
    FAILED,
    /** timeout */
    TIMEOUT,
    /** rejected_by_whitelist */
    REJECTED_BY_WHITELIST,

}
