# -*- coding: utf-8 -*-
"""
W0 数据库初始化脚本生成器 —— 7 库 27 表 DDL
依据：《系统设计》v1.0 §4.1 全局数据约定 / §4.2 单表设计（t1~t9 全五段 + §4.2.10 表清单总览）
用法：python tools/gen_sql.py
说明：所有 .sql 用 LF 换行（容器内 initdb 执行）；账号创建走 01-init-users.sh + .env 变量，密码不入库。
"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SQL_DIR = os.path.join(ROOT, 'docker', 'sql')

# §4.1 全局约定的公共列片段
AUDIT_COLS = """  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',"""

SCHOOL_COL = "  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',"

OUTBOX_TABLE = """CREATE TABLE `t_outbox_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` VARCHAR(64) NOT NULL COMMENT '事件唯一键',
  `event_type` VARCHAR(40) NOT NULL COMMENT '事件类型（Published Language Schema）',
  `aggregate_id` VARCHAR(64) NOT NULL COMMENT '业务聚合 ID',
  `target_user_id` BIGINT NOT NULL COMMENT '接收人',
  `payload` VARCHAR(1024) NOT NULL COMMENT 'JSON 载荷',
  `status` VARCHAR(20) NOT NULL DEFAULT 'INIT' COMMENT '状态：INIT/PROCESSING/SENT/RETRY/DEAD',
  `owner` VARCHAR(64) DEFAULT NULL COMMENT '领取实例',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数（≥5 → DEAD）',
  `next_retry_time` DATETIME(3) DEFAULT NULL COMMENT '下次可领取时间',
  `sent_time` DATETIME(3) DEFAULT NULL COMMENT '投递成功时间（成功证据，V5）',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_status_retry` (`status`, `next_retry_time`),
  KEY `idx_created` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Outbox 待投递事件（每业务库同构）';"""

FILES = {}

FILES['00-init-databases.sql'] = """-- =============================================================
-- 校园互助生活平台 · 数据库初始化（第 1 步：建库）
-- 依据《系统设计》§4.1：7 个业务库，每业务服务独立 schema，禁止跨库 JOIN
-- 执行方：MySQL 容器 /docker-entrypoint-initdb.d/（仅首次初始化空数据卷时执行）
-- =============================================================
SET NAMES utf8mb4;

-- 注意：MySQL 的 CREATE DATABASE 不支持 COMMENT 子句（仅表/列支持），
-- 库级说明以 SQL 注释承载；写成 COMMENT '...' 会导致初始化脚本 1064 中断。
CREATE DATABASE IF NOT EXISTS `user_db`      DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; -- M1 用户与信用
CREATE DATABASE IF NOT EXISTS `trade_db`     DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; -- M2 二手交易（含余额支付）
CREATE DATABASE IF NOT EXISTS `lostfound_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; -- M3 失物招领
CREATE DATABASE IF NOT EXISTS `errand_db`    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; -- M4 跑腿拼单
CREATE DATABASE IF NOT EXISTS `ai_db`        DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; -- M5 AI 助手
CREATE DATABASE IF NOT EXISTS `notify_db`    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; -- M6 通知
CREATE DATABASE IF NOT EXISTS `admin_db`     DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; -- M7 平台管理

-- ngram 全文解析器：MySQL 8 内置，用于替代 ES 的关键词检索（§3.1.5 / O3）
-- 内置分词器 ngram_token_size 默认 2，本地 dev 保持默认；生产如调优需写入 mysqld 配置并重启
"""

