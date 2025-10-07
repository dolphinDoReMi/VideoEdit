#!/bin/bash

# TennisInterview Video Splitting (Host Machine Approach)
# Splits video on host machine using ffmpeg, then transfers clips to Android device

set -e

echo "🎾 TennisInterview Video Splitting (Host Machine)"
echo "==============================================="
echo ""

# Configuration
INPUT_FILE="TennisInterview_converted.mp4"
OUTPUT_DIR="./tennis_clips"
CLIP_DURATION=300  # 5 minutes per clip (300 seconds)
PREFIX="tennis_interview_clip"
ANDROID_CLIPS_DIR="/sdcard/MiraWhisper/clips"

# Check if input file exists
if [[ ! -f "$INPUT_FILE" ]]; then
    echo "❌ Input file not found: $INPUT_FILE"
    echo "Please ensure TennisInterview_converted.mp4 is in the current directory"
    exit 1
fi

# Create output directory
echo "Creating output directory: $OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# Get video duration using ffprobe
echo "Getting video duration..."
DURATION=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$INPUT_FILE")
DURATION_INT=$(echo "$DURATION" | cut -d. -f1)

echo "Video duration: ${DURATION_INT} seconds ($(($DURATION_INT / 60)) minutes)"

# Calculate number of clips needed
NUM_CLIPS=$(( (DURATION_INT + CLIP_DURATION - 1) / CLIP_DURATION ))
echo "Will create $NUM_CLIPS clips of $CLIP_DURATION seconds each"
echo ""

# Split video into clips using ffmpeg
echo "Splitting video into clips..."
for i in $(seq 0 $((NUM_CLIPS - 1))); do
    START_TIME=$((i * CLIP_DURATION))
    OUTPUT_FILE="$OUTPUT_DIR/${PREFIX}_$(printf "%03d" $((i + 1))).mp4"
    
    echo "Creating clip $((i + 1))/$NUM_CLIPS: $OUTPUT_FILE"
    echo "  Start time: ${START_TIME}s"
    
    # Use ffmpeg to extract clip
    ffmpeg -i "$INPUT_FILE" -ss $START_TIME -t $CLIP_DURATION -c copy -avoid_negative_ts make_zero "$OUTPUT_FILE" -y
    
    # Verify clip was created
    if [[ -f "$OUTPUT_FILE" ]]; then
        CLIP_SIZE=$(stat -c%s "$OUTPUT_FILE" 2>/dev/null || stat -f%z "$OUTPUT_FILE")
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

# Transfer clips to Android device
echo "📱 Transferring clips to Android device..."
adb shell "mkdir -p $ANDROID_CLIPS_DIR"

for clip_file in "$OUTPUT_DIR"/*.mp4; do
    if [[ -f "$clip_file" ]]; then
        filename=$(basename "$clip_file")
        echo "Transferring: $filename"
        adb push "$clip_file" "$ANDROID_CLIPS_DIR/$filename"
    fi
done

echo ""
echo "✅ All clips transferred to Android device!"
echo ""

# Verify clips on device
echo "📋 Verifying clips on Android device..."
adb shell "ls -la $ANDROID_CLIPS_DIR/"

echo ""
echo "📊 Summary:"
echo "  Input file: $INPUT_FILE"
echo "  Duration: ${DURATION_INT} seconds ($(($DURATION_INT / 60)) minutes)"
echo "  Clips created: $NUM_CLIPS"
echo "  Clip duration: $CLIP_DURATION seconds each"
echo "  Android location: $ANDROID_CLIPS_DIR"
echo ""

echo "🚀 Next steps:"
echo "  1. Process individual clips: ./process_tennis_clips.sh"
echo "  2. Merge results: ./merge_tennis_transcriptions.sh"
echo "  3. Or run complete workflow: ./process_tennis_interview_complete.sh"
echo ""

echo "✨ Video splitting completed successfully!"
