package com.campus.common.enums;

/**
 * 失物条目状态（取值与《系统设计》§4.1 状态枚举、§3.3 核心业务对象清单一致）。
 */
public enum LostItemStatus {
    /** open */
    OPEN,
    /** matched */
    MATCHED,
    /** returned */
    RETURNED,
    /** closed */
    CLOSED,
    /** offshelf */
    OFFSHELF,

}
