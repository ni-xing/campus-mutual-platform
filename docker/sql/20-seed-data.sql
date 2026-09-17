-- =============================================================
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

-- 管理端种子账号（role=ADMIN；密码摘要对应明文见 .env.example 的 SEED_ADMIN_PASSWORD，登录后强制修改）
-- 摘要由真实明文 Admin@Campus2026 生成（BCrypt cost 10，$2b 前缀 Spring Security 兼容）
INSERT INTO `t_user_account`
  (`school_code`, `student_no`, `email`, `password_hash`, `nickname`, `role`, `status`, `credit_score`, `created_by`)
VALUES
  ('CAMPUS-MAIN', 'ADMIN0001', 'admin@campus.edu', '$2b$10$qTIk.Wra6ipTuaa04AE82exBRdvj47TADZC1hbhbjJ0sNxV9dGKJS',
   '平台管理员', 'ADMIN', 'ACTIVE', 100, 'seed');

USE `trade_db`;

-- 演示用余额账户（user_id 用子查询动态绑定 ADMIN 账号，不假定自增=1）
INSERT INTO `t_balance_account` (`school_code`, `user_id`, `balance`, `frozen`, `created_by`)
SELECT 'CAMPUS-MAIN', `id`, 100.00, 0.00, 'seed'
  FROM `user_db`.`t_user_account` WHERE `student_no` = 'ADMIN0001';
