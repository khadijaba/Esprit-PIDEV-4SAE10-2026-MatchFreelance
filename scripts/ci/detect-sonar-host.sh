#!/usr/bin/env bash
# Prints one Sonar base URL (http://host:9000) that answers /api/system/status. Used from Jenkins when SONAR_HOST_URL is unset.
set -euo pipefail

try() {
  local url="$1"
  if command -v curl >/dev/null 2>&1; then
    curl -sf --connect-timeout 2 "${url}/api/system/status" -o /dev/null 2>/dev/null && return 0
  fi
  if command -v wget >/dev/null 2>&1; then
    wget -q --timeout=2 -O /dev/null "${url}/api/system/status" 2>/dev/null && return 0
  fi
  return 1
}

NS=""
if [[ -r /etc/resolv.conf ]]; then
  NS=$(grep -E '^nameserver[[:space:]]+' /etc/resolv.conf 2>/dev/null | awk 'NR==1{print $2}' | tr -d '\r')
fi

CANDIDATES=(
  "http://host.docker.internal:9000"
  "http://172.17.0.1:9000"
  "http://127.0.0.1:9000"
)
if [[ -n "${NS}" ]]; then
  CANDIDATES+=("http://${NS}:9000")
fi

for url in "${CANDIDATES[@]}"; do
  if try "$url"; then
    printf '%s' "$url"
    exit 0
  fi
done

# Nothing answered; still print WSL-style host (common for Docker-on-WSL2 → Sonar on Windows)
if [[ -n "${NS}" ]]; then
  printf '%s' "http://${NS}:9000"
else
  printf '%s' "http://172.17.0.1:9000"
fi
