# -*- coding: utf-8 -*-
"""
W0 容器编排与密钥模板生成器
依据：《部署设计》v1.1 §2.1 环境矩阵 / §2.2 云资源清单 / §3.2 流量链路 / §4.4 配置管理 L1~L4
      《安全设计》v1.1 §5.3 主机与容器 / §6 密钥管理（SB-01~05 安全基线）
用法：python tools/gen_docker.py
说明：本地 dev 用（端口绑 127.0.0.1）；prod 参数由部署 checklist 在 W6.6 覆盖。
"""
import base64
import os
import secrets
import string

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
D = os.path.join(ROOT, 'docker')
FILES = {}

# ================================================================ docker-compose.yml
FILES['docker-compose.yml'] = """# =============================================================================
# 校园互助生活平台 · 本地开发编排（dev 环境，对应《部署设计》§2.1 dev 行）
#
#   常驻组合（5 容器，W0 即可跑通）：
#     mysql / redis / nacos / sentinel / nginx
#   业务组合（profile: app，W1 起逐个启用）：
#     gateway + M1~M7，本地开发建议直接在 IDE 运行，容器仅用于整栈联调
#   观测组合（见 docker-compose.obs.yml，profile: observability，按需启动）
#
#   内存预算（《部署设计》§2.2.4，prodtarget 2.88GB / 3GB 上限）：
#     MySQL 500M / Redis 150M / Nacos 450M / Sentinel 200M / Nginx 30M
#     Gateway 300M / M1·M3·M6·M7 160M / M2·M4 192M / M5 256M
#   本地开发机内存充裕（15.7GB），内存上限保持与生产一致，便于提前发现超预算问题。
#
#   密钥：全部从同目录 .env 注入（L4 Secret），.env 不入 Git（见 .gitignore 与《安全设计》§6.3）
# =============================================================================
name: campus-dev

x-logging: &default-logging
  driver: json-file
  options:
    max-size: "10m"
    max-file: "3"

services:

  # ---------------------------------------------------------------- MySQL 8.0.36
  mysql:
    image: mysql:8.0.36
    container_name: campus-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      TZ: Asia/Shanghai
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci
      # 应用层统一 UTC 存取（§4.1），避免时区漂移
      - --default-time-zone=+00:00
      - --innodb-buffer-pool-size=128M
      - --max-connections=200
      - --slow-query-log=1
      - --long-query-time=1
    ports:
      - "127.0.0.1:3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      # 初始化脚本按文件名顺序执行：00 建库 → 01 建账号 → 10~16 建表 → 17 审计保护 → 20 种子
      - ./sql:/docker-entrypoint-initdb.d:ro
      - ./data/backup:/backup
    networks:
      campus-net:
        aliases: [mysql]
    mem_limit: 500m
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p$$MYSQL_ROOT_PASSWORD --silent"]
      interval: 10s
      timeout: 5s
      retries: 12
      start_period: 40s
    logging: *default-logging

  # ---------------------------------------------------------------- Redis 7.2.5
  redis:
    image: redis:7.2.5
    container_name: campus-redis
    restart: always
    command:
      - redis-server
      - --appendonly
      - "yes"
      - --appendfsync
      - everysec
      - --requirepass
      - ${REDIS_PASSWORD}
      - --maxmemory
      - 150mb
      - --maxmemory-policy
      - allkeys-lru
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - redis-aof:/data
    networks:
      campus-net:
        aliases: [redis]
    mem_limit: 150m
    healthcheck:
      test: ["CMD-SHELL", "redis-cli -a $$REDIS_PASSWORD ping | grep -q PONG"]
      interval: 10s
      timeout: 5s
      retries: 6
    logging: *default-logging

  # ---------------------------------------------------------------- Nacos 2.3.2（SB-01 鉴权开启）
  nacos:
    image: nacos/nacos-server:v2.3.2
    container_name: campus-nacos
    restart: always
    environment:
      MODE: standalone
      # SB-01：开启鉴权 + 强口令（默认 nacos/nacos 禁用）
      NACOS_AUTH_ENABLE: "true"
      NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN}
      NACOS_AUTH_IDENTITY_KEY: ${NACOS_AUTH_IDENTITY_KEY}
      NACOS_AUTH_IDENTITY_VALUE: ${NACOS_AUTH_IDENTITY_VALUE}
      JVM_XMS: 256m
      JVM_XMX: 384m
      JVM_XMN: 128m
    ports:
      - "127.0.0.1:8848:8848"
      - "127.0.0.1:9848:9848"
    volumes:
      - nacos-logs:/home/nacos/logs
    networks:
      campus-net:
        aliases: [nacos]
    mem_limit: 450m
    healthcheck:
      test: ["CMD-SHELL", "curl -sf http://127.0.0.1:8848/nacos/v1/console/health/readiness || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 10
      start_period: 60s
    logging: *default-logging

  # ---------------------------------------------------------------- Sentinel Dashboard 1.8.8（SB-02 口令）
  sentinel:
    image: bladex/sentinel-dashboard:1.8.8
    container_name: campus-sentinel
    restart: always
    environment:
      JAVA_OPTS: >-
        -Dserver.port=8858
        -Dcsp.sentinel.dashboard.server=localhost:8858
        -Dproject.name=sentinel-dashboard
        -Dcsp.sentinel.app.type=1
        -Dsentinel.dashboard.auth.username=${SENTINEL_USER}
        -Dsentinel.dashboard.auth.password=${SENTINEL_PASSWORD}
        -Xmx160m
    ports:
      - "127.0.0.1:8858:8858"
    networks:
      campus-net:
        aliases: [sentinel]
    mem_limit: 200m
    logging: *default-logging

  # ---------------------------------------------------------------- Nginx 1.24（SB-04 server_tokens off）
  nginx:
    image: nginx:1.24
    container_name: campus-nginx
    restart: always
    ports:
      # 本地 dev 用 8088，避免与宿主机 80 端口占用冲突（prod 为 80/443，见《部署设计》§3.2.1）
      - "127.0.0.1:${NGINX_HTTP_PORT:-8088}:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - uploads:/usr/share/nginx/uploads
    networks:
      campus-net:
        aliases: [nginx]
    mem_limit: 30m
    healthcheck:
      test: ["CMD-SHELL", "nginx -t || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
    logging: *default-logging

  # ================================================================
  # 业务组合（profile: app）—— W1 起随实现进度逐个启用
  # 本地开发推荐直接在 IDE 运行；容器方式用于整栈联调与压测
  # ================================================================
  gateway:
    image: eclipse-temurin:21-jre
    container_name: campus-gateway
    profiles: ["app"]
    restart: always
    command: ["java", "-Xmx256m", "-jar", "/app/app.jar"]
    environment:
      TZ: Asia/Shanghai
      SPRING_PROFILES_ACTIVE: dev
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      NACOS_HOST: nacos
      JWT_SECRET: ${JWT_SECRET}
    volumes:
      - ../backend/gateway/target/gateway-1.0.0-SNAPSHOT.jar:/app/app.jar:ro
    ports:
      - "127.0.0.1:8080:8080"
    depends_on:
      redis:
        condition: service_healthy
      nacos:
        condition: service_healthy
    networks:
      campus-net:
        aliases: [campus-gateway]
    mem_limit: 300m
    logging: *default-logging

  user-credit-service:
    image: eclipse-temurin:21-jre
    container_name: campus-user-credit
    profiles: ["app"]
    restart: always
    command: ["java", "-Xmx128m", "-jar", "/app/app.jar"]
    environment:
      TZ: Asia/Shanghai
      DB_HOST: mysql
      DB_USER: ${DB_USER_USER}
      DB_PWD: ${DB_PWD_USER}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    volumes:
      - ../backend/user-credit-service/target/user-credit-service-1.0.0-SNAPSHOT.jar:/app/app.jar:ro
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      campus-net:
        aliases: [user-credit-service]
    mem_limit: 160m
    logging: *default-logging

  trade-service:
    image: eclipse-temurin:21-jre
    container_name: campus-trade
    profiles: ["app"]
    restart: always
    command: ["java", "-Xmx160m", "-jar", "/app/app.jar"]
    environment:
      TZ: Asia/Shanghai
      DB_HOST: mysql
      DB_USER: ${DB_USER_TRADE}
      DB_PWD: ${DB_PWD_TRADE}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    volumes:
      - ../backend/trade-service/target/trade-service-1.0.0-SNAPSHOT.jar:/app/app.jar:ro
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      campus-net:
        aliases: [trade-service]
    mem_limit: 192m
    logging: *default-logging

  lostfound-service:
    image: eclipse-temurin:21-jre
    container_name: campus-lostfound
    profiles: ["app"]
    restart: always
    command: ["java", "-Xmx128m", "-jar", "/app/app.jar"]
    environment:
      TZ: Asia/Shanghai
      DB_HOST: mysql
      DB_USER: ${DB_USER_LOSTFOUND}
      DB_PWD: ${DB_PWD_LOSTFOUND}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    volumes:
      - ../backend/lostfound-service/target/lostfound-service-1.0.0-SNAPSHOT.jar:/app/app.jar:ro
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      campus-net:
        aliases: [lostfound-service]
    mem_limit: 160m
    logging: *default-logging

  errand-service:
    image: eclipse-temurin:21-jre
    container_name: campus-errand
    profiles: ["app"]
    restart: always
    command: ["java", "-Xmx160m", "-jar", "/app/app.jar"]
    environment:
      TZ: Asia/Shanghai
      DB_HOST: mysql
      DB_USER: ${DB_USER_ERRAND}
      DB_PWD: ${DB_PWD_ERRAND}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    volumes:
      - ../backend/errand-service/target/errand-service-1.0.0-SNAPSHOT.jar:/app/app.jar:ro
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      campus-net:
        aliases: [errand-service]
    mem_limit: 192m
    logging: *default-logging

  ai-assistant-service:
    image: eclipse-temurin:21-jre
    container_name: campus-ai
    profiles: ["app"]
    restart: always
    command: ["java", "-Xmx200m", "-jar", "/app/app.jar"]
    environment:
      TZ: Asia/Shanghai
      DB_HOST: mysql
      DB_USER: ${DB_USER_AI}
      DB_PWD: ${DB_PWD_AI}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      DASHSCOPE_API_KEY: ${DASHSCOPE_API_KEY}
    volumes:
      - ../backend/ai-assistant-service/target/ai-assistant-service-1.0.0-SNAPSHOT.jar:/app/app.jar:ro
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      campus-net:
        aliases: [ai-assistant-service]
    mem_limit: 256m
    logging: *default-logging

  notify-service:
    image: eclipse-temurin:21-jre
    container_name: campus-notify
    profiles: ["app"]
    restart: always
    command: ["java", "-Xmx128m", "-jar", "/app/app.jar"]
    environment:
      TZ: Asia/Shanghai
      DB_HOST: mysql
      DB_USER: ${DB_USER_NOTIFY}
      DB_PWD: ${DB_PWD_NOTIFY}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    volumes:
      - ../backend/notify-service/target/notify-service-1.0.0-SNAPSHOT.jar:/app/app.jar:ro
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      campus-net:
        aliases: [notify-service]
    mem_limit: 160m
    logging: *default-logging

  admin-service:
    image: eclipse-temurin:21-jre
    container_name: campus-admin
    profiles: ["app"]
    restart: always
    command: ["java", "-Xmx128m", "-jar", "/app/app.jar"]
    environment:
      TZ: Asia/Shanghai
      DB_HOST: mysql
      DB_USER: ${DB_USER_ADMIN}
      DB_PWD: ${DB_PWD_ADMIN}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
    volumes:
      - ../backend/admin-service/target/admin-service-1.0.0-SNAPSHOT.jar:/app/app.jar:ro
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      campus-net:
        aliases: [admin-service]
    mem_limit: 160m
    logging: *default-logging

networks:
  campus-net:
    name: campus-net
    driver: bridge
    ipam:
      config:
        # 显式固定网段，禁 Docker 随机分配（《安全设计》§5.1 CIDR 建议值）
        - subnet: 172.28.0.0/16

volumes:
  mysql-data:
  redis-aof:
  nacos-logs:
  uploads:
"""

