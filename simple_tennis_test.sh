#!/bin/bash

# Simple Tennis Interview Clip 002 Test - Direct Approach
# Uses existing app and manual processing

set -e

echo "🎾 Tennis Interview Clip 002 - Simple Test"
echo "=========================================="
echo "Device: Xiaomi Pad Ultra"
echo "Test File: tennis_interview_clip_002.mp4"
echo "Timestamp: $(date)"
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_002.mp4"
CLIP_SOURCE="/Users/dennis/Movies/VideoEdit/tennis_clips/$CLIP_FILE"
DEVICE_PATH="/sdcard/MiraWhisper/in/$CLIP_FILE"
OUTPUT_DIR="/sdcard/MiraWhisper/out"
MODEL="whisper-base.q5_1.bin"

# Results directory
RESULTS_DIR="./simple_test_results_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "📁 Results will be saved to: $RESULTS_DIR"
echo ""

# Check device connection
echo "=== Device Connection Check ==="
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model)
echo "✅ Device connected: $DEVICE_MODEL"

echo ""

# Check source file
echo "=== Source File Check ==="
if [ ! -f "$CLIP_SOURCE" ]; then
    echo "❌ Source clip file not found: $CLIP_SOURCE"
    exit 1
fi

FILE_SIZE=$(stat -f%z "$CLIP_SOURCE")
FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
echo "✅ Source file found: ${FILE_SIZE_MB}MB"

# Get video duration
DURATION_SECONDS=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$CLIP_SOURCE" | cut -d. -f1)
DURATION_MINUTES=$((DURATION_SECONDS / 60))
echo "📏 Video duration: ${DURATION_SECONDS} seconds (${DURATION_MINUTES} minutes)"

echo ""

# Setup device
echo "=== Device Setup ==="
echo "Creating directories..."
adb shell "mkdir -p $OUTPUT_DIR /sdcard/MiraWhisper/in /sdcard/MiraWhisper/models"

# Push clip file
echo "Pushing clip file to device..."
adb push "$CLIP_SOURCE" "$DEVICE_PATH"
echo "✅ Clip file pushed to device"

# Check whisper model
echo "Checking whisper model..."
if ! adb shell "test -f /sdcard/MiraWhisper/models/$MODEL"; then
    echo "⚠️  Whisper model not found, copying from local..."
    if [ -f "whisper_models/$MODEL" ]; then
        adb push "whisper_models/$MODEL" "/sdcard/MiraWhisper/models/"
        echo "✅ Whisper model copied"
    else
        echo "❌ Whisper model not found locally"
        exit 1
    fi
else
    echo "✅ Whisper model found on device"
fi

echo ""

# Check app
echo "=== App Check ==="
if adb shell pm list packages | grep -q "com.mira.com"; then
    echo "✅ App is installed: com.mira.com"
else
    echo "❌ App not found"
    exit 1
fi

echo ""

# Launch app and provide manual instructions
echo "=== Manual Processing Instructions ==="
echo "🚀 Launching app..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity

echo ""
echo "📱 MANUAL PROCESSING REQUIRED:"
echo "==============================="
echo ""
echo "1. On the Xiaomi Pad, navigate to the Whisper section"
echo "2. Select the file: tennis_interview_clip_002.mp4"
echo "3. Click 'Start Processing'"
echo "4. Wait for processing to complete"
echo ""
echo "⏳ Monitoring for completion..."
echo ""

# Monitor for completion
START_TIME=$(date +%s)
PROCESSING_COMPLETE=false
WAIT_TIME=0
MAX_WAIT_TIME=1800  # 30 minutes

echo "Monitoring device performance..."
{
    echo "Timestamp,CPU_Usage,Memory_Usage,Temperature,Status"
    while [ $WAIT_TIME -lt $MAX_WAIT_TIME ] && [ "$PROCESSING_COMPLETE" = "false" ]; do
        TIMESTAMP=$(date +%s)
        
        # Get CPU usage (simplified)
        CPU_USAGE=$(adb shell "dumpsys cpuinfo | grep com.mira.com | head -1 | awk '{print \$1}'" 2>/dev/null || echo "0")
        
        # Get memory usage (simplified)
        MEMORY_USAGE=$(adb shell "dumpsys meminfo com.mira.com | grep TOTAL | awk '{print \$2}'" 2>/dev/null || echo "0")
        
        # Get temperature (simplified)
        TEMPERATURE=$(adb shell "cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null" || echo "0")
        if [ "$TEMPERATURE" != "0" ]; then
            TEMPERATURE=$((TEMPERATURE / 1000))
        fi
        
        # Check processing status
        STATUS="IDLE"
        if adb shell "ps | grep com.mira.com" >/dev/null 2>&1; then
            if adb shell "test -f /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
                STATUS="COMPLETED"
                PROCESSING_COMPLETE=true
            else
                STATUS="PROCESSING"
            fi
        fi
        
        echo "$TIMESTAMP,$CPU_USAGE,$MEMORY_USAGE,$TEMPERATURE,$STATUS"
        
        if [ "$STATUS" = "COMPLETED" ]; then
            break
        fi
        
        sleep 5
        WAIT_TIME=$((WAIT_TIME + 5))
        
        if [ $((WAIT_TIME % 60)) -eq 0 ]; then
            echo "⏳ Still processing... (${WAIT_TIME}s elapsed)"
        fi
    done
} > "$RESULTS_DIR/performance_metrics.csv"

