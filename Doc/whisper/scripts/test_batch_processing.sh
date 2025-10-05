#!/bin/bash

echo "=== Testing Complete Batch Processing Workflow ==="

# Clear logcat
adb logcat -c

# Step 1: Test the getAllVideoFiles method
echo "Step 1: Testing getAllVideoFiles method..."
adb shell am broadcast -a com.mira.whisper.GET_ALL_VIDEO_FILES
sleep 2

# Step 2: Check if files were found
echo "Step 2: Checking file discovery results..."
adb logcat -d | grep -i "getAllVideoFiles\|Getting all video files" | tail -3

# Step 3: Test batch processing with the video file
echo "Step 3: Testing batch processing..."
adb shell am broadcast -a com.mira.whisper.RUN_BATCH -e uris "file:///sdcard/Movies/video_v1_long.mp4" -e preset "Single" -e modelPath "ggml-small.en.bin"
sleep 3

# Step 4: Check batch processing logs
echo "Step 4: Checking batch processing logs..."
adb logcat -d | grep -i "runBatch\|batch.*processing\|AndroidWhisperBridge.*runBatch" | tail -5

# Step 5: Test single file processing
echo "Step 5: Testing single file processing..."
adb shell am broadcast -a com.mira.whisper.RUN -e uri "file:///sdcard/Movies/video_v1_long.mp4" -e preset "Single" -e modelPath "ggml-small.en.bin"
sleep 3

# Step 6: Check single processing logs
echo "Step 6: Checking single processing logs..."
adb logcat -d | grep -i "run.*jsonStr\|AndroidWhisperBridge.*run" | tail -5

# Step 7: Check for any job IDs or processing results
echo "Step 7: Checking for job IDs and results..."
adb logcat -d | grep -i "job.*id\|whisper_.*\|batch.*id" | tail -5

echo "=== Batch processing test completed ==="
