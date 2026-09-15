-- =============================================================
-- lostfound_db ｜ M3 失物招领服务 ｜ 4 张表
-- 依据《系统设计》§4.2.t5 / t6 / §4.2.10
-- =============================================================
USE `lostfound_db`;

-- ---------- t_lost_item（§4.2.t5 核心表）----------
CREATE TABLE `t_lost_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
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
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  KEY `idx_type_status_created` (`item_type`, `status`, `created_time`),
  FULLTEXT KEY `ft_title_desc_loc` (`title`, `description`, `location`) WITH PARSER ngram,
  KEY `idx_publisher` (`publisher_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='失物/拾物统一条目';

-- ---------- t_claim_apply（§4.2.t6 核心表）----------
CREATE TABLE `t_claim_apply` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `item_id` BIGINT NOT NULL COMMENT '条目 ID',
  `claimer_id` BIGINT NOT NULL COMMENT '申请人 ID',
  `evidence_desc` VARCHAR(200) NOT NULL COMMENT '凭证描述',
  `contact_hint` VARCHAR(50) NOT NULL COMMENT '联系提示（展示脱敏）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED/APPROVED/REJECTED/RETURNED/CLOSED',
  `resubmit_round` INT NOT NULL DEFAULT 0 COMMENT '重提轮次（≤ 3）',
  `audit_opinion` VARCHAR(200) DEFAULT NULL COMMENT '最近审核意见（联动 t_audit_record）',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核管理员',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_claimer_round` (`item_id`, `claimer_id`, `resubmit_round`),
  KEY `idx_status` (`status`, `created_time`),
  KEY `idx_claimer` (`claimer_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认领申请';

-- ---------- t_lost_item_archive（条目归档，§4.2.10）----------
CREATE TABLE `t_lost_item_archive` (
  `id` BIGINT NOT NULL COMMENT '沿用主表主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
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
