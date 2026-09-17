package com.campus.common.exception;

import lombok.Getter;

/**
 * 全局错误码注册表（《系统设计》§3.5.1）。
 *
 * <p>格式：6 位 = 1 位类别 + 2 位模块 + 3 位错误序号。
 * 类别 A=业务错误（HTTP 200）/ B=系统错误（5xx）/ C=客户端错误（4xx）；
 * 模块 01 用户信用、02 二手交易、03 失物招领、04 跑腿拼单、05 AI 助手、
 * 06 通知、07 平台管理、00 全局通用。
 */
@Getter
public enum ErrorCode {

    // ---- M1 用户与信用 ----
    STUDENT_NO_REGISTERED("A010001", 200, "M1", "学号/邮箱已注册", "不重试", "该学号已注册过，请直接登录"),
    CREDIT_NOT_ENOUGH("A010002", 200, "M1", "信用分不足无法接单", "不重试", "信用分不足，先去完成几单互助吧"),

    // ---- M2 二手交易 ----
    GOODS_UNAVAILABLE("A020001", 200, "M2", "商品已售出或已下架", "不重试", "手慢了，该商品已售出"),
    BALANCE_NOT_ENOUGH("A020002", 200, "M2", "余额不足（含冻结额度）", "不重试", "余额不足，可去余额中心模拟充值"),
    REVIEW_DUPLICATED("A020003", 200, "M2", "重复评价", "不重试", "你已评价过本单"),
    ORDER_STATE_INVALID("A020004", 200, "M2", "订单状态不允许该操作（状态机拒绝）", "不重试", "当前订单状态无法执行该操作"),
    BUY_OWN_GOODS("A020005", 200, "M2", "不能购买自己发布的商品", "不重试", "不能下单自己发布的商品"),
    REVIEW_WINDOW_CLOSED("A020006", 200, "M2", "评价窗口已关闭（完成 + 7 天）", "不重试", "该订单已过评价期"),

    // ---- M3 失物招领 ----
    CLAIM_DUPLICATED("A030001", 200, "M3", "重复认领申请", "不重试", "你已提交过认领申请，请等待审核"),

    // ---- M4 跑腿拼单 ----
    ERRAND_ALREADY_GRABBED("A040001", 200, "M4", "单已被抢", "不重试", "手慢了，该单已被抢"),
    ERRAND_REASSIGN_LIMIT("A040002", 200, "M4", "超过转派次数上限", "不重试", "该单已多次转派，已通知发单人处理"),

    // ---- M5 AI 助手 ----
    TOOL_NOT_IN_WHITELIST("A050001", 200, "M5", "工具不在白名单", "不重试", "该操作未被授权"),
    ASSERTION_FAILED("A050002", 200, "M5", "后置断言失败（未生成确认）", "换一种问法重试", "操作未完成，请稍后重试"),

    // ---- M7 平台管理 ----
    SENSITIVE_WORD_HIT("A070001", 200, "M7", "内容命中敏感词", "修改后重试", "内容包含违规词，请修改后发布"),

    // ---- 全局系统错误 ----
    SYSTEM_ERROR("B000001", 500, "GLOBAL", "系统内部错误", "可重试（指数退避）", "系统繁忙，请稍后再试"),
    DOWNSTREAM_UNAVAILABLE("B000002", 503, "GLOBAL", "下游服务不可用（Sentinel 熔断）", "退避后重试", "服务维护中，请稍后再试"),

    // ---- 全局客户端错误 ----
    PARAM_INVALID("C000001", 400, "GLOBAL", "参数校验失败", "不重试", "参数错误，请检查填写"),
    UNAUTHORIZED("C000002", 401, "GLOBAL", "未登录 / Token 失效", "重新登录", "请重新登录"),
    FORBIDDEN("C000003", 403, "GLOBAL", "无权限（角色/数据归属）", "不重试", "无权访问"),
    RATE_LIMITED("C000004", 429, "GLOBAL", "触发限流", "按 Retry-After 退避", "操作太频繁，请稍后再试"),
    LOGIN_FAILED("C000005", 200, "M1", "账号或密码错误（防枚举语义）", "不重试", "账号或密码错误"),
    ACCOUNT_BANNED("C000006", 200, "M1", "账号已封禁", "不重试", "账号已被封禁，如有疑问联系管理员");

    private final String code;
    private final int httpStatus;
    private final String module;
    private final String desc;
    private final String retryAdvice;
    private final String userMsg;

    ErrorCode(String code, int httpStatus, String module, String desc, String retryAdvice, String userMsg) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.module = module;
        this.desc = desc;
        this.retryAdvice = retryAdvice;
        this.userMsg = userMsg;
    }
}
