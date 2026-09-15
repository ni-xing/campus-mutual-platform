-- =============================================================
-- admin_db ｜ M7 平台管理服务 ｜ 4 张表
-- 依据《系统设计》§4.2.10（审核记录不可篡改：业务账号不授予 DELETE，§7.2.4）
-- =============================================================
USE `admin_db`;

-- ---------- t_audit_record（审核处置留痕，永久保留）----------
CREATE TABLE `t_audit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `biz_type` VARCHAR(20) NOT NULL COMMENT '业务类型：GOODS/LOST_ITEM/CLAIM/REPORT',
  `biz_id` BIGINT NOT NULL COMMENT '业务对象 ID',
  `auditor_id` BIGINT NOT NULL COMMENT '处置管理员',
  `decision` VARCHAR(20) NOT NULL COMMENT '决定：APPROVE/REJECT/TAKEDOWN',
  `opinion` VARCHAR(200) NOT NULL COMMENT '审核意见（必填）',
  `exec_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '执行状态：PENDING/EXECUTED/FAILED',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_auditor_created` (`auditor_id`, `created_time`),
  KEY `idx_exec_status` (`exec_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审核处置留痕（永久保留，仅 INSERT 与状态更新）';

-- ---------- t_admin_report（举报记录）----------
CREATE TABLE `t_admin_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `reporter_id` BIGINT NOT NULL COMMENT '举报人',
  `biz_type` VARCHAR(20) NOT NULL COMMENT '被举报对象类型：GOODS/LOST_ITEM/CLAIM',
  `biz_id` BIGINT NOT NULL COMMENT '被举报对象 ID',
  `reason` VARCHAR(200) NOT NULL COMMENT '举报理由',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/DONE/REJECTED',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
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
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`),
  KEY `idx_status_level` (`status`, `level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感词库（DFA 过滤，永久保留）';

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