# --------------------------------------------------------------- user_db
FILES['10-user_db.sql'] = """-- =============================================================
-- user_db ｜ M1 用户与信用服务 ｜ 4 张表
-- 依据《系统设计》§4.2.t1 / §4.2.10 / specs/05-用户与认证
-- =============================================================
USE `user_db`;

-- ---------- t_user_account（§4.2.t1 核心表）----------
CREATE TABLE `t_user_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `student_no` VARCHAR(20) NOT NULL COMMENT '学号',
  `email` VARCHAR(64) NOT NULL COMMENT '校园邮箱',
  `password_hash` VARCHAR(80) NOT NULL COMMENT 'BCrypt 密码摘要（cost 10）',
  `nickname` VARCHAR(30) NOT NULL COMMENT '昵称',
  `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色：STUDENT/ADMIN',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/BANNED',
  `credit_score` INT NOT NULL DEFAULT 100 COMMENT '信用分[0,200]',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `privacy_consent_version` VARCHAR(10) DEFAULT NULL COMMENT '隐私政策版本（F18 同意留痕）',
  `privacy_consent_time` DATETIME(3) DEFAULT NULL COMMENT '隐私同意勾选时间（F18）',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_email` (`email`),
  KEY `idx_status_created` (`status`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账号与信用主档';

-- ---------- t_credit_record（信用分增减明细，§4.2.10）----------
CREATE TABLE `t_credit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `delta` INT NOT NULL COMMENT '本次增减值（可负）',
  `source_type` VARCHAR(20) NOT NULL COMMENT '来源：REVIEW/FULFILL/DEFAULT',
  `score_after` INT NOT NULL COMMENT '变动后信用分快照',
  `biz_event_id` VARCHAR(64) NOT NULL COMMENT '业务事件键（幂等，uk）',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_event` (`biz_event_id`),
  KEY `idx_user_created` (`user_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信用分增减明细（永久保留）';

-- ---------- t_login_log（登录流水，安全审计第五维配套）----------
CREATE TABLE `t_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `user_id` BIGINT DEFAULT NULL COMMENT '用户 ID（失败且账号不存在时为空）',
  `student_no` VARCHAR(20) DEFAULT NULL COMMENT '登录标识（脱敏展示）',
  `ip` VARCHAR(45) DEFAULT NULL COMMENT '来源 IP',
  `user_agent` VARCHAR(200) DEFAULT NULL COMMENT 'UA 摘要',
  `result` VARCHAR(20) NOT NULL COMMENT '结果：SUCCESS/FAIL/LOCKED',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_time`),
  KEY `idx_ip_created` (`ip`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录流水（保留 1 年，§7.2.4）';

-- ---------- t_user_identity（社交登录绑定预留表，specs/05：本期建表不写逻辑）----------
CREATE TABLE `t_user_identity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `user_id` BIGINT NOT NULL COMMENT '本地账号 ID',
  `idp_type` VARCHAR(20) NOT NULL COMMENT '身份源：LOCAL/QQ/WECHAT/CAS',
  `openid` VARCHAR(64) NOT NULL COMMENT '身份源侧唯一标识',
  `unionid` VARCHAR(64) DEFAULT NULL COMMENT '身份源侧主体标识（微信 unionid 预留）',
  `bound_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '绑定时间',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idp_openid` (`idp_type`, `openid`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='第三方身份绑定（预留）';
"""

