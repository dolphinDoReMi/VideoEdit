#!/bin/bash

echo "=== TENNIS INTERVIEW PROCESSING MONITOR ==="
echo "Monitoring TennisInterview_converted.mp4 processing every 30 seconds"
echo "File: 2.93GB, Duration: ~2.97 hours, Expected chunks: 357"
echo ""

# Function to check processing status
check_status() {
    local timestamp=$(date '+%H:%M:%S')
    echo "[$timestamp] Checking processing status..."
    
    # Get recent logs
    local logs=$(adb logcat -d | grep -E "(WhisperConnectorService|TranscribeWorker|AndroidWhisperBridge|WhisperConnectorReceiver|STREAMING|TECHNICAL|CHUNK)" | tail -10)
    
    if [ -n "$logs" ]; then
        echo "Recent Whisper logs:"
        echo "$logs"
    else
        echo "No Whisper processing logs found yet"
    fi
    
    # Check if processing is active
    local processing=$(adb logcat -d | grep -E "(STREAMING CHUNK|TECHNICAL.*Processing.*chunks)" | tail -1)
    if [ -n "$processing" ]; then
        echo "Processing detected: $processing"
    fi
    
    echo "---"
}

# Initial check
check_status

# Monitor every 30 seconds
while true; do
    sleep 30
    check_status
done
