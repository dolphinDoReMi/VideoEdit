#!/bin/bash

# Simple whisper test - just test the core JNI functionality
echo "=== Simple Whisper Test ==="

# Step 1: Create a simple test audio file (10 seconds)
echo "1. Creating test audio..."
adb shell "ffmpeg -f lavfi -i 'sine=frequency=440:duration=10' -ar 16000 -ac 1 -f wav /sdcard/test_sine.wav"

# Step 2: Test whisper JNI directly
echo "2. Testing whisper JNI..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'direct_test' 'true'"

echo "3. Check logs for JNI activity:"
echo "adb logcat -s WhisperJNI:D | head -10"
