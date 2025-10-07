#!/bin/bash

# Simple direct JNI test - no broadcasts, no complexity
echo "=== Direct JNI Test ==="

# Create a simple test that directly calls the JNI
echo "1. Creating minimal test audio file..."
adb shell "echo 'test audio data' > /sdcard/test_audio.txt"

echo "2. Test if we can load the JNI library at all..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'test_jni_load' 'true'"

echo "3. Check for any JNI activity:"
adb logcat -d | grep -E "(WhisperJNI|whisper_jni|JNI)" | tail -3

echo "4. If no JNI logs, the library isn't loading"
