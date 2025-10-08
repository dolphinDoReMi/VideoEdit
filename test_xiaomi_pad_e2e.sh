#!/bin/bash

echo "=== XIAOMI PAD E2E WHISPER PROCESSING TEST ==="
echo "Testing with video_v1.mp4 (393MB) - End-to-End Whisper Processing"
echo "Expected behavior: Complete streaming processing with transcription results"
echo ""

# Clear logs
echo "Clearing logs..."
adb logcat -c

# Launch the app
echo "Launching Whisper app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"

echo ""
echo "=== E2E TEST INSTRUCTIONS ==="
echo "1. Wait for app to load (5 seconds)"
echo "2. Go to File Selection page"
echo "3. Navigate to /sdcard/MiraWhisper/ and select video_v1.mp4 (393MB)"
echo "4. Start processing"
echo "5. Watch for complete streaming processing and transcription results"
echo ""
echo "Expected E2E flow:"
echo "- File validation and streaming detection"
echo "- Chunked processing (13 chunks of 30s each)"
echo "- Whisper transcription for each chunk"
echo "- Final text combination and results"
echo ""
echo "Starting comprehensive E2E monitoring with 120-second timeout..."
echo ""

# Function to monitor logs with extended timeout for E2E processing
monitor_e2e_logs() {
    local timeout_seconds=120
    local start_time=$(date +%s)
    local chunk_count=0
    local completed_chunks=0
    
    adb logcat -s WhisperConnectorService:D TranscribeWorker:D WhisperConnectorReceiver:D AndroidWhisperBridge:D | while read line; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))
        
        # Check timeout
        if [ $elapsed -gt $timeout_seconds ]; then
            echo ""
            echo ">>> E2E TEST TIMEOUT REACHED (${timeout_seconds}s) - Stopping monitoring <<<"
            break
        fi
        
        echo "$line"
        
        # Track streaming progress
        if echo "$line" | grep -q "STREAMING CHUNK.*START"; then
            chunk_count=$((chunk_count + 1))
            echo ">>> CHUNK $chunk_count STARTED <<<"
        fi
        
        if echo "$line" | grep -q "STREAMING.*COMPLETE"; then
            completed_chunks=$((completed_chunks + 1))
            echo ">>> CHUNK $completed_chunks COMPLETED <<<"
        fi
        
        if echo "$line" | grep -q "Streaming processing completed successfully"; then
            echo ">>> STREAMING PROCESSING COMPLETED SUCCESSFULLY <<<"
            echo ">>> E2E TEST SUCCESS - All chunks processed <<<"
            break
        fi
        
        if echo "$line" | grep -q "libwhisper_jni.so not found"; then
            echo ">>> LIBRARY ERROR DETECTED - E2E test will fail <<<"
        fi
        
        if echo "$line" | grep -q "TECHNICAL: Final text length:"; then
            echo ">>> TRANSCRIPTION TEXT EXTRACTED <<<"
        fi
        
        if echo "$line" | grep -q "Batch.*completed with errors"; then
            echo ">>> BATCH COMPLETED WITH ERRORS - E2E test failed <<<"
        fi
        
        if echo "$line" | grep -q "Batch.*completed successfully"; then
            echo ">>> BATCH COMPLETED SUCCESSFULLY - E2E test passed <<<"
        fi
    done
}

# Start E2E monitoring
monitor_e2e_logs

echo ""
echo "=== E2E TEST COMPLETED ==="
echo "Check the logs above for complete streaming processing details"
echo "Look for:"
echo "- File validation and streaming detection"
echo "- Chunk processing progress"
echo "- Whisper transcription results"
echo "- Final text combination"
echo "- Batch completion status"
