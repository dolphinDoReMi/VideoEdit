#!/bin/bash

# Fully Automated Processing Script
# No manual intervention required - uses broadcast infrastructure

set -e

echo "🤖 Fully Automated Processing - Tennis Interview Clip 002"
echo "======================================================="
echo "No manual intervention required"
echo "Timestamp: $(date)"
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_002.mp4"
DEVICE_PATH="/sdcard/MiraWhisper/in/$CLIP_FILE"
MODEL="/sdcard/MiraWhisper/models/whisper-base.q5_1.bin"

# Results directory
RESULTS_DIR="./automated_processing_$(date +%Y%m%d_%H%M%S)"
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

# Step 1: Force stop and clear
echo "=== Step 1: Clean Start ==="
echo "🛑 Force stopping app..."
adb shell am force-stop com.mira.com
sleep 2

echo "🧹 Clearing logs..."
adb logcat -c

echo ""

# Step 2: Launch app
echo "=== Step 2: Launch App ==="
echo "🚀 Launching app..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
sleep 5

echo ""

# Step 3: Trigger processing via DirectWhisperService
echo "=== Step 3: Automated Processing ==="
echo "📡 Starting DirectWhisperService for processing..."
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$DEVICE_PATH" \
    --es "model" "$MODEL" \
    --es "threads" "4" \
    --es "lang" "auto" \
    --ez "translate" "false"

echo "✅ DirectWhisperService started"

echo ""

# Step 4: Monitor processing
echo "=== Step 4: Monitor Processing ==="
echo "⏳ Monitoring processing for 120 seconds..."

# Start monitoring in background
(
    for i in {1..24}; do
        sleep 5
        echo "⏰ Check $i/24: $(date)"
        
        # Check if transcript exists
        if adb shell "test -f /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
            echo "✅ Transcript found at check $i!"
            echo "SUCCESS" > "$RESULTS_DIR/status.txt"
            break
        fi
        
        # Check processing logs
        adb logcat -d | grep -E "(WhisperApi|TranscribeWorker|WhisperJNI)" | tail -5 > "$RESULTS_DIR/processing_logs_$i.txt"
        
        # Check CPU usage
        adb shell "dumpsys cpuinfo | grep com.mira.com | head -1" >> "$RESULTS_DIR/cpu_monitoring.txt" 2>/dev/null || true
        
        # Check memory usage
        adb shell "dumpsys meminfo com.mira.com | grep TOTAL" >> "$RESULTS_DIR/memory_monitoring.txt" 2>/dev/null || true
    done
) &

MONITOR_PID=$!

# Wait for monitoring to complete
wait $MONITOR_PID

echo ""

# Step 5: Check results
echo "=== Step 5: Check Results ==="
if [ -f "$RESULTS_DIR/status.txt" ]; then
    echo "🎉 SUCCESS: Processing completed!"
    
    # Get transcript
    adb shell "cat /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" > "$RESULTS_DIR/transcript.srt"
    
    echo "📝 Transcript preview:"
    head -10 "$RESULTS_DIR/transcript.srt"
    echo "..."
    
    TRANSCRIPT_SIZE=$(wc -c < "$RESULTS_DIR/transcript.srt")
    echo "📊 Transcript size: $TRANSCRIPT_SIZE bytes"
    
    PROCESSING_SUCCESS=true
else
    echo "❌ Processing did not complete in 120 seconds"
    PROCESSING_SUCCESS=false
fi

echo ""

# Step 6: Check processing logs
echo "=== Step 6: Check Processing Logs ==="
echo "📊 Checking processing logs..."
adb logcat -d | grep -E "(WhisperApi|TranscribeWorker|WhisperJNI|Processing)" | tail -20 > "$RESULTS_DIR/final_processing_logs.txt"

if [ -s "$RESULTS_DIR/final_processing_logs.txt" ]; then
    echo "✅ Processing logs found:"
    cat "$RESULTS_DIR/final_processing_logs.txt"
else
    echo "⚠️  No processing logs found"
fi

echo ""

# Step 7: Check performance
echo "=== Step 7: Check Performance ==="
echo "📊 Final performance metrics:"
echo "CPU Usage:"
adb shell "dumpsys cpuinfo | grep com.mira.com | head -1" > "$RESULTS_DIR/final_cpu_usage.txt"
cat "$RESULTS_DIR/final_cpu_usage.txt"

echo "Memory Usage:"
adb shell "dumpsys meminfo com.mira.com | grep TOTAL" > "$RESULTS_DIR/final_memory_usage.txt"
cat "$RESULTS_DIR/final_memory_usage.txt"

echo ""

