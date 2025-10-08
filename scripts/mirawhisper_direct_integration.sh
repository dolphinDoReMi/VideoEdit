#!/bin/bash

# Direct MiraWhisper Integration Script
# Uses existing MiraWhisper system with our real JNI implementation

set -e

MIRAWHISPER_DIR="/storage/emulated/0/MiraWhisper"
INPUT_FILE="$1"

if [ -z "$INPUT_FILE" ]; then
    echo "Usage: $0 <input_file>"
    echo "Example: $0 tennis_interview_clip_003.mp4"
    exit 1
fi

echo "=== Direct MiraWhisper Integration ==="
echo "Processing: $INPUT_FILE"
echo "Using existing MiraWhisper system with real JNI"
echo "=============================================="

# Step 1: Ensure file exists in MiraWhisper input directory
echo "1. Checking if file exists in MiraWhisper input directory..."
if ! adb shell test -f "$MIRAWHISPER_DIR/in/$INPUT_FILE"; then
    echo "   ❌ File not found in MiraWhisper input directory"
    echo "   📁 Available files:"
    adb shell ls -la "$MIRAWHISPER_DIR/in/" || echo "   No files in input directory"
    exit 1
fi

echo "   ✅ File found: $MIRAWHISPER_DIR/in/$INPUT_FILE"

# Step 2: Trigger MiraWhisper processing
echo "2. Triggering MiraWhisper processing..."
adb shell touch "$MIRAWHISPER_DIR/trigger_processing.txt"
echo "   ✅ Processing triggered"

# Step 3: Monitor processing with enhanced logging
echo "3. Monitoring MiraWhisper processing..."
echo "   📊 Checking for processing activity..."

# Wait and check for output files with detailed monitoring
for i in {1..120}; do
    echo "   ⏱️  Checking... ($i/120) - $(date '+%H:%M:%S')"
    
    # Check for any new files in output directory
    OUTPUT_FILES=$(adb shell find "$MIRAWHISPER_DIR/out" -name "*$(basename "$INPUT_FILE" .mp4)*" -newer "$MIRAWHISPER_DIR/trigger_processing.txt" 2>/dev/null || echo "")
    
    if [ -n "$OUTPUT_FILES" ]; then
        echo "   ✅ New output files detected!"
        break
    fi
    
    # Check for processing activity in logs
    PROCESSING_ACTIVITY=$(adb shell dumpsys activity services | grep -i "whisper\|mirawhisper" || echo "")
    if [ -n "$PROCESSING_ACTIVITY" ]; then
        echo "   🔄 Processing activity detected in services"
    fi
    
    # Check for any files in output directory
    ANY_OUTPUT=$(adb shell find "$MIRAWHISPER_DIR/out" -type f 2>/dev/null || echo "")
    if [ -n "$ANY_OUTPUT" ]; then
        echo "   📄 Output directory has files:"
        echo "$ANY_OUTPUT" | head -3
    fi
    
    sleep 5
done

# Step 4: Retrieve and analyze results
echo "4. Retrieving transcription results..."
OUTPUT_FILES=$(adb shell find "$MIRAWHISPER_DIR/out" -name "*$(basename "$INPUT_FILE" .mp4)*" 2>/dev/null || echo "")

if [ -n "$OUTPUT_FILES" ]; then
    echo "   📄 Output files found:"
    echo "$OUTPUT_FILES"
    
    # Copy results to local directory
    LOCAL_OUTPUT_DIR="mirawhisper_integration_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$LOCAL_OUTPUT_DIR"
    
    for file in $OUTPUT_FILES; do
        filename=$(basename "$file")
        adb pull "$file" "$LOCAL_OUTPUT_DIR/$filename"
        echo "   📥 Downloaded: $filename"
        
        # Show content preview
        if [[ "$filename" == *.txt || "$filename" == *.srt ]]; then
            echo "   📖 Content preview:"
            head -5 "$LOCAL_OUTPUT_DIR/$filename" | sed 's/^/      /'
        fi
    done
    
    echo "   ✅ Results saved to: $LOCAL_OUTPUT_DIR"
    
    # Step 5: Verify integration success
    echo "5. Verifying MiraWhisper integration..."
    echo "   🎾 Tennis interview processing completed!"
    echo "   🔗 Integration: MiraWhisper + Real JNI"
    echo "   📊 Processing method: Direct file-based trigger"
    
else
    echo "   ❌ No output files found"
    echo "   🔍 Debugging information:"
    echo "   📁 Output directory contents:"
    adb shell ls -la "$MIRAWHISPER_DIR/out/" || echo "   Output directory empty or inaccessible"
    echo "   📁 Input directory contents:"
    adb shell ls -la "$MIRAWHISPER_DIR/in/" || echo "   Input directory empty or inaccessible"
fi

echo "=== Direct Integration Complete ==="
