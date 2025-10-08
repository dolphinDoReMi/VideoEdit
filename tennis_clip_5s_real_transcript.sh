#!/bin/bash

# Tennis Interview Clip 002 - First 5 Seconds Real Transcription Test
# Processes only the first 5 seconds and waits for actual whisper transcription

set -e

echo "🎾 Tennis Interview Clip 002 - First 5 Seconds Real Transcription"
echo "==============================================================="
echo "Device: Xiaomi Pad Ultra"
echo "Test File: tennis_interview_clip_002.mp4 (first 5 seconds)"
echo "Goal: Get real whisper transcription (not mock)"
echo "Timestamp: $(date)"
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_002.mp4"
CLIP_SOURCE="/Users/dennis/Movies/VideoEdit/tennis_clips/$CLIP_FILE"
DEVICE_PATH="/sdcard/MiraWhisper/in/$CLIP_FILE"
OUTPUT_DIR="/sdcard/MiraWhisper/out"
SIDECAR_DIR="/sdcard/MiraWhisper/sidecars"
MODEL="whisper-base.q5_1.bin"
THREADS=4
LANGUAGE="en"
TRANSLATE=false
DURATION_SECONDS=5

# Results directory
RESULTS_DIR="./tennis_clip_5s_real_transcript_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "📁 Results will be saved to: $RESULTS_DIR"
echo ""

# Check device connection
echo "=== Device Connection Check ==="
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model)
echo "✅ Device connected: $DEVICE_MODEL"
echo ""

# Check if clip file exists locally
echo "=== Source File Check ==="
if [ ! -f "$CLIP_SOURCE" ]; then
    echo "❌ Source clip file not found: $CLIP_SOURCE"
    exit 1
fi

FILE_SIZE=$(stat -f%z "$CLIP_SOURCE")
FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
echo "✅ Source file found: ${FILE_SIZE_MB}MB"

# Extract first 5 seconds for testing
echo "🎬 Extracting first 5 seconds for testing..."
FIVE_SECOND_CLIP="$RESULTS_DIR/tennis_interview_clip_002_5s.mp4"
ffmpeg -i "$CLIP_SOURCE" -t 5 -c copy "$FIVE_SECOND_CLIP" -y
FIVE_SECOND_SIZE=$(stat -f%z "$FIVE_SECOND_CLIP")
FIVE_SECOND_SIZE_MB=$((FIVE_SECOND_SIZE / 1024 / 1024))
echo "✅ 5-second clip created: ${FIVE_SECOND_SIZE_MB}MB"
echo ""

# Setup device directories
echo "=== Device Setup ==="
echo "Creating directories..."
adb shell "mkdir -p $OUTPUT_DIR $SIDECAR_DIR /sdcard/MiraWhisper/in /sdcard/MiraWhisper/models"

# Push 5-second clip file to device
echo "Pushing 5-second clip file to device..."
adb push "$FIVE_SECOND_CLIP" "$DEVICE_PATH"
echo "✅ 5-second clip file pushed to device"

# Check if whisper model exists
echo "Checking whisper model..."
if ! adb shell "test -f /sdcard/MiraWhisper/models/$MODEL"; then
    echo "⚠️  Whisper model not found, copying from local..."
    if [ -f "whisper_models/$MODEL" ]; then
        adb push "whisper_models/$MODEL" "/sdcard/MiraWhisper/models/"
        echo "✅ Whisper model copied"
    else
        echo "❌ Whisper model not found locally either"
        exit 1
    fi
else
    echo "✅ Whisper model found on device"
fi

echo ""

# Build and install app
echo "=== App Build and Installation ==="
echo "Building app..."
if ./gradlew assembleDebug > /dev/null 2>&1; then
    echo "✅ App built successfully"
else
    echo "❌ App build failed"
    exit 1
fi

echo "Installing app..."
adb install -r app/build/outputs/apk/debug/app-debug.apk
echo "✅ App installed"

echo ""

# Clear logs
echo "=== Starting Real Transcription Test ==="
adb logcat -c

# Launch app
echo "🚀 Launching app..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
sleep 5

# Start transcription processing
echo "🎤 Starting real transcription processing..."
START_TIME=$(date +%s)

# Trigger processing via direct service (no broadcast pattern)
echo "📡 Triggering processing via DirectWhisperService..."
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$DEVICE_PATH" \
    --es "model" "/sdcard/MiraWhisper/models/$MODEL" \
    --es "threads" "$THREADS" \
    --es "lang" "$LANGUAGE" \
    --ez "translate" "$TRANSLATE" \
    --ei "max_seconds" "$DURATION_SECONDS"

echo "✅ Processing triggered successfully"
echo ""

# Monitor processing and wait for completion
echo "⏳ Monitoring processing for real transcription..."
PROCESSING_COMPLETE=false
WAIT_TIME=0
MAX_WAIT_TIME=600  # 10 minutes max for 5-second clip
TRANSCRIPT_FOUND=false

