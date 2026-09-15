-- =============================================================
-- notify_db ｜ M6 通知服务 ｜ 1 张表（无 outbox：本域为投递终点）
-- 依据《系统设计》§4.2.10
-- =============================================================
USE `notify_db`;

-- ---------- t_notice（站内信）----------
CREATE TABLE `t_notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `receiver_id` BIGINT NOT NULL COMMENT '接收人',
  `title` VARCHAR(50) NOT NULL COMMENT '标题',
  `content` VARCHAR(500) NOT NULL COMMENT '正文',
  `biz_type` VARCHAR(20) NOT NULL COMMENT '业务类型：ORDER/CLAIM/ERRAND/SYSTEM',
  `biz_ref` VARCHAR(64) DEFAULT NULL COMMENT '关联业务单号',
  `read_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '已读标志（0 未读 1 已读）',
  `event_id` VARCHAR(64) NOT NULL COMMENT '来源事件键（幂等，防重复投递）',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_receiver_read` (`receiver_id`, `read_flag`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内信（已读 180 天物理清理）';
