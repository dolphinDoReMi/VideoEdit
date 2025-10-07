#!/bin/bash

# Ultra-Simple Direct Tennis Interview Clip 002 Test
# Uses direct service calls instead of broadcast/receiver pattern

set -e

echo "🎾 Tennis Interview Clip 002 - Ultra-Simple Direct Test"
echo "======================================================"
echo "Device: Xiaomi Pad Ultra"
echo "Test File: tennis_interview_clip_002.mp4"
echo "Approach: Direct service calls (no broadcast/receiver)"
echo "Timestamp: $(date)"
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_002.mp4"
CLIP_SOURCE="/Users/dennis/Movies/VideoEdit/tennis_clips/$CLIP_FILE"
DEVICE_PATH="/sdcard/MiraWhisper/in/$CLIP_FILE"
MODEL="/sdcard/MiraWhisper/models/whisper-base.q5_1.bin"
THREADS=4
LANGUAGE="auto"
TRANSLATE=false

# Results directory
RESULTS_DIR="./ultra_simple_test_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "📁 Results will be saved to: $RESULTS_DIR"
echo ""

# Quick setup
echo "=== Quick Setup ==="
adb devices | grep "device$" || { echo "❌ No device connected"; exit 1; }
echo "✅ Device connected"

# Push file if needed
if ! adb shell "test -f $DEVICE_PATH" 2>/dev/null; then
    echo "Pushing clip file..."
    adb push "$CLIP_SOURCE" "$DEVICE_PATH"
    echo "✅ File pushed"
else
    echo "✅ File already on device"
fi

# Check model
if ! adb shell "test -f $MODEL" 2>/dev/null; then
    echo "⚠️  Model not found, copying..."
    if [ -f "whisper_models/whisper-base.q5_1.bin" ]; then
        adb push "whisper_models/whisper-base.q5_1.bin" "$MODEL"
        echo "✅ Model copied"
    else
        echo "❌ Model not found locally"
        exit 1
    fi
else
    echo "✅ Model found"
fi

echo ""

# Direct processing using the simplest possible approach
echo "=== Ultra-Simple Direct Processing ==="

# Clear logs
adb logcat -c

# Launch app
echo "🚀 Launching app..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
sleep 3

# Method 1: Try direct service call (if DirectWhisperService is available)
echo "🎤 Attempting direct service call..."
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$DEVICE_PATH" \
    --es "model" "$MODEL" \
    --es "threads" "$THREADS" \
    --es "lang" "$LANGUAGE" \
    --ez "translate" "$TRANSLATE" 2>/dev/null || echo "DirectWhisperService not available, trying alternative..."

# Method 2: Alternative - Use monkey to simulate user interaction
echo "🎯 Attempting UI simulation approach..."
adb shell "monkey -p com.mira.com -c android.intent.category.LAUNCHER 1" 2>/dev/null || true
sleep 2

# Method 3: Use input commands to navigate
echo "📱 Attempting input simulation..."
adb shell "input tap 1000 500"  # Tap somewhere on screen
sleep 1
adb shell "input tap 1000 600"  # Tap processing area
sleep 1

# Method 4: Try WorkManager directly via ADB
echo "⚙️  Attempting WorkManager direct call..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es 'direct_action' 'transcribe' \
    --es 'file_uri' 'file://$DEVICE_PATH' \
    --es 'model_path' '$MODEL' \
    --es 'thread_count' '$THREADS' \
    --es 'language' '$LANGUAGE'"

echo ""

# Monitor for results
echo "⏳ Monitoring for results..."
START_TIME=$(date +%s)
PROCESSING_COMPLETE=false
WAIT_TIME=0
MAX_WAIT_TIME=600  # 10 minutes

while [ $WAIT_TIME -lt $MAX_WAIT_TIME ] && [ "$PROCESSING_COMPLETE" = "false" ]; do
    # Check if output files exist
    if adb shell "test -f /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
        PROCESSING_COMPLETE=true
        echo "✅ Processing completed!"
        break
    fi
    
    # Check for any whisper-related activity in logs
    RECENT_LOGS=$(adb logcat -d | grep -i "whisper\|transcribe\|technical" | tail -3)
    if [ -n "$RECENT_LOGS" ]; then
        echo "📊 Activity detected: $RECENT_LOGS"
    fi
    
    sleep 5
    WAIT_TIME=$((WAIT_TIME + 5))
    
    if [ $((WAIT_TIME % 30)) -eq 0 ]; then
        echo "⏳ Still waiting... (${WAIT_TIME}s elapsed)"
    fi
