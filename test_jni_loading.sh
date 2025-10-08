#!/bin/bash

# Test JNI loading via existing testJNI method
echo "=== Test JNI Loading ==="

# Start the app
echo "1. Starting app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
sleep 2

# Call testJNI method via JavaScript injection
echo "2. Calling testJNI method..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'call_test_jni' 'true'"

echo "3. Check for JNI loading logs:"
adb logcat -d | grep -E "(Testing JNI|JNI loaded|JNI loading failed|Forcing WhisperBridge)" | tail -5

echo "4. Check for System.loadLibrary logs:"
adb logcat -d | grep -E "(loadLibrary|whisper_jni)" | tail -3
