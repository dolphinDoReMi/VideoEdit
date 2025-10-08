#!/bin/bash

# Auto-Clipper Shell Script
# Triggers the Auto-Clipper pipeline on Android device
# Usage: ./trigger_autoclip.sh <input_video_uri> <output_folder_uri> [segment_seconds]

set -e

# Configuration
PACKAGE_NAME="com.mira.com"
AUTO_CLIP_ACTION="com.mira.clip.PROCESS_TENNIS_INTERVIEW"
SEGMENT_SECONDS=${3:-600}  # Default 10 minutes

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

# Get input parameters
INPUT_URI="$1"
OUTPUT_URI="$2"

if [ -z "$INPUT_URI" ] || [ -z "$OUTPUT_URI" ]; then
    echo "Usage: $0 <input_video_uri> <output_folder_uri> [segment_seconds]"
    echo ""
    echo "Examples:"
    echo "  $0 'file:///sdcard/MiraWhisper/TennisInterview_converted.mp4' 'content://com.android.externalstorage.documents/tree/primary%3AMiraWhisper%2Fclips'"
    echo "  $0 'file:///sdcard/MiraWhisper/TennisInterview_converted.mp4' 'content://com.android.externalstorage.documents/tree/primary%3AMiraWhisper%2Fclips' 300"
    echo ""
    echo "Note: Use SAF URIs for output folder (obtained from file picker)"
    exit 1
fi

echo "🚀 Starting Auto-Clipper Pipeline"
echo "   Input: $INPUT_URI"
echo "   Output: $OUTPUT_URI"
echo "   Segment Duration: ${SEGMENT_SECONDS}s"
echo ""

# Trigger Auto-Clipper via broadcast
echo "📡 Sending Auto-Clipper broadcast..."
adb shell "am broadcast -a $AUTO_CLIP_ACTION \
    --es 'input_uri' '$INPUT_URI' \
    --es 'output_uri' '$OUTPUT_URI' \
    --ei 'segment_seconds' $SEGMENT_SECONDS \
    --es 'whisper_model' 'whisper-base.q5_1.bin' \
    --ei 'whisper_threads' 4 \
    --es 'whisper_language' 'auto' \
    --ez 'whisper_translate' false"

if [ $? -eq 0 ]; then
    echo "✅ Auto-Clipper broadcast sent successfully"
    echo ""
    echo "📊 Monitoring progress..."
    echo "   You can check logs with: adb logcat -s AutoClipperWorker,WhisperChainWorker"
    echo "   Or check status with: adb shell 'am broadcast -a com.mira.clip.GET_STATUS'"
    echo ""
    echo "⏳ The pipeline will:"
    echo "   1. Split video into ${SEGMENT_SECONDS}s clips using MediaExtractor/MediaMuxer"
    echo "   2. Create manifest.json with clip metadata"
    echo "   3. Chain Whisper processing for each clip"
    echo "   4. Store results in the output folder"
    echo ""
    echo "🔍 To monitor progress:"
    echo "   adb logcat -d | grep -E '(AutoClipperWorker|WhisperChainWorker)' | tail -20"
else
    echo "❌ Failed to send Auto-Clipper broadcast"
    exit 1
fi