# ================================================================ docker-compose.obs.yml
FILES['docker-compose.obs.yml'] = """# =============================================================================
# 观测组合（observability profile，按需启动，约 800MB）
# 依据《部署设计》§2.2.5 / 《系统设计》§8：Metrics / Logs / Traces 三支柱
#
#   启动：docker compose -f docker-compose.yml -f docker-compose.obs.yml --profile observability up -d
#   停止：docker compose -f docker-compose.yml -f docker-compose.obs.yml --profile observability down
#
#   预算：Prometheus 250M / Grafana 200M / Zipkin 150M / Loki+Promtail 200M
#   注意：演示期约束「压测不得与观测组合同时全开」（《系统设计》§5.5.4）
#   完整 Dashboard（6 个）与告警规则 AL-01~09 在 W6.3 落地
# =============================================================================
name: campus-dev

services:

  prometheus:
    image: prom/prometheus:v2.53.0
    container_name: campus-prometheus
    profiles: ["observability"]
    restart: always
    command:
      - --config.file=/etc/prometheus/prometheus.yml
      - --storage.tsdb.retention.time=15d
    ports:
      - "127.0.0.1:9090:9090"
    volumes:
      - ./observability/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    networks:
      campus-net:
        aliases: [prometheus]
    mem_limit: 250m

  grafana:
    image: grafana/grafana:11.1.0
    container_name: campus-grafana
    profiles: ["observability"]
    restart: always
    environment:
      # SB-02 同口径：控制台口令强制设置，禁默认 admin/admin
      GF_SECURITY_ADMIN_USER: ${GRAFANA_USER}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD}
      GF_USERS_ALLOW_SIGN_UP: "false"
    ports:
      - "127.0.0.1:3000:3000"
    volumes:
      - grafana-data:/var/lib/grafana
    networks:
      campus-net:
        aliases: [grafana]
    mem_limit: 200m

  zipkin:
    image: openzipkin/zipkin:3.4.0
    container_name: campus-zipkin
    profiles: ["observability"]
    restart: always
    environment:
      STORAGE_TYPE: mem
    ports:
      - "127.0.0.1:9411:9411"
    networks:
      campus-net:
        aliases: [zipkin]
    mem_limit: 150m

  loki:
    image: grafana/loki:3.1.0
    container_name: campus-loki
    profiles: ["observability"]
    restart: always
    command: ["-config.file=/etc/loki/loki-config.yml"]
    ports:
      - "127.0.0.1:3100:3100"
    volumes:
      - ./observability/loki-config.yml:/etc/loki/loki-config.yml:ro
      - loki-data:/loki
    networks:
      campus-net:
        aliases: [loki]
    mem_limit: 200m

networks:
  campus-net:
    name: campus-net
    external: true

volumes:
  prometheus-data:
  grafana-data:
  loki-data:
"""

