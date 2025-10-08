#!/bin/bash

# Quick Tennis Interview Test for XiaoMi Pad
# This script runs a quick test of the tennis interview processing

set -e

echo "=== Quick Tennis Interview Test ==="

# Check device connection
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

echo "✅ Device connected"

# Copy audio to device
echo "Copying audio to device..."
adb push "/Users/dennis/Movies/VideoEdit/tennis_demo_20251008_151949/tennis_interview_device.wav" /sdcard/tennis_interview_test.wav

# Launch app
echo "Launching Whisper app..."
adb shell am start -n com.mira.videoeditor/.MainActivity

# Wait for app initialization
sleep 3

# Start processing
echo "Starting Whisper processing..."
adb shell am broadcast -a "com.mira.videoeditor.whisper.RUN" \
    --es "file_path" "/sdcard/tennis_interview_test.wav"

echo "✅ Processing started - monitor logs with: adb logcat | grep -i whisper"

# Monitor for 30 seconds
echo "Monitoring performance for 30 seconds..."
for i in {1..30}; do
    CPU_USAGE=$(adb shell "top -n 1 | grep com.mira.videoeditor" | awk '{print $1}' | head -1 || echo "0")
    MEMORY_USAGE=$(adb shell "dumpsys meminfo com.mira.videoeditor" | grep "TOTAL" | awk '{print $2}' || echo "0")
    MEMORY_MB=$((MEMORY_USAGE / 1024))
    
    if [ $((i % 10)) -eq 0 ]; then
        echo "   Time ${i}s: CPU=${CPU_USAGE}%, Memory=${MEMORY_MB}MB"
    fi
    
    sleep 1
done

echo "✅ Quick test completed"
echo "Check device for transcription results: /sdcard/*.srt"
