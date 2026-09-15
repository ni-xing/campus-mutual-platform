#!/bin/bash
# =============================================================
# 第 2 步：创建 7 个业务账号（按库最小权限，§4.1 / 安全设计 §3.3.3）
# 密码由 .env 注入，禁止写入任何入库文件（安全设计 §6.3 六禁红线）
# 生产环境必须为 7 个账号设置互不相同的强口令（禁多用途共用）
# =============================================================
set -e

MYSQL_CMD="mysql -uroot -p${MYSQL_ROOT_PASSWORD}"

create_account() {
  local user="$1"
  local pwd="$2"
  local db="$3"
  if [ -z "${pwd}" ]; then
    echo "[init-users] skip ${user}: password empty"
    return 0
  fi
  ${MYSQL_CMD} <<EOSQL
CREATE USER IF NOT EXISTS '${user}'@'%' IDENTIFIED BY '${pwd}';
ALTER USER '${user}'@'%' IDENTIFIED BY '${pwd}';
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${db}\`.* TO '${user}'@'%';
FLUSH PRIVILEGES;
EOSQL
  echo "[init-users] ${user} -> ${db} ok"
}

create_account campus_user      "${DB_PWD_USER}"      user_db
create_account campus_trade     "${DB_PWD_TRADE}"     trade_db
create_account campus_lostfound "${DB_PWD_LOSTFOUND}" lostfound_db
create_account campus_errand    "${DB_PWD_ERRAND}"    errand_db
create_account campus_ai        "${DB_PWD_AI}"        ai_db
create_account campus_notify    "${DB_PWD_NOTIFY}"    notify_db
create_account campus_admin     "${DB_PWD_ADMIN}"     admin_db

echo "[init-users] all accounts ready"
