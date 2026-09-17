package com.campus.common.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.common.exception.GlobalExceptionHandler;
import com.campus.common.idempotent.IdempotentAspect;
import com.campus.common.log.MdcFilter;
import com.campus.common.log.TraceMdcWebFilter;
import com.campus.common.outbox.OutboxEventMapper;
import com.campus.common.outbox.OutboxTemplate;
import com.campus.common.web.UserContextInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnMissingBean
    public TraceMdcWebFilter traceMdcWebFilter() {
        return new TraceMdcWebFilter();
    }

    /**
     * Servlet 服务：注册网关透传头 → UserContext 拦截器（登录鉴权公共能力，§3.4）。
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public WebMvcConfigurer userContextInterceptorConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new UserContextInterceptor());
            }
        };
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