# --------------------------------------------------------------- trade_db
FILES['11-trade_db.sql'] = """-- =============================================================
-- trade_db ｜ M2 二手交易服务（含余额支付）｜ 7 张表
-- 依据《系统设计》§4.2.t2 / t3 / t4 / §4.2.10
-- =============================================================
USE `trade_db`;

-- ---------- t_goods（§4.2.t2 核心表）----------
CREATE TABLE `t_goods` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `seller_id` BIGINT NOT NULL COMMENT '卖家用户 ID',
  `title` VARCHAR(50) NOT NULL COMMENT '标题（过敏感词）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `category` VARCHAR(20) NOT NULL COMMENT '类目：BOOK/DIGITAL/DAILY/SPORT/OTHER',
  `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
  `images` VARCHAR(1024) DEFAULT NULL COMMENT '图片 URL JSON 数组（≤ 9）',
  `stock` INT NOT NULL DEFAULT 1 COMMENT '库存（单库存固定 1）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'LISTED' COMMENT '状态：LISTED/SOLD/OFFSHELF/DELETED',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PASSED' COMMENT '复审：PASSED/REJECTED',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  KEY `idx_status_category_created` (`status`, `category`, `created_time`),
  FULLTEXT KEY `ft_title_desc` (`title`, `description`) WITH PARSER ngram,
  KEY `idx_seller` (`seller_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='二手商品主档（单库存）';

-- ---------- t_trade_order（§4.2.t3 核心表）----------
CREATE TABLE `t_trade_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `order_no` VARCHAR(32) NOT NULL COMMENT '展示单号 TO+日期+序列',
  `goods_id` BIGINT NOT NULL COMMENT '商品 ID',
  `goods_title` VARCHAR(50) NOT NULL COMMENT '商品标题快照',
  `goods_price` DECIMAL(10,2) NOT NULL COMMENT '成交价快照',
  `buyer_id` BIGINT NOT NULL COMMENT '买家 ID',
  `buyer_nickname` VARCHAR(30) NOT NULL COMMENT '买家昵称快照',
  `seller_id` BIGINT NOT NULL COMMENT '卖家 ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'FROZEN' COMMENT '状态：FROZEN/COMPLETED/CANCELLED（下单事务内一步到 FROZEN，无 CREATED 落库态）',
  `idempotency_key` VARCHAR(64) NOT NULL COMMENT '下单幂等键（DB 兜底防线）',
  `remark` VARCHAR(100) DEFAULT NULL COMMENT '买家留言',
  `review_deadline` DATETIME(3) DEFAULT NULL COMMENT '评价窗口截止（完成 + 7 天）',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_idem` (`idempotency_key`),
  KEY `idx_buyer_status` (`buyer_id`, `status`, `created_time`),
  KEY `idx_seller_status` (`seller_id`, `status`),
  KEY `idx_review_deadline` (`review_deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='二手交易订单（永久保留）';

-- ---------- t_balance_account（余额账户主档，§4.2.10）----------
CREATE TABLE `t_balance_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `user_id` BIGINT NOT NULL COMMENT '归属用户',
  `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
  `frozen` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '冻结额',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='余额账户主档（注册即开户）';

-- ---------- t_balance_flow（§4.2.t4 核心表｜资金幂等与对账）----------
CREATE TABLE `t_balance_flow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `user_id` BIGINT NOT NULL COMMENT '归属用户',
  `flow_type` VARCHAR(20) NOT NULL COMMENT '类型：FREEZE/UNFREEZE/DEDUCT/INCOME/RECHARGE/REWARD',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '本笔金额（正数）',
  `direction` TINYINT(1) NOT NULL COMMENT '方向：1 增 0 减',
  `biz_req_no` VARCHAR(64) NOT NULL COMMENT '业务请求号（幂等键）',
  `biz_ref` VARCHAR(64) NOT NULL COMMENT '关联业务单号（订单/跑腿单）',
  `balance_after` DECIMAL(10,2) NOT NULL COMMENT '变动后可用余额快照',
  `frozen_after` DECIMAL(10,2) NOT NULL COMMENT '变动后冻结额快照',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_req` (`user_id`, `biz_req_no`),
  KEY `idx_user_created` (`user_id`, `created_time`),
  KEY `idx_biz_ref` (`biz_ref`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='余额流水（资金幂等与对账核心，永久保留）';

-- ---------- t_review（双向评价，§4.2.10）----------
CREATE TABLE `t_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `order_id` BIGINT NOT NULL COMMENT '关联订单 ID',
  `order_type` VARCHAR(20) NOT NULL DEFAULT 'TRADE' COMMENT '订单类型：TRADE/ERRAND',
  `rater_id` BIGINT NOT NULL COMMENT '评价人',
  `ratee_id` BIGINT NOT NULL COMMENT '被评价人',
  `rating` TINYINT NOT NULL COMMENT '评分 1~5',
  `content` VARCHAR(200) DEFAULT NULL COMMENT '评语（过敏感词）',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_rater` (`order_id`, `rater_id`),
  KEY `idx_ratee_created` (`ratee_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易/履约双向评价（永久保留）';

-- ---------- t_goods_archive（商品归档，§4.2.10｜OFFSHELF/DELETED 超 180 天）----------
CREATE TABLE `t_goods_archive` (
  `id` BIGINT NOT NULL COMMENT '沿用主表主键',
""" + SCHOOL_COL + """
  `seller_id` BIGINT NOT NULL COMMENT '卖家用户 ID',
  `title` VARCHAR(50) NOT NULL COMMENT '标题',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `category` VARCHAR(20) NOT NULL COMMENT '类目',
  `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
  `images` VARCHAR(1024) DEFAULT NULL COMMENT '图片 JSON',
  `stock` INT NOT NULL DEFAULT 1 COMMENT '库存',
  `status` VARCHAR(20) NOT NULL COMMENT '归档时状态',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PASSED' COMMENT '复审状态',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_time` DATETIME(3) DEFAULT NULL COMMENT '原创建时间',
  `updated_time` DATETIME(3) DEFAULT NULL COMMENT '原更新时间',
  `archived_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '归档时间',
  PRIMARY KEY (`id`),
  KEY `idx_seller` (`seller_id`),
  KEY `idx_archived` (`archived_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品归档（保留 1 年；归档表不保留唯一约束）';

-- ---------- t_outbox_event（同构，§4.2.t8）----------
""" + OUTBOX_TABLE + "\n"

