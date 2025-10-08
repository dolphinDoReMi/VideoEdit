#!/bin/bash

echo "=== XIAOMI PAD E2E TEST - SIMPLE APPROACH ==="
echo "Testing with video_v1.mp4 (393MB) - End-to-End Whisper Processing"
echo ""

# Clear logs
echo "Clearing logs..."
adb logcat -c

# Launch the app
echo "Launching Whisper app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
sleep 2

echo ""
echo "=== MANUAL TEST INSTRUCTIONS ==="
echo "1. On the Xiaomi Pad device:"
echo "   - Go to File Selection page"
echo "   - Navigate to /sdcard/MiraWhisper/"
echo "   - Select video_v1.mp4 (393MB)"
echo "   - Start processing"
echo ""
echo "2. After processing starts, wait 30 seconds"
echo ""
echo "3. Then run this command to check results:"
echo "   adb logcat -d | grep -E '(STREAMING|TECHNICAL|ERROR)' | tail -20"
echo ""
echo "=== TEST SETUP COMPLETE ==="
echo "App is ready. Please proceed with manual testing."
