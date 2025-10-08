#!/bin/bash

# Tennis Interview Clip 002 - Final Comprehensive Fix
# Uses UI approach to avoid process errors and ensures real transcription

set -e

echo "🎾 Tennis Interview Clip 002 - Final Comprehensive Fix"
echo "====================================================="
echo "Device: Xiaomi Pad Ultra"
echo "Goal: Final fix using UI approach to get real transcription"
echo "Timestamp: $(date)"
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_002.mp4"
CLIP_SOURCE="/Users/dennis/Movies/VideoEdit/tennis_clips/$CLIP_FILE"
DEVICE_PATH="/sdcard/MiraWhisper/in/$CLIP_FILE"
OUTPUT_DIR="/sdcard/MiraWhisper/out"
SIDECAR_DIR="/sdcard/MiraWhisper/sidecars"
MODEL="whisper-base.q5_1.bin"
THREADS=2
LANGUAGE="en"  # Force English
TRANSLATE=false
DURATION_SECONDS=3

# Results directory
RESULTS_DIR="./tennis_clip_final_fix_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "📁 Results will be saved to: $RESULTS_DIR"
echo ""

# Step 1: Complete Environment Reset
echo "=== STEP 1: Complete Environment Reset ==="
echo "Stopping all processes and clearing environment..."
adb shell "am force-stop com.mira.com"
adb shell "am force-stop com.mira.whisper"
sleep 5

echo "Clearing all logs..."
adb logcat -c

echo "Clearing all batch directories..."
adb shell "rm -rf /sdcard/MiraWhisper/out/batch_*"
adb shell "rm -rf /sdcard/MiraWhisper/sidecars/*"

echo "Clearing converted media..."
adb shell "rm -rf /sdcard/Documents/ConvertedMedia/converted_*"

echo "✅ Environment completely reset"
echo ""

# Step 2: Device and File Setup with Proper Audio Format
echo "=== STEP 2: Device and File Setup with Proper Audio Format ==="
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

# Extract first 3 seconds with proper audio format
echo "🎬 Extracting first 3 seconds with proper audio format..."
THREE_SECOND_CLIP="$RESULTS_DIR/tennis_interview_clip_002_3s.mp4"
# Use ffmpeg to ensure proper audio format with explicit audio settings
ffmpeg -i "$CLIP_SOURCE" -t 3 -c:v libx264 -c:a aac -ar 48000 -ac 2 -b:a 128k -strict -2 "$THREE_SECOND_CLIP" -y
THREE_SECOND_SIZE=$(stat -f%z "$THREE_SECOND_CLIP")
THREE_SECOND_SIZE_MB=$((THREE_SECOND_SIZE / 1024 / 1024))
echo "✅ 3-second clip created with proper audio format: ${THREE_SECOND_SIZE_MB}MB"
echo ""

# Step 3: Device Setup
echo "=== STEP 3: Device Setup ==="
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

# Step 4: App Build and Installation
echo "=== STEP 4: App Build and Installation ==="
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

# Step 5: Final Comprehensive Fix - UI Approach
echo "=== STEP 5: Final Comprehensive Fix - UI Approach ==="
echo "🔧 Applying final comprehensive fix..."
START_TIME=$(date +%s)

# Use UI approach to avoid "process is bad" error
echo "📱 Launching app with UI-based processing (final fix)..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es "file_uri" "file://$DEVICE_PATH" \
    --es "model_path" "/sdcard/MiraWhisper/models/$MODEL" \
    --ei "thread_count" "$THREADS" \
    --es "language" "$LANGUAGE" \
    --ei "max_seconds" "$DURATION_SECONDS"

echo "✅ FINAL COMPREHENSIVE FIX applied - using UI approach"
echo ""

# Step 6: Extended Monitoring for Real Transcription
echo "=== STEP 6: Extended Monitoring for Real Transcription ==="
PROCESSING_COMPLETE=false
WAIT_TIME=0
MAX_WAIT_TIME=300  # 5 minutes for comprehensive fix
TRANSCRIPT_FOUND=false
STEP_COUNT=0
INFERENCE_STARTED=false
INFERENCE_COMPLETED=false
AUDIO_SAMPLES_LOADED=false

