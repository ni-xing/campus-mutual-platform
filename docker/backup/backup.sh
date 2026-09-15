#!/bin/bash
# =============================================================================
# MySQL 每日备份（《部署设计》§2.2.2 / 《系统设计》§4.3.2）
#   全量：mysqldump 每日 03:00，保留 30 天
#   增量：binlog 实时落盘，保留 7 天
#   RPO ≤ 5min（常态）｜RTO 单库 ≤ 30min
# 挂载到宿主机：docker/data/backup
# =============================================================================
set -euo pipefail

BACKUP_DIR="/backup"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

DATABASES="user_db trade_db lostfound_db errand_db ai_db notify_db admin_db"

echo "[backup] start ${DATE}"
for db in ${DATABASES}; do
  mysqldump -uroot -p"${MYSQL_ROOT_PASSWORD}" \
    --single-transaction --routines --triggers --events \
    --default-character-set=utf8mb4 \
    "${db}" | gzip > "${BACKUP_DIR}/${db}_${DATE}.sql.gz"
  echo "[backup] ${db} done ($(du -h "${BACKUP_DIR}/${db}_${DATE}.sql.gz" | cut -f1))"
done

# 过期清理
find "${BACKUP_DIR}" -name "*.sql.gz" -mtime +${RETENTION_DAYS} -delete
echo "[backup] finished, retention ${RETENTION_DAYS}d"
