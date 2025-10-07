#!/bin/bash

# Process Tennis Interview Clip 001
# Uses Android broadcast system to trigger processing

set -e

# Optional device targeting: set DEVICE (e.g., 192.168.1.100:5555)
DEVICE="${DEVICE:-}"
ADB_CMD="adb"
if [[ -n "$DEVICE" ]]; then
    ADB_CMD="adb -s $DEVICE"
fi

# Allow overriding clip via env; auto-detect clips dir (Mirawhisper vs MiraWhisper)
CLIP_FILE_DEFAULT="/sdcard/Mirawhisper/clips/tennis_interview_clip_001.mp4"
ALT_CLIP_FILE_DEFAULT="/sdcard/MiraWhisper/clips/tennis_interview_clip_001.mp4"
OUTPUT_DIR="/sdcard/Mirawhisper/transcriptions"

# Determine actual clips directory if not using explicit CLIP_FILE
if [[ -z "${CLIP_FILE:-}" ]]; then
    if $ADB_CMD shell "test -d '/sdcard/Mirawhisper/clips'"; then
        CLIPS_DIR="/sdcard/Mirawhisper/clips"
        CLIP_FILE="$CLIP_FILE_DEFAULT"
    elif $ADB_CMD shell "test -d '/sdcard/MiraWhisper/clips'"; then
        CLIPS_DIR="/sdcard/MiraWhisper/clips"
        CLIP_FILE="$ALT_CLIP_FILE_DEFAULT"
        # Keep OUTPUT_DIR consistent with detected casing if Mirawhisper not present
        OUTPUT_DIR="/sdcard/MiraWhisper/transcriptions"
    else
        echo "❌ Neither '/sdcard/Mirawhisper/clips' nor '/sdcard/MiraWhisper/clips' exists"
        exit 1
    fi
else
    # Derive clips dir from provided CLIP_FILE
    CLIPS_DIR=$(dirname "$CLIP_FILE")
fi

echo "🎾 Processing Tennis Interview Clip 001"
echo "====================================="
echo ""

# Check device connection
echo "Checking device connection..."
if ! $ADB_CMD get-state >/dev/null 2>&1; then
    echo "❌ No Android device connected (or DEVICE='$DEVICE' not reachable)"
    exit 1
fi

# Clear logs to avoid false positives
$ADB_CMD logcat -c || true

# Create output directory
echo "Creating output directory: $OUTPUT_DIR"
$ADB_CMD shell "mkdir -p $OUTPUT_DIR"

# Check if clip exists
echo "Checking clip file..."
if ! $ADB_CMD shell "test -f '$CLIP_FILE'"; then
    echo "⚠️  Clip not found at default path: $CLIP_FILE"
    echo "🔎 Searching first media file in: $CLIPS_DIR"
    FIRST_MEDIA=$($ADB_CMD shell "ls -1 $CLIPS_DIR | grep -Ei '\\.(mp4|mkv|wav|mp3|m4a)$' | head -1" | tr -d '\r')
    if [[ -n "$FIRST_MEDIA" ]]; then
        CLIP_FILE="$CLIPS_DIR/$FIRST_MEDIA"
        echo "➡️  Using first media file: $CLIP_FILE"
    else
        echo "❌ No media files found in $CLIPS_DIR"
        exit 1
    fi
fi

CLIP_SIZE=$($ADB_CMD shell "stat -c%s '$CLIP_FILE'" 2>/dev/null | tr -d '\r') || true
if [[ -z "$CLIP_SIZE" ]]; then
    # Fallback for devices without GNU stat - try toybox busybox style
    CLIP_SIZE=$($ADB_CMD shell "toybox stat -c%s '$CLIP_FILE'" 2>/dev/null | tr -d '\r') || true
fi
if [[ -z "$CLIP_SIZE" ]]; then
    echo "ℹ️  Unable to compute file size; proceeding"
    CLIP_SIZE_MB=unknown
else
    CLIP_SIZE_MB=$((CLIP_SIZE / 1024 / 1024))
fi
echo "✅ Clip file found: ${CLIP_SIZE_MB}MB"

echo ""
echo "🚀 Starting transcription processing..."

# Send broadcast to trigger processing
$ADB_CMD shell "am broadcast -a com.mira.whisper.PROCESS_FILE \
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
    # Check for completion (filter to our app tags/markers)
    COMPLETION_LOG=$($ADB_CMD logcat -d | grep -E "((WhisperConnectorService|TranscribeWorker|AndroidWhisperBridge|WhisperConnectorReceiver).*(Completed.*job|Transcription completed))|(TECHNICAL:.*Transcription completed)" | tail -1)
    
    if [[ -n "$COMPLETION_LOG" ]]; then
        echo "✅ Processing completed successfully!"
        break
    fi
    
    # Check for errors (limit to our tags to avoid MIUI/OneTrack noise)
    ERROR_LOG=$($ADB_CMD logcat -d | grep -E "(E\/(AndroidWhisperBridge|WhisperConnectorService|TranscribeWorker)|TECHNICAL:.*(Error|Exception|Failed))" | tail -1)
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
if $ADB_CMD shell "test -d '$OUTPUT_DIR'"; then
    OUTPUT_FILES=$($ADB_CMD shell "ls $OUTPUT_DIR/" | tr -d '\r')
    if [[ -n "$OUTPUT_FILES" ]]; then
        echo "✅ Output files created:"
        $ADB_CMD shell "ls -la $OUTPUT_DIR/"
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
JSON_FILES=$($ADB_CMD shell "ls $OUTPUT_DIR/*.json 2>/dev/null" | tr -d '\r')
if [[ -n "$JSON_FILES" ]]; then
    echo "✅ JSON transcription files found:"
    echo "$JSON_FILES"
else
    echo "⚠️  No JSON transcription files found"
fi

# Check for SRT files
SRT_FILES=$($ADB_CMD shell "ls $OUTPUT_DIR/*.srt 2>/dev/null" | tr -d '\r')
if [[ -n "$SRT_FILES" ]]; then
    echo "✅ SRT caption files found:"
    echo "$SRT_FILES"
else
    echo "⚠️  No SRT caption files found"
fi

# Check for TXT files
TXT_FILES=$($ADB_CMD shell "ls $OUTPUT_DIR/*.txt 2>/dev/null" | tr -d '\r')
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
    $ADB_CMD shell "cat '$FIRST_JSON'"
fi
