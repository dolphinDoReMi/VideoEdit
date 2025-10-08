#!/bin/bash

# Direct WhisperBridge instantiation test
echo "=== Direct WhisperBridge Instantiation ==="

# Create a simple test that directly calls WhisperBridge
echo "1. Creating direct test..."
adb shell "echo 'test' > /sdcard/direct_test.txt"

# Try to trigger WhisperBridge directly via a simple broadcast
echo "2. Sending direct broadcast to trigger WhisperBridge..."
adb shell "am broadcast -a com.mira.whisper.RUN --es job_id 'direct_test' --es uri '/sdcard/direct_test.txt' --es preset 'fast' --es model_path '/sdcard/MiraWhisper/models/whisper-base.q5_1.bin' --ei threads 4"

echo "3. Check for WhisperBridge activity:"
adb logcat -d | grep -E "(WhisperBridge|TranscribeWorker|WhisperReceiver)" | tail -5

echo "4. Check for JNI loading:"
adb logcat -d | grep -E "(loadLibrary|whisper_jni)" | tail -3

echo "5. If still no logs, the broadcast receiver isn't working"
