package com.campus.common.web;

import com.campus.common.dto.UserContext;
import com.campus.common.dto.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 服务端用户上下文拦截器（《系统设计》§3.4 登录鉴权公共能力）。
 *
 * <p>读取网关透传的 X-User-Id / X-User-Role / X-School-Code 写入 ThreadLocal，
 * 请求结束清理。透传头仅信任来自网关的流量（同信任域内网，接口签名已裁剪）。
 */
public class UserContextInterceptor implements HandlerInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_SCHOOL_CODE = "X-School-Code";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String userId = request.getHeader(HEADER_USER_ID);
        if (userId != null && !userId.isEmpty()) {
            UserContextHolder.set(new UserContext(
                    Long.valueOf(userId),
                    request.getHeader(HEADER_USER_ROLE),
                    request.getHeader(HEADER_SCHOOL_CODE),
                    request.getHeader("X-Jti")));
        }
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
