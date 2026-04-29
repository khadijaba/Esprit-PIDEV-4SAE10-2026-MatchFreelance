#!/usr/bin/env bash
# Prints absolute path to Chrome-for-Testing binary (for CHROME_BIN / Karma). No root required.
set -euo pipefail
VERSION="${CHROME_CI_VERSION:-131.0.6778.204}"
WS="${WORKSPACE:?}"
ROOT="$WS/.ci-chrome"

OS="$(uname -s)"
ARCH_RAW="$(uname -m)"
case "$OS" in
  Linux*) OSL=linux ;;
  Darwin*) OSL=darwin ;;
  *) echo "Unsupported OS: $OS" >&2; exit 1 ;;
esac

PLATFORM=""
CHROME_REL=""

if [ "$OSL" = linux ] && [ "$ARCH_RAW" = x86_64 ]; then
  PLATFORM=linux64
  CHROME_REL="chrome-linux64/chrome"
elif [ "$OSL" = linux ] && { [ "$ARCH_RAW" = aarch64 ] || [ "$ARCH_RAW" = arm64 ]; }; then
  PLATFORM=linux-arm64
  CHROME_REL="chrome-linux64/chrome"
elif [ "$OSL" = darwin ] && { [ "$ARCH_RAW" = arm64 ] || [ "$ARCH_RAW" = aarch64 ]; }; then
  PLATFORM=mac-arm64
  CHROME_REL="chrome-mac-arm64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing"
elif [ "$OSL" = darwin ] && [ "$ARCH_RAW" = x86_64 ]; then
  PLATFORM=mac-x64
  CHROME_REL="chrome-mac-x64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing"
else
  echo "Unsupported platform ${OSL} ${ARCH_RAW}" >&2
  exit 1
fi

case "$PLATFORM" in
  linux64)       ZIP_NAME=chrome-linux64.zip ;;
  linux-arm64)   ZIP_NAME=chrome-linux64.zip ;;
  mac-arm64)     ZIP_NAME=chrome-mac-arm64.zip ;;
  mac-x64)       ZIP_NAME=chrome-mac-x64.zip ;;
  *) echo "Internal error: bad PLATFORM=$PLATFORM" >&2; exit 1 ;;
esac

TARGET="$ROOT/${VERSION}-${PLATFORM}"
BIN="${TARGET}/${CHROME_REL}"
mkdir -p "$ROOT"

if [ ! -x "$BIN" ]; then
  rm -rf "$TARGET"
  mkdir -p "$TARGET"
  ZIP_URL="https://storage.googleapis.com/chrome-for-testing-public/${VERSION}/${PLATFORM}/${ZIP_NAME}"
  TMP="${WS}/.ci-chrome-download.zip"
  curl -fsSL "$ZIP_URL" -o "$TMP"
  if command -v unzip >/dev/null 2>&1; then
    unzip -qo "$TMP" -d "$TARGET"
  elif command -v python3 >/dev/null 2>&1; then
    python3 -c "import zipfile,sys; zipfile.ZipFile(sys.argv[1]).extractall(sys.argv[2])" "$TMP" "$TARGET"
  else
    echo "Need unzip or python3 to extract Chrome" >&2
    exit 1
  fi
  rm -f "$TMP"
fi

if [ ! -x "$BIN" ]; then
  echo "Chrome binary not executable at $BIN" >&2
  exit 1
fi

printf '%s' "$BIN"
