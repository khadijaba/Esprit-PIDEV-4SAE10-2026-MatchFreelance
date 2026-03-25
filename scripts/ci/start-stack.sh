#!/usr/bin/env bash
# Démarre MySQL (service GitHub Actions) + Eureka + Config Server.
# Sur GitHub Actions, le hostname "mysql" ne résout pas toujours depuis le runner : on utilise
# 127.0.0.1:3306 (port publié par le service). Surcharge MYSQL_HOST si besoin.
set -euo pipefail

# Si le workflow passe encore MYSQL_HOST=mysql, le DNS échoue souvent sur le runner :
# on remappe vers l'hôte (port 3306 publié).
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
if [ "$MYSQL_HOST" = "mysql" ]; then
  MYSQL_HOST=127.0.0.1
fi
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

echo "[CI] Vérification connexion MySQL (${MYSQL_HOST}:3306)..."
sudo apt-get update -qq
sudo apt-get install -y -qq mysql-client

CONNECTED=0
for _ in $(seq 1 60); do
  if mysqladmin ping -h "$MYSQL_HOST" -P 3306 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent 2>/dev/null \
     || mysql -h "$MYSQL_HOST" -P 3306 -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1" 2>/dev/null; then
    CONNECTED=1
    break
  fi
  sleep 2
done
if [ "$CONNECTED" != "1" ]; then
  echo "[CI] ERREUR: MySQL injoignable sur ${MYSQL_HOST}:3306 (vérifie le service + ports)."
  exit 1
fi

echo "[CI] Création des bases FormationDB / EvaluationDB..."
for attempt in $(seq 1 5); do
  if mysql -h "$MYSQL_HOST" -P 3306 -uroot -p"$MYSQL_ROOT_PASSWORD" -e \
    "CREATE DATABASE IF NOT EXISTS FormationDB; CREATE DATABASE IF NOT EXISTS EvaluationDB;"; then
    echo "[CI] Bases créées (tentative ${attempt}/5)."
    break
  fi
  echo "[CI] Échec création bases, nouvel essai dans 2s... (${attempt}/5)"
  if [ "$attempt" -eq 5 ]; then
    echo "[CI] ERREUR: impossible de créer les bases."
    exit 1
  fi
  sleep 2
done

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
