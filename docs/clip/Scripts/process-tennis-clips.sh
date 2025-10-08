#!/bin/bash

# TennisInterview Clips Batch Processing Script
# Processes each clip individually using standard processing (like video_v1_long.mp4)

set -e

# Configuration
CLIPS_DIR="/sdcard/MiraWhisper/clips"
OUTPUT_DIR="/sdcard/MiraWhisper/transcriptions"
PREFIX="tennis_interview_clip"
MODEL="whisper-base.q5_1.bin"
THREADS=4
LANGUAGE="auto"
TRANSLATE=false

# Create output directories
echo "Creating output directories..."
adb shell "mkdir -p $OUTPUT_DIR"
adb shell "mkdir -p /sdcard/MiraWhisper/sidecars"

# Get list of clip files
echo "Finding clip files..."
CLIP_FILES=$(adb shell "ls $CLIPS_DIR/${PREFIX}_*.mp4" | tr -d '\r')
CLIP_COUNT=$(echo "$CLIP_FILES" | wc -l)

echo "Found $CLIP_COUNT clip files to process"
echo ""

# Process each clip
CLIP_INDEX=1
for clip_file in $CLIP_FILES; do
    # Extract clip number from filename
    CLIP_NUM=$(echo "$clip_file" | sed "s/.*${PREFIX}_\([0-9]*\)\.mp4/\1/")
    
    echo "Processing clip $CLIP_INDEX/$CLIP_COUNT: $clip_file"
    echo "  Clip number: $CLIP_NUM"
    
    # Generate output filenames following best practices
    BASE_NAME="tennis_interview_clip_${CLIP_NUM}"
    JSON_OUTPUT="$OUTPUT_DIR/${BASE_NAME}_transcription.json"
    SRT_OUTPUT="$OUTPUT_DIR/${BASE_NAME}_transcription.srt"
    TXT_OUTPUT="$OUTPUT_DIR/${BASE_NAME}_transcription.txt"
    
    echo "  Output files:"
    echo "    JSON: $JSON_OUTPUT"
    echo "    SRT: $SRT_OUTPUT"
    echo "    TXT: $TXT_OUTPUT"
    
    # Launch processing via Android app
    echo "  Starting transcription..."
    
    # Use the Android Whisper app to process the clip
    adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
        --es 'action' 'process_file' \
        --es 'file_uri' '$clip_file' \
        --es 'model' '$MODEL' \
        --ei 'threads' $THREADS \
        --es 'language' '$LANGUAGE' \
        --ez 'translate' $TRANSLATE"
    
    # Wait for processing to complete (monitor logs)
    echo "  Monitoring processing..."
    PROCESSING_START=$(date +%s)
    TIMEOUT=600  # 10 minutes timeout per clip
    
    while true; do
        # Check if processing is complete by looking for completion logs
        COMPLETION_LOG=$(adb logcat -d | grep -E "(Completed.*job|TECHNICAL.*Transcription completed)" | tail -1)
        
        if [[ -n "$COMPLETION_LOG" ]]; then
            echo "  ✓ Processing completed"
            break
        fi
        
        # Check for errors
        ERROR_LOG=$(adb logcat -d | grep -E "(ERROR|Exception|Failed)" | tail -1)
        if [[ -n "$ERROR_LOG" ]]; then
            echo "  ✗ Processing failed: $ERROR_LOG"
            exit 1
        fi
        
        # Check timeout
        CURRENT_TIME=$(date +%s)
        ELAPSED=$((CURRENT_TIME - PROCESSING_START))
        if [[ $ELAPSED -gt $TIMEOUT ]]; then
            echo "  ✗ Processing timed out after ${TIMEOUT}s"
            exit 1
        fi
        
        # Show progress
        if [[ $((ELAPSED % 30)) -eq 0 ]]; then
            echo "    Processing... ${ELAPSED}s elapsed"
        fi
        
        sleep 5
    done
    
    # Wait a bit more for file writes to complete
    sleep 10
    
    # Verify output files were created
    echo "  Verifying output files..."
    
    # Check for JSON output
    if adb shell "test -f '$JSON_OUTPUT'"; then
        JSON_SIZE=$(adb shell "stat -c%s '$JSON_OUTPUT'" | tr -d '\r')
        echo "    ✓ JSON: ${JSON_SIZE} bytes"
    else
        echo "    ✗ JSON file not found"
    fi
    
    # Check for SRT output
    if adb shell "test -f '$SRT_OUTPUT'"; then
        SRT_SIZE=$(adb shell "stat -c%s '$SRT_OUTPUT'" | tr -d '\r')
        echo "    ✓ SRT: ${SRT_SIZE} bytes"
    else
        echo "    ✗ SRT file not found"
    fi
    
    # Check for TXT output
    if adb shell "test -f '$TXT_OUTPUT'"; then
        TXT_SIZE=$(adb shell "stat -c%s '$TXT_OUTPUT'" | tr -d '\r')
        echo "    ✓ TXT: ${TXT_SIZE} bytes"
    else
        echo "    ✗ TXT file not found"
    fi
    
    echo "  ✓ Clip $CLIP_NUM processing completed"
    echo ""
    
    CLIP_INDEX=$((CLIP_INDEX + 1))
done

echo "✅ All clips processed successfully!"
echo ""
echo "Summary:"
echo "  Total clips: $CLIP_COUNT"
echo "  Output directory: $OUTPUT_DIR"
echo ""
echo "Output files:"
adb shell "ls -la $OUTPUT_DIR/"

echo ""
echo "Next step: Run merge script to combine all transcriptions"
echo "  ./merge_tennis_transcriptions.sh"
