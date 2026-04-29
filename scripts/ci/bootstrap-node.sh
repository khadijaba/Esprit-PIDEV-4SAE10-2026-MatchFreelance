#!/usr/bin/env bash
# Prints the absolute path to the Node bin directory (for PATH). Angular 21+ needs Node >= 20.19.
set -euo pipefail
VERSION="${NODE_CI_VERSION:-20.19.2}"
WS="${WORKSPACE:?}"

OS="$(uname -s)"
ARCH_RAW="$(uname -m)"
case "$OS" in
  Linux*)  OSL=linux ;;
  Darwin*) OSL=darwin ;;
  *) echo "Unsupported OS: $OS" >&2; exit 1 ;;
esac
case "$ARCH_RAW" in
  x86_64)        ARCH=x64 ;;
  aarch64|arm64) ARCH=arm64 ;;
  *) echo "Unsupported arch: $ARCH_RAW" >&2; exit 1 ;;
esac

DIST="node-v${VERSION}-${OSL}-${ARCH}"
ROOT="$WS/.ci-node"
TARGET="$ROOT/$DIST"

mkdir -p "$ROOT"
if [ ! -x "$TARGET/bin/node" ]; then
  rm -rf "$TARGET"
  curl -fsSL "https://nodejs.org/dist/v${VERSION}/${DIST}.tar.gz" | tar xz -C "$ROOT"
fi

printf '%s' "$TARGET/bin"
