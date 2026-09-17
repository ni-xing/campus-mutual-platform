package com.campus.gateway.filter;

import com.campus.common.jwt.JwtSupport;
import com.campus.gateway.config.GatewayAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.campus.common.dto.Result;
import com.campus.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关 JWT 统一鉴权过滤器（specs/05 U-08；《安全设计》§1.1 冻结结论）。
 *
 * <p>行为：
 * <ul>
 *   <li>/internal/** 直接 404（不暴露内部接口存在性，AC-9）</li>
 *   <li>白名单路径放行（注册/登录/健康检查）</li>
 *   <li>其余路径校验 JWT：缺失/无效 401（C000002），jti 命中黑名单 401（登出/互踢，AC-6/AC-7）</li>
 *   <li>校验通过透传 X-User-Id / X-User-Role / X-School-Code / X-Jti / X-Trace-Id，
 *       并剥离客户端伪造的同类头后再写入（防伪造透传头）</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> INTERNAL_PATTERNS = List.of("/internal/**");
    private static final List<String> STRIP_HEADERS = List.of(
            "X-User-Id", "X-User-Role", "X-School-Code", "X-Jti", "X-Trace-Id");

    private final JwtSupport jwtSupport;
    private final GatewayAuthProperties authProperties;
    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public JwtAuthGlobalFilter(JwtSupport jwtSupport,
                               GatewayAuthProperties authProperties,
                               ReactiveStringRedisTemplate redis) {
        this.jwtSupport = jwtSupport;
        this.authProperties = authProperties;
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. /internal/** 网关直接 404（AC-9 后半）
        if (INTERNAL_PATTERNS.stream().anyMatch(p -> MATCHER.match(p, path))) {
            log.info("GW_INTERNAL_BLOCKED path={}", path);
            return writeJson(exchange, HttpStatus.NOT_FOUND,
                    Result.fail(ErrorCode.PARAM_INVALID.getCode(), "Not Found"));
        }

        // 2. 白名单放行
        if (authProperties.getWhitelist().stream().anyMatch(w -> MATCHER.match(w, path))) {
            return chain.filter(exchange);
        }

        // 3. JWT 校验（AC-9 前半：无有效 Token 返回 401）
        String authorization = request.getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, Result.fail(ErrorCode.UNAUTHORIZED));
        }
        Claims claims;
        try {
            claims = jwtSupport.parse(authorization.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, Result.fail(ErrorCode.UNAUTHORIZED));
        }

        String jti = claims.get(JwtSupport.CLAIM_JTI, String.class);
        Object userId = claims.get(JwtSupport.CLAIM_USER_ID);
        Object role = claims.get(JwtSupport.CLAIM_ROLE);
        Object schoolCode = claims.get(JwtSupport.CLAIM_SCHOOL_CODE);

        // 4. jti 黑名单校验（登出/互踢，AC-6/AC-7）
        if (jti != null) {
            return redis.hasKey("common:jwt:blacklist:" + jti)
                    .flatMap(blacklisted -> {
                        if (Boolean.TRUE.equals(blacklisted)) {
                            return writeJson(exchange, HttpStatus.UNAUTHORIZED,
                                    Result.fail(ErrorCode.UNAUTHORIZED));
                        }
                        return forward(exchange, chain, userId, role, schoolCode, jti);
                    });
        }
        return forward(exchange, chain, userId, role, schoolCode, jti);
    }

    /** 校验通过：剥离客户端伪造头 → 写入网关透传头 → 放行 */
    private Mono<Void> forward(ServerWebExchange exchange, GatewayFilterChain chain,
                               Object userId, Object role, Object schoolCode, String jti) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        // 先剥离客户端可能伪造的透传头，再写入网关解析结果（防伪造）
        STRIP_HEADERS.forEach(h -> builder.headers(headers -> headers.remove(h)));
        if (userId != null) {
            builder.header("X-User-Id", String.valueOf(userId));
        }
        if (role != null) {
            builder.header("X-User-Role", String.valueOf(role));
        }
        if (schoolCode != null) {
            builder.header("X-School-Code", String.valueOf(schoolCode));
        }
        if (jti != null) {
            builder.header("X-Jti", jti);
        }
        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, Result<?> body) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = "{\"code\":\"B000001\",\"msg\":\"error\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 先于路由限流过滤器执行，鉴权失败不计入下游配额
        return -100;
    }
}