# --------------------------------------------------------------- lostfound_db
FILES['12-lostfound_db.sql'] = """-- =============================================================
-- lostfound_db ｜ M3 失物招领服务 ｜ 4 张表
-- 依据《系统设计》§4.2.t5 / t6 / §4.2.10
-- =============================================================
USE `lostfound_db`;

-- ---------- t_lost_item（§4.2.t5 核心表）----------
CREATE TABLE `t_lost_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `publisher_id` BIGINT NOT NULL COMMENT '发布人 ID',
  `item_type` VARCHAR(10) NOT NULL COMMENT '类型：LOST（失物）/FOUND（拾物）',
  `title` VARCHAR(30) NOT NULL COMMENT '物品名（匹配键）',
  `description` VARCHAR(200) NOT NULL COMMENT '描述',
  `location` VARCHAR(50) NOT NULL COMMENT '地点（匹配键）',
  `occurred_at` DATETIME(3) DEFAULT NULL COMMENT '发生时间（匹配键）',
  `images` VARCHAR(512) DEFAULT NULL COMMENT '图片 JSON 数组（≤ 3）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/MATCHED/RETURNED/CLOSED/OFFSHELF',
  `match_count` INT NOT NULL DEFAULT 0 COMMENT '已匹配提醒次数',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  KEY `idx_type_status_created` (`item_type`, `status`, `created_time`),
  FULLTEXT KEY `ft_title_desc_loc` (`title`, `description`, `location`) WITH PARSER ngram,
  KEY `idx_publisher` (`publisher_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='失物/拾物统一条目';

-- ---------- t_claim_apply（§4.2.t6 核心表）----------
CREATE TABLE `t_claim_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `item_id` BIGINT NOT NULL COMMENT '条目 ID',
  `claimer_id` BIGINT NOT NULL COMMENT '申请人 ID',
  `evidence_desc` VARCHAR(200) NOT NULL COMMENT '凭证描述',
  `contact_hint` VARCHAR(50) NOT NULL COMMENT '联系提示（展示脱敏）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED/APPROVED/REJECTED/RETURNED/CLOSED',
  `resubmit_round` INT NOT NULL DEFAULT 0 COMMENT '重提轮次（≤ 3）',
  `audit_opinion` VARCHAR(200) DEFAULT NULL COMMENT '最近审核意见（联动 t_audit_record）',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核管理员',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_claimer_round` (`item_id`, `claimer_id`, `resubmit_round`),
  KEY `idx_status` (`status`, `created_time`),
  KEY `idx_claimer` (`claimer_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认领申请';

-- ---------- t_lost_item_archive（条目归档，§4.2.10）----------
CREATE TABLE `t_lost_item_archive` (
  `id` BIGINT NOT NULL COMMENT '沿用主表主键',
""" + SCHOOL_COL + """
  `publisher_id` BIGINT NOT NULL COMMENT '发布人 ID',
  `item_type` VARCHAR(10) NOT NULL COMMENT '类型：LOST/FOUND',
  `title` VARCHAR(30) NOT NULL COMMENT '物品名',
  `description` VARCHAR(200) NOT NULL COMMENT '描述',
  `location` VARCHAR(50) NOT NULL COMMENT '地点',
  `occurred_at` DATETIME(3) DEFAULT NULL COMMENT '发生时间',
  `images` VARCHAR(512) DEFAULT NULL COMMENT '图片 JSON',
  `status` VARCHAR(20) NOT NULL COMMENT '归档时状态',
  `match_count` INT NOT NULL DEFAULT 0 COMMENT '已提醒次数',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_time` DATETIME(3) DEFAULT NULL COMMENT '原创建时间',
  `updated_time` DATETIME(3) DEFAULT NULL COMMENT '原更新时间',
  `archived_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '归档时间',
  PRIMARY KEY (`id`),
  KEY `idx_publisher` (`publisher_id`),
  KEY `idx_archived` (`archived_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='失物条目归档（保留 1 年）';

-- ---------- t_outbox_event（同构，§4.2.t8）----------
""" + OUTBOX_TABLE + "\n"

