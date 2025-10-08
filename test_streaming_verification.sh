#!/bin/bash

echo "=== STREAMING PROCESSING VERIFICATION TEST ==="
echo "Testing with 375MB video_v1_long.mp4 file"
echo "Expected behavior: Should use streaming processing (>100MB threshold)"
echo ""

# Clear logs
echo "Clearing logs..."
adb logcat -c

# Start monitoring logs in background
echo "Starting log monitoring..."
adb logcat -s WhisperConnectorService:D TranscribeWorker:D WhisperConnectorReceiver:D AndroidWhisperBridge:D &
LOGCAT_PID=$!

echo ""
echo "=== TEST INSTRUCTIONS ==="
echo "1. Open the Whisper app on Xiaomi Pad"
echo "2. Go to File Selection page"
echo "3. Select the test_streaming.mp4 file (375MB)"
echo "4. Start processing"
echo "5. Watch for streaming processing logs"
echo ""
echo "Expected log patterns:"
echo "- 'TECHNICAL: Large file detected - using streaming processing'"
echo "- 'TECHNICAL: Processing X chunks of 30000ms each'"
echo "- 'TECHNICAL: Processing chunk X/Y'"
echo "- 'TECHNICAL: Streaming progress: X%'"
echo ""
echo "Press Ctrl+C to stop monitoring when done testing"
echo ""

# Wait for user to complete test
wait $LOGCAT_PID