while [ $WAIT_TIME -lt $MAX_WAIT_TIME ] && [ "$PROCESSING_COMPLETE" = "false" ]; do
    echo "⏳ Checking for results... (${WAIT_TIME}s elapsed)"
    
    # Check for new batch directories
    NEW_BATCH_DIR=$(adb shell "find /sdcard/MiraWhisper/out -name 'batch_*' -type d -newer /sdcard/MiraWhisper/out/test_audio.json 2>/dev/null | tail -1")
    
    if [ -n "$NEW_BATCH_DIR" ]; then
        echo "📁 Found new batch directory: $NEW_BATCH_DIR"
        
        # Check for transcript files in the batch directory
        TRANSCRIPT_FILE=$(adb shell "find $NEW_BATCH_DIR -name '*.srt' -o -name '*.txt' -o -name '*.json' 2>/dev/null | head -1")
        
        if [ -n "$TRANSCRIPT_FILE" ]; then
            echo "📝 Found transcript file: $TRANSCRIPT_FILE"
            
            # Get the transcript content
            TRANSCRIPT_CONTENT=$(adb shell "cat $TRANSCRIPT_FILE" 2>/dev/null)
            
            # Check if it's real content (not mock)
            if [[ "$TRANSCRIPT_CONTENT" != *"Hello world"* ]] && [[ "$TRANSCRIPT_CONTENT" != *"Mock whisper result"* ]] && [[ -n "$TRANSCRIPT_CONTENT" ]]; then
                echo "✅ Real transcript found!"
                echo "$TRANSCRIPT_CONTENT" > "$RESULTS_DIR/real_transcript.txt"
                echo "$TRANSCRIPT_CONTENT" > "$RESULTS_DIR/real_transcript.srt"
                TRANSCRIPT_FOUND=true
                PROCESSING_COMPLETE=true
                break
            else
                echo "⚠️  Mock transcript found, continuing to wait..."
            fi
        fi
    fi
    
    # Check logs for completion indicators
    RECENT_LOGS=$(adb logcat -d | grep -E "(TranscribeWorker|whisper|TECHNICAL|COMPLETED|SUCCESS)" | tail -5)
    if [[ "$RECENT_LOGS" == *"COMPLETED"* ]] || [[ "$RECENT_LOGS" == *"SUCCESS"* ]]; then
        echo "✅ Processing completed according to logs"
        PROCESSING_COMPLETE=true
        break
    fi
    
    # Check for errors
    ERROR_LOGS=$(adb logcat -d | grep -i "error\|exception\|crash" | tail -3)
    if [ -n "$ERROR_LOGS" ]; then
        echo "⚠️  Error detected in logs:"
        echo "$ERROR_LOGS"
    fi
    
    sleep 10
    WAIT_TIME=$((WAIT_TIME + 10))
done

END_TIME=$(date +%s)
PROCESSING_TIME=$((END_TIME - START_TIME))

echo ""
echo "=== Processing Results ==="

if [ "$TRANSCRIPT_FOUND" = "true" ]; then
    echo "✅ Real transcription completed successfully!"
    echo "📝 Transcript saved to: $RESULTS_DIR/real_transcript.txt"
    echo "⏱️  Processing time: ${PROCESSING_TIME} seconds"
    echo ""
    
    # Display the transcript
    echo "🎾 TENNIS INTERVIEW CLIP 002 - FIRST 5 SECONDS TRANSCRIPT:"
    echo "========================================================="
    cat "$RESULTS_DIR/real_transcript.txt"
    echo ""
    echo "========================================================="
    
else
    echo "⚠️  Real transcription not found within ${MAX_WAIT_TIME} seconds"
    echo "⏱️  Processing time: ${PROCESSING_TIME} seconds"
    
    # Check what we found
    echo ""
    echo "📋 Checking available results..."
    
    # Get all batch directories
    ALL_BATCH_DIRS=$(adb shell "find /sdcard/MiraWhisper/out -name 'batch_*' -type d | tail -5")
    echo "📁 Recent batch directories:"
    echo "$ALL_BATCH_DIRS"
    
    # Check the most recent batch directory
    LATEST_BATCH=$(adb shell "find /sdcard/MiraWhisper/out -name 'batch_*' -type d | tail -1")
    if [ -n "$LATEST_BATCH" ]; then
        echo ""
        echo "📝 Content of latest batch directory ($LATEST_BATCH):"
        adb shell "ls -la $LATEST_BATCH/"
        
        # Get any transcript files
        TRANSCRIPT_FILES=$(adb shell "find $LATEST_BATCH -name '*.srt' -o -name '*.txt' -o -name '*.json' 2>/dev/null")
        if [ -n "$TRANSCRIPT_FILES" ]; then
            echo ""
            echo "📄 Transcript files found:"
            for file in $TRANSCRIPT_FILES; do
                echo "File: $file"
                echo "Content:"
                adb shell "cat $file" 2>/dev/null | head -10
                echo "---"
            done
        fi
    fi
fi

# Collect logs
echo ""
echo "📋 Collecting logs..."
adb logcat -d > "$RESULTS_DIR/full_logs.txt"
adb logcat -d | grep -E "(TranscribeWorker|whisper|TECHNICAL)" > "$RESULTS_DIR/whisper_logs.txt"

# Save test summary
cat > "$RESULTS_DIR/test_summary.txt" << EOF
Test: Tennis Interview Clip 002 - First 5 Seconds Real Transcription
Processing Time: ${PROCESSING_TIME} seconds
Video Duration: 5 seconds
File Size: ${FIVE_SECOND_SIZE_MB}MB
Model: $MODEL
Threads: $THREADS
Language: $LANGUAGE
Real Transcript Found: $TRANSCRIPT_FOUND
Timestamp: $(date)
EOF

echo ""
echo "=== Test Summary ==="
echo "📊 Results saved to: $RESULTS_DIR"
echo "📋 Logs collected: $RESULTS_DIR/whisper_logs.txt"
echo "📝 Test summary: $RESULTS_DIR/test_summary.txt"

if [ "$TRANSCRIPT_FOUND" = "true" ]; then
    echo "✅ Real transcript available: $RESULTS_DIR/real_transcript.txt"
    echo "🎉 Successfully obtained real transcription for first 5 seconds!"
else
    echo "⚠️  Real transcript not found - check logs for details"
fi

echo ""
echo "🎾 Tennis interview 5-second real transcription test completed!"
