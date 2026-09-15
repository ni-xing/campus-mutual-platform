package com.campus.common.enums;

/**
 * 二手订单状态（下单事务内一步到 FROZEN，无 CREATED 落库态）（取值与《系统设计》§4.1 状态枚举、§3.3 核心业务对象清单一致）。
 */
public enum OrderStatus {
    /** frozen */
    FROZEN,
    /** completed */
    COMPLETED,
    /** cancelled */
    CANCELLED,

}
