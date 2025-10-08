#!/bin/bash

echo "=== Comprehensive Whisper Pipeline Test ==="

# Clear logcat
adb logcat -c

# Step 1: Launch the app
echo "Step 1: Launching WhisperFileSelectionActivity..."
adb shell am force-stop com.mira.com
sleep 2
adb shell am start -n com.mira.com/.whisper.WhisperFileSelectionActivity
sleep 3

# Step 2: Test browse local functionality
echo "Step 2: Testing browse local functionality..."
adb shell input tap 1200 1200
sleep 2

# Step 3: Check logs for file discovery
echo "Step 3: Checking file discovery logs..."
adb logcat -d | grep -i "getAllVideoFiles\|Local media scan\|Found.*video files" | tail -5

# Step 4: Test manual file selection
echo "Step 4: Testing manual file selection..."
adb shell input tap 800 1200
sleep 2

# Step 5: Check logs for file picker
echo "Step 5: Checking file picker logs..."
adb logcat -d | grep -i "File picker\|Opening file picker" | tail -3

# Step 6: Test navigation to processing
echo "Step 6: Testing navigation to processing..."
adb shell am start -n com.mira.com/.whisper.WhisperProcessingActivity
sleep 2

# Step 7: Test navigation to results
echo "Step 7: Testing navigation to results..."
adb shell am start -n com.mira.com/.whisper.WhisperResultsActivity
sleep 2

# Step 8: Final status check
echo "Step 8: Final status check..."
adb logcat -d | grep -i "WhisperProcessingActivity\|WhisperResultsActivity" | tail -3

echo "=== Comprehensive test completed ==="
