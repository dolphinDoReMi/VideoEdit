#!/bin/bash

# Auto-Clipper Status Check Script
# Checks the status of Auto-Clipper operations on Android device

set -e

PACKAGE_NAME="com.mira.com"
STATUS_ACTION="com.mira.clip.GET_STATUS"

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

echo "📊 Checking Auto-Clipper Status..."
echo ""

# Get status via broadcast
echo "📡 Requesting status..."
adb shell "am broadcast -a $STATUS_ACTION"

echo ""
echo "📋 Recent Auto-Clipper logs:"
adb logcat -d | grep -E "(AutoClipperWorker|WhisperChainWorker|AutoClipperService)" | tail -20

echo ""
echo "📋 Recent WorkManager logs:"
adb logcat -d | grep -E "(WorkManager|WorkInfo)" | tail -10

echo ""
echo "📋 Recent Whisper processing logs:"
adb logcat -d | grep -E "(TranscribeWorker|WhisperBridge)" | tail -10

echo ""
echo "🔍 To monitor live progress:"
echo "   adb logcat | grep -E '(AutoClipperWorker|WhisperChainWorker)'"
echo ""
echo "🛑 To cancel all operations:"
echo "   adb shell 'am broadcast -a com.mira.clip.CANCEL_AUTO_CLIP'"
