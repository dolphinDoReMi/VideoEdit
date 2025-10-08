#!/bin/bash

# Direct JNI test - bypass all complexity
echo "=== Direct JNI Test ==="

# Start the app
echo "1. Starting app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
sleep 2

# Try to call the testJNI method directly via JavaScript
echo "2. Attempting to call testJNI method..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'call_test_jni' 'true'"

echo "3. Check for JNI loading logs:"
adb logcat -d | grep -E "(Testing JNI|JNI loaded|JNI loading failed|Forcing WhisperBridge)" | tail -5

echo "4. Check for System.loadLibrary calls:"
adb logcat -d | grep -E "(loadLibrary|whisper_jni)" | tail -3

echo "5. If no logs, the JNI library is still not loading"
