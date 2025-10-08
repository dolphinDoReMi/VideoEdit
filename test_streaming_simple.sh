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
echo "Starting log monitoring..."
echo "Press Ctrl+C to stop monitoring"
echo ""

# Start monitoring logs
adb logcat -s WhisperConnectorService:D TranscribeWorker:D WhisperConnectorReceiver:D AndroidWhisperBridge:D
