#!/usr/bin/env bash
#
# Cloud Agent install script for Slate (Android app; Kotlin + Jetpack Compose).
#
# Prepares the Android SDK required to build the app and warms the Gradle
# dependency cache. Designed to be idempotent: the SDK is installed only when
# missing, so on a prebuilt snapshot this converges quickly.
set -euo pipefail

# Pin the toolchain to what the project targets (see gradle/libs.versions.toml).
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
CMDLINE_TOOLS_VERSION="13114758"
SDK_PACKAGES=(
  "platform-tools"
  "platforms;android-36"
  "build-tools;36.0.0"
)

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Create the SDK root, using sudo only when the target is not writable.
ensure_sdk_root() {
  if [ -d "$ANDROID_SDK_ROOT" ] && [ -w "$ANDROID_SDK_ROOT" ]; then
    return 0
  fi
  local parent; parent="$(dirname "$ANDROID_SDK_ROOT")"
  if [ -w "$parent" ] || mkdir -p "$ANDROID_SDK_ROOT" 2>/dev/null; then
    mkdir -p "$ANDROID_SDK_ROOT"
  else
    sudo mkdir -p "$ANDROID_SDK_ROOT"
    sudo chown -R "$(id -un):$(id -gn)" "$ANDROID_SDK_ROOT"
  fi
}

install_cmdline_tools() {
  local sdkmanager="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
  if [ -x "$sdkmanager" ]; then
    return 0
  fi
  echo "Installing Android command-line tools (${CMDLINE_TOOLS_VERSION})..."
  local tmp; tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/cmdline-tools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp"
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  rm -rf "$tmp"
}

ensure_sdk_root
install_cmdline_tools

SDKMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"

# Accept licenses (idempotent) and install/update the pinned SDK packages.
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
echo "Ensuring SDK packages: ${SDK_PACKAGES[*]}"
"$SDKMANAGER" "${SDK_PACKAGES[@]}" >/dev/null

# Point Gradle at the SDK without mutating shell profiles.
echo "sdk.dir=$ANDROID_SDK_ROOT" > "$REPO_DIR/local.properties"

# Warm the Gradle wrapper and dependency cache with the documented build.
chmod +x "$REPO_DIR/gradlew"
echo "Building debug APK to warm caches..."
( cd "$REPO_DIR" && ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT" ./gradlew assembleDebug )

echo "Slate environment ready. Debug APK: app/build/outputs/apk/debug/app-debug.apk"
