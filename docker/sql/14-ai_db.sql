-- =============================================================
-- ai_db ｜ M5 AI 助手服务 ｜ 3 张表
-- 依据《系统设计》§4.2.t9 / §4.2.10（工具白名单 → ACL 适配器 → 后置断言 → 证据表，V4）
-- =============================================================
USE `ai_db`;

-- ---------- t_ai_session（AI 会话，§4.2.10）----------
CREATE TABLE `t_ai_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `user_id` BIGINT NOT NULL COMMENT '归属用户',
  `agent_name` VARCHAR(20) NOT NULL COMMENT 'Agent：TRADE/LOSTFOUND/ERRAND',
  `title` VARCHAR(50) DEFAULT NULL COMMENT '会话标题（首条消息摘要）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/CLOSED',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
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
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `session_id` BIGINT NOT NULL COMMENT '会话 ID',
  `message_id` BIGINT NOT NULL COMMENT '触发消息 ID',
  `agent_name` VARCHAR(20) NOT NULL COMMENT 'Agent：TRADE/LOSTFOUND/ERRAND',
  `tool_name` VARCHAR(40) NOT NULL COMMENT '工具名（白名单键）',
  `idempotency_key` VARCHAR(64) NOT NULL COMMENT '幂等键（sessionId+messageId）',
  `call_result` VARCHAR(20) NOT NULL COMMENT '结果：SUCCESS/FAILED/TIMEOUT/REJECTED_BY_WHITELIST',
  `asserted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '后置断言是否通过',
  `biz_ref` VARCHAR(64) DEFAULT NULL COMMENT '系统证据（订单号/条目 ID）',
  `latency_ms` INT NOT NULL DEFAULT 0 COMMENT '工具调用耗时',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idem` (`idempotency_key`),
  KEY `idx_session` (`session_id`, `created_time`),
  KEY `idx_agent_asserted` (`agent_name`, `asserted`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 工具调用记录（V4 证据表，永久保留）';
