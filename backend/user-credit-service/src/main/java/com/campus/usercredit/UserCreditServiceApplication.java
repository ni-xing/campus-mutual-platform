package com.campus.usercredit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * M1 用户与信用服务：注册登录 / 信用分 / 隐私同意
 *
 * <p>Owner：季祥浩（单人项目）｜模块归属见《系统设计》§3.2.2。
 * <p>W1 起实现认证主链路（specs/05-用户与认证）：注册（BCrypt + 隐私同意）、
 * 登录（失败锁定 + JWT 签发 + 互踢黑名单）、个人主页、信用分查询与内部回流。
 */
@SpringBootApplication
@MapperScan({"com.campus.usercredit.mapper", "com.campus.common.outbox"})
public class UserCreditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserCreditServiceApplication.class, args);
    }
}
