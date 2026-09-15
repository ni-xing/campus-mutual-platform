-- =============================================================
-- errand_db ｜ M4 跑腿拼单服务 ｜ 5 张表
-- 依据《系统设计》§4.2.t7 / §4.2.10（并发抢单三重防线：Redisson 锁 → DB 乐观锁 → uk 约束）
-- =============================================================
USE `errand_db`;

-- ---------- t_errand_order（§4.2.t7 核心表）----------
CREATE TABLE `t_errand_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
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
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
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
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
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
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `order_id` BIGINT NOT NULL COMMENT '所属跑腿单 ID',
  `note` VARCHAR(50) DEFAULT NULL COMMENT '子任务备注',
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN/ACCEPTED/DONE',
  `acceptor_id` BIGINT DEFAULT NULL COMMENT '接单人',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_acceptor` (`order_id`, `acceptor_id`),
  KEY `idx_order_status` (`order_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='顺路拼单子任务（D-08，随主单归档）';

-- ---------- t_errand_order_archive（跑腿单归档，§4.2.10）----------
CREATE TABLE `t_errand_order_archive` (
  `id` BIGINT NOT NULL COMMENT '沿用主表主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
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
