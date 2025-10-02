#!/usr/bin/env bash
set -euo pipefail
. "$(dirname "$0")/_env.sh"
ensure_app
push_wav "${1:-real_speech_16k.wav}"

adb logcat -c
adb shell am broadcast -a com.mira.whisper.ASR_FILE_RUN \
  --es path "$APP_DIR/_asr.wav" --ez wordTimestamps true >/dev/null
sleep "${SLEEP_FOR:-8}"
adb logcat -d | grep "$LOGTAG" | grep -m1 "wordTS=1" && echo "✅ wordTimestamps reached decoder" || echo "⚠️ wordTimestamps not observed"
