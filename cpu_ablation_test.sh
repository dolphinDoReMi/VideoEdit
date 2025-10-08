#!/bin/bash

# Simple CPU-Only Ablation Test
# Tests CPU performance without Vulkan acceleration

set -e

echo "🎾 Simple CPU-Only Ablation Test"
echo "==============================="
echo "Testing CPU performance baseline"
echo "Timestamp: $(date)"
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_002.mp4"
DEVICE_PATH="/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/$CLIP_FILE"
MODEL="/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin"

# Results directory
RESULTS_DIR="./cpu_ablation_test_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "📁 Results will be saved to: $RESULTS_DIR"
echo ""

# Quick setup check
echo "=== Quick Setup Check ==="
adb devices | grep "device$" || { echo "❌ No device connected"; exit 1; }
echo "✅ Device connected"

# Check if file exists
if ! adb shell "test -f $DEVICE_PATH" 2>/dev/null; then
    echo "❌ File not found: $DEVICE_PATH"
    exit 1
else
    echo "✅ File found: $DEVICE_PATH"
fi

# Check model
if ! adb shell "test -f $MODEL" 2>/dev/null; then
    echo "❌ Model not found: $MODEL"
    exit 1
else
    echo "✅ Model found: $MODEL"
fi

echo ""

# Test 1: Manual UI Processing (CPU Only)
echo "=== Test 1: Manual UI Processing (CPU Only) ==="
echo "Clearing logs..."
adb logcat -c

echo "🚀 Launching app for CPU-only processing..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity

echo ""
echo "📋 MANUAL STEPS REQUIRED:"
echo "1. Tap 'Select Files' button"
echo "2. Navigate to /storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/"
echo "3. Select tennis_interview_clip_002.mp4"
echo "4. Tap 'Start Processing'"
echo "5. Wait for processing to complete"
echo ""
echo "⏱️  Start timing now and note the processing time!"
echo ""
echo "Press Enter when processing is complete..."

# Wait for user input
read -p "Press Enter when processing is complete..."

# Record end time
END_TIME=$(date +%s)
echo "✅ Processing completed at: $(date)"

echo ""

# Check results
echo "=== Checking Results ==="
if adb shell "test -f /storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
    echo "✅ Transcript found!"
    adb shell "cat /storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/out/tennis_interview_clip_002.srt" > "$RESULTS_DIR/cpu_transcript.srt"
    
    echo "📝 Transcript preview:"
    head -10 "$RESULTS_DIR/cpu_transcript.srt"
    echo "..."
    
    TRANSCRIPT_SIZE=$(wc -c < "$RESULTS_DIR/cpu_transcript.srt")
    echo "📊 Transcript size: $TRANSCRIPT_SIZE bytes"
    CPU_SUCCESS=true
else
    echo "❌ No transcript found"
    CPU_SUCCESS=false
fi

echo ""

# Check processing logs
echo "=== Checking Processing Logs ==="
echo "📊 Checking CPU processing logs..."
adb logcat -d | grep -E "(WhisperApi|TranscribeWorker|WhisperJNI|Processing)" | tail -20 > "$RESULTS_DIR/cpu_processing_logs.txt"

if [ -s "$RESULTS_DIR/cpu_processing_logs.txt" ]; then
    echo "✅ Processing logs found:"
    cat "$RESULTS_DIR/cpu_processing_logs.txt"
else
    echo "⚠️  No processing logs found"
fi

echo ""

# Check performance
echo "=== Checking Performance ==="
echo "📊 Current performance metrics:"
echo "CPU Usage:"
adb shell "dumpsys cpuinfo | grep com.mira.com | head -1" > "$RESULTS_DIR/cpu_usage.txt"
cat "$RESULTS_DIR/cpu_usage.txt"

echo "Memory Usage:"
adb shell "dumpsys meminfo com.mira.com | grep TOTAL" > "$RESULTS_DIR/memory_usage.txt"
cat "$RESULTS_DIR/memory_usage.txt"

echo ""

# Generate report
echo "=== Generating Report ==="
REPORT_FILE="$RESULTS_DIR/cpu_ablation_report.md"

cat > "$REPORT_FILE" << EOF
# CPU-Only Ablation Test Report

**Test Date:** $(date)  
**File:** tennis_interview_clip_002.mp4  
**Model:** small.en-q5_1.bin  
**Acceleration:** CPU Only (No Vulkan)  
**Method:** Manual UI Processing  

## Results

### Transcript Generation
EOF

if [ "$CPU_SUCCESS" = true ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **SUCCESS: CPU-only transcript generated**
- File: \`$RESULTS_DIR/cpu_transcript.srt\`
- Size: $(wc -c < "$RESULTS_DIR/cpu_transcript.srt") bytes
- CPU-only processing working correctly
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **FAILED: No CPU-only transcript generated**
- CPU-only processing did not complete successfully
- Check app logs for errors
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Performance Metrics
EOF

if [ -f "$RESULTS_DIR/cpu_usage.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**CPU Usage:**
\`\`\`
$(cat "$RESULTS_DIR/cpu_usage.txt")
\`\`\`
EOF
fi

if [ -f "$RESULTS_DIR/memory_usage.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Memory Usage:**
\`\`\`
$(cat "$RESULTS_DIR/memory_usage.txt")
\`\`\`
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Processing Logs
EOF

if [ -s "$RESULTS_DIR/cpu_processing_logs.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**CPU Processing Activity:**
\`\`\`
$(cat "$RESULTS_DIR/cpu_processing_logs.txt")
\`\`\`
EOF
else
    cat >> "$REPORT_FILE" << EOF
**No processing logs found**
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Analysis

**Status:** $(if [ "$CPU_SUCCESS" = true ]; then echo "✅ SUCCESS"; else echo "❌ FAILED"; fi)

**CPU-Only Performance:**
- **Processing Method:** Manual UI
- **Acceleration:** CPU Only
- **Result:** $(if [ "$CPU_SUCCESS" = true ]; then echo "Transcript generated successfully"; else echo "Processing failed"; fi)

**Next Steps:**
1. **If successful:** This establishes CPU-only baseline
2. **For Vulkan comparison:** Re-enable Vulkan and repeat test
3. **For ablation:** Compare CPU vs Vulkan performance

## Files Generated
- **CPU Transcript:** \`$RESULTS_DIR/cpu_transcript.srt\`
- **CPU Processing Logs:** \`$RESULTS_DIR/cpu_processing_logs.txt\`
- **CPU Usage:** \`$RESULTS_DIR/cpu_usage.txt\`
- **Memory Usage:** \`$RESULTS_DIR/memory_usage.txt\`

---

*Report generated automatically*
EOF

echo "✅ Report generated: $REPORT_FILE"

echo ""
echo "=== Summary ==="
echo "📊 Results saved to: $RESULTS_DIR"
echo "📋 Report generated: $REPORT_FILE"
echo ""

if [ "$CPU_SUCCESS" = true ]; then
    echo "🎉 SUCCESS: CPU-only processing working!"
    echo "✅ Transcript generated successfully"
    echo "📝 Size: $(wc -c < "$RESULTS_DIR/cpu_transcript.srt") bytes"
    echo ""
    echo "🎯 CPU baseline established!"
    echo "📋 Next: Re-enable Vulkan and repeat for comparison"
else
    echo "❌ CPU-only processing failed"
    echo "📋 Check report for details: $REPORT_FILE"
fi

echo ""
echo "🎯 CPU-only ablation test completed!"
