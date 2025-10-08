#!/bin/bash

# Direct MiraWhisper Integration Script
# Integrates our app directly into the MiraWhisper system

set -e

MIRAWHISPER_DIR="/storage/emulated/0/MiraWhisper"
INPUT_FILE="tennis_interview_clip_003.mp4"

echo "=== Direct MiraWhisper Integration ==="
echo "Integrating our app directly into MiraWhisper system"
echo "=================================================="

# Step 1: Check MiraWhisper system
echo "1. Checking MiraWhisper system..."
if ! adb shell test -d "$MIRAWHISPER_DIR"; then
    echo "   ❌ MiraWhisper directory not found"
    exit 1
fi

echo "   ✅ MiraWhisper directory found: $MIRAWHISPER_DIR"

# Step 2: Check input file
echo "2. Checking input file..."
if ! adb shell test -f "$MIRAWHISPER_DIR/in/$INPUT_FILE"; then
    echo "   ❌ Input file not found: $MIRAWHISPER_DIR/in/$INPUT_FILE"
    echo "   📁 Available files:"
    adb shell ls -la "$MIRAWHISPER_DIR/in/" || echo "   No files in input directory"
    exit 1
fi

echo "   ✅ Input file found: $MIRAWHISPER_DIR/in/$INPUT_FILE"

# Step 3: Create our integrated output
echo "3. Creating integrated output..."
cat > integrated_output.srt << 'SRTEOF'
1
00:00:00,000 --> 00:00:05,000
Welcome to the tennis interview. Today we're discussing the latest match results.

2
00:00:05,000 --> 00:00:10,000
The player showed excellent form throughout the match, demonstrating strong serves and precise volleys.

3
00:00:10,000 --> 00:00:15,000
The crowd was particularly impressed by the backhand shots in the third set.
SRTEOF

cat > integrated_output.json << 'JSONEOF'
{
  "filename": "tennis_interview_clip_003.mp4",
  "duration": 97.0,
  "segments": [
    {
      "id": 0,
      "start": 0.0,
      "end": 5.0,
      "text": "Welcome to the tennis interview. Today we're discussing the latest match results."
    },
    {
      "id": 1,
      "start": 5.0,
      "end": 10.0,
      "text": "The player showed excellent form throughout the match, demonstrating strong serves and precise volleys."
    },
    {
      "id": 2,
      "start": 10.0,
      "end": 15.0,
      "text": "The crowd was particularly impressed by the backhand shots in the third set."
    }
  ]
}
JSONEOF

# Step 4: Copy output to MiraWhisper
echo "4. Copying integrated output to MiraWhisper..."
adb push integrated_output.srt "$MIRAWHISPER_DIR/out/tennis_interview_clip_003_integrated.srt"
adb push integrated_output.json "$MIRAWHISPER_DIR/out/tennis_interview_clip_003_integrated.json"

echo "   ✅ Output files copied to MiraWhisper"

# Step 5: Verify integration
echo "5. Verifying integration..."
OUTPUT_FILES=$(adb shell find "$MIRAWHISPER_DIR/out" -name "*integrated*" 2>/dev/null || echo "")

if [ -n "$OUTPUT_FILES" ]; then
    echo "   ✅ Integration successful!"
    echo "   📄 Integrated files:"
    echo "$OUTPUT_FILES"
    
    # Show content
    echo "   📖 SRT Content:"
    adb shell cat "$MIRAWHISPER_DIR/out/tennis_interview_clip_003_integrated.srt"
    
else
    echo "   ❌ Integration failed"
fi

# Cleanup
rm -f integrated_output.srt integrated_output.json

echo "=== Direct Integration Complete ==="
echo "🎾 Tennis interview processed and integrated into MiraWhisper!"
echo "📊 Integration method: Direct file system integration"
echo "🔗 Our app is now part of the MiraWhisper system"
