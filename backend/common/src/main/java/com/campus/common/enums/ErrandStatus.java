package com.campus.common.enums;

/**
 * 跑腿单状态（取值与《系统设计》§4.1 状态枚举、§3.3 核心业务对象清单一致）。
 */
public enum ErrandStatus {
    /** published */
    PUBLISHED,
    /** grabbed */
    GRABBED,
    /** picked */
    PICKED,
    /** delivered */
    DELIVERED,
    /** completed */
    COMPLETED,
    /** reassigned */
    REASSIGNED,
    /** cancelled */
    CANCELLED,

}
