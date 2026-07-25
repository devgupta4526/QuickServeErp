#!/bin/bash
# backup.sh — Daily backup script for QuickServe ERP
set -euo pipefail

BACKUP_DIR="/var/backups/quickserve"
DATE=$(date +%Y%m%d_%H%M%S)
POSTGRES_CONTAINER="quickserve-postgres"
MINIO_CONTAINER="quickserve-minio"

echo "💾 Starting QuickServe backup — $DATE"
mkdir -p "$BACKUP_DIR"

# PostgreSQL backup
echo "  📊 Dumping PostgreSQL..."
docker exec "$POSTGRES_CONTAINER" pg_dump \
    -U "${DB_USERNAME:-quickserve}" \
    -d quickserve \
    --format=custom \
    --compress=9 \
    > "$BACKUP_DIR/postgres_$DATE.dump"

# Compress and remove older backups
find "$BACKUP_DIR" -name "postgres_*.dump" -mtime +30 -delete
find "$BACKUP_DIR" -name "minio_*.tar.gz" -mtime +7 -delete

echo "✅ Backup complete: $BACKUP_DIR"
ls -lh "$BACKUP_DIR" | tail -5
