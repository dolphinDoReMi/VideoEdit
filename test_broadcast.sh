#!/bin/bash

# Test broadcast receiver directly
echo "=== Testing Broadcast Receiver ==="

# Send ACTION_RUN broadcast directly
echo "1. Sending ACTION_RUN broadcast..."
adb shell "am broadcast -a com.mira.whisper.RUN --es job_id 'test123' --es uri '/sdcard/MiraWhisper/clips/tennis_interview_clip_001.mp4' --es preset 'fast' --es model_path '/sdcard/MiraWhisper/models/whisper-base.q5_1.bin' --ei threads 4"

echo "2. Check for WhisperReceiver logs:"
echo "adb logcat -s WhisperReceiver:D | head -5"

echo "3. Check for TranscribeWorker logs:"
echo "adb logcat -s TranscribeWorker:D | head -5"
