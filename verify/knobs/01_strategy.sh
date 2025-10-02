#!/usr/bin/env bash
set -euo pipefail
. "$(dirname "$0")/_env.sh"
ensure_app
push_wav "${1:-real_speech_16k.wav}"

# Greedy
adb logcat -c
adb shell am broadcast -a com.mira.whisper.ASR_FILE_RUN \
  --es path "$APP_DIR/_asr.wav" --es lang "en" --ez translate false \
  --ei threads 2 --ez useBeam false >/dev/null
sleep "${SLEEP_FOR:-8}"
echo "---- GREEDY ----"
adb logcat -d | grep -m1 "$LOGTAG" | sed -n '1,3p'

# Beam
adb logcat -c
adb shell am broadcast -a com.mira.whisper.ASR_FILE_RUN \
  --es path "$APP_DIR/_asr.wav" --es lang "en" --ez translate false \
  --ei threads 2 --ez useBeam true --ei beamSize 5 --ef patience 1.0 >/dev/null
sleep "${SLEEP_FOR:-12}"
echo "---- BEAM ----"
adb logcat -d | grep "$LOGTAG" | sed -n '1,3p'
echo "✅ Strategy toggle applied (inspect cfg lines above)"
