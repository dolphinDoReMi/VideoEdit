#!/bin/bash

# Direct whisper test script - bypasses broadcast system
# This tests the core whisper functionality step by step

echo "=== Direct Whisper Test ==="
echo "Testing whisper functionality without broadcast system"
echo

# Step 1: Check if model file exists and is accessible
echo "1. Checking model file..."
adb shell "ls -la /sdcard/MiraWhisper/models/whisper-base.q5_1.bin"
if [ $? -eq 0 ]; then
    echo "✅ Model file exists"
else
    echo "❌ Model file not found"
    exit 1
fi

# Step 2: Check if test audio file exists
echo
echo "2. Checking test audio file..."
adb shell "ls -la /sdcard/MiraWhisper/clips/tennis_interview_clip_001.mp4"
if [ $? -eq 0 ]; then
    echo "✅ Test audio file exists"
else
    echo "❌ Test audio file not found"
    exit 1
fi

# Step 3: Extract a small sample of audio for testing
echo
echo "3. Extracting 10-second audio sample..."
adb shell "ffmpeg -i /sdcard/MiraWhisper/clips/tennis_interview_clip_001.mp4 -t 10 -ar 16000 -ac 1 -f wav /sdcard/MiraWhisper/test_sample.wav"
if [ $? -eq 0 ]; then
    echo "✅ Audio sample extracted"
else
    echo "❌ Audio extraction failed"
    exit 1
fi

# Step 4: Test whisper model loading through JNI
echo
echo "4. Testing whisper model loading..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'test_mode' 'true' --es 'test_action' 'load_model' --es 'model_path' '/sdcard/MiraWhisper/models/whisper-base.q5_1.bin'"

# Step 5: Test whisper inference with sample audio
echo
echo "5. Testing whisper inference..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'test_mode' 'true' --es 'test_action' 'transcribe' --es 'audio_path' '/sdcard/MiraWhisper/test_sample.wav' --es 'model_path' '/sdcard/MiraWhisper/models/whisper-base.q5_1.bin'"

echo
echo "=== Test completed ==="
echo "Check the app logs for results:"
echo "adb logcat -s WhisperJNI:D WhisperBridge:D | head -20"