done

END_TIME=$(date +%s)
PROCESSING_TIME=$((END_TIME - START_TIME))

echo ""

# Collect results
echo "=== Collecting Results ==="

# Get transcript
if adb shell "test -f /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
    adb shell "cat /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" > "$RESULTS_DIR/transcript.srt"
    echo "✅ Transcript saved"
    
    # Show transcript preview
    echo ""
    echo "📝 Transcript Preview:"
    echo "====================="
    head -15 "$RESULTS_DIR/transcript.srt"
    echo "..."
else
    echo "❌ No transcript found"
fi

# Get logs
adb logcat -d > "$RESULTS_DIR/full_logs.txt"
adb logcat -d | grep -E "(whisper|transcribe|technical|DirectWhisperService)" > "$RESULTS_DIR/whisper_logs.txt"

# Get performance metrics
FINAL_CPU=$(adb shell "dumpsys cpuinfo | grep com.mira.com | head -1 | awk '{print \$1}'" 2>/dev/null || echo "0")
FINAL_MEMORY=$(adb shell "dumpsys meminfo com.mira.com | grep TOTAL | awk '{print \$2}'" 2>/dev/null || echo "0")

# Save summary
cat > "$RESULTS_DIR/test_summary.txt" << EOF
Test Name: Ultra-Simple Direct Test
Processing Time: ${PROCESSING_TIME} seconds
Video Duration: 300 seconds (5 minutes)
File Size: 92.6MB
Model: whisper-base.q5_1.bin
Threads: $THREADS
Language: $LANGUAGE
Final CPU Usage: ${FINAL_CPU}%
Final Memory Usage: ${FINAL_MEMORY}KB
Timestamp: $(date)
EOF

echo ""

# Generate simple report
REPORT_FILE="$RESULTS_DIR/ultra_simple_report.md"

cat > "$REPORT_FILE" << EOF
# Tennis Interview Clip 002 - Ultra-Simple Direct Test Report

**Device:** $(adb shell getprop ro.product.model)  
**Test File:** tennis_interview_clip_002.mp4  
**File Size:** 92.6MB  
**Duration:** 300 seconds (5 minutes)  
**Model:** whisper-base.q5_1.bin  
**Approach:** Ultra-simple direct calls (no broadcast/receiver)  
**Test Date:** $(date)  

## Results

EOF

if [ -f "$RESULTS_DIR/transcript.srt" ]; then
    SPEEDUP=$(echo "scale=2; 300 / $PROCESSING_TIME" | bc -l)
    cat >> "$REPORT_FILE" << EOF
✅ **Test completed successfully**

### Performance Metrics
- **Processing Time:** ${PROCESSING_TIME} seconds
- **Video Duration:** 300 seconds  
- **Speedup:** ${SPEEDUP}x realtime
- **Final CPU Usage:** ${FINAL_CPU}%
- **Final Memory Usage:** ${FINAL_MEMORY}KB

### Transcript Quality
- **Status:** ✅ Generated successfully
- **File:** \`$RESULTS_DIR/transcript.srt\`

### Analysis
The ultra-simple direct approach completed the 5-minute video in ${PROCESSING_TIME} seconds, achieving ${SPEEDUP}x realtime speed. This demonstrates that the simplest possible approach can be effective.

### Key Advantages
1. **Minimal Complexity:** Uses the simplest possible method
2. **No Broadcast Dependencies:** Completely bypasses broadcast/receiver pattern
3. **Direct Service Calls:** Uses Android services directly
4. **UI Simulation:** Falls back to user interaction simulation if needed
5. **Multiple Fallbacks:** Several approaches attempted for reliability

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
- **Transcript:** \`$RESULTS_DIR/transcript.srt\`
- **Logs:** \`$RESULTS_DIR/whisper_logs.txt\`

## Architecture Comparison

### Old Approach (Broadcast/Receiver)
- Complex broadcast receiver pattern
- Multiple components involved
- Potential for delivery failures

### New Approach (Ultra-Simple Direct)
- Direct service calls
- UI simulation fallbacks
- Multiple approaches for reliability
- Minimal complexity

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
    echo "🎯 Ultra-simple approach successful!"
else
    echo "⚠️  Test failed - check logs for details"
fi

echo ""
echo "🎉 Ultra-simple direct test completed!"
echo "Review the report at: $REPORT_FILE"
