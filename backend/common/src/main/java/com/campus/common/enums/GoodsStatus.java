package com.campus.common.enums;

/**
 * 商品状态（取值与《系统设计》§4.1 状态枚举、§3.3 核心业务对象清单一致）。
 */
public enum GoodsStatus {
    /** listed */
    LISTED,
    /** sold */
    SOLD,
    /** offshelf */
    OFFSHELF,
    /** deleted */
    DELETED,

}
