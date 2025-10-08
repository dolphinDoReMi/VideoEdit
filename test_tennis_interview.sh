#!/bin/bash

echo "Testing TennisInterview.mkv transcription on Xiaomi Pad..."

# Clear logs
adb logcat -c

# Start monitoring logs in background
adb logcat | grep -E "(WhisperBridge|getBatchInfo|TennisInterview|File selection|Processing)" &
LOG_PID=$!

# Launch the app
echo "Launching Whisper app..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity

# Wait a moment for the app to load
sleep 3

# Try to trigger file picker by tapping on the Select Files button area
echo "Attempting to trigger file picker..."
adb shell input tap 1600 600

# Wait for file picker to appear
sleep 2

# Try to navigate to the file location and select it
echo "Attempting to select TennisInterview.mkv..."
adb shell input tap 1000 500

# Wait for file selection
sleep 2

# Try to start processing
echo "Attempting to start transcription..."
adb shell input tap 1600 800

# Monitor for a while
echo "Monitoring transcription process..."
sleep 30

# Stop monitoring
kill $LOG_PID

echo "Test completed. Check logs for results."