# ================================================================ prometheus 配置
FILES['observability/prometheus.yml'] = """# Prometheus 抓取配置（基础版；完整指标分层与告警规则在 W6.3 落地，见《系统设计》§8.1）
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    env: dev

scrape_configs:
  - job_name: prometheus
    static_configs:
      - targets: ["localhost:9090"]

  # 业务服务 Actuator（SB-03：仅暴露 health/info 与 liveness/readiness）
  - job_name: campus-services
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - campus-gateway:8080
          - user-credit-service:8081
          - trade-service:8082
          - lostfound-service:8083
          - errand-service:8084
          - ai-assistant-service:8085
          - notify-service:8086
          - admin-service:8087
"""

FILES['observability/loki-config.yml'] = """# Loki 最简配置（单机 filesystem；< 200MB/天，保留 30 天，见《系统设计》§8.2.1）
auth_enabled: false

server:
  http_listen_port: 3100
  log_level: warn

common:
  path_prefix: /loki
  storage:
    filesystem:
      chunks_directory: /loki/chunks
      rules_directory: /loki/rules
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory

schema_config:
  configs:
    - from: 2026-01-01
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h

limits_config:
  retention_period: 720h
  ingestion_rate_mb: 4
  ingestion_burst_size_mb: 8

compactor:
  working_directory: /loki/compactor
  retention_enabled: true
  delete_request_store: filesystem
"""

