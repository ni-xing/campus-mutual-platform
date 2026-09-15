package com.campus.common.enums;

/**
 * 余额流水类型（取值与《系统设计》§4.1 状态枚举、§3.3 核心业务对象清单一致）。
 */
public enum BalanceFlowType {
    /** freeze */
    FREEZE,
    /** unfreeze */
    UNFREEZE,
    /** deduct */
    DEDUCT,
    /** income */
    INCOME,
    /** recharge */
    RECHARGE,
    /** reward */
    REWARD,

}