# --------------------------------------------------------------- errand_db
FILES['13-errand_db.sql'] = """-- =============================================================
-- errand_db ｜ M4 跑腿拼单服务 ｜ 5 张表
-- 依据《系统设计》§4.2.t7 / §4.2.10（并发抢单三重防线：Redisson 锁 → DB 乐观锁 → uk 约束）
-- =============================================================
USE `errand_db`;

-- ---------- t_errand_order（§4.2.t7 核心表）----------
CREATE TABLE `t_errand_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `errand_no` VARCHAR(32) NOT NULL COMMENT '展示单号 EO+日期+序列',
  `publisher_id` BIGINT NOT NULL COMMENT '发单人',
  `grabber_id` BIGINT DEFAULT NULL COMMENT '接单员（转派后置空）',
  `errand_type` VARCHAR(10) NOT NULL COMMENT '类型：PICKUP/BUY/SEND',
  `title` VARCHAR(30) NOT NULL COMMENT '标题',
  `description` VARCHAR(200) NOT NULL COMMENT '描述',
  `reward` DECIMAL(10,2) NOT NULL COMMENT '酬劳（发单即冻结）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED/GRABBED/PICKED/DELIVERED/COMPLETED/REASSIGNED/CANCELLED',
  `rideshare_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '顺路拼单标注（D-08）',
  `rideshare_note` VARCHAR(50) DEFAULT NULL COMMENT '拼单备注',
  `deadline` DATETIME(3) NOT NULL COMMENT '任务截止时间',
  `pickup_deadline` DATETIME(3) DEFAULT NULL COMMENT '取件超时线（GRABBED+30min，转派扫描用）',
  `reassign_count` INT NOT NULL DEFAULT 0 COMMENT '转派次数（≤ 3）',
  `idempotency_key` VARCHAR(64) NOT NULL COMMENT '发单幂等键',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本（抢单双保险之二）',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_errand_no` (`errand_no`),
  UNIQUE KEY `uk_idem` (`idempotency_key`),
  KEY `idx_status_created` (`status`, `created_time`),
  KEY `idx_publisher` (`publisher_id`, `status`),
  KEY `idx_status_pickup_deadline` (`status`, `pickup_deadline`),
  KEY `idx_grabber` (`grabber_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿单（并发抢单核心表）';

-- ---------- t_grab_record（抢单流水｜V3 压测与审计证据，永久保留）----------
CREATE TABLE `t_grab_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `order_id` BIGINT NOT NULL COMMENT '跑腿单 ID（一单一条，uk 兜底防重复接单）',
  `grabber_id` BIGINT NOT NULL COMMENT '抢单用户',
  `grab_result` VARCHAR(20) NOT NULL COMMENT '结果：SUCCESS/CONFLICT',
  `latency_ms` INT NOT NULL DEFAULT 0 COMMENT '抢单耗时（压测证据）',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '抢单时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_grabber_created` (`grabber_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='抢单流水（永久保留，V3 证据）';

-- ---------- t_errand_subtask（顺路拼单子任务，D-08，§4.2.10）----------
CREATE TABLE `t_errand_subtask` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `order_id` BIGINT NOT NULL COMMENT '所属跑腿单 ID',
  `note` VARCHAR(50) DEFAULT NULL COMMENT '子任务备注',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/ACCEPTED/DONE',
  `acceptor_id` BIGINT DEFAULT NULL COMMENT '接单人',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_acceptor` (`order_id`, `acceptor_id`),
  KEY `idx_order_status` (`order_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='顺路拼单子任务（D-08，随主单归档）';

-- ---------- t_errand_order_archive（跑腿单归档，§4.2.10）----------
CREATE TABLE `t_errand_order_archive` (
  `id` BIGINT NOT NULL COMMENT '沿用主表主键',
""" + SCHOOL_COL + """
  `errand_no` VARCHAR(32) NOT NULL COMMENT '展示单号',
  `publisher_id` BIGINT NOT NULL COMMENT '发单人',
  `grabber_id` BIGINT DEFAULT NULL COMMENT '接单员',
  `errand_type` VARCHAR(10) NOT NULL COMMENT '类型',
  `title` VARCHAR(30) NOT NULL COMMENT '标题',
  `description` VARCHAR(200) NOT NULL COMMENT '描述',
  `reward` DECIMAL(10,2) NOT NULL COMMENT '酬劳',
  `status` VARCHAR(20) NOT NULL COMMENT '归档时状态',
  `rideshare_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '顺路拼单标注',
  `rideshare_note` VARCHAR(50) DEFAULT NULL COMMENT '拼单备注',
  `deadline` DATETIME(3) DEFAULT NULL COMMENT '任务截止时间',
  `reassign_count` INT NOT NULL DEFAULT 0 COMMENT '转派次数',
  `created_time` DATETIME(3) DEFAULT NULL COMMENT '原创建时间',
  `updated_time` DATETIME(3) DEFAULT NULL COMMENT '原更新时间',
  `archived_time` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '归档时间',
  PRIMARY KEY (`id`),
  KEY `idx_publisher` (`publisher_id`),
  KEY `idx_archived` (`archived_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跑腿单归档（保留 1 年）';

-- ---------- t_outbox_event（同构，§4.2.t8）----------
""" + OUTBOX_TABLE + "\n"

