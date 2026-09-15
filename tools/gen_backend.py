# -*- coding: utf-8 -*-
"""
W0 工程骨架生成器 —— 后端多模块 Maven 工程（父 POM + 9 模块 + 启动类 + 配置）
依据：《系统设计》v1.0 §3.1.4 工程结构 / §3.2.2 模块清单 / §3.5.3 限流 / 安全设计 SB-03
用法：python tools/gen_backend.py
说明：W0 阶段服务模块为"可编译空壳"，只引 Spring Boot 原生 starter；
      Nacos/Gateway/Sentinel/Dubbo 等 SCA 组件在 W1 按 §3.1.5 版本矩阵接入（见 W0 报告风险项 R-01）。
"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BE = os.path.join(ROOT, 'backend')

POM_HEAD = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
'''

# ---------------------------------------------------------------- 父 POM
PARENT_POM = POM_HEAD + '''  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.5</version>
    <relativePath/>
  </parent>

  <groupId>com.campus</groupId>
  <artifactId>campus-mutual-platform</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <name>campus-mutual-platform</name>
  <description>校园互助生活平台 · 微服务后端（M1~M7 + 网关）</description>

  <modules>
    <module>common</module>
    <module>gateway</module>
    <module>user-credit-service</module>
    <module>trade-service</module>
    <module>lostfound-service</module>
    <module>errand-service</module>
    <module>ai-assistant-service</module>
    <module>notify-service</module>
    <module>admin-service</module>
  </modules>

  <properties>
    <java.version>21</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

    <mybatis-plus.version>3.5.9</mybatis-plus.version>
    <mysql-connector.version>8.4.0</mysql-connector.version>
    <jjwt.version>0.12.6</jjwt.version>
    <redisson.version>3.30.0</redisson.version>
    <knife4j.version>4.5.0</knife4j.version>

    <spring-cloud.version>2024.0.0</spring-cloud.version>
    <spring-cloud-alibaba.version>2023.0.3.2</spring-cloud-alibaba.version>
    <dubbo.version>3.3.3</dubbo.version>
    <spring-ai.version>1.0.0</spring-ai.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
      </dependency>
      <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>${mysql-connector.version}</version>
      </dependency>
      <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>${jjwt.version}</version>
      </dependency>
      <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>${jjwt.version}</version>
      </dependency>
      <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>${jjwt.version}</version>
      </dependency>
      <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>${redisson.version}</version>
      </dependency>
      <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>${knife4j.version}</version>
      </dependency>
      <dependency>
        <groupId>com.campus</groupId>
        <artifactId>campus-common</artifactId>
        <version>${project.version}</version>
      </dependency>

      <!-- ============================================================
           W1 接入项（本阶段不激活，避免与 W0 空壳编译强耦合）
           接入前必须先验证版本矩阵：Spring Boot 3.4.5 与
           Spring Cloud Alibaba 2023.0.3.2 的官方适配组合需实测；
           备选路径见 docs 的 W0 交付报告风险项 R-01。
           <dependency>
             <groupId>org.springframework.cloud</groupId>
             <artifactId>spring-cloud-dependencies</artifactId>
             <version>${spring-cloud.version}</version>
             <type>pom</type><scope>import</scope>
           </dependency>
           <dependency>
             <groupId>com.alibaba.cloud</groupId>
             <artifactId>spring-cloud-alibaba-dependencies</artifactId>
             <version>${spring-cloud-alibaba.version}</version>
             <type>pom</type><scope>import</scope>
           </dependency>
           ============================================================ -->
    </dependencies>
  </dependencyManagement>

  <!-- 子模块统一继承 lombok（版本由 spring-boot-starter-parent 管理） -->
  <dependencies>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <scope>provided</scope>
    </dependency>
  </dependencies>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-maven-plugin</artifactId>
          <configuration>
            <excludes>
              <exclude>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
              </exclude>
            </excludes>
          </configuration>
        </plugin>
      </plugins>
    </pluginManagement>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <release>${java.version}</release>
          <parameters>true</parameters>
          <encoding>UTF-8</encoding>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
'''

# ---------------------------------------------------------------- common POM
COMMON_POM = POM_HEAD + '''  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.campus</groupId>
    <artifactId>campus-mutual-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>campus-common</artifactId>
  <name>campus-common</name>
  <description>公共模块（Shared Kernel）：Result / 异常与错误码 / 枚举 / OutboxTemplate / 幂等 / MDC</description>

  <dependencies>
    <!-- optional：避免向 gateway（WebFlux）反向传递 Servlet 栈 -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-aop</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-redis</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>com.baomidou</groupId>
      <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
    </dependency>
  </dependencies>
</project>
'''

# ---------------------------------------------------------------- 服务定义
# (模块目录, 应用名, 端口, 描述, 附加依赖 XML)
SERVICES = [
    ('gateway', 'campus-gateway', 8080, 'API 网关：路由 + JWT 统一鉴权 + 全局限流', 'webflux'),
    ('user-credit-service', 'user-credit-service', 8081, 'M1 用户与信用服务：注册登录 / 信用分 / 隐私同意', 'mvc'),
    ('trade-service', 'trade-service', 8082, 'M2 二手交易服务：商品 / 订单 / 余额支付', 'mvc'),
    ('lostfound-service', 'lostfound-service', 8083, 'M3 失物招领服务：失物拾物 / 认领审核', 'mvc'),
    ('errand-service', 'errand-service', 8084, 'M4 跑腿拼单服务：发单 / 并发抢单 / 履约（含 Dubbo Provider）', 'mvc'),
    ('ai-assistant-service', 'ai-assistant-service', 8085, 'M5 AI 助手服务：三域 Agent / 工具白名单 / 后置断言', 'mvc'),
    ('notify-service', 'notify-service', 8086, 'M6 通知服务：Outbox 轮询投递 / 站内信', 'mvc'),
    ('admin-service', 'admin-service', 8087, 'M7 平台管理服务：审核 / 举报 / 下架 / 敏感词', 'mvc'),
]

SERVICE_POM = POM_HEAD + '''  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.campus</groupId>
    <artifactId>campus-mutual-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>{artifact}</artifactId>
  <name>{artifact}</name>
  <description>{desc}</description>

  <dependencies>
    <dependency>
      <groupId>com.campus</groupId>
      <artifactId>campus-common</artifactId>
    </dependency>
{extra_deps}  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
'''

WEBFLUX_DEPS = '''    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
'''

MVC_DEPS = '''    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>com.baomidou</groupId>
      <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
'''

APP_CLASS = '''package com.campus.{pkg};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * {desc}
 *
 * <p>Owner：季祥浩（单人项目）｜模块归属见《系统设计》§3.2.2。
 * <p>W0 阶段为可编译空壳；W1 起按实施计划逐周接入注册发现、网关路由与业务实现。
 */
@SpringBootApplication
public class {cls} {{

    public static void main(String[] args) {{
        SpringApplication.run({cls}.class, args);
    }}
}}
'''

APP_YML = '''# {desc}
# 环境变量统一由 docker/.env 注入（L4 Secret 不入代码，见《安全设计》§6）
server:
  port: {port}

spring:
  application:
    name: {appname}
  profiles:
    active: ${{SPRING_PROFILES_ACTIVE:dev}}
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${{DB_HOST:127.0.0.1}}:${{DB_PORT:3306}}/{db}?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
    username: ${{DB_USER:{dbuser}}}
    password: ${{DB_PWD:}}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 3000
  data:
    redis:
      host: ${{REDIS_HOST:127.0.0.1}}
      port: ${{REDIS_PORT:6379}}
      password: ${{REDIS_PASSWORD:}}
      timeout: 3000ms

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# SB-03：Actuator 仅暴露 liveness/readiness，管理端口不映射公网（《安全设计》§5.3）
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never

logging:
  level:
    root: info
    com.campus: debug
'''

GATEWAY_YML = '''# API 网关：路由 + JWT 统一鉴权 + 全局限流（W1 接入 spring-cloud-starter-gateway）
server:
  port: 8080

spring:
  application:
    name: campus-gateway
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never

logging:
  level:
    root: info
    com.campus: debug
'''

GITIGNORE = '''# ---- 构建产物 ----
target/
*.jar
*.war
!.mvn/wrapper/maven-wrapper.jar

# ---- IDE ----
.idea/
*.iml
.vscode/
*.swp

# ---- 环境与密钥（红线：L4 Secret 一律不入库，《安全设计》§6.3）----
.env
.env.local
*.env
!*.env.example
**/application-local.yml
**/application-prod.yml

# ---- 本地运行产物 ----
logs/
*.log
docker/data/
docker/logs/

# ---- 前端 ----
node_modules/
dist/
.vite/
'''

DB_OF_SERVICE = {
    'user-credit-service': ('user_db', 'campus_user'),
    'trade-service': ('trade_db', 'campus_trade'),
    'lostfound-service': ('lostfound_db', 'campus_lostfound'),
    'errand-service': ('errand_db', 'campus_errand'),
    'ai-assistant-service': ('ai_db', 'campus_ai'),
    'notify-service': ('notify_db', 'campus_notify'),
    'admin-service': ('admin_db', 'campus_admin'),
}

CLS_OF_SERVICE = {
    'gateway': ('Gateway', 'gateway'),
    'user-credit-service': ('UserCreditService', 'usercredit'),
    'trade-service': ('TradeService', 'trade'),
    'lostfound-service': ('LostfoundService', 'lostfound'),
    'errand-service': ('ErrandService', 'errand'),
    'ai-assistant-service': ('AiAssistantService', 'ai'),
    'notify-service': ('NotifyService', 'notify'),
    'admin-service': ('AdminService', 'admin'),
}


def write(path, content):
    full = os.path.join(BE, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)
    return full


count = 0
write('pom.xml', PARENT_POM)
count += 1
write('common/pom.xml', COMMON_POM)
count += 1

for mod, appname, port, desc, kind in SERVICES:
    cls_base, pkg = CLS_OF_SERVICE[mod]
    cls = cls_base + 'Application'
    extra = WEBFLUX_DEPS if kind == 'webflux' else MVC_DEPS
    write('%s/pom.xml' % mod, SERVICE_POM.format(artifact=mod, desc=desc, extra_deps=extra))
    count += 1
    write('%s/src/main/java/com/campus/%s/%s.java' % (mod, pkg, cls),
          APP_CLASS.format(pkg=pkg, cls=cls, desc=desc))
    count += 1
    if mod == 'gateway':
        yml = GATEWAY_YML
    else:
        db, dbuser = DB_OF_SERVICE[mod]
        yml = APP_YML.format(desc=desc, port=port, appname=appname, db=db, dbuser=dbuser)
    write('%s/src/main/resources/application.yml' % mod, yml)
    count += 1

write('../.gitignore', GITIGNORE)
count += 1

print('生成文件数：%d' % count)
print('输出目录：%s' % BE)
