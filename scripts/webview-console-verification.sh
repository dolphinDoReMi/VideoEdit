#!/bin/bash

# WebView console test - direct WhisperBridge call
echo "=== WebView Console Test ==="

# Start the app and wait for WebView to load
echo "1. Starting app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
sleep 3

# Check if WebView is loaded
echo "2. Checking WebView status..."
adb logcat -d | grep -E "(WhisperBridge available|WebView)" | tail -3

echo "3. Attempting to call testJNI via JavaScript injection..."
# Try to inject JavaScript to call the testJNI method
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'inject_js' 'AndroidWhisper.testJNI()'"

echo "4. Check for JNI activity:"
adb logcat -d | grep -E "(Testing JNI|JNI loaded|JNI loading failed)" | tail -3

echo "5. If no logs, the JavaScript injection isn't working"