# ================================================================ nginx
FILES['nginx/nginx.conf'] = """user  nginx;
worker_processes  auto;

error_log  /var/log/nginx/error.log warn;
pid        /var/run/nginx.pid;

events {
    worker_connections  1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    # SB-04：隐藏 Nginx 版本号（《安全设计》§5.3）
    server_tokens off;

    # 统一 upstream 日志格式（含 X-Forwarded-For，入口防护日志维度，§7.2.2）
    log_format campus_main '$remote_addr - $remote_user [$time_local] "$request" '
                           '$status $body_bytes_sent "$http_referer" '
                           '"$http_user_agent" "$http_x_forwarded_for" '
                           'rt=$request_time urt=$upstream_response_time';

    access_log  /var/log/nginx/access.log  campus_main;

    sendfile        on;
    keepalive_timeout  65;
    # 上传体积上限 8m，覆盖图片 5MB 上限（《安全设计》§5.2.1）
    client_max_body_size 8m;
    client_body_buffer_size 128k;

    gzip on;
    gzip_min_length 1k;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

    include /etc/nginx/conf.d/*.conf;
}
"""

FILES['nginx/conf.d/campus.conf'] = """# 入口配置（dev 版本；prod 增加 443 TLS 终结、HSTS 与 80→443 强制跳转，见《安全设计》§3.2.1）
# TLS 终结在 Nginx、鉴权在 Gateway（《系统设计》§6.2.1 入口决策）

upstream campus_gateway {
    server campus-gateway:8080;
    keepalive 32;
}

server {
    listen       80;
    server_name  _;

    # 静态图片目录（Nginx 直出，见《部署设计》§3.2.1）
    location /uploads/ {
        alias /usr/share/nginx/uploads/;
        expires 7d;
        # 图片目录禁止执行任何脚本（《安全设计》§5.3 文件上传四层校验）
        location ~* \\.(php|jsp|sh|py)$ {
            deny all;
        }
    }

    location /api/ {
        proxy_pass         http://campus_gateway;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        # SB-05：CORS 仅放行自有前端域名（dev 为本地 Vite 端口）
        add_header Access-Control-Allow-Origin  $http_origin always;
        add_header Access-Control-Allow-Methods "GET,POST,PUT,DELETE,OPTIONS" always;
        add_header Access-Control-Allow-Headers "Authorization,Content-Type,X-Idempotency-Key,X-Trace-Id" always;
        add_header Access-Control-Allow-Credentials "true" always;
        if ($request_method = OPTIONS) {
            return 204;
        }
        proxy_read_timeout 65s;
    }

    # AI 流式接口（SSE）：关闭缓冲，避免流式响应被 Nginx 缓存（§3.1.5 SSE）
    location /api/v1/ai/chat/stream {
        proxy_pass         http://campus_gateway;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_buffering    off;
        proxy_cache        off;
        proxy_read_timeout 300s;
    }

    location = /healthz {
        access_log off;
        return 200 'ok';
        add_header Content-Type text/plain;
    }
}
"""