while [ $WAIT_TIME -lt $MAX_WAIT_TIME ] && [ "$PROCESSING_COMPLETE" = "false" ]; do
    STEP_COUNT=$((STEP_COUNT + 1))
    echo "🔍 Final Fix Monitoring Step $STEP_COUNT (${WAIT_TIME}s elapsed)..."
    
    # Check if app is running
    if adb shell "ps | grep com.mira.com" >/dev/null 2>&1; then
        echo "✅ App is running"
    else
        echo "⚠️  App not running, checking for results..."
    fi
    
    # Check logs for processing status
    RECENT_LOGS=$(adb logcat -d | grep -E "(TranscribeWorker|whisper|TECHNICAL|WhisperMainActivity|WhisperJNI|AudioIO)" | tail -12)
    echo "📋 Recent processing logs:"
    echo "$RECENT_LOGS" | head -6
    
    # Check for audio samples loaded
    if [[ "$RECENT_LOGS" == *"Samples:"* ]]; then
        SAMPLE_COUNT=$(echo "$RECENT_LOGS" | grep "Samples:" | tail -1 | sed 's/.*Samples: \([0-9]*\).*/\1/')
        if [ -n "$SAMPLE_COUNT" ] && [ "$SAMPLE_COUNT" -gt 1000 ]; then
            if [ "$AUDIO_SAMPLES_LOADED" = "false" ]; then
                echo "🎯 AUDIO SAMPLES LOADED SUCCESSFULLY: $SAMPLE_COUNT samples!"
                AUDIO_SAMPLES_LOADED=true
            fi
        elif [ -n "$SAMPLE_COUNT" ] && [ "$SAMPLE_COUNT" -lt 100 ]; then
            echo "⚠️  LOW SAMPLE COUNT DETECTED: $SAMPLE_COUNT samples (expected 48,000+)"
        fi
    fi
    
    # Check for inference start
    if [[ "$RECENT_LOGS" == *"Running whisper inference"* ]]; then
        if [ "$INFERENCE_STARTED" = "false" ]; then
            echo "🎯 WHISPER INFERENCE STARTED!"
            INFERENCE_STARTED=true
        fi
    fi
    
    # Check for inference completion
    if [[ "$RECENT_LOGS" == *"inference completed"* ]] || [[ "$RECENT_LOGS" == *"finished"* ]] || [[ "$RECENT_LOGS" == *"SUCCESS"* ]]; then
        if [ "$INFERENCE_COMPLETED" = "false" ]; then
            echo "🎉 WHISPER INFERENCE COMPLETED!"
            INFERENCE_COMPLETED=true
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
                
                echo "📄 File content (first 500 chars):"
                echo "${TRANSCRIPT_CONTENT:0:500}..."
                
                # Check if it's real content (not mock)
                if [[ "$TRANSCRIPT_CONTENT" != *"Hello world"* ]] && \
                   [[ "$TRANSCRIPT_CONTENT" != *"Mock whisper result"* ]] && \
                   [[ "$TRANSCRIPT_CONTENT" != *"This is a test"* ]] && \
                   [[ "$TRANSCRIPT_CONTENT" != *"Audio transcription in progress"* ]] && \
                   [[ -n "$TRANSCRIPT_CONTENT" ]] && \
                   [[ ${#TRANSCRIPT_CONTENT} -gt 50 ]]; then
                    
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
    
    # Check for completion indicators
    if [[ "$RECENT_LOGS" == *"COMPLETED"* ]] || [[ "$RECENT_LOGS" == *"SUCCESS"* ]] || [[ "$RECENT_LOGS" == *"finished"* ]]; then
        echo "✅ Processing completed according to logs"
        PROCESSING_COMPLETE=true
        break
    fi
    
    # Check for critical errors
    CRITICAL_ERRORS=$(adb logcat -d | grep -E "(FATAL|CRASH|SEGFAULT|SIGSEGV)" | tail -2)
    if [ -n "$CRITICAL_ERRORS" ]; then
        echo "❌ CRITICAL ERROR detected:"
        echo "$CRITICAL_ERRORS"
        break
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
    echo "🎉 SUCCESS: Real transcription completed with FINAL COMPREHENSIVE FIX!"
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
    echo "🎯 Audio samples loaded: $AUDIO_SAMPLES_LOADED"
    echo "🎯 Whisper inference started: $INFERENCE_STARTED"
    echo "🎯 Whisper inference completed: $INFERENCE_COMPLETED"
    
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
    adb logcat -d | grep -E "(TranscribeWorker|whisper|TECHNICAL|WhisperMainActivity|WhisperJNI|AudioIO)" | tail -30
fi

# Collect all logs
echo ""
echo "📋 Collecting all logs..."
adb logcat -d > "$RESULTS_DIR/full_logs.txt"
adb logcat -d | grep -E "(TranscribeWorker|whisper|TECHNICAL|WhisperMainActivity|WhisperJNI|AudioIO)" > "$RESULTS_DIR/whisper_logs.txt"

# Save comprehensive test summary
cat > "$RESULTS_DIR/test_summary.txt" << EOF
Tennis Interview Clip 002 - Final Comprehensive Fix
=================================================

Test Configuration:
- Processing Time: ${PROCESSING_TIME} seconds
- Video Duration: 3 seconds
- File Size: ${THREE_SECOND_SIZE_MB}MB
- Model: $MODEL
- Threads: $THREADS (REDUCED to avoid conflicts)
- Language: $LANGUAGE (FORCED ENGLISH)
- Translate: $TRANSLATE
- Real Transcript Found: $TRANSCRIPT_FOUND
- Audio Samples Loaded: $AUDIO_SAMPLES_LOADED
- Whisper Inference Started: $INFERENCE_STARTED
- Whisper Inference Completed: $INFERENCE_COMPLETED
- Monitoring Steps: $STEP_COUNT

Final Comprehensive Fix Applied:
- Used proper audio format conversion with ffmpeg
- Used UI approach to avoid "process is bad" error
- Extended monitoring time (5 minutes)
- Forced English language processing
- Reduced threads to avoid conflicts
- Complete environment reset

Results:
- Transcript Found: $TRANSCRIPT_FOUND
- Processing Time: ${PROCESSING_TIME} seconds
- Audio Samples Loaded: $AUDIO_SAMPLES_LOADED
- Whisper Inference Started: $INFERENCE_STARTED
- Whisper Inference Completed: $INFERENCE_COMPLETED
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
    echo "🎉 SUCCESS: Final comprehensive fix completed with real transcription!"
else
    echo "⚠️  Real transcript not found - detailed diagnostic information available"
    echo "🔍 Check logs and batch directories for troubleshooting"
fi

echo ""
echo "🎾 Tennis interview final comprehensive fix test completed!"
