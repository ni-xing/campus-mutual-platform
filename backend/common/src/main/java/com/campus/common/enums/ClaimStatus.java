package com.campus.common.enums;

/**
 * 认领申请状态（取值与《系统设计》§4.1 状态枚举、§3.3 核心业务对象清单一致）。
 */
public enum ClaimStatus {
    /** submitted */
    SUBMITTED,
    /** approved */
    APPROVED,
    /** rejected */
    REJECTED,
    /** returned */
    RETURNED,
    /** closed */
    CLOSED,

}
