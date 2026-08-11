#!/usr/bin/env bash
# ============================================================
# MySQL 数据库备份脚本
# 备份全部 campus_* 数据库到 ./backups 目录，保留最近 7 份
# 用法：
#   ./scripts/backup-db.sh
# 定时任务（每天 03:00 备份）：
#   crontab -e
#   0 3 * * * cd /opt/campus-idle-cloud && ./scripts/backup-db.sh >> ./backups/backup.log 2>&1
# ============================================================
set -euo pipefail
cd "$(dirname "$0")/.."

BACKUP_DIR=./backups
KEEP=7
STAMP="$(date +%Y%m%d_%H%M%S)"

mkdir -p "$BACKUP_DIR"

DBS=(campus_auth campus_user campus_item campus_order campus_admin)

for db in "${DBS[@]}"; do
  echo "[$(date +%F' '%T)] backing up $db ..."
  docker compose exec -T mysql sh -c "exec mysqldump -uroot -p\"\$MYSQL_ROOT_PASSWORD\" --databases $db --single-transaction --routines --triggers --events" \
    > "$BACKUP_DIR/${db}_${STAMP}.sql"
done

# 压缩为 tar.gz 并删除原始 sql，减少磁盘占用
(cd "$BACKUP_DIR" && tar -czf "campus_db_${STAMP}.tar.gz" *_"${STAMP}".sql && rm -f *_"${STAMP}".sql)

# 保留最近 ${KEEP} 份压缩包
ls -1t "$BACKUP_DIR"/campus_db_*.tar.gz 2>/dev/null | tail -n +"$((KEEP+1))" | xargs -r rm -f

echo "[$(date +%F' '%T)] backup done -> $BACKUP_DIR/campus_db_${STAMP}.tar.gz"
