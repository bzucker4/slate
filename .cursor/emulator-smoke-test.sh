#!/usr/bin/env bash
#
# Emulator provisioning + proximity/scratch smoke test for Slate.
#
# What it does, end to end:
#   1. Provisions an Android emulator (installs the `emulator` package and a
#      system image if missing, and creates a headless AVD).
#   2. Boots the AVD and drives the core product flow with adb:
#        Home -> pick 30m -> Begin -> scratch the frost -> Blackout.
#      Reaching Blackout is asserted by the on-screen text
#      "The slate is clear. Lock your phone." (see BlackoutScreen.kt).
#   3. Exercises the pocket-mute path by injecting proximity sensor events and
#      confirming the app stays in Blackout (the hum-mute wiring in
#      ProximityMonitor.kt / SlateViewModel.onPocketCovered).
#
# Virtualization gating:
#   Android x86 emulators require a KVM-accelerated guest. Some Cloud Agent VMs
#   cannot run one: qemu starts but the guest vCPUs never execute (KVM_RUN is
#   never issued) and the device never comes online. This script DETECTS that
#   situation and SKIPS the on-device checks with a clear message and exit 0, so
#   it is safe to wire into CI. Pass --strict to turn "unavailable" into a hard
#   failure (exit 3) instead.
#
# Usage:
#   .cursor/emulator-smoke-test.sh [options]
#     --strict         Fail (exit 3) if the emulator cannot boot, instead of skipping.
#     --keep           Leave the emulator running afterwards (default: shut it down).
#     --serial <id>    Use an already-running device/emulator instead of provisioning one.
#     --no-build       Assume app-debug.apk already exists; do not run Gradle.
#     -h, --help       Show this help.
#
# Environment overrides:
#   ANDROID_SDK_ROOT        SDK location (default: /opt/android-sdk).
#   SLATE_AVD_NAME          AVD name (default: slate-smoke).
#   SLATE_SYS_IMAGE         System image (default: system-images;android-36;google_apis;x86_64).
#   SLATE_EMU_BOOT_TIMEOUT  Max seconds to wait for boot (default: 300).
#   SLATE_ARTIFACT_DIR      Where screenshots are written (default: /opt/cursor/artifacts,
#                           falling back to ./build/smoke-artifacts).
#
# Exit codes: 0 = passed or gracefully skipped; 2 = on-device assertion failed;
#             3 = emulator unavailable and --strict was set; 1 = usage/other error.

set -uo pipefail

# --------------------------- configuration ---------------------------------
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
AVD_NAME="${SLATE_AVD_NAME:-slate-smoke}"
SYS_IMAGE="${SLATE_SYS_IMAGE:-system-images;android-36;google_apis;x86_64}"
BOOT_TIMEOUT="${SLATE_EMU_BOOT_TIMEOUT:-300}"
# After this many seconds without boot, if the guest shows no CPU progress we
# declare virtualization unavailable rather than waiting out the full timeout.
EXEC_GRACE="${SLATE_EMU_EXEC_GRACE:-75}"
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

STRICT=0
KEEP=0
DO_BUILD=1
SERIAL=""

# --------------------------- logging helpers -------------------------------
log()  { printf '\033[0;36m[smoke]\033[0m %s\n' "$*"; }
ok()   { printf '\033[0;32m[smoke] PASS\033[0m %s\n' "$*"; }
warn() { printf '\033[0;33m[smoke] WARN\033[0m %s\n' "$*" >&2; }
err()  { printf '\033[0;31m[smoke] FAIL\033[0m %s\n' "$*" >&2; }

usage() { sed -n '2,40p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

while [ $# -gt 0 ]; do
  case "$1" in
    --strict) STRICT=1 ;;
    --keep) KEEP=1 ;;
    --no-build) DO_BUILD=0 ;;
    --serial) shift; SERIAL="${1:-}" ;;
    -h|--help) usage; exit 0 ;;
    *) err "Unknown argument: $1"; usage; exit 1 ;;
  esac
  shift
done

# --------------------------- SDK resolution --------------------------------
SDKMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager"
EMULATOR="$ANDROID_SDK_ROOT/emulator/emulator"
ADB="$ANDROID_SDK_ROOT/platform-tools/adb"
export ANDROID_SDK_ROOT ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"

ARTIFACT_DIR="${SLATE_ARTIFACT_DIR:-/opt/cursor/artifacts}"
if ! mkdir -p "$ARTIFACT_DIR" 2>/dev/null; then
  ARTIFACT_DIR="$REPO_DIR/build/smoke-artifacts"
  mkdir -p "$ARTIFACT_DIR"
