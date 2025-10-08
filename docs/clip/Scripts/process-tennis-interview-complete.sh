#!/bin/bash

# Complete TennisInterview Processing Workflow
# Splits large video into clips, processes each clip individually, then merges results

set -e

echo "🎾 TennisInterview Complete Processing Workflow"
echo "=============================================="
echo ""

# Configuration
INPUT_FILE="/sdcard/MiraWhisper/TennisInterview_converted.mp4"
CLIPS_DIR="/sdcard/MiraWhisper/clips"
TRANSCRIPTIONS_DIR="/sdcard/MiraWhisper/transcriptions"
FINAL_DIR="/sdcard/MiraWhisper/final"

# Step 1: Split video into clips
echo "Step 1: Splitting TennisInterview into manageable clips..."
echo "--------------------------------------------------------"
./split_tennis_interview.sh

if [[ $? -ne 0 ]]; then
    echo "❌ Video splitting failed!"
    exit 1
fi

echo ""
echo "✅ Video splitting completed successfully!"
echo ""

# Step 2: Process each clip individually
echo "Step 2: Processing individual clips..."
echo "-------------------------------------"
./process_tennis_clips.sh

if [[ $? -ne 0 ]]; then
    echo "❌ Clip processing failed!"
    exit 1
fi

echo ""
echo "✅ All clips processed successfully!"
echo ""

# Step 3: Merge transcriptions
echo "Step 3: Merging transcription results..."
echo "----------------------------------------"
./merge_tennis_transcriptions.sh

if [[ $? -ne 0 ]]; then
    echo "❌ Transcription merging failed!"
    exit 1
fi

echo ""
echo "🎉 Complete processing workflow finished successfully!"
echo ""

# Final summary
echo "📊 Final Summary"
echo "==============="
echo ""

# Count files in each directory
CLIP_COUNT=$(adb shell "ls $CLIPS_DIR/tennis_interview_clip_*.mp4 2>/dev/null | wc -l" | tr -d '\r')
TRANSCRIPTION_COUNT=$(adb shell "ls $TRANSCRIPTIONS_DIR/tennis_interview_clip_*_transcription.json 2>/dev/null | wc -l" | tr -d '\r')

echo "📁 File counts:"
echo "  Clips created: $CLIP_COUNT"
echo "  Transcriptions generated: $TRANSCRIPTION_COUNT"
echo ""

echo "📂 Directory structure:"
echo "  Clips: $CLIPS_DIR"
echo "  Individual transcriptions: $TRANSCRIPTIONS_DIR"
echo "  Final merged results: $FINAL_DIR"
echo ""

echo "📄 Final output files:"
adb shell "ls -la $FINAL_DIR/"

echo ""
echo "🔍 Quality check:"
if [[ -n "$(adb shell "test -f '$FINAL_DIR/tennis_interview_complete_transcription.json' && echo 'exists'")" ]]; then
    TOTAL_DURATION=$(adb shell "jq -r '.duration' '$FINAL_DIR/tennis_interview_complete_transcription.json'" | tr -d '\r')
    TOTAL_SEGMENTS=$(adb shell "jq '.segments | length' '$FINAL_DIR/tennis_interview_complete_transcription.json'" | tr -d '\r')
    AVG_RTF=$(adb shell "jq -r '.rtf' '$FINAL_DIR/tennis_interview_complete_transcription.json'" | tr -d '\r')
    
    echo "  ✅ Complete JSON transcription created"
    echo "  📏 Total duration: ${TOTAL_DURATION}s ($(($TOTAL_DURATION / 60)) minutes)"
    echo "  📝 Total segments: $TOTAL_SEGMENTS"
    echo "  ⚡ Average RTF: $AVG_RTF"
else
    echo "  ❌ Final JSON transcription not found"
fi

if [[ -n "$(adb shell "test -f '$FINAL_DIR/tennis_interview_complete_transcription.srt' && echo 'exists'")" ]]; then
    SRT_SIZE=$(adb shell "stat -c%s '$FINAL_DIR/tennis_interview_complete_transcription.srt'" | tr -d '\r')
    echo "  ✅ Complete SRT captions created (${SRT_SIZE} bytes)"
else
    echo "  ❌ Final SRT captions not found"
fi

if [[ -n "$(adb shell "test -f '$FINAL_DIR/tennis_interview_complete_transcription.txt' && echo 'exists'")" ]]; then
    TXT_SIZE=$(adb shell "stat -c%s '$FINAL_DIR/tennis_interview_complete_transcription.txt'" | tr -d '\r')
    echo "  ✅ Complete TXT transcription created (${TXT_SIZE} bytes)"
else
    echo "  ❌ Final TXT transcription not found"
fi

echo ""
echo "🎯 Processing method: Individual clips (standard processing)"
echo "💡 Benefits:"
echo "  - Uses same reliable processing as video_v1_long.mp4"
echo "  - No complex streaming/chunking overhead"
echo "  - Better error handling and recovery"
echo "  - Easier to debug and monitor"
echo "  - Consistent naming convention"
echo ""

echo "🚀 Next steps:"
echo "  1. Review transcription quality in final files"
echo "  2. Use SRT file for video subtitles"
echo "  3. Use JSON file for further processing/analysis"
echo "  4. Clean up individual clip files if needed"
echo ""

echo "✨ TennisInterview processing complete!"
