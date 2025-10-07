#!/bin/bash

# TennisInterview Processing - Direct Android App Approach
# Uses the Android app's built-in streaming processing without file splitting

set -e

echo "🎾 TennisInterview Processing (Direct Android App)"
echo "================================================"
echo ""

# Configuration
INPUT_FILE="/sdcard/MiraWhisper/TennisInterview_converted.mp4"
OUTPUT_DIR="/sdcard/MiraWhisper/transcriptions"
MODEL="whisper-base.q5_1.bin"
THREADS=4
LANGUAGE="auto"
TRANSLATE=false

# Create output directory
echo "Creating output directory: $OUTPUT_DIR"
adb shell "mkdir -p $OUTPUT_DIR"

# Check if input file exists
echo "Checking input file..."
if ! adb shell "test -f '$INPUT_FILE'"; then
    echo "❌ Input file not found: $INPUT_FILE"
    exit 1
fi

FILE_SIZE=$(adb shell "stat -c%s '$INPUT_FILE'" | tr -d '\r')
FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
echo "✅ Input file found: ${FILE_SIZE_MB}MB"

# Get video duration using Android's MediaMetadataRetriever
echo ""
echo "Getting video duration..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es 'action' 'get_duration' \
    --es 'file_uri' '$INPUT_FILE' \
    --es 'output_file' '/sdcard/MiraWhisper/duration.txt'"

# Wait for the operation to complete
sleep 5

# Read duration
if adb shell "test -f '/sdcard/MiraWhisper/duration.txt'"; then
    DURATION_MS=$(adb shell "cat '/sdcard/MiraWhisper/duration.txt'" | tr -d '\r')
    DURATION_SECONDS=$((DURATION_MS / 1000))
    echo "✅ Video duration: ${DURATION_SECONDS} seconds ($(($DURATION_SECONDS / 60)) minutes)"
else
    echo "⚠️  Could not get exact duration, using estimated duration..."
    # Rough estimate: 1MB per minute for compressed video
    DURATION_SECONDS=$((FILE_SIZE_MB * 60))
    echo "📏 Estimated duration: ${DURATION_SECONDS} seconds ($(($DURATION_SECONDS / 60)) minutes)"
fi

echo ""
echo "🚀 Starting transcription processing..."
echo "This will use the Android app's built-in streaming processing"
echo ""

# Process the video using the Android app's streaming capability
echo "Launching Android app for processing..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es 'action' 'process_file' \
    --es 'file_uri' '$INPUT_FILE' \
    --es 'model' '$MODEL' \
    --ei 'threads' $THREADS \
    --es 'language' '$LANGUAGE' \
    --ez 'translate' $TRANSLATE \
    --es 'output_dir' '$OUTPUT_DIR' \
    --es 'use_streaming' 'true'"

echo "✅ Processing started!"
echo ""

# Monitor processing progress
echo "📊 Monitoring processing progress..."
PROCESSING_START=$(date +%s)
TIMEOUT=3600  # 1 hour timeout for the entire video
LAST_PROGRESS=0

while true; do
    # Check for completion
    COMPLETION_LOG=$(adb logcat -d | grep -E "(Completed.*job|TECHNICAL.*Transcription completed|STREAMING.*COMPLETE)" | tail -1)
    
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
    
    # Check for progress updates
    PROGRESS_LOG=$(adb logcat -d | grep -E "(STREAMING.*progress|Overall progress)" | tail -1)
    if [[ -n "$PROGRESS_LOG" ]]; then
        CURRENT_PROGRESS=$(echo "$PROGRESS_LOG" | grep -o '[0-9]*%' | head -1)
        if [[ "$CURRENT_PROGRESS" != "$LAST_PROGRESS" ]]; then
            echo "📈 Progress: $CURRENT_PROGRESS"
            LAST_PROGRESS="$CURRENT_PROGRESS"
        fi
    fi
    
    # Check timeout
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - PROCESSING_START))
    if [[ $ELAPSED -gt $TIMEOUT ]]; then
        echo "❌ Processing timed out after ${TIMEOUT}s"
        exit 1
    fi
    
    # Show elapsed time every 30 seconds
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

# Check for specific output files
echo ""
echo "🔍 Checking for specific output files..."

# Look for JSON transcription
JSON_FILES=$(adb shell "ls $OUTPUT_DIR/*.json 2>/dev/null" | tr -d '\r')
if [[ -n "$JSON_FILES" ]]; then
    echo "✅ JSON transcription files found:"
    echo "$JSON_FILES"
else
    echo "⚠️  No JSON transcription files found"
fi

# Look for SRT files
SRT_FILES=$(adb shell "ls $OUTPUT_DIR/*.srt 2>/dev/null" | tr -d '\r')
if [[ -n "$SRT_FILES" ]]; then
    echo "✅ SRT caption files found:"
    echo "$SRT_FILES"
else
    echo "⚠️  No SRT caption files found"
fi

# Look for TXT files
TXT_FILES=$(adb shell "ls $OUTPUT_DIR/*.txt 2>/dev/null" | tr -d '\r')
if [[ -n "$TXT_FILES" ]]; then
    echo "✅ TXT transcription files found:"
    echo "$TXT_FILES"
else
    echo "⚠️  No TXT transcription files found"
fi

echo ""
echo "🎉 TennisInterview processing completed!"
echo ""
echo "📊 Summary:"
echo "  Input file: $INPUT_FILE (${FILE_SIZE_MB}MB)"
echo "  Duration: ${DURATION_SECONDS} seconds ($(($DURATION_SECONDS / 60)) minutes)"
echo "  Processing method: Android app streaming processing"
echo "  Output directory: $OUTPUT_DIR"
echo "  Processing time: $((CURRENT_TIME - PROCESSING_START)) seconds"
echo ""

# Show final output
echo "📄 Final output files:"
adb shell "ls -la $OUTPUT_DIR/"

echo ""
echo "✨ Processing complete! Check the output files for transcription results."
