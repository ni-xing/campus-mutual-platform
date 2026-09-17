package com.campus.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 签发/解析统一工具（《安全设计》§2.1.3）。
 *
 * <p>HS256；签发方为 user-credit-service，校验方为 gateway。
 * claims 约定：userId / role / schoolCode / jti，有效期 2h。
 * secret ≥ 256bit，经环境变量注入，禁止硬编码。
 */
public final class JwtSupport {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_SCHOOL_CODE = "schoolCode";
    public static final String CLAIM_JTI = "jti";

    private final SecretKey key;
    private final Duration expire;

    public JwtSupport(String secret, Duration expire) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expire = expire;
    }

    /**
     * 签发 Token，返回 token 本体（jti 由调用方经由 result 取出写 Redis 会话映射）。
     */
    public SignedToken sign(long userId, String role, String schoolCode) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_ROLE, role);
        claims.put(CLAIM_SCHOOL_CODE, schoolCode);
        claims.put(CLAIM_JTI, jti);
        String token = Jwts.builder()
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expire)))
                .signWith(key)
                .compact();
        return new SignedToken(token, jti, now.plus(expire));
    }

    /**
     * 解析并验签；无效/过期抛 JjwtException（含 ExpiredJwtException）。
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /**
     * 剩余有效期（黑名单 TTL 用）；已过期返回 Duration.ZERO。
     */
    public Duration remaining(Claims claims) {
        Date exp = claims.getExpiration();
        if (exp == null) {
            return Duration.ZERO;
        }
        Duration d = Duration.between(Instant.now(), exp.toInstant());
        return d.isNegative() ? Duration.ZERO : d;
    }

    /**
     * 签发结果：token + jti + 过期时间。
     */
    public record SignedToken(String token, String jti, Instant expiresAt) {
    }
}
