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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
 *
 * <p>类隔离约定（W1 实测教训）：Servlet/WebMvc 专用 Bean 收敛在 {@link ServletBeans}
 * （@ConditionalOnClass 类级隔离），确保 gateway 等 WebFlux 服务加载本配置类时
 * 不会因 classpath 缺失 WebMvc/Servlet 类型而在类加载阶段失败。
 */
@Configuration(proxyBeanMethods = false)
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /** WebFlux 版 MDC 过滤器（gateway 用；Servlet 服务用 {@link ServletBeans#mdcFilter()}） */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnMissingBean
    public TraceMdcWebFilter traceMdcWebFilter() {
        return new TraceMdcWebFilter();
    }

    /**
     * MyBatis-Plus 存在时装配 Outbox 模板。
     * OutboxEventMapper 需由业务服务 @MapperScan 扫描 com.campus.common.outbox 才有实例；
     * 未扫描的服务经 @ConditionalOnBean 跳过装配，不影响启动。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(BaseMapper.class)
    static class MybatisBeans {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(OutboxEventMapper.class)
        public OutboxTemplate outboxTemplate(OutboxEventMapper outboxEventMapper, ObjectMapper objectMapper) {
            return new OutboxTemplate(outboxEventMapper, objectMapper);
        }
    }

    /**
     * Servlet/WebMvc 专用 Bean：仅 Servlet Web 应用且 WebMvcConfigurer 在 classpath 时装配。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(WebMvcConfigurer.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class ServletBeans {

        @Bean
        @ConditionalOnMissingBean
        public MdcFilter mdcFilter() {
            return new MdcFilter();
        }

        /** 网关透传头 → UserContext 拦截器（登录鉴权公共能力，《系统设计》§3.4） */
        @Bean
        public WebMvcConfigurer userContextInterceptorConfigurer() {
            return new WebMvcConfigurer() {
                @Override
                public void addInterceptors(InterceptorRegistry registry) {
                    registry.addInterceptor(new UserContextInterceptor());
                }
            };
        }

        @Bean
        @ConditionalOnMissingBean
        public IdempotentAspect idempotentAspect(StringRedisTemplate stringRedisTemplate) {
            return new IdempotentAspect(stringRedisTemplate);
        }
    }
}
