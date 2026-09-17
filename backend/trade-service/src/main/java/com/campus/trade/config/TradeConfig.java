package com.campus.trade.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.campus.common.outbox.OutboxEventMapper;
import com.campus.common.outbox.OutboxTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * M2 基础设施配置：MyBatis-Plus 乐观锁插件（实体 updateById 场景；
 * 资金/库存走条件 UPDATE + 行数判断，见各 Mapper）+ Outbox 模板。
 *
 * <p>OutboxTemplate 在此显式装配而非依赖 common 自动配置：common 的
 * {@code @ConditionalOnBean(OutboxEventMapper)} 在 auto-config 评估时看不到
 * {@code @MapperScan} 经 BeanDefinitionRegistryPostProcessor 注册的 mapper 定义（W1 同款坑）。
 */
@Configuration
public class TradeConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Bean
    public OutboxTemplate outboxTemplate(OutboxEventMapper outboxEventMapper, ObjectMapper objectMapper) {
        return new OutboxTemplate(outboxEventMapper, objectMapper);
    }
}
