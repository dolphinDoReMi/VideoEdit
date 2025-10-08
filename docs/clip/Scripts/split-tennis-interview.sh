#!/bin/bash

# TennisInterview Video Splitting Script
# Breaks down large TennisInterview.mp4 into manageable clips for standard processing

set -e

# Configuration
INPUT_FILE="/sdcard/MiraWhisper/TennisInterview_converted.mp4"
OUTPUT_DIR="/sdcard/MiraWhisper/clips"
CLIP_DURATION=300  # 5 minutes per clip (300 seconds)
PREFIX="tennis_interview_clip"

# Create output directory
echo "Creating output directory: $OUTPUT_DIR"
adb shell "mkdir -p $OUTPUT_DIR"

# Get video duration
echo "Getting video duration..."
DURATION=$(adb shell "ffprobe -v quiet -show_entries format=duration -of csv=p=0 '$INPUT_FILE'" | tr -d '\r')
DURATION_INT=$(echo "$DURATION" | cut -d. -f1)

echo "Video duration: ${DURATION_INT} seconds ($(($DURATION_INT / 60)) minutes)"

# Calculate number of clips needed
NUM_CLIPS=$(( (DURATION_INT + CLIP_DURATION - 1) / CLIP_DURATION ))
echo "Will create $NUM_CLIPS clips of $CLIP_DURATION seconds each"

# Split video into clips
for i in $(seq 0 $((NUM_CLIPS - 1))); do
    START_TIME=$((i * CLIP_DURATION))
    OUTPUT_FILE="$OUTPUT_DIR/${PREFIX}_$(printf "%03d" $((i + 1))).mp4"
    
    echo "Creating clip $((i + 1))/$NUM_CLIPS: $OUTPUT_FILE"
    echo "  Start time: ${START_TIME}s"
    
    # Use ffmpeg to extract clip
    adb shell "ffmpeg -i '$INPUT_FILE' -ss $START_TIME -t $CLIP_DURATION -c copy -avoid_negative_ts make_zero '$OUTPUT_FILE'"
    
    # Verify clip was created
    if adb shell "test -f '$OUTPUT_FILE'"; then
        CLIP_SIZE=$(adb shell "stat -c%s '$OUTPUT_FILE'" | tr -d '\r')
        CLIP_SIZE_MB=$((CLIP_SIZE / 1024 / 1024))
        echo "  ✓ Created: ${CLIP_SIZE_MB}MB"
    else
        echo "  ✗ Failed to create clip"
        exit 1
    fi
done

echo ""
echo "✅ Video splitting completed!"
echo "Created $NUM_CLIPS clips in $OUTPUT_DIR"
echo ""
echo "Clip files:"
adb shell "ls -la $OUTPUT_DIR/${PREFIX}_*.mp4"

echo ""
echo "Next steps:"
echo "1. Run batch processing: ./process_tennis_clips.sh"
echo "2. Merge results: ./merge_tennis_transcriptions.sh"
