#!/bin/bash

# Test real transcript generation with tennis interview clip
echo "🎯 Testing Real Transcript Generation"
echo "====================================="

# Clear previous logs
adb logcat -c

# Start processing the tennis interview clip
echo "📱 Starting app and processing tennis_interview_clip_002.mp4..."

# Launch the app
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
sleep 3

# Simulate file selection and processing
echo "🎬 Simulating file selection..."
adb shell "input tap 500 600"  # Tap to select file
sleep 2

echo "▶️ Starting processing..."
adb shell "input tap 500 800"  # Tap to start processing
sleep 2

# Monitor processing for 5 minutes
echo "⏳ Monitoring processing for 300 seconds..."
for i in {1..60}; do
    sleep 5
    echo "⏰ Check $i/60: $(date)"
    
    # Check for completion
    if adb logcat -d | grep -q "Whisper inference completed successfully"; then
        echo "✅ Processing completed!"
        break
    fi
    
    # Show progress
    adb logcat -d | grep -E "(WhisperJNI|Processing)" | tail -3
done

echo ""
echo "📊 Final Results:"
echo "================="

# Check for real transcript files
echo "🔍 Looking for transcript files..."
adb shell "find /sdcard/MiraWhisper -name '*tennis_interview_clip_002*' -type f"

echo ""
echo "📝 Recent processing logs:"
adb logcat -d | grep -E "(WhisperJNI|Processing|tennis)" | tail -10

echo ""
echo "📄 Checking for any new transcript files..."
adb shell "find /sdcard/MiraWhisper/out -name '*.txt' -o -name '*.srt' -o -name '*.json' -newermt '$(date -d '5 minutes ago' '+%Y-%m-%d %H:%M:%S')'"

echo ""
echo "🎯 Test completed!"
