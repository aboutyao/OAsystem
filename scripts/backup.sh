#!/bin/bash
# OA System - Database Backup Script
# Usage: ./scripts/backup.sh [daily|weekly|manual]
# Schedule via cron: 0 2 * * * /path/to/scripts/backup.sh daily

set -euo pipefail

BACKUP_TYPE="${1:-manual}"
BACKUP_DIR="/opt/oa-backups"
MYSQL_CONTAINER="oa-mysql"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123456}"
DATABASE="oa_system"
RETENTION_DAYS_DAILY=7
RETENTION_DAYS_WEEKLY=30

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/${DATABASE}_${BACKUP_TYPE}_${TIMESTAMP}.sql.gz"

echo "[$(date)] Starting ${BACKUP_TYPE} backup..."

docker exec "$MYSQL_CONTAINER" \
    mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction --routines --triggers \
    "$DATABASE" | gzip > "$BACKUP_FILE"

FILESIZE=$(du -h "$BACKUP_FILE" | cut -f1)
echo "[$(date)] Backup completed: $BACKUP_FILE ($FILESIZE)"

# Cleanup old backups
case "$BACKUP_TYPE" in
    daily)
        find "$BACKUP_DIR" -name "${DATABASE}_daily_*.sql.gz" -mtime +${RETENTION_DAYS_DAILY} -delete
        ;;
    weekly)
        find "$BACKUP_DIR" -name "${DATABASE}_weekly_*.sql.gz" -mtime +${RETENTION_DAYS_WEEKLY} -delete
        ;;
esac

echo "[$(date)] Cleanup done."
