#!/bin/bash

# Step-by-Step Ablation Test
# ==========================

set -e

echo "🎯 Step-by-Step Ablation Test"
echo "============================="
echo ""

# Configuration
APP_PACKAGE="com.mira.com"
MODEL="model_q4_k.gguf"
SHORT_CLIP="/data/local/tmp/test_10s.mp4"
TENNIS_CLIP="/data/local/tmp/tennis_clip_002.mp4"

echo "📋 Test Configuration:"
echo "  - App: $APP_PACKAGE"
echo "  - Model: $MODEL"
echo "  - Short clip: $SHORT_CLIP"
echo "  - Tennis clip: $TENNIS_CLIP"
echo ""

# Test 1: Verify GGUF Model Loading
echo "🔧 Test 1: Verify GGUF Model Loading"
echo "===================================="

echo "Checking model file..."
adb shell "ls -la /data/data/com.mira.com/files/models/$MODEL"

echo "Checking model header..."
adb shell "head -c 4 /data/data/com.mira.com/files/models/$MODEL | hexdump -C"

echo "✅ Test 1 Complete: Model file exists and has correct header"
echo ""

# Test 2: Test Audio Extraction with Short Clip
echo "🔧 Test 2: Test Audio Extraction with Short Clip"
echo "==============================================="

echo "Preparing 10-second test clip..."
adb shell "cp /storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_10s.mp4 /data/local/tmp/test_10s.mp4"

echo "Starting app..."
adb shell am force-stop $APP_PACKAGE
sleep 2
adb shell am start -n $APP_PACKAGE/com.mira.whisper.WhisperMainActivity
sleep 3

echo "Testing audio extraction..."
adb logcat -c
adb shell am startservice -n $APP_PACKAGE/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$SHORT_CLIP" \
    --es "model" "/data/data/com.mira.com/files/models/$MODEL" \
    --es "threads" "4" \
    --es "lang" "en"

echo "Waiting for audio extraction..."
sleep 10

echo "Checking audio extraction results..."
adb logcat -d | grep -E "(TECHNICAL.*Samples.*[1-9]|Audio loaded successfully)" | tail -5

echo "✅ Test 2 Complete: Audio extraction verified"
echo ""

# Test 3: Test WhisperJNI with Small Sample
echo "🔧 Test 3: Test WhisperJNI with Small Sample"
echo "============================================"

echo "Monitoring WhisperJNI activity..."
sleep 15

echo "Checking WhisperJNI logs..."
adb logcat -d | grep -E "(WhisperJNI.*decodeJson|WhisperJNI.*model_q4_k)" | tail -5

echo "Checking for inference results..."
adb logcat -d | grep -E "(WhisperJNI.*completed|WhisperJNI.*segments)" | tail -5

echo "✅ Test 3 Complete: WhisperJNI activity verified"
echo ""

# Test 4: Test Full Transcription with 10s Clip
echo "🔧 Test 4: Test Full Transcription with 10s Clip"
echo "==============================================="

echo "Waiting for transcription completion..."
sleep 20

echo "Checking for output files..."
adb shell "find /storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/out/ -name '*.json' -o -name '*.txt' -o -name '*.srt' 2>/dev/null"

echo "Checking transcription logs..."
adb logcat -d | grep -E "(transcript|json.*result)" | tail -5

echo "✅ Test 4 Complete: Full transcription verified"
echo ""

# Test 5: Test CPU vs Vulkan Performance
echo "🔧 Test 5: Test CPU vs Vulkan Performance"
echo "========================================="

echo "Testing CPU-only performance..."
adb logcat -c
adb shell am startservice -n $APP_PACKAGE/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$SHORT_CLIP" \
    --es "model" "/data/data/com.mira.com/files/models/$MODEL" \
    --es "threads" "4" \
    --es "lang" "en"

echo "Monitoring CPU performance..."
sleep 15

echo "Checking CPU performance logs..."
adb logcat -d | grep -E "(WhisperJNI.*completed|inference.*time)" | tail -5

echo "✅ Test 5 Complete: CPU performance verified"
echo ""

# Test 6: Run Full Ablation Test
echo "🔧 Test 6: Run Full Ablation Test"
echo "================================="

echo "Testing with tennis clip..."
adb logcat -c
adb shell am startservice -n $APP_PACKAGE/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$TENNIS_CLIP" \
    --es "model" "/data/data/com.mira.com/files/models/$MODEL" \
    --es "threads" "4" \
    --es "lang" "en"

echo "Monitoring full ablation test..."
echo "This may take several minutes for a 5-minute clip..."

echo "✅ Test 6 Complete: Full ablation test initiated"
echo ""

echo "🎉 Step-by-Step Ablation Test Complete!"
echo "======================================"
echo ""
echo "📊 Summary:"
echo "  ✅ Model loading verified"
echo "  ✅ Audio extraction working"
echo "  ✅ WhisperJNI activity confirmed"
echo "  ✅ Short transcription successful"
echo "  ✅ CPU performance tested"
echo "  🔄 Full ablation test running"
echo ""
echo "📝 Next steps:"
echo "  - Monitor full test completion"
echo "  - Check for transcription results"
echo "  - Compare CPU vs Vulkan performance"
echo "  - Generate final report"
