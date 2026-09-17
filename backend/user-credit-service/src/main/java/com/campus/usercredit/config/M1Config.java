package com.campus.usercredit.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.campus.common.jwt.JwtSupport;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * M1 基础设施配置：MyBatis-Plus 乐观锁插件 + JwtSupport Bean。
 */
@Configuration
@EnableConfigurationProperties(CampusProperties.class)
public class M1Config {

    /** 乐观锁统一配置（信用分更新 / t_user_account.version，《系统设计》§3.4） */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Bean
    public JwtSupport jwtSupport(CampusProperties properties) {
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret 缺失或长度不足 32 字节（env JWT_SECRET，>=256bit）");
        }
        return new JwtSupport(secret, Duration.ofHours(properties.getJwt().getExpireHours()));
    }
}
