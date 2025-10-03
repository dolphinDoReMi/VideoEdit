#!/bin/bash

# CLIP Feature End-to-End Test Script
# Tests the CLIP video embedding pipeline via ADB broadcast

set -e

echo "🎬 CLIP Feature E2E Test"
echo "========================="

# Configuration
PACKAGE="com.mira.clip"
VIDEO_FILE="video_v1.mp4"
OUTPUT_DIR="/sdcard/MiraClip/out"
VARIANT="ViT-B_32"
FRAME_COUNT=32

echo "📱 Package: $PACKAGE"
echo "🎥 Video: $VIDEO_FILE"
echo "📁 Output: $OUTPUT_DIR"
echo "🔧 Variant: $VARIANT"
echo "🎞️  Frames: $FRAME_COUNT"
echo ""

# Step 1: Push test video (if it exists locally)
if [ -f "$VIDEO_FILE" ]; then
    echo "📤 Pushing test video..."
    adb push "$VIDEO_FILE" "/sdcard/Mira/$VIDEO_FILE"
    echo "✅ Video pushed to /sdcard/Mira/$VIDEO_FILE"
else
    echo "⚠️  Test video not found locally. Using existing video at /sdcard/Mira/$VIDEO_FILE"
fi

# Step 2: Trigger CLIP pipeline
echo ""
echo "🚀 Triggering CLIP pipeline..."
adb shell am broadcast \
  -a "${PACKAGE}.CLIP.RUN" \
  --es input "file:///sdcard/Mira/$VIDEO_FILE" \
  --es outdir "file:///sdcard/MiraClip/out" \
  --es variant "$VARIANT" \
  --ei frame_count $FRAME_COUNT

echo "✅ Broadcast sent"

# Step 3: Wait for processing
echo ""
echo "⏳ Waiting for processing (10 seconds)..."
sleep 10

# Step 4: Check output files
echo ""
echo "🔍 Checking output files..."
EMBEDDING_DIR="$OUTPUT_DIR/embeddings/$VARIANT"
VIDEO_ID=$(basename "$VIDEO_FILE" .mp4)

# Check if files exist on device
adb shell "ls -la $EMBEDDING_DIR/"

echo ""
echo "📥 Pulling output files..."
mkdir -p ./clip_test_output

# Pull embedding file
adb pull "$EMBEDDING_DIR/$VIDEO_ID.f32" ./clip_test_output/ 2>/dev/null || echo "❌ Failed to pull .f32 file"
adb pull "$EMBEDDING_DIR/$VIDEO_ID.json" ./clip_test_output/ 2>/dev/null || echo "❌ Failed to pull .json file"

# Step 5: Validate files
echo ""
echo "✅ Validation:"
if [ -f "./clip_test_output/$VIDEO_ID.f32" ]; then
    F32_SIZE=$(stat -f%z "./clip_test_output/$VIDEO_ID.f32" 2>/dev/null || echo "unknown")
    echo "  📄 $VIDEO_ID.f32: $F32_SIZE bytes"
else
    echo "  ❌ $VIDEO_ID.f32: Missing"
fi

if [ -f "./clip_test_output/$VIDEO_ID.json" ]; then
    echo "  📄 $VIDEO_ID.json: $(cat "./clip_test_output/$VIDEO_ID.json" | wc -c) bytes"
    echo "  📋 JSON content:"
    cat "./clip_test_output/$VIDEO_ID.json" | python3 -m json.tool 2>/dev/null || cat "./clip_test_output/$VIDEO_ID.json"
else
    echo "  ❌ $VIDEO_ID.json: Missing"
fi

# Step 6: Check logs
echo ""
echo "📋 Recent CLIP logs:"
adb logcat -d | grep -E "(ClipRunner|ClipReceiver)" | tail -10

echo ""
echo "🎉 CLIP Feature Test Complete!"
echo "=============================="
echo "📁 Output files saved to: ./clip_test_output/"
echo "🔧 To run again: ./test_clip_feature.sh"
