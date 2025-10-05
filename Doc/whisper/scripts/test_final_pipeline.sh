#!/bin/bash

echo "=== Final Comprehensive Pipeline Test ==="

# Clear logcat and start fresh
adb logcat -c

# Step 1: Launch the file selection activity
echo "Step 1: Launching file selection activity..."
adb shell am force-stop com.mira.com
sleep 3
adb shell am start -n com.mira.com/.whisper.WhisperFileSelectionActivity
sleep 5

# Step 2: Take a screenshot to see the current state
echo "Step 2: Taking screenshot..."
adb shell screencap -p /sdcard/pipeline_test.png
adb pull /sdcard/pipeline_test.png pipeline_test.png

# Step 3: Test the browse local button
echo "Step 3: Testing browse local button..."
adb shell input tap 1200 1200
sleep 3

# Step 4: Check for any console logs or errors
echo "Step 4: Checking console logs..."
adb logcat -d | grep -i "chromium.*console\|error\|exception" | tail -5

# Step 5: Test the select files button
echo "Step 5: Testing select files button..."
adb shell input tap 800 1200
sleep 3

# Step 6: Check for file picker logs
echo "Step 6: Checking file picker logs..."
adb logcat -d | grep -i "file.*picker\|AndroidWhisperBridge.*Opening" | tail -3

# Step 7: Test navigation to processing
echo "Step 7: Testing navigation to processing..."
adb shell am start -n com.mira.com/.whisper.WhisperProcessingActivity
sleep 3

# Step 8: Test navigation to results
echo "Step 8: Testing navigation to results..."
adb shell am start -n com.mira.com/.whisper.WhisperResultsActivity
sleep 3

# Step 9: Final status check
echo "Step 9: Final status check..."
adb logcat -d | grep -i "WhisperProcessingActivity\|WhisperResultsActivity\|Page finished loading" | tail -5

echo "=== Final test completed ==="
