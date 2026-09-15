package com.campus.common.enums;

/**
 * 抢单结果（并发证据）（取值与《系统设计》§4.1 状态枚举、§3.3 核心业务对象清单一致）。
 */
public enum GrabResult {
    /** success */
    SUCCESS,
    /** conflict */
    CONFLICT,

}
