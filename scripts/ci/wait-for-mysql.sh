#!/usr/bin/env bash
# Attend que MySQL reponde sur 127.0.0.1:3306 (port publie par le service Docker GitHub Actions).
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
if [ "$MYSQL_HOST" = "mysql" ]; then
  MYSQL_HOST=127.0.0.1
fi
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"

echo "[CI] Attente MySQL (${MYSQL_HOST}:3306)..."
sudo apt-get update -qq
sudo apt-get install -y -qq mysql-client

for i in $(seq 1 45); do
  if mysqladmin ping -h "$MYSQL_HOST" -P 3306 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent 2>/dev/null; then
    echo "[CI] MySQL pret (tentative ${i}/45)."
    exit 0
  fi
  echo "[CI] MySQL pas encore pret... (${i}/45)"
  sleep 2
done

echo "[CI] ERREUR: MySQL injoignable sur ${MYSQL_HOST}:3306."
exit 1
