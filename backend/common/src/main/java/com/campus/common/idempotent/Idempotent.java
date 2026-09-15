package com.campus.common.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等注解（《系统设计》§3.5.2）。
 *
 * <p>幂等键来源见 {@link #key()}：支持 SpEL 取值或请求头。
 * 资金类（冻结/扣减/划转/充值）与并发竞争类（抢单/接子任务）强制使用；
 * DB 唯一索引仍是 Redis 幂等键丢失后的最终防线。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 幂等键来源：SpEL 表达式，如 "#req.orderNo"；留空则取请求头 X-Idempotency-Key */
    String key() default "";

    /** Redis 幂等占位 TTL（秒），默认 24h */
    long ttlSeconds() default 86400L;

    /** 重复请求时的提示语 */
    String message() default "请勿重复提交";
}