# ================================================================ 备份脚本
FILES['backup/backup.sh'] = """#!/bin/bash
# =============================================================================
# MySQL 每日备份（《部署设计》§2.2.2 / 《系统设计》§4.3.2）
#   全量：mysqldump 每日 03:00，保留 30 天
#   增量：binlog 实时落盘，保留 7 天
#   RPO ≤ 5min（常态）｜RTO 单库 ≤ 30min
# 挂载到宿主机：docker/data/backup
# =============================================================================
set -euo pipefail

BACKUP_DIR="/backup"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

DATABASES="user_db trade_db lostfound_db errand_db ai_db notify_db admin_db"

echo "[backup] start ${DATE}"
for db in ${DATABASES}; do
  mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \\
    --single-transaction --routines --triggers --events \\
    --default-character-set=utf8mb4 \\
    "${db}" | gzip > "${BACKUP_DIR}/${db}_${DATE}.sql.gz"
  echo "[backup] ${db} done ($(du -h "${BACKUP_DIR}/${db}_${DATE}.sql.gz" | cut -f1))"
done

# 过期清理
find "${BACKUP_DIR}" -name "*.sql.gz" -mtime +${RETENTION_DAYS} -delete
echo "[backup] finished, retention ${RETENTION_DAYS}d"
"""

