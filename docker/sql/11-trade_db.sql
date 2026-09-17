-- =============================================================
-- trade_db ｜ M2 二手交易服务（含余额支付）｜ 7 张表
-- 依据《系统设计》§4.2.t2 / t3 / t4 / §4.2.10
-- =============================================================
USE `trade_db`;

-- ---------- t_goods（§4.2.t2 核心表）----------
CREATE TABLE `t_goods` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `seller_id` BIGINT NOT NULL COMMENT '卖家用户 ID',
  `title` VARCHAR(50) NOT NULL COMMENT '标题（过敏感词）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `category` VARCHAR(20) NOT NULL COMMENT '类目：BOOK/DIGITAL/DAILY/SPORT/OTHER',
  `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
  `images` VARCHAR(1024) DEFAULT NULL COMMENT '图片 URL JSON 数组（≤ 9）',
  `stock` INT NOT NULL DEFAULT 1 COMMENT '库存（单库存固定 1）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'LISTED' COMMENT '状态：LISTED/SOLD/OFFSHELF/DELETED',
  `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PASSED' COMMENT '复审：PASSED/REJECTED',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量（Redis 自增 + 定时任务回写，W2）',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  KEY `idx_status_category_created` (`status`, `category`, `created_time`),
  FULLTEXT KEY `ft_title_desc` (`title`, `description`) WITH PARSER ngram,
  KEY `idx_seller` (`seller_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='二手商品主档（单库存）';

-- ---------- t_trade_order（§4.2.t3 核心表）----------
CREATE TABLE `t_trade_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
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
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
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
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `user_id` BIGINT NOT NULL COMMENT '归属用户',
  `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额',
  `frozen` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '冻结额',
  `points` INT NOT NULL DEFAULT 0 COMMENT '交易积分（订单金额 1% 向下取整，W2）',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='余额账户主档（注册即开户）';

-- ---------- t_balance_flow（§4.2.t4 核心表｜资金幂等与对账）----------
CREATE TABLE `t_balance_flow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `user_id` BIGINT NOT NULL COMMENT '归属用户',
  `flow_type` VARCHAR(20) NOT NULL COMMENT '类型：FREEZE/UNFREEZE/DEDUCT/INCOME/RECHARGE/REWARD',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '本笔金额（正数）',
  `direction` TINYINT(1) NOT NULL COMMENT '方向：1 增 0 减',
  `biz_req_no` VARCHAR(64) NOT NULL COMMENT '业务请求号（幂等键）',
  `biz_ref` VARCHAR(64) NOT NULL COMMENT '关联业务单号（订单/跑腿单）',
  `balance_after` DECIMAL(10,2) NOT NULL COMMENT '变动后可用余额快照',
  `frozen_after` DECIMAL(10,2) NOT NULL COMMENT '变动后冻结额快照',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_req` (`user_id`, `biz_req_no`),
  KEY `idx_user_created` (`user_id`, `created_time`),
  KEY `idx_biz_ref` (`biz_ref`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='余额流水（资金幂等与对账核心，永久保留）';

-- ---------- t_review（双向评价，§4.2.10）----------
CREATE TABLE `t_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `order_id` BIGINT NOT NULL COMMENT '关联订单 ID',
  `order_type` VARCHAR(20) NOT NULL DEFAULT 'TRADE' COMMENT '订单类型：TRADE/ERRAND',
  `rater_id` BIGINT NOT NULL COMMENT '评价人',
  `ratee_id` BIGINT NOT NULL COMMENT '被评价人',
  `rating` TINYINT NOT NULL COMMENT '评分 1~5',
  `content` VARCHAR(200) DEFAULT NULL COMMENT '评语（过敏感词）',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_rater` (`order_id`, `rater_id`),
  KEY `idx_ratee_created` (`ratee_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易/履约双向评价（永久保留）';

-- ---------- t_goods_archive（商品归档，§4.2.10｜OFFSHELF/DELETED 超 180 天）----------
CREATE TABLE `t_goods_archive` (
  `id` BIGINT NOT NULL COMMENT '沿用主表主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
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
CREATE TABLE `t_outbox_event` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Outbox 待投递事件（每业务库同构）';