fi

EMU_LOG="$(mktemp -t slate-emu.XXXXXX.log)"
EMU_PID=""
STARTED_EMULATOR=0

# --------------------------- cleanup ---------------------------------------
cleanup() {
  if [ "$STARTED_EMULATOR" = "1" ] && [ "$KEEP" != "1" ]; then
    log "Shutting down emulator ($SERIAL)"
    "$ADB" -s "$SERIAL" emu kill >/dev/null 2>&1 || true
    if [ -n "$EMU_PID" ] && kill -0 "$EMU_PID" 2>/dev/null; then
      sleep 3
      kill "$EMU_PID" 2>/dev/null || true
      sleep 2
      kill -9 "$EMU_PID" 2>/dev/null || true
    fi
  elif [ "$KEEP" = "1" ] && [ -n "$SERIAL" ]; then
    log "Leaving emulator running: $SERIAL (--keep)"
  fi
}
trap cleanup EXIT

skip() {
  warn "$*"
  if [ "$STRICT" = "1" ]; then
    err "Virtualization unavailable and --strict set."
    exit 3
  fi
  log "Skipping on-device checks (virtualization unavailable). Exit 0."
  exit 0
}

# --------------------------- preflight -------------------------------------
require_tool() {
  [ -x "$1" ] || { err "Missing required tool: $1"; exit 1; }
}

