package com.campus.common.dto;

/**
 * 用户上下文 ThreadLocal 持有器（《系统设计》§3.2.1）。
 *
 * <p>网关透传的 X-User-Id / X-User-Role / X-School-Code 由拦截器写入，
 * 服务内部通过本类获取，避免逐层传参。
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext context) {
        HOLDER.set(context);
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    public static Long currentUserId() {
        UserContext context = HOLDER.get();
        return context == null ? null : context.userId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