# ================================================================ .env.example
FILES['.env.example'] = """# =============================================================================
# 本地开发环境变量模板（L4 Secret，见《部署设计》§4.4）
#   用法：复制为 .env 并填入真实值；.env 已被 .gitignore 忽略，禁止提交（《安全设计》§6.3 六禁红线）
#   服务器部署：.env 权限必须设为 600，且 prod/uat 不共用任何密钥
#   轮换基线：90 天（JWT secret / 百炼 API Key / MySQL 密码），见《安全设计》§6.1
# =============================================================================

# ---------------- 数据库 ----------------
MYSQL_ROOT_PASSWORD=change_me_root
# 7 个业务账号（按库最小权限，生产必须互不相同）
DB_PWD_USER=change_me
DB_PWD_TRADE=change_me
DB_PWD_LOSTFOUND=change_me
DB_PWD_LOSTFOUND_=change_me
DB_PWD_ERRAND=change_me
DB_PWD_AI=change_me
DB_PWD_NOTIFY=change_me
DB_PWD_ADMIN=change_me
# 服务侧账号名（与 sql/01-init-users.sh 一致）
DB_USER_USER=campus_user
DB_USER_TRADE=campus_trade
DB_USER_LOSTFOUND=campus_lostfound
DB_USER_ERRAND=campus_errand
DB_USER_AI=campus_ai
DB_USER_NOTIFY=campus_notify
DB_USER_ADMIN=campus_admin

# ---------------- Redis（requirepass ≥ 24 位，§5.4）----------------
REDIS_PASSWORD=change_me_redis_24chars_min

# ---------------- Nacos（SB-01 鉴权开启）----------------
NACOS_AUTH_TOKEN=change_me_base64_token_at_least_32_bytes
NACOS_AUTH_IDENTITY_KEY=nacos_identity_key
NACOS_AUTH_IDENTITY_VALUE=nacos_identity_value
NACOS_USER=nacos
NACOS_PASSWORD=change_me_nacos

# ---------------- Sentinel Dashboard（SB-02 控制台口令）----------------
SENTINEL_USER=sentinel
SENTINEL_PASSWORD=change_me_sentinel

# ---------------- Grafana（观测组合）----------------
GRAFANA_USER=admin
GRAFANA_PASSWORD=change_me_grafana

# ---------------- 应用密钥 ----------------
# JWT HS256 密钥，≥ 256bit（《安全设计》§6.1③）
JWT_SECRET=change_me_jwt_secret_at_least_32_chars_long
# 阿里云百炼 API Key（唯一外部上游 E-01）
DASHSCOPE_API_KEY=sk-change_me

# ---------------- 本地端口 ----------------
NGINX_HTTP_PORT=8088

# ---------------- 种子数据 ----------------
# 管理端种子账号明文密码（首次登录后强制修改 + 绑定 TOTP，见《安全设计》§2.1.1）
SEED_ADMIN_PASSWORD=Admin@Campus2026
"""


def write(rel, content):
    full = os.path.join(D, rel)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)


count = 0
for rel, content in FILES.items():
    write(rel, content)
    count += 1

# 生成实际 .env（随机强口令），已 gitignore
alphabet = string.ascii_letters + string.digits + "_-"
def rnd(n=28):
    return ''.join(secrets.choice(alphabet) for _ in range(n))

env = FILES['.env.example']
repl = {
    'change_me_root': rnd(24) + '@R',
    'change_me_redis_24chars_min': rnd(30),
    # NACOS_AUTH_TOKEN 必须是【标准】Base64（解码后 >= 32 字节）。
    # 注意不能用 token_urlsafe：其 '-'/''_' 字符 Java Base64.Decoder 不认，
    # 会导致 Nacos 2.3.2 开鉴权时 PrometheusAuthFilter 初始化失败、容器崩溃循环。
    'change_me_base64_token_at_least_32_bytes': base64.b64encode(secrets.token_bytes(48)).decode('ascii'),
    'nacos_identity_key': 'campus-identity-key',
    'nacos_identity_value': rnd(24),
    'change_me_nacos': rnd(20) + '@N',
    'change_me_sentinel': rnd(20) + '@S',
    'change_me_grafana': rnd(20) + '@G',
    'change_me_jwt_secret_at_least_32_chars_long': rnd(64),
    'sk-change_me': 'sk-REPLACE_WITH_DASHSCOPE_KEY',
    'change_me': rnd(24) + '@D',
    'DB_PWD_LOSTFOUND_=change_me\n': 'DB_PWD_LOSTFOUND_=unused_legacy_key\n',
}
for k, v in repl.items():
    env = env.replace(k, v)
write('.env', env)
count += 1

# 备份目录占位
os.makedirs(os.path.join(D, 'data', 'backup'), exist_ok=True)

print('生成 docker 文件数：%d' % count)
for n in sorted(FILES):
    print('  -', n)
print('  - .env （已生成真实密钥，勿提交）')
