#!/bin/bash

# Test JNI loading via WebView JavaScript
echo "=== WebView JNI Test ==="

# Start the app and wait for it to load
echo "1. Starting app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
sleep 3

# Inject JavaScript to call testJNI
echo "2. Calling testJNI via JavaScript..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'inject_js' 'AndroidWhisper.testJNI()'"

echo "3. Check for JNI logs:"
adb logcat -d | grep -E "(Testing JNI|JNI loaded|JNI loading failed)" | tail -3
