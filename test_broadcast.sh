#!/bin/bash

# Test DirectWhisperService instead of broadcast receiver
echo "=== Testing DirectWhisperService ==="

# Send DirectWhisperService request directly
echo "1. Starting DirectWhisperService..."
adb shell "am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es 'action' 'com.mira.com.feature.whisper.PROCESS_DIRECT' \
    --es 'uri' 'file:///sdcard/MiraWhisper/clips/tennis_interview_clip_001.mp4' \
    --es 'model' '/sdcard/MiraWhisper/models/whisper-base.q5_1.bin' \
    --es 'threads' '4' \
    --es 'lang' 'auto' \
    --ez 'translate' 'false'"

echo "2. Check for DirectWhisperService logs:"
echo "adb logcat -s DirectWhisperService:D | head -5"

echo "3. Check for TranscribeWorker logs:"
echo "adb logcat -s TranscribeWorker:D | head -5"
