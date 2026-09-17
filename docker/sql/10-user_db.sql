-- =============================================================
-- user_db ｜ M1 用户与信用服务 ｜ 4 张表
-- 依据《系统设计》§4.2.t1 / §4.2.10 / specs/05-用户与认证
-- =============================================================
USE `user_db`;

-- ---------- t_user_account（§4.2.t1 核心表）----------
CREATE TABLE `t_user_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
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
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_email` (`email`),
  KEY `idx_status_created` (`status`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账号与信用主档';

-- ---------- t_credit_record（信用分增减明细，§4.2.10）----------
CREATE TABLE `t_credit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `delta` INT NOT NULL COMMENT '本次增减值（可负）',
  `source_type` VARCHAR(20) NOT NULL COMMENT '来源：REVIEW/FULFILL/DEFAULT',
  `score_after` INT NOT NULL COMMENT '变动后信用分快照',
  `biz_event_id` VARCHAR(64) NOT NULL COMMENT '业务事件键（幂等，uk）',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_event` (`biz_event_id`),
  KEY `idx_user_created` (`user_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='信用分增减明细（永久保留）';

-- ---------- t_login_log（登录流水，安全审计第五维配套）----------
CREATE TABLE `t_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
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
  `school_code` VARCHAR(20) NOT NULL DEFAULT 'CAMPUS-MAIN' COMMENT '学校维度预留字段（§4.1）',
  `user_id` BIGINT NOT NULL COMMENT '本地账号 ID',
  `idp_type` VARCHAR(20) NOT NULL COMMENT '身份源：LOCAL/QQ/WECHAT/CAS',
  `openid` VARCHAR(64) NOT NULL COMMENT '身份源侧唯一标识',
  `unionid` VARCHAR(64) DEFAULT NULL COMMENT '身份源侧主体标识（微信 unionid 预留）',
  `bound_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '绑定时间',
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志（0 存在 1 已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idp_openid` (`idp_type`, `openid`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='第三方身份绑定（预留）';
