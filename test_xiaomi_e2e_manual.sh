#!/bin/bash

echo "=== XIAOMI PAD E2E TEST - MANUAL + AUTOMATED ==="
echo "Testing with video_v1.mp4 (393MB) - End-to-End Whisper Processing"
echo ""

# Clear logs
echo "Clearing logs..."
adb logcat -c

# Launch the app
echo "Launching Whisper app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
sleep 3

echo ""
echo "=== MANUAL STEPS REQUIRED ==="
echo "1. On the Xiaomi Pad device:"
echo "   - Navigate to File Selection page"
echo "   - Select 'Browse Files' or 'File Manager'"
echo "   - Navigate to /sdcard/MiraWhisper/"
echo "   - Select video_v1.mp4 (393MB)"
echo "   - Click 'Start Processing' or similar button"
echo ""
echo "2. After starting processing, wait 30 seconds then run:"
echo "   ./check_e2e_results.sh"
echo ""

# Start background log monitoring
echo "Starting background log monitoring..."
adb logcat -s WhisperConnectorService:D TranscribeWorker:D WhisperConnectorReceiver:D AndroidWhisperBridge:D > e2e_logs.txt &
LOG_PID=$!

echo "Background monitoring started (PID: $LOG_PID)"
echo "Logs are being saved to e2e_logs.txt"
echo ""
echo "=== MANUAL TEST IN PROGRESS ==="
echo "Please complete the manual steps above, then run:"
echo "kill $LOG_PID && ./check_e2e_results.sh"
