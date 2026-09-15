# -*- coding: utf-8 -*-
"""
W0 工程骨架生成器 —— common 公共模块（Shared Kernel）
依据：《系统设计》v1.0 §3.2.1 模块公共约束 / §3.5.1 错误码注册表 / §3.5.2 幂等约定 /
      §4.2.t8 Outbox 表结构 / §8.2.2 日志规范（traceId + tenantId）/ 安全设计 §5.3（SB-03）
用法：python tools/gen_common.py
"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = os.path.join(ROOT, 'backend', 'common', 'src', 'main', 'java', 'com', 'campus', 'common')
RES = os.path.join(ROOT, 'backend', 'common', 'src', 'main', 'resources')

FILES = {}

# ------------------------------------------------------------------ DTO
FILES['dto/Result.java'] = '''package com.campus.common.dto;

import com.campus.common.exception.ErrorCode;
import org.slf4j.MDC;

import java.time.Instant;

/**
 * 统一返回结构（《系统设计》§3.5.1）。
 *
 * <p>成功码 {@link #SUCCESS_CODE} 为 W0 阶段补充的项目约定（上游文档只定义了错误码分段规则），
 * 需在 W1 前后端联调时与前端约定确认（W0 报告 H-02）。
 */
public record Result<T>(String code, String msg, T data, String traceId, Instant timestamp) {

    public static final String SUCCESS_CODE = "000000";
    public static final String SUCCESS_MSG = "success";

    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MSG, data, currentTraceId(), Instant.now());
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getUserMsg(), null, currentTraceId(), Instant.now());
    }

    public static <T> Result<T> fail(String code, String msg) {
        return new Result<>(code, msg, null, currentTraceId(), Instant.now());
    }

    public boolean success() {
        return SUCCESS_CODE.equals(code);
    }

    private static String currentTraceId() {
        return MDC.get("traceId");
    }
}
'''

FILES['dto/BaseRequest.java'] = '''package com.campus.common.dto;

import lombok.Data;

/** 请求基类：携带 traceId 透传位（《系统设计》§3.2.1）。 */
@Data
public class BaseRequest {
    private String traceId;
}
'''

FILES['dto/PageRequest.java'] = '''package com.campus.common.dto;

/**
 * 分页请求：pageSize 上限 100（《系统设计》§4.3.3 分页上限，防爬）。
 */
public record PageRequest(Integer pageNo, Integer pageSize) {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 10;

    public int safePageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public long offset() {
        return (long) (safePageNo() - 1) * safePageSize();
    }
}
'''

FILES['dto/PageResponse.java'] = '''package com.campus.common.dto;

import java.util.List;

/** 分页响应（《系统设计》§3.2.1）。 */
public record PageResponse<T>(long total, long pages, List<T> records) {

    public static <T> PageResponse<T> of(long total, int pageSize, List<T> records) {
        long pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResponse<>(total, pages, records);
    }

    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(0, 0, List.of());
    }
}
'''

FILES['dto/UserContext.java'] = '''package com.campus.common.dto;

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
'''

FILES['dto/UserContextHolder.java'] = '''package com.campus.common.dto;

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
'''

# ------------------------------------------------------------------ 枚举
ENUMS = {
    'UserRole': ('用户角色', ['STUDENT', 'ADMIN']),
    'UserStatus': ('用户状态', ['ACTIVE', 'BANNED']),
    'GoodsStatus': ('商品状态', ['LISTED', 'SOLD', 'OFFSHELF', 'DELETED']),
    'GoodsCategory': ('商品类目', ['BOOK', 'DIGITAL', 'DAILY', 'SPORT', 'OTHER']),
    'OrderStatus': ('二手订单状态（下单事务内一步到 FROZEN，无 CREATED 落库态）', ['FROZEN', 'COMPLETED', 'CANCELLED']),
    'ReviewStatus': ('评价状态', ['PENDING', 'DONE', 'EXPIRED']),
    'BalanceFlowType': ('余额流水类型', ['FREEZE', 'UNFREEZE', 'DEDUCT', 'INCOME', 'RECHARGE', 'REWARD']),
    'LostItemType': ('失物条目类型', ['LOST', 'FOUND']),
    'LostItemStatus': ('失物条目状态', ['OPEN', 'MATCHED', 'RETURNED', 'CLOSED', 'OFFSHELF']),
    'ClaimStatus': ('认领申请状态', ['SUBMITTED', 'APPROVED', 'REJECTED', 'RETURNED', 'CLOSED']),
    'ErrandType': ('跑腿单类型', ['PICKUP', 'BUY', 'SEND']),
    'ErrandStatus': ('跑腿单状态', ['PUBLISHED', 'GRABBED', 'PICKED', 'DELIVERED',
                                   'COMPLETED', 'REASSIGNED', 'CANCELLED']),
    'ErrandSubtaskStatus': ('顺路拼单子任务状态', ['OPEN', 'ACCEPTED', 'DONE']),
    'GrabResult': ('抢单结果（并发证据）', ['SUCCESS', 'CONFLICT']),
    'OutboxStatus': ('Outbox 事件状态', ['INIT', 'PROCESSING', 'SENT', 'RETRY', 'DEAD']),
    'NoticeBizType': ('站内信业务类型', ['ORDER', 'CLAIM', 'ERRAND', 'SYSTEM']),
    'AiAgentName': ('AI 三域 Agent', ['TRADE', 'LOSTFOUND', 'ERRAND']),
    'ToolCallResult': ('AI 工具调用结果', ['SUCCESS', 'FAILED', 'TIMEOUT', 'REJECTED_BY_WHITELIST']),
    'AuditBizType': ('审核业务类型', ['GOODS', 'LOST_ITEM', 'CLAIM', 'REPORT']),
    'AuditDecision': ('审核处置决定', ['APPROVE', 'REJECT', 'TAKEDOWN']),
    'AuditExecStatus': ('审核执行状态', ['PENDING', 'EXECUTED', 'FAILED']),
}

ENUM_TMPL = '''package com.campus.common.enums;

/**
 * {desc}（取值与《系统设计》§4.1 状态枚举、§3.3 核心业务对象清单一致）。
 */
public enum {name} {{
{values}
}}
'''

ENUM_VALUE = '    /** {text} */\n    {value},\n'

for name, (desc, values) in ENUMS.items():
    body = ''
    for v in values:
        body += ENUM_VALUE.format(text=v.lower(), value=v)
    FILES['enums/%s.java' % name] = ENUM_TMPL.format(name=name, desc=desc, values=body)

# ------------------------------------------------------------------ 异常
FILES['exception/ErrorCode.java'] = '''package com.campus.common.exception;

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
'''

FILES['exception/BizException.java'] = '''package com.campus.common.exception;

import lombok.Getter;

/** 业务异常：携带 {@link ErrorCode}，由全局异常处理器统一转 Result（《系统设计》§3.5.1）。 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getDesc());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String extraMsg) {
        super(errorCode.getDesc() + ": " + extraMsg);
        this.errorCode = errorCode;
    }

    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode);
    }
}
'''

FILES['exception/GlobalExceptionHandler.java'] = '''package com.campus.common.exception;

import com.campus.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理（《系统设计》§3.5.1 / 安全设计 §4.2）：
 * 禁止向前端泄露堆栈、SQL、路径与版本信息。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("biz error code={} desc={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.fail(e.getErrorCode());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidation(BindException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("param invalid: {}", detail);
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(),
                ErrorCode.PARAM_INVALID.getUserMsg() + (detail.isEmpty() ? "" : "（" + detail + "）"));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public Result<Void> handleBadRequest(Exception e) {
        log.warn("bad request: {}", e.getMessage());
        return Result.fail(ErrorCode.PARAM_INVALID);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        // 堆栈只落服务端日志，不返回前端
        log.error("unexpected error", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
'''

# ------------------------------------------------------------------ Outbox
FILES['outbox/OutboxEvent.java'] = '''package com.campus.common.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Outbox 待投递事件（《系统设计》§4.2.t8，各业务库同构）。
 *
 * <p>业务服务在本地事务内写入本表（见 {@link OutboxTemplate}），
 * notify-service 通过原子领取 UPDATE 抢占投递。
 */
@Data
@TableName("t_outbox_event")
public class OutboxEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件唯一键（防重复投递） */
    private String eventId;

    /** 事件类型（Published Language Schema） */
    private String eventType;

    /** 业务聚合 ID */
    private String aggregateId;

    /** 接收人 */
    private Long targetUserId;

    /** JSON 业务载荷 */
    private String payload;

    /** INIT / PROCESSING / SENT / RETRY / DEAD */
    private String status;

    /** 领取实例标识（原子领取） */
    private String owner;

    /** 重试次数（≥ 5 → DEAD） */
    private Integer retryCount;

    /** 下次可领取时间（退避） */
    private LocalDateTime nextRetryTime;

    /** 投递成功时间（成功证据，V5） */
    private LocalDateTime sentTime;

    /** 事件发生时间 */
    private LocalDateTime createdTime;
}
'''

FILES['outbox/OutboxEventMapper.java'] = '''package com.campus.common.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** Outbox 事件表 Mapper（各业务服务扫描到自己的库）。 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {
}
'''

FILES['outbox/OutboxTemplate.java'] = '''package com.campus.common.outbox;

import com.campus.common.enums.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox 写入组件（《系统设计》§2.2.2 C-05 / §4.2.t8）。
 *
 * <p>必须在业务本地事务内调用：与业务数据同事务写入，保证"业务成功则事件必存在"，
 * 是 V5「通知投递成功率 ≥ 99%」的源头保障。
 */
@Slf4j
@RequiredArgsConstructor
public class OutboxTemplate {

    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, String aggregateId, Long targetUserId, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("outbox payload serialize failed: " + eventType, e);
        }
        if (json.length() > 1024) {
            // payload 列 VARCHAR(1024)，超长直接截断前落日志，避免事务因字段溢出失败
            log.warn("outbox payload truncated, eventType={} len={}", eventType, json.length());
            json = json.substring(0, 1024);
        }
        OutboxEvent event = new OutboxEvent();
        event.setEventId(UUID.randomUUID().toString().replace("-", ""));
        event.setEventType(eventType);
        event.setAggregateId(aggregateId);
        event.setTargetUserId(targetUserId);
        event.setPayload(json);
        event.setStatus(OutboxStatus.INIT.name());
        event.setRetryCount(0);
        event.setCreatedTime(LocalDateTime.now());
        outboxEventMapper.insert(event);
    }
}
'''

# ------------------------------------------------------------------ 幂等
FILES['idempotent/Idempotent.java'] = '''package com.campus.common.idempotent;

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
'''

FILES['idempotent/IdempotentAspect.java'] = '''package com.campus.common.idempotent;

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
'''

# ------------------------------------------------------------------ 日志
FILES['log/MdcFilter.java'] = '''package com.campus.common.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * traceId / tenantId MDC 过滤器（《系统设计》§8.2.2）。
 *
 * <p>traceId 优先取上游 X-Trace-Id（网关生成），否则本地生成；
 * tenantId 取 school_code，默认 CAMPUS-MAIN。
 */
public class MdcFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String TENANT_ID = "tenantId";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_SCHOOL_CODE = "X-School-Code";
    public static final String DEFAULT_SCHOOL_CODE = "CAMPUS-MAIN";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        String schoolCode = request.getHeader(HEADER_SCHOOL_CODE);
        if (schoolCode == null || schoolCode.isEmpty()) {
            schoolCode = DEFAULT_SCHOOL_CODE;
        }
        MDC.put(TRACE_ID, traceId);
        MDC.put(TENANT_ID, schoolCode);
        response.setHeader(HEADER_TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(TENANT_ID);
        }
    }
}
'''

# ------------------------------------------------------------------ 自动配置
FILES['config/CommonAutoConfiguration.java'] = '''package com.campus.common.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.common.exception.GlobalExceptionHandler;
import com.campus.common.idempotent.IdempotentAspect;
import com.campus.common.log.MdcFilter;
import com.campus.common.outbox.OutboxEventMapper;
import com.campus.common.outbox.OutboxTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * common 模块自动配置（仅装配各服务共用的基础设施 Bean）。
 *
 * <p>通过 META-INF/spring/...AutoConfiguration.imports 生效，业务服务无需手动 @Import。
 */
@Configuration(proxyBeanMethods = false)
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean
    public MdcFilter mdcFilter() {
        return new MdcFilter();
    }

    @Bean
    @ConditionalOnClass(BaseMapper.class)
    @ConditionalOnMissingBean
    public OutboxTemplate outboxTemplate(OutboxEventMapper outboxEventMapper, ObjectMapper objectMapper) {
        return new OutboxTemplate(outboxEventMapper, objectMapper);
    }

    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(StringRedisTemplate stringRedisTemplate,
                                             HttpServletRequest request) {
        return new IdempotentAspect(stringRedisTemplate, request);
    }
}
'''

FILES['@RES@META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports'] = (
    'com.campus.common.config.CommonAutoConfiguration\n'
)

count = 0
for rel, content in FILES.items():
    if rel.startswith('@RES@'):
        full = os.path.join(RES, rel[len('@RES@'):])
    else:
        full = os.path.join(BASE, rel)
    full = os.path.normpath(full)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)
    count += 1

print('common 生成文件数：%d' % count)
