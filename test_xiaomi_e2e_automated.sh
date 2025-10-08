#!/bin/bash

echo "=== XIAOMI PAD E2E TEST - AUTOMATED FLOW ==="
echo "Testing with video_v1.mp4 (393MB) - End-to-End Whisper Processing"
echo ""

# Clear logs
echo "Clearing logs..."
adb logcat -c

# Launch the app
echo "Launching Whisper app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
sleep 2

# Try to trigger file processing directly via broadcast
echo "Triggering file processing..."
adb shell "am broadcast -a com.mira.whisper.START_PROCESSING --es 'batch_id' 'e2e_test_batch' --es 'uris' 'file:///sdcard/MiraWhisper/video_v1.mp4'"

# Wait a bit for processing to start
echo "Waiting for processing to start..."
sleep 5

# Check for streaming processing logs
echo "=== CHECKING FOR STREAMING PROCESSING ==="
adb logcat -d | grep -E '(STREAMING|TECHNICAL.*streaming|TECHNICAL.*chunk|TECHNICAL.*Large file)' | tail -10

echo ""
echo "=== CHECKING FOR ERRORS ==="
adb logcat -d | grep -E '(ERROR|libwhisper_jni)' | tail -5

echo ""
echo "=== CHECKING FOR BATCH PROCESSING ==="
adb logcat -d | grep -E '(Batch|Processing)' | tail -5

echo ""
echo "=== RECENT TECHNICAL LOGS ==="
adb logcat -d | grep 'TECHNICAL:' | tail -10

echo ""
echo "=== E2E TEST COMPLETE ==="
echo "If you see streaming processing logs above, the E2E test is working!"
echo "If you see library errors, that's the known issue we identified earlier."
