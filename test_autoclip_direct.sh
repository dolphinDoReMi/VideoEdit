#!/bin/bash

# Direct Auto-Clipper Test Script
# This script will trigger the Auto-Clipper directly

set -e

echo "🚀 Testing Auto-Clipper with TennisInterview.mp4"
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

# Launch the app
echo "📱 Launching app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"

# Wait for app to load
sleep 3

# Trigger Auto-Clipper via JavaScript interface
echo "🔧 Triggering Auto-Clipper via JavaScript interface..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'action' 'execute_js' --es 'js_code' 'console.log(\"Testing Auto-Clipper...\"); if(window.AndroidWhisperBridge && window.AndroidWhisperBridge.startAutoClip) { console.log(\"Bridge available, calling startAutoClip...\"); const result = window.AndroidWhisperBridge.startAutoClip(\"file:///sdcard/Documents/TennisInterview_converted.mp4\", \"content://com.android.externalstorage.documents/tree/primary%3ADocuments\"); console.log(\"Result:\", result); result; } else { console.log(\"Bridge not available\"); \"Bridge not available\"; }'"

echo ""
echo "📊 Checking logs for Auto-Clipper activity..."
sleep 2

# Check logs
echo "📋 Recent Auto-Clipper logs:"
adb logcat -d | grep -E "(AutoClipperWorker|AndroidWhisperBridge.*AutoClip|startAutoClip)" | tail -10

echo ""
echo "📋 Recent AndroidWhisperBridge logs:"
adb logcat -d | grep "AndroidWhisperBridge" | tail -5

echo ""
echo "📋 Recent console logs:"
adb logcat -d | grep "console.log" | tail -5

echo ""
echo "🔍 To monitor live progress:"
echo "   adb logcat | grep -E '(AutoClipperWorker|AndroidWhisperBridge)'"
