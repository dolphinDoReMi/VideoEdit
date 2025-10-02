#!/usr/bin/env bash
set -euo pipefail
PKG="com.mira.whisper.debug"
APP_DIR="/data/user/0/$PKG/files/asr_in"
LOGTAG="WhisperJNI"

ensure_app() { 
    ./gradlew :mira_whisper:installDebug >/dev/null
    adb shell run-as $PKG mkdir -p "$APP_DIR"
}

push_wav() { 
    local SRC="${1:?wav path}"
    # Copy directly to app's internal storage
    adb shell "run-as $PKG sh -c 'cat > $APP_DIR/_asr.wav'" < "$SRC"
}

push_mp4() { 
    local SRC="${1:?mp4 path}"
    # Copy directly to app's internal storage
    adb shell "run-as $PKG sh -c 'cat > $APP_DIR/_asr.mp4'" < "$SRC" 2>/dev/null || true
}
