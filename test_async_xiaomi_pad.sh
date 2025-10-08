#!/bin/bash

echo "🧪 Xiaomi Pad Ultra Async Whisper Testing Script"
echo "=============================================="

# Check device connection
echo ""
echo "=== Device Status ==="
adb devices | grep "device$"
if [ $? -eq 0 ]; then
    echo "✅ Device connected"
else
    echo "❌ No device connected"
    exit 1
fi

# Check app status
echo ""
echo "=== App Status ==="
APP_RUNNING=$(adb shell "ps | grep com.mira.com" | wc -l)
if [ "$APP_RUNNING" -gt 0 ]; then
    echo "✅ App is running"
else
    echo "❌ App is not running. Launching..."
    adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
    sleep 3
    APP_RUNNING=$(adb shell "ps | grep com.mira.com" | wc -l)
    if [ "$APP_RUNNING" -gt 0 ]; then
        echo "✅ App launched successfully"
    else
        echo "❌ App launch failed"
        exit 1
    fi
fi

# Take screenshot
echo ""
echo "=== Taking Screenshot ==="
adb shell screencap -p /sdcard/test_async_$(date +%H%M%S).png
echo "✅ Screenshot saved"

# Start log monitoring
echo ""
echo "=== Starting Log Monitoring ==="
echo "Monitoring async whisper processing logs..."
echo "Press Ctrl+C to stop monitoring"
echo ""
adb logcat -c
adb logcat | grep -E "(WhisperConnectorService|AndroidWhisperBridge|TranscribeWorker|runBatch|setTimeout)" --line-buffered
