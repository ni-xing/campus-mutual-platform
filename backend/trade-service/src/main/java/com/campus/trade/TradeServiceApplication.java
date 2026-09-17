package com.campus.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * M2 二手交易服务：商品 / 订单 / 余额支付（specs/01）。
 *
 * <p>Owner：季祥浩（单人项目）｜模块归属见《系统设计》§3.2.2。
 * <p>MapperScan 同时扫描 common.outbox（OutboxTemplate 事件落 trade_db）。
 */
@SpringBootApplication
@MapperScan({"com.campus.trade.mapper", "com.campus.common.outbox"})
@EnableFeignClients(basePackages = "com.campus.trade.feign")
@EnableScheduling
public class TradeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeServiceApplication.class, args);
    }
}
