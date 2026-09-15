package com.campus.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * M7 平台管理服务：审核 / 举报 / 下架 / 敏感词
 *
 * <p>Owner：季祥浩（单人项目）｜模块归属见《系统设计》§3.2.2。
 * <p>W0 阶段为可编译空壳；W1 起按实施计划逐周接入注册发现、网关路由与业务实现。
 */
@SpringBootApplication
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
