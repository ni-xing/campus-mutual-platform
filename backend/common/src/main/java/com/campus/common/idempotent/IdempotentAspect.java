package com.campus.common.idempotent;

import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 幂等切面（《系统设计》§3.5.2 幂等设计五问落地）。
 *
 * <p>SETNX 占位成功 → 执行业务；占位失败 → 返回 C000004「处理中请稍候」，前端退避重查；
 * 业务失败撤销占位，允许同键重试。
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class IdempotentAspect {

    private static final String KEY_PREFIX = "campus:idem:";

    private final StringRedisTemplate stringRedisTemplate;
    private final HttpServletRequest request;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = resolveKey(idempotent);
        if (key == null || key.isEmpty()) {
            // 未提供幂等键时不拦截（读接口或非资金类），由业务侧 DB 唯一约束兜底
            return joinPoint.proceed();
        }
        String redisKey = KEY_PREFIX + joinPoint.getSignature().toShortString() + ":" + key;
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", Duration.ofSeconds(idempotent.ttlSeconds()));
        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("idempotent hit, key={}", redisKey);
            throw new BizException(ErrorCode.RATE_LIMITED, idempotent.message());
        }
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            stringRedisTemplate.delete(redisKey);
            throw e;
        }
    }

    private String resolveKey(Idempotent idempotent) {
        if (!idempotent.key().isEmpty()) {
            return idempotent.key();
        }
        return request.getHeader("X-Idempotency-Key");
    }
}
