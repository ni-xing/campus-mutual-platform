-- =============================================================
-- W2 增量变更：2026-09-17 ｜ M2 二手交易模块实现（specs/01）
-- 适用于已初始化的现库（全新初始化走 11-trade_db.sql，无需本脚本）
--   1. t_goods + view_count   ：浏览量（Redis 自增 + 定时回写，S-03）
--   2. t_balance_account + points：交易积分（订单金额 1%，S-07）
-- =============================================================
USE `trade_db`;

ALTER TABLE `t_goods`
  ADD COLUMN `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量（Redis 自增 + 定时任务回写，W2）'
  AFTER `audit_status`;

ALTER TABLE `t_balance_account`
  ADD COLUMN `points` INT NOT NULL DEFAULT 0 COMMENT '交易积分（订单金额 1% 向下取整，W2）'
  AFTER `frozen`;
