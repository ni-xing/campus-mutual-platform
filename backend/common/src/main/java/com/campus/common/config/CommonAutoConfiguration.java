package com.campus.common.config;

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
