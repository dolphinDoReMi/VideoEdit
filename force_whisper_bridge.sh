#!/bin/bash

# Ultra-simple test - force WhisperBridge instantiation
echo "=== Force WhisperBridge Instantiation ==="

# Create a simple test file that will trigger WhisperBridge
echo "1. Creating test file..."
adb shell "echo 'test' > /sdcard/test.txt"

# Try to trigger transcription directly via WorkManager
echo "2. Triggering WorkManager directly..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'force_workmanager' 'true' --es 'file_path' '/sdcard/test.txt'"

echo "3. Check for any WhisperBridge activity:"
adb logcat -d | grep -E "(WhisperBridge|whisper_jni|loadLibrary)" | tail -5

echo "4. If no logs, WhisperBridge is never being accessed"
