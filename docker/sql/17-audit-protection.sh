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
  local table="$3"
  if [ -z "$4" ]; then
    return 0
  fi
  ${MYSQL_CMD} <<EOSQL
REVOKE DELETE ON \`${db}\`.\`${table}\` FROM '${user}'@'%';
FLUSH PRIVILEGES;
EOSQL
  echo "[audit-protect] ${db}.${table} -> ${user} DELETE revoked"
}

protect campus_admin  admin_db  t_audit_record   "${DB_PWD_ADMIN}"
protect campus_trade  trade_db  t_balance_flow   "${DB_PWD_TRADE}"
protect campus_ai     ai_db     t_ai_tool_call   "${DB_PWD_AI}"
protect campus_errand errand_db t_grab_record    "${DB_PWD_ERRAND}"

echo "[audit-protect] done"
