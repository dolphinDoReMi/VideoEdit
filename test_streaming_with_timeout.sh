#!/bin/bash

echo "=== STREAMING PROCESSING VERIFICATION TEST WITH TIMEOUT ==="
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

# Function to monitor logs with timeout
monitor_logs_with_timeout() {
    local timeout_seconds=60
    local start_time=$(date +%s)
    
    adb logcat -s WhisperConnectorService:D TranscribeWorker:D WhisperConnectorReceiver:D AndroidWhisperBridge:D | while read line; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))
        
        # Check timeout
        if [ $elapsed -gt $timeout_seconds ]; then
            echo ""
            echo ">>> TIMEOUT REACHED (${timeout_seconds}s) - Stopping log monitoring <<<"
            break
        fi
        
        echo "$line"
        
        # Check for key streaming indicators
        if echo "$line" | grep -q "STREAMING CHUNK.*START"; then
            echo ">>> STREAMING CHUNK DETECTED <<<"
        fi
        
        if echo "$line" | grep -q "TIMEOUT"; then
            echo ">>> TIMEOUT DETECTED <<<"
        fi
        
        if echo "$line" | grep -q "STREAMING.*COMPLETE"; then
            echo ">>> CHUNK COMPLETED <<<"
        fi
        
        if echo "$line" | grep -q "Streaming processing completed successfully"; then
            echo ">>> STREAMING PROCESSING COMPLETED SUCCESSFULLY <<<"
            break
        fi
        
        if echo "$line" | grep -q "libwhisper_jni.so not found"; then
            echo ">>> LIBRARY ERROR DETECTED - This is the root cause <<<"
        fi
    done
}

# Start monitoring with timeout
monitor_logs_with_timeout

echo ""
echo "=== TEST COMPLETED ==="
echo "Check the logs above for streaming processing details"
echo "Note: The 'libwhisper_jni.so not found' error indicates a missing native library"