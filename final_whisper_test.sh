#!/bin/bash

# Final test - direct WhisperBridge instantiation
echo "=== Final WhisperBridge Test ==="

# Create a simple test that will force WhisperBridge to be instantiated
echo "1. Creating minimal test..."
adb shell "echo 'test' > /sdcard/minimal_test.txt"

# Try to trigger any WhisperBridge usage
echo "2. Attempting to trigger WhisperBridge..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'minimal_test' 'true'"

echo "3. Check for ANY WhisperBridge activity:"
adb logcat -d | grep -i whisperbridge | tail -3

echo "4. Check for System.loadLibrary calls:"
adb logcat -d | grep -E "(loadLibrary|whisper_jni)" | tail -3

echo "5. If no logs, the issue is that WhisperBridge is never accessed"
