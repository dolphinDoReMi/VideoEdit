#!/bin/bash

# TennisInterview Video Splitting Script (Android-compatible)
# Uses Android's MediaMetadataRetriever and manual splitting approach

set -e

# Configuration
INPUT_FILE="/sdcard/MiraWhisper/TennisInterview_converted.mp4"
OUTPUT_DIR="/sdcard/MiraWhisper/clips"
CLIP_DURATION=300  # 5 minutes per clip (300 seconds)
PREFIX="tennis_interview_clip"

echo "🎾 TennisInterview Video Splitting (Android-compatible)"
echo "====================================================="
echo ""

# Create output directory
echo "Creating output directory: $OUTPUT_DIR"
adb shell "mkdir -p $OUTPUT_DIR"

# Get video duration using Android's MediaMetadataRetriever
echo "Getting video duration using Android MediaMetadataRetriever..."
DURATION_MS=$(adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es 'action' 'get_duration' \
    --es 'file_uri' '$INPUT_FILE' \
    --es 'output_file' '/sdcard/MiraWhisper/duration.txt'")

# Wait a moment for the operation to complete
sleep 3

# Read duration from file
if adb shell "test -f '/sdcard/MiraWhisper/duration.txt'"; then
    DURATION_MS=$(adb shell "cat '/sdcard/MiraWhisper/duration.txt'" | tr -d '\r')
    DURATION_SECONDS=$((DURATION_MS / 1000))
    echo "Video duration: ${DURATION_SECONDS} seconds ($(($DURATION_SECONDS / 60)) minutes)"
else
    echo "Could not get duration, using estimated duration..."
    # Estimate based on file size (rough calculation)
    FILE_SIZE=$(adb shell "stat -c%s '$INPUT_FILE'" | tr -d '\r')
    FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
    # Rough estimate: 1MB per minute for compressed video
    DURATION_SECONDS=$((FILE_SIZE_MB * 60))
    echo "Estimated duration: ${DURATION_SECONDS} seconds ($(($DURATION_SECONDS / 60)) minutes)"
fi

# Calculate number of clips needed
NUM_CLIPS=$(( (DURATION_SECONDS + CLIP_DURATION - 1) / CLIP_DURATION ))
echo "Will create $NUM_CLIPS clips of $CLIP_DURATION seconds each"
echo ""

if [[ $NUM_CLIPS -eq 0 ]]; then
    echo "No clips to create!"
    exit 1
fi

# Since we don't have ffmpeg on Android, let's use a different approach
# We'll create a script that the Android app can use to process the video in chunks
echo "Creating Android-compatible splitting configuration..."

# Create a configuration file for the Android app to use
cat > /tmp/split_config.json << EOF
{
  "input_file": "$INPUT_FILE",
  "output_dir": "$OUTPUT_DIR",
  "clip_duration": $CLIP_DURATION,
  "total_duration": $DURATION_SECONDS,
  "num_clips": $NUM_CLIPS,
  "prefix": "$PREFIX"
}
EOF

# Upload configuration to device
adb push /tmp/split_config.json /sdcard/MiraWhisper/split_config.json

echo "Configuration created and uploaded to device"
echo ""

# Alternative approach: Use the Android app's built-in chunking capability
echo "Using Android app's built-in processing with chunking..."
echo "This will process the video in chunks without creating separate files"
echo ""

# Create a processing script that uses the app's streaming capability
cat > /tmp/process_large_video.sh << 'EOF'
#!/bin/bash

# Process large video using Android app's streaming capability
# This avoids the need for ffmpeg splitting

INPUT_FILE="/sdcard/MiraWhisper/TennisInterview_converted.mp4"
OUTPUT_DIR="/sdcard/MiraWhisper/transcriptions"
CLIP_DURATION=300  # 5 minutes per clip
PREFIX="tennis_interview_clip"

echo "Processing large video using streaming approach..."
echo "Input: $INPUT_FILE"
echo "Output: $OUTPUT_DIR"
echo ""

# Create output directory
adb shell "mkdir -p $OUTPUT_DIR"

# Get video duration
echo "Getting video duration..."
DURATION_MS=$(adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es 'action' 'get_duration' \
    --es 'file_uri' '$INPUT_FILE'")

sleep 3

# Read duration
if adb shell "test -f '/sdcard/MiraWhisper/duration.txt'"; then
    DURATION_MS=$(adb shell "cat '/sdcard/MiraWhisper/duration.txt'" | tr -d '\r')
    DURATION_SECONDS=$((DURATION_MS / 1000))
else
    # Estimate duration
    FILE_SIZE=$(adb shell "stat -c%s '$INPUT_FILE'" | tr -d '\r')
    FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
    DURATION_SECONDS=$((FILE_SIZE_MB * 60))
fi

echo "Duration: ${DURATION_SECONDS} seconds"

# Calculate number of processing chunks
NUM_CHUNKS=$(( (DURATION_SECONDS + CLIP_DURATION - 1) / CLIP_DURATION ))
echo "Will process in $NUM_CHUNKS chunks of $CLIP_DURATION seconds each"
echo ""

# Process each chunk using the Android app's streaming capability
for i in $(seq 0 $((NUM_CHUNKS - 1))); do
    START_TIME=$((i * CLIP_DURATION))
    END_TIME=$((START_TIME + CLIP_DURATION))
    
    # Ensure we don't exceed the video duration
    if [[ $END_TIME -gt $DURATION_SECONDS ]]; then
        END_TIME=$DURATION_SECONDS
    fi
    
    CHUNK_DURATION=$((END_TIME - START_TIME))
    
    echo "Processing chunk $((i + 1))/$NUM_CHUNKS: ${START_TIME}s - ${END_TIME}s (${CHUNK_DURATION}s)"
    
    # Use the Android app to process this time range
    adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
        --es 'action' 'process_time_range' \
        --es 'file_uri' '$INPUT_FILE' \
        --es 'start_time' '$START_TIME' \
        --es 'end_time' '$END_TIME' \
        --es 'output_prefix' '${PREFIX}_$(printf "%03d" $((i + 1)))' \
        --es 'model' 'whisper-base.q5_1.bin' \
        --ei 'threads' 4 \
        --es 'language' 'auto' \
        --ez 'translate' false"
    
    # Wait for processing to complete
    echo "  Waiting for processing to complete..."
    PROCESSING_START=$(date +%s)
    TIMEOUT=600  # 10 minutes timeout per chunk
    
    while true; do
        # Check if processing is complete
        COMPLETION_LOG=$(adb logcat -d | grep -E "(Completed.*job|TECHNICAL.*Transcription completed)" | tail -1)
        
        if [[ -n "$COMPLETION_LOG" ]]; then
            echo "  ✓ Chunk $((i + 1)) processing completed"
            break
        fi
        
        # Check for errors
        ERROR_LOG=$(adb logcat -d | grep -E "(ERROR|Exception|Failed)" | tail -1)
        if [[ -n "$ERROR_LOG" ]]; then
            echo "  ✗ Chunk $((i + 1)) processing failed: $ERROR_LOG"
            exit 1
        fi
        
        # Check timeout
        CURRENT_TIME=$(date +%s)
        ELAPSED=$((CURRENT_TIME - PROCESSING_START))
        if [[ $ELAPSED -gt $TIMEOUT ]]; then
            echo "  ✗ Chunk $((i + 1)) processing timed out after ${TIMEOUT}s"
            exit 1
        fi
        
        sleep 5
    done
    
    echo "  ✓ Chunk $((i + 1)) completed"
    echo ""
done

echo "✅ All chunks processed successfully!"
echo ""
echo "Output files:"
adb shell "ls -la $OUTPUT_DIR/"

EOF

# Make the script executable and run it
chmod +x /tmp/process_large_video.sh
echo "Running large video processing..."
./tmp/process_large_video.sh

echo ""
echo "✅ TennisInterview processing completed!"
echo ""
echo "Summary:"
echo "  Input file: $INPUT_FILE"
echo "  Duration: ${DURATION_SECONDS} seconds ($(($DURATION_SECONDS / 60)) minutes)"
echo "  Processing method: Streaming chunks (Android app built-in)"
echo "  Output directory: $OUTPUT_DIR"
echo ""
echo "Next step: Merge the chunk results into final transcription"
echo "  ./merge_tennis_transcriptions.sh"
