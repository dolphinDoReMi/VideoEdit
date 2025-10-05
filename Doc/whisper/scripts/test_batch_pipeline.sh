#!/bin/bash

echo "=== Testing Whisper Batch Pipeline with video_v1_long.mp4 ==="

# Step 1: Verify video file exists on device
echo "Step 1: Checking video file on device..."
adb shell ls -la /sdcard/Movies/video_v1_long.mp4

# Step 2: Test file selection by checking getAllVideoFiles
echo "Step 2: Testing file discovery..."
adb shell am broadcast -a com.mira.whisper.GET_ALL_VIDEO_FILES

# Step 3: Test batch processing
echo "Step 3: Testing batch processing..."
adb shell am broadcast -a com.mira.whisper.RUN_BATCH -e uris "file:///sdcard/Movies/video_v1_long.mp4" -e preset "Single" -e modelPath "ggml-small.en.bin"

# Step 4: Check for any processing logs
echo "Step 4: Checking processing logs..."
adb logcat -d | grep -i "whisper\|batch\|processing" | tail -10

echo "=== Pipeline test completed ==="
