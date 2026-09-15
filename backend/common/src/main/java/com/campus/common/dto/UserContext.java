package com.campus.common.dto;

/**
 * 网关解析 JWT 后透传的用户上下文（《系统设计》§3.2.1 / §7.2.1）。
 *
 * @param userId     用户 ID
 * @param role       STUDENT / ADMIN
 * @param schoolCode 学校维度（日志 tenantId 取值即此字段，§8.2.2）
 * @param jti        Token 唯一标识（互踢黑名单校验用）
 */
public record UserContext(Long userId, String role, String schoolCode, String jti) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
