#!/bin/bash

# Tennis Interview Clip Processing - Xiaomi Pad Implementation
# Replicates host-side workflow: locate clip, extract audio, sample transcription, background processing

set -e

echo "🎾 Tennis Interview Clip Processing - Xiaomi Pad"
echo "=============================================="
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_001.mp4"
SAMPLE_DURATION=90  # 90 seconds sample
MODEL="whisper-base.q5_1.bin"
THREADS=4
LANGUAGE="auto"
TRANSLATE=false

# Directories
INPUT_DIR="/sdcard/MiraWhisper"
OUTPUT_DIR="/sdcard/MiraWhisper/transcriptions"
SAMPLE_DIR="/sdcard/MiraWhisper/samples"

echo "📁 Setting up directories..."
adb shell "mkdir -p $OUTPUT_DIR"
adb shell "mkdir -p $SAMPLE_DIR"

echo ""
echo "🔍 Locating clip file: $CLIP_FILE"

# Search for the clip file in common locations
CLIP_PATHS=(
    "$INPUT_DIR/clips/$CLIP_FILE"
    "$INPUT_DIR/$CLIP_FILE"
    "/sdcard/Download/$CLIP_FILE"
    "/sdcard/Movies/$CLIP_FILE"
    "/sdcard/DCIM/$CLIP_FILE"
    "/sdcard/Documents/$CLIP_FILE"
)

CLIP_FOUND=""
for path in "${CLIP_PATHS[@]}"; do
    if adb shell "test -f '$path'" 2>/dev/null; then
        CLIP_FOUND="$path"
        echo "✅ Found clip file: $path"
        break
    fi
done

if [[ -z "$CLIP_FOUND" ]]; then
    echo "❌ Clip file not found in common locations"
    echo "Searched paths:"
    for path in "${CLIP_PATHS[@]}"; do
        echo "  - $path"
    done
    echo ""
    echo "Please ensure the file exists or provide the correct path"
    exit 1
fi

# Get file info
FILE_SIZE=$(adb shell "stat -c%s '$CLIP_FOUND'" | tr -d '\r')
FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
echo "📊 File size: ${FILE_SIZE_MB}MB"

# Get video duration
echo ""
echo "⏱️  Getting video duration..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es 'action' 'get_duration' \
    --es 'file_uri' '$CLIP_FOUND' \
    --es 'output_file' '/sdcard/MiraWhisper/duration.txt'"

sleep 3

if adb shell "test -f '/sdcard/MiraWhisper/duration.txt'"; then
    DURATION_MS=$(adb shell "cat '/sdcard/MiraWhisper/duration.txt'" | tr -d '\r')
    DURATION_SECONDS=$((DURATION_MS / 1000))
    echo "✅ Video duration: ${DURATION_SECONDS} seconds ($(($DURATION_SECONDS / 60)) minutes)"
else
    echo "⚠️  Could not get exact duration, using estimated duration..."
    DURATION_SECONDS=$((FILE_SIZE_MB * 60))
    echo "📏 Estimated duration: ${DURATION_SECONDS} seconds ($(($DURATION_SECONDS / 60)) minutes)"
fi

echo ""
echo "🎵 Extracting 16kHz mono audio sample (${SAMPLE_DURATION}s)..."

# Extract audio sample using Android's MediaCodec
SAMPLE_AUDIO="$SAMPLE_DIR/clip001_sample_${SAMPLE_DURATION}s.wav"
echo "📤 Extracting audio sample to: $SAMPLE_AUDIO"

adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es 'action' 'extract_audio_sample' \
    --es 'file_uri' '$CLIP_FOUND' \
    --es 'output_file' '$SAMPLE_AUDIO' \
    --ei 'duration_seconds' $SAMPLE_DURATION"

# Wait for extraction to complete
echo "⏳ Waiting for audio extraction..."
sleep 10

# Check if sample was created
if adb shell "test -f '$SAMPLE_AUDIO'"; then
    SAMPLE_SIZE=$(adb shell "stat -c%s '$SAMPLE_AUDIO'" | tr -d '\r')
    SAMPLE_SIZE_MB=$((SAMPLE_SIZE / 1024 / 1024))
    echo "✅ Audio sample extracted: ${SAMPLE_SIZE_MB}MB"
else
    echo "❌ Failed to extract audio sample"
    echo "Falling back to direct processing..."
fi

echo ""
echo "🎤 Transcribing ${SAMPLE_DURATION}s sample with auto language detection..."

# Clear logs before transcription
adb logcat -c

# Start sample transcription
SAMPLE_JOB_ID="sample_${SAMPLE_DURATION}s_$(date +%s)"
echo "📝 Starting sample transcription (Job ID: $SAMPLE_JOB_ID)"

adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es 'action' 'process_sample' \
    --es 'file_uri' '$CLIP_FOUND' \
    --es 'model' '$MODEL' \
    --ei 'threads' $THREADS \
    --es 'language' '$LANGUAGE' \
    --ez 'translate' $TRANSLATE \
    --ei 'max_seconds' $SAMPLE_DURATION \
    --es 'output_dir' '$OUTPUT_DIR' \
    --es 'job_id' '$SAMPLE_JOB_ID'"

echo "⏳ Monitoring sample transcription progress..."

# Monitor transcription progress
SAMPLE_START=$(date +%s)
TIMEOUT=300  # 5 minutes timeout for sample
LAST_PROGRESS=0

