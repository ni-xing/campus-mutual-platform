package com.campus.common.enums;

/**
 * Outbox 事件状态（取值与《系统设计》§4.1 状态枚举、§3.3 核心业务对象清单一致）。
 */
public enum OutboxStatus {
    /** init */
    INIT,
    /** processing */
    PROCESSING,
    /** sent */
    SENT,
    /** retry */
    RETRY,
    /** dead */
    DEAD,

}
