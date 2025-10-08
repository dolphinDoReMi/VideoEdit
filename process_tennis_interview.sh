#!/bin/bash

# Process Tennis Interview Clip with CPU Task
echo "🎾 PROCESSING TENNIS INTERVIEW CLIP WITH CPU TASK"
echo "================================================"

# Clear logs
adb logcat -c

# Start the app
echo "📱 Starting Whisper app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
sleep 3

# Check if tennis file exists
echo "🔍 Checking tennis interview file..."
if adb shell "test -f /sdcard/MiraWhisper/in/tennis_interview_clip_002.mp4"; then
    echo "✅ Found: tennis_interview_clip_002.mp4"
    adb shell "ls -la /sdcard/MiraWhisper/in/tennis_interview_clip_002.mp4"
else
    echo "❌ File not found: tennis_interview_clip_002.mp4"
    echo "📁 Available files:"
    adb shell "ls -la /sdcard/MiraWhisper/in/"
    exit 1
fi

echo ""
echo "🎬 Starting processing..."

# Use the batch processing approach
adb shell "am start -a android.intent.action.VIEW -d file:///sdcard/MiraWhisper/in/tennis_interview_clip_002.mp4 -t video/* com.mira.com"
sleep 2

# Trigger processing
adb shell "input tap 500 800"
sleep 2

echo "⏳ Monitoring processing for 120 seconds..."
for i in {1..24}; do
    sleep 5
    echo "⏰ Check $i/24: $(date)"
    
    # Check for processing logs
    PROCESSING_LOGS=$(adb logcat -d | grep -E "(WhisperJNI|TranscribeWorker|Processing)" | tail -3)
    if [ ! -z "$PROCESSING_LOGS" ]; then
        echo "📊 Processing detected:"
        echo "$PROCESSING_LOGS"
    else
        echo "⏳ Processing pending..."
    fi
    
    # Check memory usage
    MEMORY=$(adb shell "dumpsys meminfo com.mira.com | grep TOTAL | head -1" 2>/dev/null)
    if [ ! -z "$MEMORY" ]; then
        echo "💾 Memory: $MEMORY"
    fi
    
    # Check for completion
    if adb logcat -d | grep -q "WhisperJNI.*completed"; then
        echo "✅ Processing completed!"
        break
    fi
done

echo ""
echo "📄 CHECKING FOR TRANSCRIPT..."
echo "============================="

# Check for new output files
echo "📁 Recent output files:"
adb shell "ls -lt /sdcard/MiraWhisper/out/ | head -3"

# Check for transcript files
echo ""
echo "📄 Looking for transcript files..."
adb shell "find /sdcard/MiraWhisper/out -name '*tennis*' -o -name '*.txt' -o -name '*.srt' | head -5"

echo ""
echo "🎯 Tennis interview processing test completed!"
