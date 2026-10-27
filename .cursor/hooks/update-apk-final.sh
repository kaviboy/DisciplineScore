#!/bin/bash
# Installs the debug APK once, after a Cursor agent turn finishes.
cat >/dev/null || true
set -u

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT" || exit 0

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

LOG="/tmp/disciplinescore-apk-update.log"
LOCK_DIR="/tmp/disciplinescore-apk.lock"
APK="app/build/outputs/apk/debug/app-debug.apk"

log() {
  printf '%s %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >>"$LOG"
}

newest_source_mtime() {
  find app/src core/src \
    -type f \
    \( -name '*.kt' -o -name '*.kts' -o -name '*.xml' -o -name '*.ttf' -o -name '*.webp' -o -name '*.png' \) \
    -exec stat -f '%m' {} + 2>/dev/null |
    sort -n |
    tail -1
}

apk_mtime() {
  if [ -f "$APK" ]; then
    stat -f '%m' "$APK"
  else
    echo 0
  fi
}

newest="$(newest_source_mtime)"
newest="${newest:-0}"
built="$(apk_mtime)"

if [ "$newest" -le "$built" ]; then
  log "final check: no source changes"
  echo '{}'
  exit 0
fi

if ! mkdir "$LOCK_DIR" 2>/dev/null; then
  log "final check: build already running"
  echo '{}'
  exit 0
fi

finish() {
  rmdir "$LOCK_DIR" 2>/dev/null || true
  echo '{}'
}
trap finish EXIT

log "final changes, building"
if ! ./gradlew :app:assembleDebug --console=plain >>"$LOG" 2>&1; then
  log "build failed"
  exit 0
fi
cp "$APK" DisciplineScore.apk

if ! adb devices | awk 'NR>1 && $2=="device" { found=1 } END { exit !found }'; then
  log "starting emulator"
  nohup emulator -avd DisciplineScore -netdelay none -netspeed full -no-boot-anim -gpu auto >>"$LOG" 2>&1 &
  booted=0
  for _ in $(seq 1 90); do
    if adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' | grep -q '^1$'; then
      booted=1
      break
    fi
    sleep 2
  done
  if [ "$booted" -ne 1 ]; then
    log "emulator did not finish booting"
    exit 0
  fi
fi

if ! adb install -r "$APK" >>"$LOG" 2>&1; then
  log "install failed"
  exit 0
fi
adb shell am force-stop com.disciplinescore.app >>"$LOG" 2>&1 || true
adb shell am start -n com.disciplinescore.app/.MainActivity >>"$LOG" 2>&1 || true
log "installed and launched"
exit 0
