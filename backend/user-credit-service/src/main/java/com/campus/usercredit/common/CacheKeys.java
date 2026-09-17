package com.campus.usercredit.common;

/**
 * M1 Redis 键约定（specs/05 §5）。
 */
public final class CacheKeys {

    /** 登录失败计数：user:login:fail:{account} */
    public static final String LOGIN_FAIL = "user:login:fail:";

    /** 当前有效 jti 映射：common:jwt:active:{userId}，值 = jti|过期epoch秒 */
    public static final String JWT_ACTIVE = "common:jwt:active:";

    /** Token 黑名单：common:jwt:blacklist:{jti}（《安全设计》§2.1.3） */
    public static final String JWT_BLACKLIST = "common:jwt:blacklist:";

    private CacheKeys() {
    }
}
