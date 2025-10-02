#!/usr/bin/env bash
set -euo pipefail
. "$(dirname "$0")/_env.sh"
ensure_app
push_wav "${1:-real_speech_16k.wav}"

adb logcat -c
adb shell am broadcast -a com.mira.whisper.ASR_FILE_RUN \
  --es path "$APP_DIR/_asr.wav" --es lang "auto" --ez translate false >/dev/null
sleep "${SLEEP_FOR:-6}"
echo "AUTO lang:"
adb logcat -d | grep "$LOGTAG" | grep -m1 "cfg:"

adb logcat -c
adb shell am broadcast -a com.mira.whisper.ASR_FILE_RUN \
  --es path "$APP_DIR/_asr.wav" --es lang "en" --ez translate true >/dev/null
sleep "${SLEEP_FOR:-6}"
echo "EN + translate:"
adb logcat -d | grep "$LOGTAG" | grep -m1 "cfg:"
echo "✅ Lang/Translate flags passed to decoder"
