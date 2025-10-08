#!/bin/bash

# Tennis Interview Clip 002 - Minimal Direct Test
# Simple, direct approach to get real transcription

set -e

echo "🎾 Tennis Interview Clip 002 - Minimal Direct Test"
echo "==============================================="
echo "Device: Xiaomi Pad Ultra"
echo "Goal: Get real transcription with minimal setup"
echo "Timestamp: $(date)"
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_002.mp4"
CLIP_SOURCE="/Users/dennis/Movies/VideoEdit/tennis_clips/$CLIP_FILE"
DEVICE_PATH="/sdcard/MiraWhisper/in/$CLIP_FILE"
MODEL="whisper-base.q5_1.bin"
THREADS=1
LANGUAGE="en"
TRANSLATE=false
DURATION_SECONDS=3

# Results directory
RESULTS_DIR="./tennis_clip_minimal_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "📁 Results will be saved to: $RESULTS_DIR"
echo ""

# Step 1: Quick Setup
echo "=== STEP 1: Quick Setup ==="
adb shell "am force-stop com.mira.com"
sleep 2
adb logcat -c

# Extract 3 seconds
echo "🎬 Extracting 3 seconds..."
THREE_SECOND_CLIP="$RESULTS_DIR/tennis_interview_clip_002_3s.mp4"
ffmpeg -i "$CLIP_SOURCE" -t 3 -c:v libx264 -c:a aac -ar 48000 -ac 2 -b:a 128k -strict -2 "$THREE_SECOND_CLIP" -y > /dev/null 2>&1

# Push to device
adb shell "mkdir -p /sdcard/MiraWhisper/in"
adb push "$THREE_SECOND_CLIP" "$DEVICE_PATH" > /dev/null 2>&1
echo "✅ File ready on device"
echo ""

# Step 2: Direct Service Test
echo "=== STEP 2: Direct Service Test ==="
echo "🚀 Starting service directly..."

# Start the service
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$DEVICE_PATH" \
    --es "model" "/sdcard/MiraWhisper/models/$MODEL" \
    --es "threads" "$THREADS" \
    --es "lang" "$LANGUAGE" \
    --ez "translate" "$TRANSLATE" \
    --ei "max_seconds" "$DURATION_SECONDS"

echo "✅ Service started"
echo ""

# Step 3: Wait and Monitor
echo "=== STEP 3: Wait and Monitor ==="
echo "⏳ Waiting for processing..."

# Wait for processing
sleep 60

# Check for results
echo "🔍 Checking for results..."

# Check batch directories
BATCH_DIRS=$(adb shell "find /sdcard/MiraWhisper/out -name 'batch_*' -type d 2>/dev/null")
if [ -n "$BATCH_DIRS" ]; then
    echo "📁 Found batch directories:"
    echo "$BATCH_DIRS"
    
    # Get the latest batch directory
    LATEST_BATCH=$(adb shell "find /sdcard/MiraWhisper/out -name 'batch_*' -type d 2>/dev/null | tail -1")
    echo "📋 Latest batch: $LATEST_BATCH"
    
    # Check contents
    CONTENTS=$(adb shell "ls -la $LATEST_BATCH/ 2>/dev/null")
    echo "📄 Contents:"
    echo "$CONTENTS"
    
    # Check for transcript files
    TRANSCRIPT_FILES=$(adb shell "find $LATEST_BATCH -name '*.srt' -o -name '*.txt' -o -name '*.json' 2>/dev/null")
    if [ -n "$TRANSCRIPT_FILES" ]; then
        echo "📝 Found transcript files: $TRANSCRIPT_FILES"
        
        for file in $TRANSCRIPT_FILES; do
            echo "📖 Reading: $file"
            CONTENT=$(adb shell "cat $file 2>/dev/null")
            echo "📄 Content:"
            echo "$CONTENT"
            echo ""
            
            # Save if it looks real
            if [[ "$CONTENT" != *"Hello world"* ]] && \
               [[ "$CONTENT" != *"Mock whisper result"* ]] && \
               [[ "$CONTENT" != *"This is a test"* ]] && \
               [[ -n "$CONTENT" ]] && \
               [[ ${#CONTENT} -gt 20 ]]; then
                echo "$CONTENT" > "$RESULTS_DIR/real_transcript.txt"
                echo "🎉 REAL TRANSCRIPT SAVED!"
                break
            fi
        done
    else
        echo "📝 No transcript files found"
    fi
else
    echo "📁 No batch directories found"
fi

# Check logs
echo "📋 Recent logs:"
adb logcat -d | grep -E "(TranscribeWorker|whisper|TECHNICAL|DirectWhisperService|WhisperJNI|AudioIO)" | tail -10

# Save logs
adb logcat -d > "$RESULTS_DIR/full_logs.txt"

echo ""
echo "=== RESULTS ==="
if [ -f "$RESULTS_DIR/real_transcript.txt" ]; then
    echo "🎉 SUCCESS: Real transcript found!"
    echo "📝 Transcript:"
    cat "$RESULTS_DIR/real_transcript.txt"
else
    echo "⚠️  No real transcript found"
fi

echo ""
echo "🎾 Minimal test completed!"
