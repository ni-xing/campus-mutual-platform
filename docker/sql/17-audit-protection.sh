#!/bin/bash
# =============================================================
# 第 3 步：审计表防篡改（安全设计 §7.2.4）
# 业务账号对审计类表只保留 SELECT/INSERT/UPDATE，撤销 DELETE —— 审计记录不可删除
# =============================================================
set -e

MYSQL_CMD="mysql -uroot -p${MYSQL_ROOT_PASSWORD}"

protect() {
  local user="$1"
  local db="$2"
  if [ -z "$3" ]; then
    return 0
  fi
  # 注意：账号授权是库级（ON db.*），REVOKE 必须同粒度——按表级 REVOKE 会报
  # ERROR 1147 且 set -e 中断整个 init（曾导致 20-seed-data.sql 未执行）。
  # 做法：整库收回全部权限后按最小集重授（不含 DELETE）。
  ${MYSQL_CMD} <<EOSQL
REVOKE ALL PRIVILEGES ON \`${db}\`.* FROM '${user}'@'%';
GRANT SELECT, INSERT, UPDATE ON \`${db}\`.* TO '${user}'@'%';
FLUSH PRIVILEGES;
EOSQL
  echo "[audit-protect] ${db} -> ${user} DELETE revoked"
}

protect campus_admin  admin_db   "${DB_PWD_ADMIN}"
protect campus_trade  trade_db   "${DB_PWD_TRADE}"
protect campus_ai     ai_db      "${DB_PWD_AI}"
protect campus_errand errand_db  "${DB_PWD_ERRAND}"

echo "[audit-protect] done"