# --------------------------------------------------------------- ai_db
FILES['14-ai_db.sql'] = """-- =============================================================
-- ai_db ｜ M5 AI 助手服务 ｜ 3 张表
-- 依据《系统设计》§4.2.t9 / §4.2.10（工具白名单 → ACL 适配器 → 后置断言 → 证据表，V4）
-- =============================================================
USE `ai_db`;

-- ---------- t_ai_session（AI 会话，§4.2.10）----------
CREATE TABLE `t_ai_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `user_id` BIGINT NOT NULL COMMENT '归属用户',
  `agent_name` VARCHAR(20) NOT NULL COMMENT 'Agent：TRADE/LOSTFOUND/ERRAND',
  `title` VARCHAR(50) DEFAULT NULL COMMENT '会话标题（首条消息摘要）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/CLOSED',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话（CLOSED 180 天清理）';

-- ---------- t_ai_message（会话消息，§4.2.10）----------
CREATE TABLE `t_ai_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` BIGINT NOT NULL COMMENT '会话 ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：USER/ASSISTANT/TOOL',
  `content` TEXT COMMENT '消息内容（上下文最小化，§3.3.7）',
  `evidence_json` VARCHAR(2048) DEFAULT NULL COMMENT '工具证据快照（bizRef 等）',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '消息时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_created` (`session_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话消息（随会话清理）';

-- ---------- t_ai_tool_call（§4.2.t9 核心表｜V4 证据表）----------
CREATE TABLE `t_ai_tool_call` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `session_id` BIGINT NOT NULL COMMENT '会话 ID',
  `message_id` BIGINT NOT NULL COMMENT '触发消息 ID',
  `agent_name` VARCHAR(20) NOT NULL COMMENT 'Agent：TRADE/LOSTFOUND/ERRAND',
  `tool_name` VARCHAR(40) NOT NULL COMMENT '工具名（白名单键）',
  `idempotency_key` VARCHAR(64) NOT NULL COMMENT '幂等键（sessionId+messageId）',
  `call_result` VARCHAR(20) NOT NULL COMMENT '结果：SUCCESS/FAILED/TIMEOUT/REJECTED_BY_WHITELIST',
  `asserted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '后置断言是否通过',
  `biz_ref` VARCHAR(64) DEFAULT NULL COMMENT '系统证据（订单号/条目 ID）',
  `latency_ms` INT NOT NULL DEFAULT 0 COMMENT '工具调用耗时',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idem` (`idempotency_key`),
  KEY `idx_session` (`session_id`, `created_time`),
  KEY `idx_agent_asserted` (`agent_name`, `asserted`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 工具调用记录（V4 证据表，永久保留）';
"""

# --------------------------------------------------------------- notify_db
FILES['15-notify_db.sql'] = """-- =============================================================
-- notify_db ｜ M6 通知服务 ｜ 1 张表（无 outbox：本域为投递终点）
-- 依据《系统设计》§4.2.10
-- =============================================================
USE `notify_db`;

-- ---------- t_notice（站内信）----------
CREATE TABLE `t_notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `receiver_id` BIGINT NOT NULL COMMENT '接收人',
  `title` VARCHAR(50) NOT NULL COMMENT '标题',
  `content` VARCHAR(500) NOT NULL COMMENT '正文',
  `biz_type` VARCHAR(20) NOT NULL COMMENT '业务类型：ORDER/CLAIM/ERRAND/SYSTEM',
  `biz_ref` VARCHAR(64) DEFAULT NULL COMMENT '关联业务单号',
  `read_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '已读标志（0 未读 1 已读）',
  `event_id` VARCHAR(64) NOT NULL COMMENT '来源事件键（幂等，防重复投递）',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_receiver_read` (`receiver_id`, `read_flag`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内信（已读 180 天物理清理）';
"""

