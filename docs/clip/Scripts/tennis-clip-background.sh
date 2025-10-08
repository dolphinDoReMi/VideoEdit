#!/bin/bash

# Tennis Interview Clip 002 - Background Processing for Real Transcription
# Keeps app running in background and waits longer for whisper inference completion

set -e

echo "🎾 Tennis Interview Clip 002 - Background Processing for Real Transcription"
echo "========================================================================="
echo "Device: Xiaomi Pad Ultra"
echo "Goal: Keep app running in background and wait for whisper inference completion"
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
LANGUAGE="en"  # Force English
TRANSLATE=false
DURATION_SECONDS=3

# Results directory
RESULTS_DIR="./tennis_clip_background_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "📁 Results will be saved to: $RESULTS_DIR"
echo ""

# Step 1: Device and File Validation
echo "=== STEP 1: Device and File Validation ==="
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model)
echo "✅ Device connected: $DEVICE_MODEL"

if [ ! -f "$CLIP_SOURCE" ]; then
    echo "❌ Source clip file not found: $CLIP_SOURCE"
    exit 1
fi

FILE_SIZE=$(stat -f%z "$CLIP_SOURCE")
FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
echo "✅ Source file found: ${FILE_SIZE_MB}MB"

# Extract first 3 seconds
echo "🎬 Extracting first 3 seconds..."
THREE_SECOND_CLIP="$RESULTS_DIR/tennis_interview_clip_002_3s.mp4"
ffmpeg -i "$CLIP_SOURCE" -t 3 -c copy "$THREE_SECOND_CLIP" -y
THREE_SECOND_SIZE=$(stat -f%z "$THREE_SECOND_CLIP")
THREE_SECOND_SIZE_MB=$((THREE_SECOND_SIZE / 1024 / 1024))
echo "✅ 3-second clip created: ${THREE_SECOND_SIZE_MB}MB"
echo ""

# Step 2: Device Setup
echo "=== STEP 2: Device Setup ==="
echo "Creating directories..."
adb shell "mkdir -p $OUTPUT_DIR $SIDECAR_DIR /sdcard/MiraWhisper/in /sdcard/MiraWhisper/models"

echo "Pushing 3-second clip file to device..."
adb push "$THREE_SECOND_CLIP" "$DEVICE_PATH"
echo "✅ 3-second clip file pushed to device"

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

# Step 3: App Build and Installation
echo "=== STEP 3: App Build and Installation ==="
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

# Step 4: Clear Environment
echo "=== STEP 4: Clear Environment ==="
echo "Clearing logs and stopping any running processes..."
adb logcat -c
adb shell "am force-stop com.mira.com"
sleep 3

echo "Clearing old batch directories..."
adb shell "rm -rf /sdcard/MiraWhisper/out/batch_*"
echo "✅ Environment cleared"
echo ""

# Step 5: Background Processing Setup
echo "=== STEP 5: Background Processing Setup ==="
echo "🚀 Starting background processing (no UI launch)..."
START_TIME=$(date +%s)

echo "📡 Triggering background processing via DirectWhisperService..."
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$DEVICE_PATH" \
    --es "model" "/sdcard/MiraWhisper/models/$MODEL" \
    --es "threads" "$THREADS" \
    --es "lang" "$LANGUAGE" \
    --ez "translate" "$TRANSLATE" \
    --ei "max_seconds" "$DURATION_SECONDS"

echo "✅ Background processing triggered successfully"
echo ""

# Step 6: Extended Monitoring for Background Processing
echo "=== STEP 6: Extended Monitoring for Background Processing ==="
PROCESSING_COMPLETE=false
WAIT_TIME=0
MAX_WAIT_TIME=300  # 5 minutes for background processing
TRANSCRIPT_FOUND=false
STEP_COUNT=0
INFERENCE_STARTED=false

