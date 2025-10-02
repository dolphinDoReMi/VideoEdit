#!/usr/bin/env bash
set -euo pipefail
. "$(dirname "$0")/_env.sh"
ensure_app
push_wav "${1:-real_speech_16k.wav}"

parse_rtf() { 
    adb logcat -d | grep "$LOGTAG" | grep -Eo 'rtf=[0-9. ]+' | tail -n1 | awk -F= '{print $2}' || echo "0.0"
}

run_with_threads() {
  local thr="$1"
  adb logcat -c
  adb shell am broadcast -a com.mira.whisper.ASR_FILE_RUN \
    --es path "$APP_DIR/_asr.wav" --es lang "en" --ei threads "$thr" --ez useBeam false >/dev/null
  sleep "${SLEEP_FOR:-8}"
  parse_rtf
}

RTF1=$(run_with_threads 1)
RTF4=$(run_with_threads 4)
echo "RTF @1 thread: $RTF1"
echo "RTF @4 threads: $RTF4"
python3 - <<PY
r1=float("$RTF1"); r4=float("$RTF4")
print("✅ RTF improved" if r4<r1 else "⚠️ No RTF improvement (device bound?)")
PY
