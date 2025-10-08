#!/bin/bash

# Direct WebView test - force JNI loading
echo "=== Direct WebView JNI Test ==="

# Start the app
echo "1. Starting app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
sleep 3

# Check WebView status
echo "2. Checking WebView status..."
adb logcat -d | grep -E "(WhisperBridge available|Whisper Unified Interface loaded)" | tail -2

# Try to call testJNI directly via JavaScript
echo "3. Attempting direct JavaScript call..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'direct_js_call' 'AndroidWhisper.testJNI()'"

echo "4. Check for JNI activity:"
adb logcat -d | grep -E "(Testing JNI|JNI loaded|JNI loading failed|Forcing WhisperBridge)" | tail -3

echo "5. Check for System.loadLibrary calls:"
adb logcat -d | grep -E "(loadLibrary|whisper_jni)" | tail -3

echo "6. If no logs, the JNI library is still not loading"