while [ $WAIT_TIME -lt $MAX_WAIT_TIME ] && [ "$PROCESSING_COMPLETE" = "false" ]; do
    STEP_COUNT=$((STEP_COUNT + 1))
    echo "🔍 Background Monitoring Step $STEP_COUNT (${WAIT_TIME}s elapsed)..."
    
    # Check if app is still running
    if adb shell "ps | grep com.mira.com" >/dev/null 2>&1; then
        echo "✅ App is running in background"
    else
        echo "⚠️  App not running, checking for results..."
    fi
    
    # Check logs for processing status
    RECENT_LOGS=$(adb logcat -d | grep -E "(TranscribeWorker|whisper|TECHNICAL|DirectWhisperService|WhisperJNI)" | tail -10)
    echo "📋 Recent processing logs:"
    echo "$RECENT_LOGS" | head -5
    
    # Check for inference start
    if [[ "$RECENT_LOGS" == *"Running whisper inference"* ]]; then
        if [ "$INFERENCE_STARTED" = "false" ]; then
            echo "🎯 WHISPER INFERENCE STARTED!"
            INFERENCE_STARTED=true
        fi
    fi
    
    # Check for new batch directories
    NEW_BATCH_DIR=$(adb shell "find /sdcard/MiraWhisper/out -name 'batch_*' -type d 2>/dev/null | tail -1")
    
    if [ -n "$NEW_BATCH_DIR" ]; then
        echo "📁 Found batch directory: $NEW_BATCH_DIR"
        
        # Check contents of batch directory
        BATCH_CONTENTS=$(adb shell "ls -la $NEW_BATCH_DIR/ 2>/dev/null")
        echo "📄 Batch directory contents:"
        echo "$BATCH_CONTENTS"
        
        # Check for transcript files
        TRANSCRIPT_FILES=$(adb shell "find $NEW_BATCH_DIR -name '*.srt' -o -name '*.txt' -o -name '*.json' 2>/dev/null")
        
        if [ -n "$TRANSCRIPT_FILES" ]; then
            echo "📝 Found transcript files: $TRANSCRIPT_FILES"
            
            for file in $TRANSCRIPT_FILES; do
                echo "📖 Reading file: $file"
                TRANSCRIPT_CONTENT=$(adb shell "cat $file 2>/dev/null")
                
                echo "📄 File content (first 400 chars):"
                echo "${TRANSCRIPT_CONTENT:0:400}..."
                
                # Check if it's real content (not mock)
                if [[ "$TRANSCRIPT_CONTENT" != *"Hello world"* ]] && \
                   [[ "$TRANSCRIPT_CONTENT" != *"Mock whisper result"* ]] && \
                   [[ "$TRANSCRIPT_CONTENT" != *"This is a test"* ]] && \
                   [[ "$TRANSCRIPT_CONTENT" != *"Audio transcription in progress"* ]] && \
                   [[ -n "$TRANSCRIPT_CONTENT" ]] && \
                   [[ ${#TRANSCRIPT_CONTENT} -gt 30 ]]; then
                    
                    echo "🎉 REAL TRANSCRIPT FOUND!"
                    echo "📝 Full transcript:"
                    echo "$TRANSCRIPT_CONTENT"
                    echo ""
                    
                    # Save the real transcript
                    echo "$TRANSCRIPT_CONTENT" > "$RESULTS_DIR/real_transcript.txt"
                    echo "$TRANSCRIPT_CONTENT" > "$RESULTS_DIR/real_transcript.srt"
                    TRANSCRIPT_FOUND=true
                    PROCESSING_COMPLETE=true
                    break
                else
                    echo "⚠️  Mock/test transcript detected, continuing..."
                fi
            done
        else
            echo "📝 No transcript files found in batch directory yet"
        fi
    else
        echo "📁 No batch directories found yet"
    fi
    
    # Check for completion indicators in logs
    if [[ "$RECENT_LOGS" == *"COMPLETED"* ]] || [[ "$RECENT_LOGS" == *"SUCCESS"* ]] || [[ "$RECENT_LOGS" == *"finished"* ]]; then
        echo "✅ Processing completed according to logs"
        PROCESSING_COMPLETE=true
        break
    fi
    
    # Check for errors (excluding Xiaomi system errors)
    ERROR_LOGS=$(adb logcat -d | grep -i "error\|exception\|crash" | grep -v "PushService\|ProcessManager\|xiaomi\|MiEngine\|SLM-HAL\|SQLiteDatabaseLockedException" | tail -2)
    if [ -n "$ERROR_LOGS" ]; then
        echo "⚠️  Relevant error detected:"
        echo "$ERROR_LOGS"
    fi
    
    echo "⏳ Waiting 20 seconds before next check..."
    sleep 20
    WAIT_TIME=$((WAIT_TIME + 20))
done

END_TIME=$(date +%s)
PROCESSING_TIME=$((END_TIME - START_TIME))

echo ""
echo "=== FINAL RESULTS ==="

if [ "$TRANSCRIPT_FOUND" = "true" ]; then
    echo "🎉 SUCCESS: Real transcription completed with background processing!"
    echo "📝 Transcript saved to: $RESULTS_DIR/real_transcript.txt"
    echo "⏱️  Processing time: ${PROCESSING_TIME} seconds"
    echo ""
    
    # Display the transcript
    echo "🎾 TENNIS INTERVIEW CLIP 002 - FIRST 3 SECONDS REAL TRANSCRIPT:"
    echo "=============================================================="
    cat "$RESULTS_DIR/real_transcript.txt"
    echo ""
    echo "=============================================================="
    
else
    echo "⚠️  Real transcription not found within ${MAX_WAIT_TIME} seconds"
    echo "⏱️  Processing time: ${PROCESSING_TIME} seconds"
    echo "🎯 Whisper inference started: $INFERENCE_STARTED"
    
    # Final diagnostic
    echo ""
    echo "🔍 FINAL DIAGNOSTIC:"
    
    # Check all batch directories
    ALL_BATCH_DIRS=$(adb shell "find /sdcard/MiraWhisper/out -name 'batch_*' -type d 2>/dev/null")
    echo "📁 All batch directories found:"
    echo "$ALL_BATCH_DIRS"
    
    # Check the most recent batch directory in detail
    LATEST_BATCH=$(adb shell "find /sdcard/MiraWhisper/out -name 'batch_*' -type d 2>/dev/null | tail -1")
    if [ -n "$LATEST_BATCH" ]; then
        echo ""
        echo "📋 Detailed analysis of latest batch directory ($LATEST_BATCH):"
        adb shell "ls -la $LATEST_BATCH/"
        
        # Get all files in the batch directory
        ALL_FILES=$(adb shell "find $LATEST_BATCH -type f 2>/dev/null")
        echo "📄 All files in batch directory:"
        echo "$ALL_FILES"
        
        # Check each file
        for file in $ALL_FILES; do
            echo ""
            echo "📖 File: $file"
            echo "📄 Content:"
            adb shell "cat $file 2>/dev/null" | head -20
            echo "---"
        done
    fi
    
    # Check recent logs
    echo ""
    echo "📋 Recent processing logs:"
    adb logcat -d | grep -E "(TranscribeWorker|whisper|TECHNICAL|DirectWhisperService|WhisperJNI)" | tail -30
fi

# Collect all logs
echo ""
echo "📋 Collecting all logs..."
adb logcat -d > "$RESULTS_DIR/full_logs.txt"
adb logcat -d | grep -E "(TranscribeWorker|whisper|TECHNICAL|DirectWhisperService|WhisperJNI)" > "$RESULTS_DIR/whisper_logs.txt"

# Save comprehensive test summary
cat > "$RESULTS_DIR/test_summary.txt" << EOF
Tennis Interview Clip 002 - Background Processing for Real Transcription
======================================================================

Test Configuration:
- Processing Time: ${PROCESSING_TIME} seconds
- Video Duration: 3 seconds
- File Size: ${THREE_SECOND_SIZE_MB}MB
- Model: $MODEL
- Threads: $THREADS
- Language: $LANGUAGE (FORCED ENGLISH)
- Translate: $TRANSLATE
- Real Transcript Found: $TRANSCRIPT_FOUND
- Whisper Inference Started: $INFERENCE_STARTED
- Monitoring Steps: $STEP_COUNT

Key Changes:
- Background processing (no UI launch to avoid force finish)
- Extended monitoring time (5 minutes)
- Forced English language processing
- Focused on whisper inference completion

Results:
- Transcript Found: $TRANSCRIPT_FOUND
- Processing Time: ${PROCESSING_TIME} seconds
- Whisper Inference Started: $INFERENCE_STARTED
- Monitoring Steps: $STEP_COUNT

Timestamp: $(date)
EOF

echo ""
echo "=== COMPREHENSIVE TEST SUMMARY ==="
echo "📊 Results saved to: $RESULTS_DIR"
echo "📋 Full logs: $RESULTS_DIR/full_logs.txt"
echo "📝 Whisper logs: $RESULTS_DIR/whisper_logs.txt"
echo "📄 Test summary: $RESULTS_DIR/test_summary.txt"

if [ "$TRANSCRIPT_FOUND" = "true" ]; then
    echo "✅ Real transcript available: $RESULTS_DIR/real_transcript.txt"
    echo "🎉 SUCCESS: Background processing completed with real transcription!"
else
    echo "⚠️  Real transcript not found - detailed diagnostic information available"
    echo "🔍 Check logs and batch directories for troubleshooting"
fi

echo ""
echo "🎾 Tennis interview background processing test completed!"
