package com.campus.gateway.config;

import com.campus.common.jwt.JwtSupport;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 网关基础设施配置：JwtSupport Bean + 限流 KeyResolver。
 */
@Configuration
@EnableConfigurationProperties(GatewayAuthProperties.class)
public class GatewayConfig {

    @Bean
    public JwtSupport jwtSupport(GatewayAuthProperties properties,
                                 org.springframework.core.env.Environment env) {
        String secret = env.getProperty("campus.jwt.secret");
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret 缺失或长度不足 32 字节（env JWT_SECRET，>=256bit）");
        }
        return new JwtSupport(secret, Duration.ofHours(2));
    }

    /** 单 IP 限流键（60 QPS，重保接口维度；X-Forwarded-First 取真实客户端 IP） */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String xff = request.getHeaders().getFirst("X-Forwarded-For");
            String ip = xff != null ? xff.split(",")[0].trim()
                    : (request.getRemoteAddress() != null
                    ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown");
            return Mono.just("ip:" + ip);
        };
    }

    /** 全局限流键（500 QPS，单机全站水位） */
    @Bean
    public KeyResolver globalKeyResolver() {
        return exchange -> Mono.just("global");
    }
}
