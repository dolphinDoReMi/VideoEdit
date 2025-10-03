#!/bin/bash

# Video Transcription Test Script
# Tests the WhisperEngine with video_v1.mp4

echo "🎬 Video Transcription Test - Testing with video_v1.mp4"
echo "=================================================="

# Check if video file exists
VIDEO_FILE="app/src/main/assets/video_v1.mp4"
if [ ! -f "$VIDEO_FILE" ]; then
    echo "❌ Error: Video file not found at $VIDEO_FILE"
    exit 1
fi

echo "✅ Video file found: $VIDEO_FILE"

# Get video file info
VIDEO_SIZE=$(stat -f%z "$VIDEO_FILE" 2>/dev/null || stat -c%s "$VIDEO_FILE" 2>/dev/null)
VIDEO_SIZE_MB=$((VIDEO_SIZE / 1024 / 1024))

echo "📊 Video file size: ${VIDEO_SIZE_MB}MB"

# Check if the test activity exists
TEST_ACTIVITY="app/src/main/java/com/mira/videoeditor/VideoTranscriptionTestActivity.kt"
if [ ! -f "$TEST_ACTIVITY" ]; then
    echo "❌ Error: Test activity not found at $TEST_ACTIVITY"
    exit 1
fi

echo "✅ Test activity found: $TEST_ACTIVITY"

# Check if WhisperEngine exists and is properly configured
WHISPER_ENGINE="app/src/main/java/com/mira/videoeditor/whisper/WhisperEngine.kt"
if [ ! -f "$WHISPER_ENGINE" ]; then
    echo "❌ Error: WhisperEngine not found at $WHISPER_ENGINE"
    exit 1
fi

echo "✅ WhisperEngine found: $WHISPER_ENGINE"

# Check for MediaExtractor usage (for audio extraction)
if grep -q "MediaExtractor" "$WHISPER_ENGINE"; then
    echo "✅ MediaExtractor integration found"
else
    echo "❌ Warning: MediaExtractor not found in WhisperEngine"
fi

# Check for video transcription method
if grep -q "transcribeVideo" "$WHISPER_ENGINE"; then
    echo "✅ transcribeVideo method found"
else
    echo "❌ Warning: transcribeVideo method not found"
fi

# Check AndroidManifest for test activity
if grep -q "VideoTranscriptionTestActivity" "app/src/main/AndroidManifest.xml"; then
    echo "✅ Test activity registered in AndroidManifest"
else
    echo "❌ Warning: Test activity not registered in AndroidManifest"
fi

echo ""
echo "🧪 Test Summary:"
echo "================"
echo "• Video file: video_v1.mp4 (${VIDEO_SIZE_MB}MB)"
echo "• Test activity: VideoTranscriptionTestActivity"
echo "• WhisperEngine: Updated for video processing"
echo "• Audio extraction: MediaExtractor integration"
echo "• Transcription: Simulation-based (ready for real implementation)"

echo ""
echo "🚀 Next Steps:"
echo "=============="
echo "1. Build the app: ./gradlew assembleDebug"
echo "2. Install on device: adb install app/build/outputs/apk/debug/app-debug.apk"
echo "3. Launch test activity: adb shell am start -n com.mira.videoeditor/.VideoTranscriptionTestActivity"
echo "4. Check logs: adb logcat | grep -E '(VideoTranscriptionTest|WhisperEngine)'"

echo ""
echo "📱 Manual Testing:"
echo "=================="
echo "1. Open the 'Video Transcription Test' activity"
echo "2. Click 'Initialize WhisperEngine'"
echo "3. Click 'Transcribe Video' to process video_v1.mp4"
echo "4. View the transcription result"
echo "5. Click 'Parse Segments' to see word-level timing"

echo ""
echo "✅ Test setup complete! Ready for verification."
