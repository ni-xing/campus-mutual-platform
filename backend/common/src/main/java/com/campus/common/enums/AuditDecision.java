package com.campus.common.enums;

/**
 * 审核处置决定（取值与《系统设计》§4.1 状态枚举、§3.3 核心业务对象清单一致）。
 */
public enum AuditDecision {
    /** approve */
    APPROVE,
    /** reject */
    REJECT,
    /** takedown */
    TAKEDOWN,

}