# --------------------------------------------------------------- admin_db
FILES['16-admin_db.sql'] = """-- =============================================================
-- admin_db ｜ M7 平台管理服务 ｜ 4 张表
-- 依据《系统设计》§4.2.10（审核记录不可篡改：业务账号不授予 DELETE，§7.2.4）
-- =============================================================
USE `admin_db`;

-- ---------- t_audit_record（审核处置留痕，永久保留）----------
CREATE TABLE `t_audit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `biz_type` VARCHAR(20) NOT NULL COMMENT '业务类型：GOODS/LOST_ITEM/CLAIM/REPORT',
  `biz_id` BIGINT NOT NULL COMMENT '业务对象 ID',
  `auditor_id` BIGINT NOT NULL COMMENT '处置管理员',
  `decision` VARCHAR(20) NOT NULL COMMENT '决定：APPROVE/REJECT/TAKEDOWN',
  `opinion` VARCHAR(200) NOT NULL COMMENT '审核意见（必填）',
  `exec_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '执行状态：PENDING/EXECUTED/FAILED',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_auditor_created` (`auditor_id`, `created_time`),
  KEY `idx_exec_status` (`exec_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审核处置留痕（永久保留，仅 INSERT 与状态更新）';

-- ---------- t_admin_report（举报记录）----------
CREATE TABLE `t_admin_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
""" + SCHOOL_COL + """
  `reporter_id` BIGINT NOT NULL COMMENT '举报人',
  `biz_type` VARCHAR(20) NOT NULL COMMENT '被举报对象类型：GOODS/LOST_ITEM/CLAIM',
  `biz_id` BIGINT NOT NULL COMMENT '被举报对象 ID',
  `reason` VARCHAR(200) NOT NULL COMMENT '举报理由',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/DONE/REJECTED',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  KEY `idx_status_created` (`status`, `created_time`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='举报记录（处置完 1 年归档）';

-- ---------- t_sensitive_word（敏感词库，配置表）----------
CREATE TABLE `t_sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `word` VARCHAR(50) NOT NULL COMMENT '敏感词',
  `level` VARCHAR(20) NOT NULL DEFAULT 'BLOCK' COMMENT '级别：BLOCK（拦截）/REVIEW（转人工复审）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
""" + AUDIT_COLS + """
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`),
  KEY `idx_status_level` (`status`, `level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感词库（DFA 过滤，永久保留）';

-- ---------- t_outbox_event（同构，§4.2.t8）----------
""" + OUTBOX_TABLE + "\n"

# --------------------------------------------------------------- 账号初始化
INIT_USERS = """#!/bin/bash
# =============================================================
# 第 2 步：创建 7 个业务账号（按库最小权限，§4.1 / 安全设计 §3.3.3）
# 密码由 .env 注入，禁止写入任何入库文件（安全设计 §6.3 六禁红线）
# 生产环境必须为 7 个账号设置互不相同的强口令（禁多用途共用）
# =============================================================
set -e

MYSQL_CMD="mysql -uroot -p${MYSQL_ROOT_PASSWORD}"

create_account() {
  local user="$1"
  local pwd="$2"
  local db="$3"
  if [ -z "${pwd}" ]; then
    echo "[init-users] skip ${user}: password empty"
    return 0
  fi
  ${MYSQL_CMD} <<EOSQL
CREATE USER IF NOT EXISTS '${user}'@'%' IDENTIFIED BY '${pwd}';
ALTER USER '${user}'@'%' IDENTIFIED BY '${pwd}';
GRANT SELECT, INSERT, UPDATE, DELETE ON \\`${db}\\`.* TO '${user}'@'%';
FLUSH PRIVILEGES;
EOSQL
  echo "[init-users] ${user} -> ${db} ok"
}

create_account campus_user      "${DB_PWD_USER}"      user_db
create_account campus_trade     "${DB_PWD_TRADE}"     trade_db
create_account campus_lostfound "${DB_PWD_LOSTFOUND}" lostfound_db
create_account campus_errand    "${DB_PWD_ERRAND}"    errand_db
create_account campus_ai        "${DB_PWD_AI}"        ai_db
create_account campus_notify    "${DB_PWD_NOTIFY}"    notify_db
create_account campus_admin     "${DB_PWD_ADMIN}"     admin_db

echo "[init-users] all accounts ready"
"""

