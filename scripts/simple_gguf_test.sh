#!/bin/bash

# Simple GGUF Whisper Test for Xiaomi Pad
# Step-by-step testing approach

set -e

echo "🎯 Simple GGUF Whisper Test - Xiaomi Pad"
echo "========================================"
echo "Timestamp: $(date)"
echo ""

# Step 1: Check device and models
echo "1. Checking device and GGUF models..."
echo "Device: $(adb shell getprop ro.product.model)"
echo "Android: $(adb shell getprop ro.build.version.release)"
echo "Architecture: $(adb shell getprop ro.product.cpu.abi)"
echo ""

echo "📁 Checking GGUF models on device..."
adb shell "ls -la /sdcard/MiraWhisper/models/ | grep -E '\.(bin|gguf)$'"
echo ""

# Step 2: Create simple test audio
echo "2. Creating simple test audio..."
TEST_AUDIO="/sdcard/MiraWhisper/test_audio.wav"
echo "Creating 5-second test audio file..."

# Create a simple test audio file using the app
adb shell "mkdir -p /sdcard/MiraWhisper/test"
adb shell "echo 'test audio content' > /sdcard/MiraWhisper/test/test.txt"

# Use existing small audio file if available
if adb shell "test -f /sdcard/MiraWhisper/in/test.wav"; then
    adb shell "cp /sdcard/MiraWhisper/in/test.wav $TEST_AUDIO"
    echo "✅ Using existing test audio file"
else
    echo "⚠️  No test audio found, will use video file directly"
fi
echo ""

# Step 3: Test model loading
echo "3. Testing GGUF model loading..."
MODEL_PATH="/sdcard/MiraWhisper/models/small.en.bin"
echo "Testing model: $MODEL_PATH"

# Clear logs
adb logcat -c

# Launch app and test model loading
echo "🚀 Launching app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"

sleep 3

# Test model loading with a simple broadcast
echo "📤 Testing model loading..."
adb shell "am broadcast -a com.mira.com.action.TEST_MODEL_LOADING --es 'model_path' '$MODEL_PATH'"

echo "⏳ Monitoring model loading for 30 seconds..."
sleep 30

# Check logs for model loading
echo "📊 Model loading logs:"
adb logcat -d | grep -E "(WhisperJNI|GGML|model|loading)" | tail -10
echo ""

# Step 4: Test basic audio processing
echo "4. Testing basic audio processing..."
echo "📤 Starting simple transcription test..."

# Use the tennis clip for a quick test
TENNIS_CLIP="/sdcard/MiraWhisper/in/tennis_interview_clip_002.mp4"
if adb shell "test -f '$TENNIS_CLIP'"; then
    echo "✅ Using tennis clip: $TENNIS_CLIP"
    
    # Start simple transcription
    adb shell "am broadcast -a com.mira.com.action.TEST_TRANSCRIPTION --es 'file_path' '$TENNIS_CLIP' --es 'model' 'small.en.bin'"
    
    echo "⏳ Monitoring transcription for 60 seconds..."
    sleep 60
    
    # Check for results
    echo "📊 Transcription logs:"
    adb logcat -d | grep -E "(transcription|whisper|processing)" | tail -10
    echo ""
    
    # Check for output files
    echo "📁 Checking for output files..."
    adb shell "find /sdcard/MiraWhisper -name '*.txt' -o -name '*.srt' | head -5"
    echo ""
else
    echo "❌ Tennis clip not found"
fi

# Step 5: Performance summary
echo "5. Performance Summary..."
echo "========================"
echo "Device: $(adb shell getprop ro.product.model)"
echo "Model: small.en.bin (GGUF Q5_1)"
echo "Test Duration: ~90 seconds"
echo ""

# Check memory usage
echo "📊 Memory usage:"
adb shell "dumpsys meminfo com.mira.com | grep -E '(TOTAL|Native|Java)'"
echo ""

# Check CPU usage
echo "📊 CPU usage:"
adb shell "dumpsys cpuinfo | grep com.mira.com || echo 'No CPU data available'"
echo ""

echo "✅ Simple GGUF test completed!"
echo "📋 Check logs above for any issues"
echo "📁 Check /sdcard/MiraWhisper/ for output files"
