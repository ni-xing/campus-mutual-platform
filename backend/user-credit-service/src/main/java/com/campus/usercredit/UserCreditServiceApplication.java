package com.campus.usercredit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * M1 用户与信用服务：注册登录 / 信用分 / 隐私同意
 *
 * <p>Owner：季祥浩（单人项目）｜模块归属见《系统设计》§3.2.2。
 * <p>W0 阶段为可编译空壳；W1 起按实施计划逐周接入注册发现、网关路由与业务实现。
 */
@SpringBootApplication
public class UserCreditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserCreditServiceApplication.class, args);
    }
}
