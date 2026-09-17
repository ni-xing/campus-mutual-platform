package com.campus.common.idempotent;

import com.campus.common.exception.BizException;
import com.campus.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * 幂等切面（《系统设计》§3.5.2 幂等设计五问落地）。
 *
 * <p>SETNX 占位成功 → 执行业务；占位失败 → 返回 C000004「处理中请稍候」，前端退避重查；
 * 业务失败撤销占位，允许同键重试。
 *
 * <p>注：request 经 RequestContextHolder 懒获取（HttpServletRequest 是请求作用域对象，
 * 不能作为 bean 构造参数注入）。
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class IdempotentAspect {

    private static final String KEY_PREFIX = "campus:idem:";

    private final StringRedisTemplate stringRedisTemplate;

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
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getHeader("X-Idempotency-Key");
        }
        return null;
    }
}
