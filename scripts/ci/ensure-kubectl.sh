#!/usr/bin/env bash
# Prints absolute path to kubectl, downloading a static binary into WORKSPACE/.ci-bin if missing.
set -euo pipefail

WS="${WORKSPACE:?WORKSPACE not set}"
BIN="${WS}/.ci-bin"
mkdir -p "$BIN"
K="${BIN}/kubectl"
VERSION="${KUBECTL_VERSION:-v1.30.2}"

ARCH="$(uname -m)"
case "$ARCH" in
  x86_64 | amd64) DL_ARCH=amd64 ;;
  aarch64 | arm64) DL_ARCH=arm64 ;;
  *)
    echo "ensure-kubectl.sh: unsupported arch: ${ARCH}" >&2
    exit 1
    ;;
esac

if [ ! -x "$K" ]; then
  curl -fsSL -o "${K}.tmp" "https://dl.k8s.io/release/${VERSION}/bin/linux/${DL_ARCH}/kubectl"
  mv "${K}.tmp" "$K"
  chmod +x "$K"
fi

printf '%s' "$K"
