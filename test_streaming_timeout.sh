#!/bin/bash

echo "=== STREAMING PROCESSING VERIFICATION TEST ==="
echo "Testing with 375MB video_v1_long.mp4 file"
echo "Expected behavior: Should use streaming processing with detailed logging"
echo ""

# Clear logs
echo "Clearing logs..."
adb logcat -c

# Launch the app
echo "Launching Whisper app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"

echo ""
echo "=== TEST INSTRUCTIONS ==="
echo "1. Wait for app to load (5 seconds)"
echo "2. Go to File Selection page"
echo "3. Select the test_streaming.mp4 file (375MB)"
echo "4. Start processing"
echo "5. Watch for detailed streaming logs"
echo ""
echo "Starting log monitoring with 60-second timeout..."
echo ""

# Use gtimeout (GNU timeout) if available, otherwise use a simple timeout approach
if command -v gtimeout >/dev/null 2>&1; then
    echo "Using gtimeout for log monitoring..."
    gtimeout 60 adb logcat -s WhisperConnectorService:D TranscribeWorker:D WhisperConnectorReceiver:D AndroidWhisperBridge:D
elif command -v timeout >/dev/null 2>&1; then
    echo "Using timeout for log monitoring..."
    timeout 60 adb logcat -s WhisperConnectorService:D TranscribeWorker:D WhisperConnectorReceiver:D AndroidWhisperBridge:D
else
    echo "No timeout command available, using background process with kill..."
    adb logcat -s WhisperConnectorService:D TranscribeWorker:D WhisperConnectorReceiver:D AndroidWhisperBridge:D &
    LOG_PID=$!
    sleep 60
    kill $LOG_PID 2>/dev/null
    wait $LOG_PID 2>/dev/null
fi

echo ""
echo "=== TEST COMPLETED ==="
echo "Check the logs above for streaming processing details"