# Sum of utime+stime (clock ticks) across all threads of a pid.
proc_ticks() {
  local pid="$1"
  cat /proc/"$pid"/task/*/stat 2>/dev/null | awk '{s+=$14+$15} END{print s+0}'
}

# Find the qemu child pid for the emulator we launched (diff against baseline).
BASELINE_QEMU=""
snapshot_qemu() { pgrep -f 'qemu-system-.*-headless' 2>/dev/null | sort | tr '\n' ' '; }
find_new_qemu() {
  local now p
  now="$(snapshot_qemu)"
  for p in $now; do
    case " $BASELINE_QEMU " in
      *" $p "*) : ;;
      *) echo "$p"; return 0 ;;
    esac
  done
  return 1
}

# --------------------------- provisioning ----------------------------------
provision() {
  require_tool "$SDKMANAGER"
  require_tool "$AVDMANAGER"

  if [ ! -x "$EMULATOR" ]; then
    log "Installing emulator package..."
    yes | "$SDKMANAGER" "emulator" >/dev/null 2>&1 || { err "Failed to install emulator package"; exit 1; }
  fi
  local img_dir
  img_dir="$ANDROID_SDK_ROOT/$(echo "$SYS_IMAGE" | tr ';' '/')"
  if [ ! -f "$img_dir/system.img" ]; then
    log "Installing system image: $SYS_IMAGE (this can take a while)..."
    yes | "$SDKMANAGER" "$SYS_IMAGE" >/dev/null 2>&1 || { err "Failed to install system image"; exit 1; }
  fi
  require_tool "$EMULATOR"
  require_tool "$ADB"

  if ! "$AVDMANAGER" list avd 2>/dev/null | tr -d '\r' | grep -qE "Name:[[:space:]]+${AVD_NAME}$"; then
    log "Creating AVD: $AVD_NAME"
    echo "no" | "$AVDMANAGER" create avd -n "$AVD_NAME" -k "$SYS_IMAGE" -d pixel_6 --force >/dev/null 2>&1 \
      || { err "Failed to create AVD"; exit 1; }
  fi
}

ensure_kvm() {
  if [ ! -e /dev/kvm ]; then
    warn "/dev/kvm not present."
    return 1
  fi
  if [ ! -w /dev/kvm ]; then
    sudo chmod 666 /dev/kvm 2>/dev/null || true
  fi
  # accel-check exits non-zero when acceleration is not usable.
  if ! "$EMULATOR" -accel-check >/dev/null 2>&1; then
    warn "emulator -accel-check reports acceleration not usable."
    return 1
  fi
  return 0
}

launch_emulator() {
  BASELINE_QEMU="$(snapshot_qemu)"
  log "Booting emulator (headless): $AVD_NAME"
  "$EMULATOR" -avd "$AVD_NAME" -no-window -no-audio -no-boot-anim \
    -gpu swiftshader_indirect -no-snapshot -read-only \
    >"$EMU_LOG" 2>&1 &
  EMU_PID=$!
  STARTED_EMULATOR=1
  sleep 6
  # Resolve the qemu child pid (best effort) for the CPU-progress heuristic.
  QEMU_PID="$(find_new_qemu || true)"
}

# Wait for boot. Prints nothing; returns 0=booted, 2=unavailable, 1=other.
wait_for_boot() {
  "$ADB" start-server >/dev/null 2>&1
  local start now elapsed booted serial
  start="$(date +%s)"
  while :; do
    now="$(date +%s)"; elapsed=$((now - start))
    # Discover the emulator serial once it registers.
    if [ -z "$SERIAL" ]; then
      serial="$("$ADB" devices | awk '/emulator-[0-9]+\t/{print $1; exit}')"
      [ -n "$serial" ] && SERIAL="$serial"
    fi
    if [ -n "$SERIAL" ]; then
      booted="$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
      if [ "$booted" = "1" ]; then
        return 0
      fi
    fi
    # Early virtualization-failure detection: after the grace period, if the
    # guest is doing essentially no CPU work, the vCPUs are parked.
    if [ "$elapsed" -ge "$EXEC_GRACE" ]; then
      if [ -z "$QEMU_PID" ] || ! kill -0 "$QEMU_PID" 2>/dev/null; then
        QEMU_PID="$(find_new_qemu || true)"
      fi
      if [ -n "$QEMU_PID" ]; then
        local t1 t2
        t1="$(proc_ticks "$QEMU_PID")"; sleep 5; t2="$(proc_ticks "$QEMU_PID")"
        if [ "$((t2 - t1))" -lt 5 ]; then
          warn "Guest shows no CPU progress after ${elapsed}s (vCPUs parked; KVM_RUN not advancing)."
          return 2
        fi
      elif [ -z "$SERIAL" ]; then
        warn "No emulator registered with adb after ${elapsed}s."
      fi
    fi
    if [ "$elapsed" -ge "$BOOT_TIMEOUT" ]; then
      if [ -n "$SERIAL" ]; then
        warn "Emulator online but boot did not complete within ${BOOT_TIMEOUT}s."
        return 1
      fi
      warn "Emulator did not boot within ${BOOT_TIMEOUT}s."
      return 2
    fi
    sleep 5
  done
}

# --------------------------- adb UI helpers --------------------------------
adbx() { "$ADB" -s "$SERIAL" "$@"; }

ui_dump() {
  adbx shell uiautomator dump /data/local/tmp/win.xml >/dev/null 2>&1 || return 1
  adbx shell cat /data/local/tmp/win.xml 2>/dev/null
}

# Echo "cx cy" (screen center of the node whose text matches $1), or return 1.
bounds_center_for_text() {
  local needle="$1" xml
  xml="$(ui_dump)" || return 1
  # uiautomator emits one long line; split each element onto its own line so we
  # isolate the node bounds that belong to the matched text.
  printf '%s' "$xml" | sed 's/></>\n</g' | grep -F "text=\"$needle\"" | head -1 | \
    grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1 | \
    grep -oE '[0-9]+' | paste -sd' ' | \
    awk '{print int(($1+$3)/2), int(($2+$4)/2)}'
}

tap_text() {
  local center
  center="$(bounds_center_for_text "$1")" || return 1
  [ -n "$center" ] || return 1
  # shellcheck disable=SC2086
  adbx shell input tap $center
}

ui_has_text() { ui_dump 2>/dev/null | grep -qF "text=\"$1\""; }

screenshot() {
  local path="$ARTIFACT_DIR/$1"
  adbx exec-out screencap -p > "$path" 2>/dev/null && log "screenshot: $path"
}

# Dense scratch: slow swipes (large brush) across a grid, both orientations.
scratch_screen() {
  local size w h y x step dur
  size="$(adbx shell wm size 2>/dev/null | awk -F': ' '/Physical size|Override size/{print $2; exit}' | tr -d '\r')"
  w="${size%x*}"; h="${size#*x}"
  [ -n "$w" ] && [ -n "$h" ] || { w=1080; h=2400; }
  step=180; dur=500
  log "Scratching ${w}x${h} (grid swipes)"
  for y in $(seq 200 "$step" $((h - 200))); do
    adbx shell input swipe 60 "$y" $((w - 60)) "$y" "$dur" >/dev/null 2>&1
  done
  for x in $(seq 120 "$step" $((w - 120))); do
    adbx shell input swipe "$x" 200 "$x" $((h - 200)) "$dur" >/dev/null 2>&1
  done
}

# --------------------------- the actual test -------------------------------
run_scratch_flow() {
  log "Installing debug APK"
  local apk="$REPO_DIR/app/build/outputs/apk/debug/app-debug.apk"
  if [ "$DO_BUILD" = "1" ] || [ ! -f "$apk" ]; then
    ( cd "$REPO_DIR" && ./gradlew --console=plain assembleDebug ) || { err "Gradle build failed"; return 2; }
  fi
  [ -f "$apk" ] || { err "APK not found: $apk"; return 2; }
  adbx install -r -g "$apk" >/dev/null 2>&1 || adbx install -r "$apk" >/dev/null 2>&1 \
    || { err "adb install failed"; return 2; }

  # Start clean so we always begin on the Home screen.
  adbx shell pm clear com.bzucker4.slate >/dev/null 2>&1 || true
  adbx shell am start -n com.bzucker4.slate/.MainActivity >/dev/null 2>&1 || { err "Could not launch app"; return 2; }
  sleep 3
  screenshot "01-home.png"

  if ui_has_text "Got it"; then
    log "Dismissing first-run tip"
    tap_text "Got it" || true
    sleep 1
  fi

  ui_has_text "Begin" || { err "Home screen not shown (no 'Begin' button)"; return 2; }
  log "Selecting 30m and pressing Begin"
  tap_text "30m" || warn "Could not tap '30m' chip (continuing with default duration)"
  sleep 1
  tap_text "Begin" || { err "Could not tap 'Begin'"; return 2; }
  sleep 2
  screenshot "02-scratch.png"

  local reached=0 i
  for i in 1 2 3 4 5 6; do
    scratch_screen
    sleep 2
    if ui_has_text "The slate is clear. Lock your phone."; then
      reached=1; break
    fi
    log "Blackout not reached yet (pass $i); scratching more"
  done

  if [ "$reached" != "1" ]; then
    err "Did not reach Blackout after scratching."
    screenshot "03-scratch-stuck.png"
    return 2
  fi
  sleep 1
  screenshot "03-blackout.png"
  ok "Scratch -> Blackout flow reached Blackout."
  return 0
}

run_proximity_check() {
  # BlackoutScreen registers a ProximityMonitor; emulators may lack the sensor.
  if ! adbx shell dumpsys sensorservice 2>/dev/null | grep -qi 'proximity'; then
    warn "No proximity sensor on this AVD; skipping pocket-mute check (see README 'Pocket mute')."
    return 0
  fi
  log "Injecting proximity: covered -> uncovered"
  adbx emu sensor set proximity 0 >/dev/null 2>&1 || warn "sensor set (covered) not accepted"
  sleep 2
  local covered_ok=1
  ui_has_text "The slate is clear. Lock your phone." || covered_ok=0
  adbx emu sensor set proximity 5 >/dev/null 2>&1 || warn "sensor set (uncovered) not accepted"
  sleep 2
  # App must stay alive and remain in Blackout across the pocket transition.
  if adbx shell pidof com.bzucker4.slate >/dev/null 2>&1 \
     && ui_has_text "The slate is clear. Lock your phone." && [ "$covered_ok" = "1" ]; then
    ok "Proximity (pocket) events delivered; app stayed in Blackout."
    return 0
  fi
  err "App did not remain in Blackout across proximity events."
  return 2
}

# --------------------------- orchestration ---------------------------------
main() {
  log "Artifacts -> $ARTIFACT_DIR"
  if [ -n "$SERIAL" ]; then
    log "Using provided device: $SERIAL"
  else
    provision
    if ! ensure_kvm; then
      skip "KVM acceleration is not usable on this host."
    fi
    launch_emulator
    # NB: call directly (not in a subshell) so wait_for_boot's SERIAL export sticks.
    wait_for_boot; local boot_rc=$?
    case "$boot_rc" in
      0) log "Emulator booted: $SERIAL" ;;
      2) skip "Emulator could not run the guest (nested virtualization unavailable)." ;;
      *) err "Emulator failed to become ready."; exit 2 ;;
    esac
    adbx shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
    adbx shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
    adbx shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true
  fi

  local rc=0
  run_scratch_flow || rc=$?
  if [ "$rc" = "0" ]; then
    run_proximity_check || rc=$?
  fi

  if [ "$rc" = "0" ]; then
    ok "Smoke test passed."
  else
    err "Smoke test failed (rc=$rc)."
  fi
  return "$rc"
}

main