# 审计表防篡改：业务账号不授予 DELETE（安全设计 §7.2.4）
FILES['17-audit-protection.sh'] = """#!/bin/bash
# =============================================================
# 第 3 步：审计表防篡改（安全设计 §7.2.4）
# 业务账号对审计类表只保留 SELECT/INSERT/UPDATE，撤销 DELETE —— 审计记录不可删除
# =============================================================
set -e

MYSQL_CMD="mysql -uroot -p${MYSQL_ROOT_PASSWORD}"

protect() {
  local user="$1"
  local db="$2"
  local table="$3"
  if [ -z "$4" ]; then
    return 0
  fi
  ${MYSQL_CMD} <<EOSQL
REVOKE DELETE ON \\`${db}\\`.\\`${table}\\` FROM '${user}'@'%';
FLUSH PRIVILEGES;
EOSQL
  echo "[audit-protect] ${db}.${table} -> ${user} DELETE revoked"
}

protect campus_admin  admin_db  t_audit_record   "${DB_PWD_ADMIN}"
protect campus_trade  trade_db  t_balance_flow   "${DB_PWD_TRADE}"
protect campus_ai     ai_db     t_ai_tool_call   "${DB_PWD_AI}"
protect campus_errand errand_db t_grab_record    "${DB_PWD_ERRAND}"

echo "[audit-protect] done"
"""

FILES['01-init-users.sh'] = INIT_USERS

# --------------------------------------------------------------- 种子数据
FILES['20-seed-data.sql'] = """-- =============================================================
-- 种子数据（本地 dev 用，非生产）
-- 依据《系统设计》§4.2 / 安全设计 §2.1.2（管理端账号种子脚本初始化）
-- 说明：管理员密码为 BCrypt(cost 10) 摘要，明文见 docker/.env.example 的 SEED_ADMIN_PASSWORD，
--       首次登录后必须立即修改（部署设计 §4.5.2 首次发布流程）
-- =============================================================
USE `admin_db`;

-- 敏感词库初始样本（BLOCK 直接拦截 / REVIEW 转人工复审）
INSERT INTO `t_sensitive_word` (`word`, `level`, `status`, `created_by`) VALUES
  ('代考', 'BLOCK', 'ENABLED', 'seed'),
  ('代写论文', 'BLOCK', 'ENABLED', 'seed'),
  ('刷单', 'BLOCK', 'ENABLED', 'seed'),
  ('博彩', 'BLOCK', 'ENABLED', 'seed'),
  ('借贷', 'REVIEW', 'ENABLED', 'seed'),
  ('办证', 'REVIEW', 'ENABLED', 'seed');

USE `user_db`;

-- 管理端种子账号（role=ADMIN；密码摘要对应明文见 .env.example，登录后强制修改 + TOTP 绑定）
INSERT INTO `t_user_account`
  (`school_code`, `student_no`, `email`, `password_hash`, `nickname`, `role`, `status`, `credit_score`, `created_by`)
VALUES
  ('CAMPUS-MAIN', 'ADMIN0001', 'admin@campus.edu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
   '平台管理员', 'ADMIN', 'ACTIVE', 100, 'seed');

USE `trade_db`;

-- 演示用余额账户（对应上面的 ADMIN 账号，id 由 user_db 自增决定，此处以 1 假定本地首次初始化）
INSERT INTO `t_balance_account` (`school_code`, `user_id`, `balance`, `frozen`, `created_by`)
VALUES ('CAMPUS-MAIN', 1, 100.00, 0.00, 'seed');
"""

count = 0
os.makedirs(SQL_DIR, exist_ok=True)
for name, content in FILES.items():
    with open(os.path.join(SQL_DIR, name), 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)
    count += 1

print('生成 SQL/初始化脚本数：%d' % count)
for n in sorted(FILES):
    print('  -', n)
