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
