package com.campus.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权配置（campus-gateway.auth.*）：JWT 白名单路径。
 */
@Data
@ConfigurationProperties(prefix = "campus-gateway.auth")
public class GatewayAuthProperties {

    /** 无需 JWT 即可访问的路径（精确匹配） */
    private List<String> whitelist = new ArrayList<>();
}
