#!/usr/bin/env bash
# Build Spring Boot JARs for all Java microservices (atelier: mvn before docker compose).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODULES=(
  eureka-server
  config-server
  user-service
  project-service
  candidature-service
  contract-service
  interview-service
  evaluation-service
  formation-service
  skill-service
  analytics-service
  api-gateway
)
for m in "${MODULES[@]}"; do
  echo ">>> mvn package: $m"
  (cd "$ROOT/$m" && mvn -B clean package -DskipTests -q)
done
echo "Done. Run: docker compose up --build"