# Step 8: Check WorkManager status
echo "=== Step 8: Check WorkManager Status ==="
echo "🔍 Checking WorkManager jobs..."
adb shell "dumpsys jobscheduler | grep com.mira.com" > "$RESULTS_DIR/workmanager_jobs.txt" 2>/dev/null || echo "No WorkManager jobs found"

if [ -s "$RESULTS_DIR/workmanager_jobs.txt" ]; then
    echo "✅ WorkManager jobs found:"
    cat "$RESULTS_DIR/workmanager_jobs.txt"
else
    echo "⚠️  No WorkManager jobs found"
fi

echo ""

# Generate comprehensive report
echo "=== Generating Report ==="
REPORT_FILE="$RESULTS_DIR/automated_processing_report.md"

cat > "$REPORT_FILE" << EOF
# Fully Automated Processing Report

**Test Date:** $(date)  
**File:** tennis_interview_clip_002.mp4  
**Model:** whisper-base.q5_1.bin  
**Method:** Automated Broadcast Processing  
**Duration:** 120 seconds monitoring  

## Results

### Processing Status
EOF

if [ "$PROCESSING_SUCCESS" = true ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **SUCCESS: Automated processing completed**
- Transcript generated successfully
- File: \`$RESULTS_DIR/transcript.srt\`
- Size: $(wc -c < "$RESULTS_DIR/transcript.srt") bytes
- Processing time: < 120 seconds
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **FAILED: Automated processing did not complete**
- No transcript generated within 120 seconds
- Check processing logs for errors
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Processing Logs
EOF

if [ -s "$RESULTS_DIR/final_processing_logs.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Processing Activity:**
\`\`\`
$(cat "$RESULTS_DIR/final_processing_logs.txt")
\`\`\`
EOF
else
    cat >> "$REPORT_FILE" << EOF
**No processing logs found**
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Performance Metrics
EOF

if [ -f "$RESULTS_DIR/final_cpu_usage.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Final CPU Usage:**
\`\`\`
$(cat "$RESULTS_DIR/final_cpu_usage.txt")
\`\`\`
EOF
fi

if [ -f "$RESULTS_DIR/final_memory_usage.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Final Memory Usage:**
\`\`\`
$(cat "$RESULTS_DIR/final_memory_usage.txt")
\`\`\`
EOF
fi

cat >> "$REPORT_FILE" << EOF

### WorkManager Status
EOF

if [ -s "$RESULTS_DIR/workmanager_jobs.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**WorkManager Jobs:**
\`\`\`
$(cat "$RESULTS_DIR/workmanager_jobs.txt")
\`\`\`
EOF
else
    cat >> "$REPORT_FILE" << EOF
**No WorkManager jobs found**
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Analysis

**Status:** $(if [ "$PROCESSING_SUCCESS" = true ]; then echo "✅ SUCCESS"; else echo "❌ FAILED"; fi)

**Automated Processing:**
- **Method:** Broadcast to WhisperReceiver
- **Infrastructure:** Existing WorkManager system
- **Result:** $(if [ "$PROCESSING_SUCCESS" = true ]; then echo "Transcript generated successfully"; else echo "Processing failed or timed out"; fi)

**Next Steps:**
1. **If successful:** Proceed with CPU vs Vulkan ablation testing
2. **If failed:** Debug broadcast/WorkManager chain
3. **For ablation:** Use this automated approach for both CPU and Vulkan tests

## Files Generated
- **Transcript:** \`$RESULTS_DIR/transcript.srt\`
- **Processing Logs:** \`$RESULTS_DIR/final_processing_logs.txt\`
- **CPU Usage:** \`$RESULTS_DIR/final_cpu_usage.txt\`
- **Memory Usage:** \`$RESULTS_DIR/final_memory_usage.txt\`
- **WorkManager Jobs:** \`$RESULTS_DIR/workmanager_jobs.txt\`
- **CPU Monitoring:** \`$RESULTS_DIR/cpu_monitoring.txt\`
- **Memory Monitoring:** \`$RESULTS_DIR/memory_monitoring.txt\`

---

*Report generated automatically*
EOF

echo "✅ Report generated: $REPORT_FILE"

echo ""
echo "=== Summary ==="
echo "📊 Results saved to: $RESULTS_DIR"
echo "📋 Report generated: $REPORT_FILE"
echo ""

if [ "$PROCESSING_SUCCESS" = true ]; then
    echo "🎉 SUCCESS: Fully automated processing working!"
    echo "✅ Transcript generated successfully"
    echo "📝 Size: $(wc -c < "$RESULTS_DIR/transcript.srt") bytes"
    echo ""
    echo "🎯 Ready for CPU vs Vulkan ablation testing!"
else
    echo "❌ Automated processing failed"
    echo "📋 Check report for details: $REPORT_FILE"
fi

echo ""
echo "🤖 Fully automated processing completed!"
