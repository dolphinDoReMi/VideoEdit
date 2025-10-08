#!/bin/bash

# MiraWhisper Integration Script
# Bridges our infra-storage service with MiraWhisper processing

set -e

MIRAWHISPER_DIR="/storage/emulated/0/MiraWhisper"
INPUT_FILE="$1"

if [ -z "$INPUT_FILE" ]; then
    echo "Usage: $0 <input_file>"
    echo "Example: $0 tennis_interview_clip_003.mp4"
    exit 1
fi

echo "=== MiraWhisper Integration ==="
echo "Processing: $INPUT_FILE"
echo "=============================="

# Step 1: Copy file to MiraWhisper input directory
echo "1. Copying file to MiraWhisper input directory..."
adb push "$INPUT_FILE" "$MIRAWHISPER_DIR/in/"

# Step 2: Trigger processing
echo "2. Triggering MiraWhisper processing..."
adb shell touch "$MIRAWHISPER_DIR/trigger_processing.txt"

# Step 3: Monitor processing
echo "3. Monitoring processing..."
echo "   Waiting for transcription to complete..."

# Wait and check for output files
for i in {1..60}; do
    OUTPUT_FILES=$(adb shell find "$MIRAWHISPER_DIR/out" -name "*$(basename "$INPUT_FILE" .mp4)*" 2>/dev/null || echo "")
    if [ -n "$OUTPUT_FILES" ]; then
        echo "   ✅ Transcription completed!"
        break
    fi
    echo "   Waiting... ($i/60)"
    sleep 5
done

# Step 4: Retrieve results
echo "4. Retrieving transcription results..."
OUTPUT_FILES=$(adb shell find "$MIRAWHISPER_DIR/out" -name "*$(basename "$INPUT_FILE" .mp4)*" 2>/dev/null || echo "")

if [ -n "$OUTPUT_FILES" ]; then
    echo "   📄 Output files found:"
    echo "$OUTPUT_FILES"
    
    # Copy results to local directory
    LOCAL_OUTPUT_DIR="mirawhisper_results_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$LOCAL_OUTPUT_DIR"
    
    for file in $OUTPUT_FILES; do
        filename=$(basename "$file")
        adb pull "$file" "$LOCAL_OUTPUT_DIR/$filename"
        echo "   📥 Downloaded: $filename"
    done
    
    echo "   ✅ Results saved to: $LOCAL_OUTPUT_DIR"
else
    echo "   ❌ No output files found"
fi

echo "=== Integration Complete ==="
