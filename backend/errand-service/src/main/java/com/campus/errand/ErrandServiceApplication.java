package com.campus.errand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * M4 跑腿拼单服务：发单 / 并发抢单 / 履约（含 Dubbo Provider）
 *
 * <p>Owner：季祥浩（单人项目）｜模块归属见《系统设计》§3.2.2。
 * <p>W0 阶段为可编译空壳；W1 起按实施计划逐周接入注册发现、网关路由与业务实现。
 */
@SpringBootApplication
public class ErrandServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErrandServiceApplication.class, args);
    }
}
