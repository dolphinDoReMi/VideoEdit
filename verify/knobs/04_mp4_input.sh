#!/usr/bin/env bash
set -euo pipefail
. "$(dirname "$0")/_env.sh"
ensure_app
MP4_IN="${1:-speech.mp4}"
adb push "$MP4_IN" /sdcard/Download/_asr.mp4 >/dev/null
adb shell run-as $PKG cp /sdcard/Download/_asr.mp4 "$APP_DIR/_asr.mp4"

adb logcat -c
adb shell am broadcast -a com.mira.whisper.ASR_DIR_SCAN \
  --es lang "en" --ez useBeam false >/dev/null
sleep "${SLEEP_FOR:-12}"

adb shell run-as $PKG ls -l "$APP_DIR" | grep _asr.json && echo "✅ MP4 JSON produced" || (echo "❌ MP4 failed"; exit 1)
