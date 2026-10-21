#!/usr/bin/env bash
# Copy of the production database (design.md 17.4; WORKFLOW Task 4.8, decision 13).
# Run every day by cron and by deploy.sh before each deploy. Writes backups/db-<UTC time>.sql.gz in APP_DIR,
# the folder of docker-compose.prod.yml and its .env, and deletes the copies older than 7 days.
#
#   /opt/trip-planner/backup.sh                     on the server
#   APP_DIR=/other/folder ./backup.sh               the application lives elsewhere
#
# Restore: docs/deploy/aws.md 15.4.
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/trip-planner}"
cd "$APP_DIR"

# The copies hold the whole database, password hashes included: readable by the owner only
umask 077
mkdir -p backups

stamp="$(date -u +%Y%m%d-%H%M%S)"
target="backups/db-$stamp.sql.gz"
partial="$target.partial"
# A failed run leaves nothing behind that looks like a good copy
trap 'rm -f "$partial"' EXIT

# Runs mysqldump inside the mysql container, as the user of the application, with the password the container
# already has: no password on this machine's command line. --single-transaction: one consistent picture of the
# InnoDB tables without locking them, the site keeps working during the copy. --no-tablespaces: not needed for
# one database, and it would ask for a privilege the user does not have
docker compose -f docker-compose.prod.yml exec -T mysql \
  sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump --single-transaction --no-tablespaces --routines --triggers -u"$MYSQL_USER" "$MYSQL_DATABASE"' \
  | gzip > "$partial"

# mysqldump writes this line last, only when it got to the end
if ! gzip -dc "$partial" | tail -n 1 | grep -q '^-- Dump completed'; then
  echo "$(date -u '+%F %T') backup FAILED: the dump is incomplete" >&2
  exit 1
fi
mv "$partial" "$target"

find backups -maxdepth 1 -name 'db-*.sql.gz' -mtime +7 -delete

echo "$(date -u '+%F %T') backup ok: $target ($(du -h "$target" | cut -f1))"
