#!/bin/bash

# Micro Test 2: App Launch Test
# Following XiaoMi Pad Inference Optimization guide

echo "🚀 Micro Test 2: App Launch Test"
echo "================================"
echo "Timestamp: $(date)"
echo ""

# Clear any existing app state
echo "🧹 Cleaning app state..."
adb shell "am force-stop com.mira.com"
adb logcat -c
echo ""

# Launch app (as per guide)
echo "📱 Launching Whisper app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"

# Wait for app to initialize
echo "⏳ Waiting 5 seconds for app initialization..."
sleep 5

# Check if app is running
echo "🔍 Checking if app is running..."
if adb shell "pgrep -f com.mira.com" >/dev/null 2>&1; then
    echo "✅ App is running"
else
    echo "❌ App is not running"
fi
echo ""

# Check logs for any immediate errors
echo "📊 App launch logs (last 10 lines):"
adb logcat -d | tail -10
echo ""

echo "✅ Micro Test 2 completed!"
