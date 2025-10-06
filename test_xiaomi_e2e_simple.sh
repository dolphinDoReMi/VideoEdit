#!/bin/bash

echo "=== XIAOMI PAD E2E TEST - NON-HANGING VERSION ==="
echo "Testing with video_v1.mp4 (393MB) - End-to-End Whisper Processing"
echo ""

# Clear logs
echo "Clearing logs..."
adb logcat -c

# Launch the app
echo "Launching Whisper app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"

echo ""
echo "=== MANUAL TEST INSTRUCTIONS ==="
echo "1. Wait for app to load"
echo "2. Go to File Selection page"
echo "3. Navigate to /sdcard/MiraWhisper/ and select video_v1.mp4 (393MB)"
echo "4. Start processing"
echo "5. Wait for processing to complete (may take several minutes)"
echo ""
echo "After processing, run this command to check results:"
echo "adb logcat -d | grep -E '(STREAMING|TECHNICAL|ERROR|SUCCESS)' | tail -50"
echo ""
echo "Expected to see:"
echo "- 'TECHNICAL: Large file detected - using streaming processing'"
echo "- 'TECHNICAL: Processing X chunks of 30000ms each'"
echo "- '=== STREAMING CHUNK X/Y START ==='"
echo "- 'TECHNICAL: Chunk loaded in Xms'"
echo "- 'TECHNICAL: Whisper processing: Xms'"
echo "- 'TECHNICAL: Streaming processing completed successfully'"
echo ""
echo "=== TEST SETUP COMPLETE ==="
echo "Please proceed with manual testing on the device."
