#!/usr/bin/env bash
# Démarre MySQL (déjà fourni comme service GitHub Actions) + Eureka + Config Server.
# Usage : depuis la racine du dépôt, MYSQL_HOST=mysql (hostname du service Docker).
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-mysql}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

echo "[CI] Attente MySQL (${MYSQL_HOST})..."
sudo apt-get update -qq
sudo apt-get install -y -qq mysql-client

for _ in $(seq 1 40); do
  if mysql -h "$MYSQL_HOST" -P 3306 -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1" 2>/dev/null; then
    break
  fi
  sleep 2
done

echo "[CI] Création des bases FormationDB / EvaluationDB..."
mysql -h "$MYSQL_HOST" -P 3306 -uroot -p"$MYSQL_ROOT_PASSWORD" -e \
  "CREATE DATABASE IF NOT EXISTS FormationDB; CREATE DATABASE IF NOT EXISTS EvaluationDB;"

echo "[CI] Build + démarrage Eureka (8761)..."
cd "$ROOT_DIR/backend/EurekaServer/EurekaServer"
chmod +x mvnw
./mvnw -B package -DskipTests -q
nohup java -jar target/eurekaServer-0.0.1-SNAPSHOT.jar > /tmp/eureka-ci.log 2>&1 &
echo $! > /tmp/eureka-ci.pid

for _ in $(seq 1 60); do
  if bash -c "echo >/dev/tcp/127.0.0.1/8761" 2>/dev/null; then
    echo "[CI] Eureka OK (port 8761)"
    break
  fi
  sleep 2
done

echo "[CI] Build + démarrage Config Server (8888)..."
cd "$ROOT_DIR"
chmod +x ./backend/EurekaServer/EurekaServer/mvnw
./backend/EurekaServer/EurekaServer/mvnw -B -f backend/ConfigServer/pom.xml package -DskipTests -q
nohup java -jar backend/ConfigServer/target/config-server-0.0.1-SNAPSHOT.jar > /tmp/config-ci.log 2>&1 &
echo $! > /tmp/config-ci.pid

for _ in $(seq 1 60); do
  if bash -c "echo >/dev/tcp/127.0.0.1/8888" 2>/dev/null; then
    echo "[CI] Config Server OK (port 8888)"
    break
  fi
  sleep 2
done

echo "[CI] Stack prête (MySQL + Eureka + Config)."
