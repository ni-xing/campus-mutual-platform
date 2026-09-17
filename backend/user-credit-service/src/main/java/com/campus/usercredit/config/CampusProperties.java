package com.campus.usercredit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * M1 业务配置项（campus.* 前缀）。
 *
 * <p>邮箱域名白名单 / 登录锁定阈值 / 隐私政策版本均可配置化，
 * 新增学校或调整策略只改配置（Nacos 配置中心下发），不改代码。
 */
@Data
@ConfigurationProperties(prefix = "campus")
public class CampusProperties {

    private Jwt jwt = new Jwt();
    private Email email = new Email();
    private Login login = new Login();
    private Privacy privacy = new Privacy();

    @Data
    public static class Jwt {
        /** HS256 密钥（≥256bit，env JWT_SECRET 注入，L4 凭证不入 Git） */
        private String secret;
        /** Token 有效期（小时），《安全设计》§2.1.3 冻结 2h */
        private int expireHours = 2;
    }

    @Data
    public static class Email {
        /** 校园邮箱域名白名单 */
        private List<String> allowedDomains = List.of("stu.campus.edu.cn");
    }

    @Data
    public static class Login {
        /** 连续失败锁定阈值 */
        private int maxFail = 5;
        /** 锁定时长（分钟） */
        private int lockMinutes = 15;
    }

    @Data
    public static class Privacy {
        /** 当前隐私政策版本（注册留痕） */
        private String currentVersion = "v1.0";
    }
}
