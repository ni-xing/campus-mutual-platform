-- =============================================================
-- 校园互助生活平台 · 数据库初始化（第 1 步：建库）
-- 依据《系统设计》§4.1：7 个业务库，每业务服务独立 schema，禁止跨库 JOIN
-- 执行方：MySQL 容器 /docker-entrypoint-initdb.d/（仅首次初始化空数据卷时执行）
-- =============================================================
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `user_db`      DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'M1 用户与信用';
CREATE DATABASE IF NOT EXISTS `trade_db`     DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'M2 二手交易（含余额支付）';
CREATE DATABASE IF NOT EXISTS `lostfound_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'M3 失物招领';
CREATE DATABASE IF NOT EXISTS `errand_db`    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'M4 跑腿拼单';
CREATE DATABASE IF NOT EXISTS `ai_db`        DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'M5 AI 助手';
CREATE DATABASE IF NOT EXISTS `notify_db`    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'M6 通知';
CREATE DATABASE IF NOT EXISTS `admin_db`     DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT 'M7 平台管理';

-- ngram 全文解析器：MySQL 8 内置，用于替代 ES 的关键词检索（§3.1.5 / O3）
-- 内置分词器 ngram_token_size 默认 2，本地 dev 保持默认；生产如调优需写入 mysqld 配置并重启