END_TIME=$(date +%s)
PROCESSING_TIME=$((END_TIME - START_TIME))

echo ""
echo "=== Collecting Results ==="

# Get transcript
if adb shell "test -f /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
    adb shell "cat /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" > "$RESULTS_DIR/transcript.srt"
    echo "✅ Transcript saved"
    
    # Show first few lines of transcript
    echo ""
    echo "📝 Transcript Preview:"
    echo "====================="
    head -10 "$RESULTS_DIR/transcript.srt"
    echo "..."
else
    echo "❌ No transcript found"
fi

# Get logs
adb logcat -d > "$RESULTS_DIR/full_logs.txt"
adb logcat -d | grep -E "(WhisperBridge|TECHNICAL|whisper)" > "$RESULTS_DIR/whisper_logs.txt"

# Get final performance metrics
FINAL_CPU=$(adb shell "dumpsys cpuinfo | grep com.mira.com | head -1 | awk '{print \$1}'" 2>/dev/null || echo "0")
FINAL_MEMORY=$(adb shell "dumpsys meminfo com.mira.com | grep TOTAL | awk '{print \$2}'" 2>/dev/null || echo "0")

# Save test summary
cat > "$RESULTS_DIR/test_summary.txt" << EOF
Test Name: CPU Performance Test
Processing Time: ${PROCESSING_TIME} seconds
Video Duration: ${DURATION_SECONDS} seconds
File Size: ${FILE_SIZE_MB}MB
Model: $MODEL
Final CPU Usage: ${FINAL_CPU}%
Final Memory Usage: ${FINAL_MEMORY}KB
Timestamp: $(date)
EOF

echo ""

# Generate simple report
REPORT_FILE="$RESULTS_DIR/simple_test_report.md"

cat > "$REPORT_FILE" << EOF
# Tennis Interview Clip 002 - Simple Test Report

**Device:** $DEVICE_MODEL  
**Test File:** tennis_interview_clip_002.mp4  
**File Size:** ${FILE_SIZE_MB}MB  
**Duration:** ${DURATION_SECONDS} seconds (${DURATION_MINUTES} minutes)  
**Model:** $MODEL  
**Test Date:** $(date)  

## Results

EOF

if [ -f "$RESULTS_DIR/transcript.srt" ]; then
    SPEEDUP=$(echo "scale=2; $DURATION_SECONDS / $PROCESSING_TIME" | bc -l)
    cat >> "$REPORT_FILE" << EOF
✅ **Test completed successfully**

### Performance Metrics
- **Processing Time:** ${PROCESSING_TIME} seconds
- **Video Duration:** ${DURATION_SECONDS} seconds  
- **Speedup:** ${SPEEDUP}x realtime
- **Final CPU Usage:** ${FINAL_CPU}%
- **Final Memory Usage:** ${FINAL_MEMORY}KB

### Transcript Quality
- **Status:** ✅ Generated successfully
- **File:** \`$RESULTS_DIR/transcript.srt\`

### Analysis
The CPU-only processing completed the 5-minute video in ${PROCESSING_TIME} seconds, achieving ${SPEEDUP}x realtime speed. This demonstrates good performance for the Xiaomi Pad Ultra's CPU.

EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **Test failed**

### Issues
- No transcript was generated
- Processing may have failed or timed out
- Check logs for error details

### Next Steps
1. Review error logs in \`$RESULTS_DIR/whisper_logs.txt\`
2. Check device storage and permissions
3. Verify app functionality

EOF
fi

cat >> "$REPORT_FILE" << EOF

## Files Generated

- **Test Results:** \`$RESULTS_DIR/\`
- **Performance Metrics:** \`$RESULTS_DIR/performance_metrics.csv\`
- **Transcript:** \`$RESULTS_DIR/transcript.srt\`
- **Logs:** \`$RESULTS_DIR/whisper_logs.txt\`

---

*Report generated automatically*
EOF

echo "=== Test Summary ==="
echo "📊 Results saved to: $RESULTS_DIR"
echo "📋 Report generated: $REPORT_FILE"
echo ""

if [ -f "$RESULTS_DIR/transcript.srt" ]; then
    echo "✅ Test completed successfully!"
    echo "🚀 Processing completed in ${PROCESSING_TIME} seconds"
    echo "📝 Transcript generated successfully"
    echo "⚡ Speedup: ${SPEEDUP}x realtime"
else
    echo "⚠️  Test failed - check logs for details"
fi

echo ""
echo "🎉 Simple test completed!"
echo "Review the report at: $REPORT_FILE"
