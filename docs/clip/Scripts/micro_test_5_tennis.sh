#!/bin/bash

# Micro Test 5: Tennis Clip Processing with DirectWhisperService
# Following XiaoMi Pad Inference Optimization guide

echo "🎾 Micro Test 5: Tennis Clip Processing"
echo "====================================="
echo "Timestamp: $(date)"
echo ""

# Check tennis clip exists
TENNIS_CLIP="/sdcard/MiraWhisper/in/tennis_interview_clip_002.mp4"
echo "🔍 Checking tennis clip..."
if adb shell "test -f '$TENNIS_CLIP'"; then
    echo "✅ Tennis clip found: $TENNIS_CLIP"
    FILE_SIZE=$(adb shell "stat -c%s '$TENNIS_CLIP'" | tr -d '\r')
    FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
    echo "📊 File size: ${FILE_SIZE_MB}MB"
else
    echo "❌ Tennis clip not found"
    exit 1
fi
echo ""

# Check GGUF model exists
MODEL_PATH="/sdcard/MiraWhisper/models/small.en.bin"
echo "🔍 Checking GGUF model..."
if adb shell "test -f '$MODEL_PATH'"; then
    echo "✅ GGUF model found: $MODEL_PATH"
    MODEL_SIZE=$(adb shell "stat -c%s '$MODEL_PATH'" | tr -d '\r')
    MODEL_SIZE_MB=$((MODEL_SIZE / 1024 / 1024))
    echo "📊 Model size: ${MODEL_SIZE_MB}MB"
else
    echo "❌ GGUF model not found"
    exit 1
fi
echo ""

# Clear logs
adb logcat -c

# Try to process the tennis clip using the service
echo "🎬 Starting tennis clip processing..."
echo "📤 Sending processing request to DirectWhisperService..."

# Use a simple broadcast to trigger processing
adb shell "am broadcast -a com.mira.com.whisper.PROCESS_FILE \
    --es 'file_path' '$TENNIS_CLIP' \
    --es 'model_path' '$MODEL_PATH' \
    --es 'output_dir' '/sdcard/MiraWhisper/out' \
    --ei 'threads' 4 \
    --es 'language' 'auto'"

echo "⏳ Monitoring processing for 30 seconds..."
sleep 30

# Check for any output files
echo "📁 Checking for output files..."
adb shell "find /sdcard/MiraWhisper -name '*.txt' -o -name '*.srt' -o -name '*.json' | head -10"
echo ""

# Check processing logs
echo "📊 Processing logs:"
adb logcat -d | grep -E "(WhisperJNI|GGML|transcription|processing)" | tail -15
echo ""

# Check service status
echo "🔍 Service status:"
adb shell "dumpsys activity services | grep DirectWhisperService"
echo ""

echo "✅ Micro Test 5 completed!"
echo "📋 Summary:"
echo "  - Tennis clip: ${FILE_SIZE_MB}MB"
echo "  - GGUF model: ${MODEL_SIZE_MB}MB"
echo "  - Processing time: 30 seconds"
echo "  - Check logs above for results"