while true; do
    # Check for completion
    COMPLETION_LOG=$(adb logcat -d | grep -E "(Completed.*job|TECHNICAL.*Transcription completed|STREAMING.*COMPLETE)" | tail -1)
    
    if [[ -n "$COMPLETION_LOG" ]]; then
        echo "✅ Sample transcription completed!"
        break
    fi
    
    # Check for errors
    ERROR_LOG=$(adb logcat -d | grep -E "(ERROR|Exception|Failed)" | tail -1)
    if [[ -n "$ERROR_LOG" ]]; then
        echo "❌ Sample transcription failed: $ERROR_LOG"
        echo "Continuing with full transcription..."
        break
    fi
    
    # Check for progress updates
    PROGRESS_LOG=$(adb logcat -d | grep -E "(STREAMING.*progress|Overall progress)" | tail -1)
    if [[ -n "$PROGRESS_LOG" ]]; then
        CURRENT_PROGRESS=$(echo "$PROGRESS_LOG" | grep -o '[0-9]*%' | head -1)
        if [[ "$CURRENT_PROGRESS" != "$LAST_PROGRESS" ]]; then
            echo "📈 Sample progress: $CURRENT_PROGRESS"
            LAST_PROGRESS="$CURRENT_PROGRESS"
        fi
    fi
    
    # Check timeout
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - SAMPLE_START))
    if [[ $ELAPSED -gt $TIMEOUT ]]; then
        echo "⚠️  Sample transcription timed out after ${TIMEOUT}s"
        echo "Continuing with full transcription..."
        break
    fi
    
    sleep 5
done

echo ""
echo "📄 Retrieving sample transcript..."

# Look for sample transcript files
SAMPLE_TRANSCRIPT=""
SAMPLE_SRT=""

# Check for transcript files
for ext in "txt" "json" "srt"; do
    SAMPLE_FILE="$OUTPUT_DIR/${SAMPLE_JOB_ID}.$ext"
    if adb shell "test -f '$SAMPLE_FILE'" 2>/dev/null; then
        if [[ "$ext" == "txt" ]]; then
            SAMPLE_TRANSCRIPT="$SAMPLE_FILE"
        elif [[ "$ext" == "srt" ]]; then
            SAMPLE_SRT="$SAMPLE_FILE"
        fi
    fi
done

# Also check for sidecar files
SIDECAR_FILE="$OUTPUT_DIR/${SAMPLE_JOB_ID}_sidecar.json"
if adb shell "test -f '$SIDECAR_FILE'" 2>/dev/null; then
    echo "📋 Found sidecar file: $SIDECAR_FILE"
fi

# Display sample transcript if available
if [[ -n "$SAMPLE_TRANSCRIPT" ]]; then
    echo ""
    echo "📝 Sample transcript (first ~60 lines):"
    echo "======================================"
    
    # Get first 60 lines of transcript
    adb shell "head -60 '$SAMPLE_TRANSCRIPT'" | while read -r line; do
        echo "$line"
    done
    
    echo ""
    echo "📊 Sample transcript stats:"
    TOTAL_LINES=$(adb shell "wc -l '$SAMPLE_TRANSCRIPT'" | awk '{print $1}')
    echo "  - Total lines: $TOTAL_LINES"
    echo "  - File: $SAMPLE_TRANSCRIPT"
fi

# Display SRT preview if available
if [[ -n "$SAMPLE_SRT" ]]; then
    echo ""
    echo "📺 SRT preview:"
    echo "==============="
    
    # Get first 10 lines of SRT
    adb shell "head -10 '$SAMPLE_SRT'" | while read -r line; do
        echo "$line"
    done
fi

echo ""
echo "🚀 Starting full transcription in background..."

# Start full transcription
FULL_JOB_ID="full_clip001_$(date +%s)"
echo "📝 Starting full transcription (Job ID: $FULL_JOB_ID)"

adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es 'action' 'process_file' \
    --es 'file_uri' '$CLIP_FOUND' \
    --es 'model' '$MODEL' \
    --ei 'threads' $THREADS \
    --es 'language' '$LANGUAGE' \
    --ez 'translate' $TRANSLATE \
    --es 'output_dir' '$OUTPUT_DIR' \
    --es 'job_id' '$FULL_JOB_ID' \
    --es 'use_streaming' 'true'"

echo "✅ Full transcription started in background!"
echo ""
echo "📊 Processing Summary:"
echo "====================="
echo "  - Input file: $CLIP_FOUND (${FILE_SIZE_MB}MB)"
echo "  - Duration: ${DURATION_SECONDS}s ($(($DURATION_SECONDS / 60)) minutes)"
echo "  - Sample duration: ${SAMPLE_DURATION}s"
echo "  - Model: $MODEL"
echo "  - Threads: $THREADS"
echo "  - Language: $LANGUAGE (auto detection)"
echo "  - Sample Job ID: $SAMPLE_JOB_ID"
echo "  - Full Job ID: $FULL_JOB_ID"
echo ""
echo "📁 Output files will appear in: $OUTPUT_DIR"
echo "  - Full transcript: ${FULL_JOB_ID}.txt"
echo "  - Full SRT: ${FULL_JOB_ID}.srt"
echo "  - Sidecar: ${FULL_JOB_ID}_sidecar.json"
echo ""
echo "🔍 Monitor progress with:"
echo "  adb logcat -s TranscribeWorker:D WhisperConnectorService:D"
echo ""
echo "📥 Retrieve results with:"
echo "  adb pull $OUTPUT_DIR/${FULL_JOB_ID}.txt ./clip001_full.txt"
echo "  adb pull $OUTPUT_DIR/${FULL_JOB_ID}.srt ./clip001_full.srt"
echo ""
echo "🎾 Tennis clip processing initiated successfully!"
