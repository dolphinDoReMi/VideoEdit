#!/bin/bash

# Process Tennis Interview Clip 001
# Uses Android broadcast system to trigger processing

set -e

CLIP_FILE="/sdcard/MiraWhisper/clips/tennis_interview_clip_001.mp4"
OUTPUT_DIR="/sdcard/MiraWhisper/transcriptions"

echo "🎾 Processing Tennis Interview Clip 001"
echo "====================================="
echo ""

# Create output directory
echo "Creating output directory: $OUTPUT_DIR"
adb shell "mkdir -p $OUTPUT_DIR"

# Check if clip exists
echo "Checking clip file..."
if ! adb shell "test -f '$CLIP_FILE'"; then
    echo "❌ Clip file not found: $CLIP_FILE"
    exit 1
fi

CLIP_SIZE=$(adb shell "stat -c%s '$CLIP_FILE'" | tr -d '\r')
CLIP_SIZE_MB=$((CLIP_SIZE / 1024 / 1024))
echo "✅ Clip file found: ${CLIP_SIZE_MB}MB"

echo ""
echo "🚀 Starting transcription processing..."

# Send broadcast to trigger processing
adb shell "am broadcast -a com.mira.whisper.PROCESS_FILE \
    --es 'file_uri' '$CLIP_FILE' \
    --es 'model' 'whisper-base.q5_1.bin' \
    --ei 'threads' 4 \
    --es 'language' 'auto' \
    --ez 'translate' false \
    --es 'output_dir' '$OUTPUT_DIR'"

echo "✅ Processing broadcast sent!"
echo ""

# Monitor processing progress
echo "📊 Monitoring processing progress..."
PROCESSING_START=$(date +%s)
TIMEOUT=600  # 10 minutes timeout

while true; do
    # Check for completion
    COMPLETION_LOG=$(adb logcat -d | grep -E "(Completed.*job|TECHNICAL.*Transcription completed)" | tail -1)
    
    if [[ -n "$COMPLETION_LOG" ]]; then
        echo "✅ Processing completed successfully!"
        break
    fi
    
    # Check for errors
    ERROR_LOG=$(adb logcat -d | grep -E "(ERROR|Exception|Failed)" | tail -1)
    if [[ -n "$ERROR_LOG" ]]; then
        echo "❌ Processing failed: $ERROR_LOG"
        exit 1
    fi
    
    # Check timeout
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - PROCESSING_START))
    if [[ $ELAPSED -gt $TIMEOUT ]]; then
        echo "❌ Processing timed out after ${TIMEOUT}s"
        exit 1
    fi
    
    # Show progress every 30 seconds
    if [[ $((ELAPSED % 30)) -eq 0 ]]; then
        echo "⏱️  Processing... ${ELAPSED}s elapsed"
    fi
    
    sleep 5
done

# Wait for file writes to complete
echo ""
echo "⏳ Waiting for file writes to complete..."
sleep 10

# Check output files
echo ""
echo "📁 Checking output files..."
if adb shell "test -d '$OUTPUT_DIR'"; then
    OUTPUT_FILES=$(adb shell "ls $OUTPUT_DIR/" | tr -d '\r')
    if [[ -n "$OUTPUT_FILES" ]]; then
        echo "✅ Output files created:"
        adb shell "ls -la $OUTPUT_DIR/"
    else
        echo "⚠️  No output files found in $OUTPUT_DIR"
    fi
else
    echo "❌ Output directory not found: $OUTPUT_DIR"
fi

# Look for specific output files
echo ""
echo "🔍 Looking for transcription files..."

# Check for JSON transcription
JSON_FILES=$(adb shell "ls $OUTPUT_DIR/*.json 2>/dev/null" | tr -d '\r')
if [[ -n "$JSON_FILES" ]]; then
    echo "✅ JSON transcription files found:"
    echo "$JSON_FILES"
else
    echo "⚠️  No JSON transcription files found"
fi

# Check for SRT files
SRT_FILES=$(adb shell "ls $OUTPUT_DIR/*.srt 2>/dev/null" | tr -d '\r')
if [[ -n "$SRT_FILES" ]]; then
    echo "✅ SRT caption files found:"
    echo "$SRT_FILES"
else
    echo "⚠️  No SRT caption files found"
fi

# Check for TXT files
TXT_FILES=$(adb shell "ls $OUTPUT_DIR/*.txt 2>/dev/null" | tr -d '\r')
if [[ -n "$TXT_FILES" ]]; then
    echo "✅ TXT transcription files found:"
    echo "$TXT_FILES"
else
    echo "⚠️  No TXT transcription files found"
fi

echo ""
echo "🎉 Processing completed!"
echo ""
echo "📊 Summary:"
echo "  Input clip: $CLIP_FILE (${CLIP_SIZE_MB}MB)"
echo "  Processing time: $((CURRENT_TIME - PROCESSING_START)) seconds"
echo "  Output directory: $OUTPUT_DIR"
echo ""

# Show the first transcription file content if available
if [[ -n "$JSON_FILES" ]]; then
    FIRST_JSON=$(echo "$JSON_FILES" | head -1)
    echo "📄 First transcription file content:"
    echo "File: $FIRST_JSON"
    echo "Content:"
    adb shell "cat '$FIRST_JSON'"
fi
