#!/bin/bash

# Direct WebView console test
echo "=== Direct WebView Console Test ==="

# Start the app and wait for WebView to load
echo "1. Starting app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
sleep 3

# Inject JavaScript to directly call WhisperBridge
echo "2. Injecting JavaScript to call WhisperBridge..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'inject_js' 'console.log(\"Testing WhisperBridge...\"); AndroidWhisper.testJNI();'"

echo "3. Check for JNI loading logs:"
adb logcat -d | grep -E "(Testing JNI|JNI loaded|JNI loading failed)" | tail -5

echo "4. Check for console logs:"
adb logcat -d | grep -E "(Testing WhisperBridge|console.log)" | tail -3
