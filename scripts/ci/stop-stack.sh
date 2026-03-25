#!/usr/bin/env bash
set -euo pipefail
[ -f /tmp/config-ci.pid ] && kill "$(cat /tmp/config-ci.pid)" 2>/dev/null || true
[ -f /tmp/eureka-ci.pid ] && kill "$(cat /tmp/eureka-ci.pid)" 2>/dev/null || true
echo "[CI] Processus Eureka / Config arrêtés."
