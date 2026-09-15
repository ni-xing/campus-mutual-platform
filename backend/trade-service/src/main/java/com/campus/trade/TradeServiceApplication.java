package com.campus.trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * M2 二手交易服务：商品 / 订单 / 余额支付
 *
 * <p>Owner：季祥浩（单人项目）｜模块归属见《系统设计》§3.2.2。
 * <p>W0 阶段为可编译空壳；W1 起按实施计划逐周接入注册发现、网关路由与业务实现。
 */
@SpringBootApplication
public class TradeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeServiceApplication.class, args);
    }
}
