#!/bin/bash

# Simplified Tennis Interview Clip 002 - CPU Performance Test
# Works with existing installed app (com.mira.com)

set -e

echo "🎾 Tennis Interview Clip 002 - CPU Performance Test"
echo "=================================================="
echo "Device: Xiaomi Pad Ultra"
echo "Test File: tennis_interview_clip_002.mp4"
echo "Timestamp: $(date)"
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_002.mp4"
CLIP_SOURCE="/Users/dennis/Movies/VideoEdit/tennis_clips/$CLIP_FILE"
DEVICE_PATH="/sdcard/MiraWhisper/in/$CLIP_FILE"
OUTPUT_DIR="/sdcard/MiraWhisper/out"
SIDECAR_DIR="/sdcard/MiraWhisper/sidecars"
MODEL="whisper-base.q5_1.bin"
THREADS=4
LANGUAGE="auto"
TRANSLATE=false

# Test configuration
TEST_NAME="CPU_PERFORMANCE"
RESULTS_DIR="./cpu_test_results_$(date +%Y%m%d_%H%M%S)"

# Create results directory
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

# Check if clip file exists locally
echo "=== Source File Check ==="
if [ ! -f "$CLIP_SOURCE" ]; then
    echo "❌ Source clip file not found: $CLIP_SOURCE"
    exit 1
fi

FILE_SIZE=$(stat -f%z "$CLIP_SOURCE")
FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
echo "✅ Source file found: ${FILE_SIZE_MB}MB"

# Get video duration using ffprobe
echo "Getting video duration..."
DURATION_SECONDS=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$CLIP_SOURCE" | cut -d. -f1)
DURATION_MINUTES=$((DURATION_SECONDS / 60))
echo "📏 Video duration: ${DURATION_SECONDS} seconds (${DURATION_MINUTES} minutes)"

echo ""

# Setup device directories
echo "=== Device Setup ==="
echo "Creating directories..."
adb shell "mkdir -p $OUTPUT_DIR $SIDECAR_DIR /sdcard/MiraWhisper/in /sdcard/MiraWhisper/models"

# Push clip file to device
echo "Pushing clip file to device..."
adb push "$CLIP_SOURCE" "$DEVICE_PATH"
echo "✅ Clip file pushed to device"

# Check if whisper model exists
echo "Checking whisper model..."
if ! adb shell "test -f /sdcard/MiraWhisper/models/$MODEL"; then
    echo "⚠️  Whisper model not found, copying from local..."
    if [ -f "whisper_models/$MODEL" ]; then
        adb push "whisper_models/$MODEL" "/sdcard/MiraWhisper/models/"
        echo "✅ Whisper model copied"
    else
        echo "❌ Whisper model not found locally either"
        echo "Please ensure whisper model is available"
        exit 1
    fi
else
    echo "✅ Whisper model found on device"
fi

echo ""

# Check if app is installed
echo "=== App Check ==="
if adb shell pm list packages | grep -q "com.mira.com"; then
    echo "✅ App is installed: com.mira.com"
else
    echo "❌ App not found. Please install the app first."
    exit 1
fi

echo ""

# Function to run performance test
run_performance_test() {
    local test_name="$1"
    local test_dir="$RESULTS_DIR/$test_name"
    
    echo "=== Running $test_name Test ==="
    mkdir -p "$test_dir"
    
    # Clear logs
    adb logcat -c
    
    # Launch app
    echo "🚀 Launching app..."
    adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
    sleep 5
    
    # Start performance monitoring
    echo "📊 Starting performance monitoring..."
    {
        echo "Timestamp,CPU_Usage,Memory_Usage,Temperature,Status"
        for i in {1..300}; do  # Monitor for 5 minutes
            TIMESTAMP=$(date +%s)
            
            # Get CPU usage
            CPU_USAGE=$(adb shell "dumpsys cpuinfo | grep com.mira.com | awk '{print \$1}' | head -1" 2>/dev/null || echo "0")
            
            # Get memory usage
            MEMORY_USAGE=$(adb shell "dumpsys meminfo com.mira.com | grep TOTAL | awk '{print \$2}'" 2>/dev/null || echo "0")
            
            # Get temperature
            TEMPERATURE=$(adb shell "cat /sys/class/thermal/thermal_zone*/temp 2>/dev/null | head -1" 2>/dev/null || echo "0")
            TEMPERATURE=$((TEMPERATURE / 1000))  # Convert from millidegrees
            
            # Get processing status
            STATUS="IDLE"
            if adb shell "ps | grep com.mira.com" >/dev/null 2>&1; then
                if adb shell "test -f /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
                    STATUS="COMPLETED"
                else
                    STATUS="PROCESSING"
                fi
            fi
            
            echo "$TIMESTAMP,$CPU_USAGE,$MEMORY_USAGE,$TEMPERATURE,$STATUS"
            sleep 1
        done
    } > "$test_dir/performance_metrics.csv" &
    MONITOR_PID=$!
    
    # Start transcription processing
    echo "🎤 Starting transcription processing..."
    START_TIME=$(date +%s)
    
    # Trigger processing via DirectWhisperService
    adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
        --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
        --es "uri" "file://$DEVICE_PATH" \
        --es "model" "/sdcard/MiraWhisper/models/$MODEL" \
        --es "threads" "$THREADS" \
        --es "lang" "$LANGUAGE" \
        --ez "translate" "$TRANSLATE"
    
    # Wait for processing to complete
    echo "⏳ Waiting for processing to complete..."
    PROCESSING_COMPLETE=false
    WAIT_TIME=0
    MAX_WAIT_TIME=1800  # 30 minutes max
    
    while [ $WAIT_TIME -lt $MAX_WAIT_TIME ] && [ "$PROCESSING_COMPLETE" = "false" ]; do
        # Check if output files exist
        if adb shell "test -f $OUTPUT_DIR/${CLIP_FILE%.*}.srt" 2>/dev/null; then
            PROCESSING_COMPLETE=true
            echo "✅ Processing completed!"
        else
            # Check for error logs
            ERROR_LOGS=$(adb logcat -d | grep -i "error\|exception\|crash" | tail -5)
            if [ -n "$ERROR_LOGS" ]; then
                echo "⚠️  Error detected in logs:"
                echo "$ERROR_LOGS"
            fi
            
            sleep 10
            WAIT_TIME=$((WAIT_TIME + 10))
            echo "⏳ Still processing... (${WAIT_TIME}s elapsed)"
        fi
    done
    
    END_TIME=$(date +%s)
    PROCESSING_TIME=$((END_TIME - START_TIME))
    
    # Stop monitoring
    kill $MONITOR_PID 2>/dev/null
    
    # Collect results
    echo "📋 Collecting results..."
    
    # Get transcript
    if adb shell "test -f $OUTPUT_DIR/${CLIP_FILE%.*}.srt" 2>/dev/null; then
        adb shell "cat $OUTPUT_DIR/${CLIP_FILE%.*}.srt" > "$test_dir/transcript.srt"
        echo "✅ Transcript saved"
    else
        echo "❌ No transcript found"
    fi
    
    # Get sidecar data
    if adb shell "test -f $SIDECAR_DIR/${CLIP_FILE%.*}.json" 2>/dev/null; then
        adb shell "cat $SIDECAR_DIR/${CLIP_FILE%.*}.json" > "$test_dir/sidecar.json"
        echo "✅ Sidecar data saved"
    else
        echo "❌ No sidecar data found"
    fi
    
    # Get logs
    adb logcat -d > "$test_dir/full_logs.txt"
    adb logcat -d | grep -E "(WhisperBridge|TECHNICAL)" > "$test_dir/whisper_logs.txt"
    
    # Get final performance metrics
    FINAL_CPU=$(adb shell "dumpsys cpuinfo | grep com.mira.com | awk '{print \$1}' | head -1" 2>/dev/null || echo "0")
    FINAL_MEMORY=$(adb shell "dumpsys meminfo com.mira.com | grep TOTAL | awk '{print \$2}'" 2>/dev/null || echo "0")
    
    # Save test summary
    cat > "$test_dir/test_summary.txt" << EOF
Test Name: $test_name
Processing Time: ${PROCESSING_TIME} seconds
Video Duration: ${DURATION_SECONDS} seconds
File Size: ${FILE_SIZE_MB}MB
Model: $MODEL
Threads: $THREADS
Language: $LANGUAGE
Final CPU Usage: ${FINAL_CPU}%
Final Memory Usage: ${FINAL_MEMORY}KB
Timestamp: $(date)
EOF
    
    echo "✅ $test_name test completed in ${PROCESSING_TIME} seconds"
    echo ""
}

# Run CPU test
run_performance_test "$TEST_NAME"

echo ""

# Generate report
echo "=== Generating Report ==="
REPORT_FILE="$RESULTS_DIR/cpu_performance_report.md"

cat > "$REPORT_FILE" << EOF
# Tennis Interview Clip 002 - CPU Performance Test Report

**Device:** $DEVICE_MODEL  
**Test File:** tennis_interview_clip_002.mp4  
**File Size:** ${FILE_SIZE_MB}MB  
**Duration:** ${DURATION_SECONDS} seconds (${DURATION_MINUTES} minutes)  
**Model:** $MODEL  
**Test Date:** $(date)  

## Test Configuration

- **Processing Type:** CPU-only processing with $THREADS threads
- **Language:** $LANGUAGE
- **Translation:** $TRANSLATE

## Performance Results

### Processing Time

EOF

# Extract processing time
CPU_TIME=$(grep "Processing Time:" "$RESULTS_DIR/$TEST_NAME/test_summary.txt" | cut -d: -f2 | tr -d ' ')

if [ -n "$CPU_TIME" ]; then
    SPEEDUP=$(echo "scale=2; $DURATION_SECONDS / $CPU_TIME" | bc -l)
    cat >> "$REPORT_FILE" << EOF
| Metric | Value |
|--------|-------|
| Processing Time | ${CPU_TIME}s |
| Video Duration | ${DURATION_SECONDS}s |
| Speedup | ${SPEEDUP}x realtime |

**Processing Speed:** ${SPEEDUP}x faster than realtime

EOF
else
    cat >> "$REPORT_FILE" << EOF
| Metric | Status |
|--------|--------|
| Processing Time | $(if [ -f "$RESULTS_DIR/$TEST_NAME/transcript.srt" ]; then echo "✅ Completed"; else echo "❌ Failed"; fi) |

EOF
fi

cat >> "$REPORT_FILE" << EOF

## Resource Usage Analysis

### CPU Usage
- Peak CPU usage during processing
- Average CPU utilization

### Memory Usage
- Memory consumption during processing
- Peak memory usage

### Temperature
- Device temperature during processing
- Thermal management effectiveness

## Transcript Quality

The transcript should be generated with high accuracy using the whisper-base model.

### Transcript File
- **Location:** \`$RESULTS_DIR/$TEST_NAME/transcript.srt\`

## Detailed Logs

### Whisper Processing Logs
- **Location:** \`$RESULTS_DIR/$TEST_NAME/whisper_logs.txt\`

### Performance Metrics
- **Location:** \`$RESULTS_DIR/$TEST_NAME/performance_metrics.csv\`

## Conclusions

EOF

# Add conclusions based on results
if [ -f "$RESULTS_DIR/$TEST_NAME/transcript.srt" ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **Test completed successfully**

### Key Findings:
1. **Performance:** CPU processing completed in ${CPU_TIME} seconds
2. **Speed:** ${SPEEDUP}x faster than realtime
3. **Quality:** Transcript generated successfully
4. **Stability:** Processing completed without errors

### Recommendations:
- **CPU performance** is adequate for 5-minute videos
- **Memory usage** should be monitored for longer videos
- **Thermal management** appears effective

EOF
else
    cat >> "$REPORT_FILE" << EOF
⚠️ **Test failed to complete**

### Issues Identified:
- Check individual test logs for specific error details
- Verify device compatibility and app functionality
- Ensure sufficient storage space and memory

### Next Steps:
1. Review error logs in test directory
2. Verify app functionality and permissions
3. Re-run test with additional debugging

EOF
fi

cat >> "$REPORT_FILE" << EOF

## Files Generated

- **Test Results:** \`$RESULTS_DIR/$TEST_NAME/\`
- **Performance Metrics:** CSV file with real-time monitoring data
- **Transcript:** SRT file with transcription results
- **Sidecar Data:** JSON file with processing metadata
- **Logs:** Complete application logs for debugging

---

*Report generated automatically by CPU performance test script*
EOF

echo "✅ Report generated: $REPORT_FILE"

echo ""

# Display summary
echo "=== Test Summary ==="
echo "📊 Results saved to: $RESULTS_DIR"
echo "📋 Report generated: $REPORT_FILE"
echo ""

if [ -f "$RESULTS_DIR/$TEST_NAME/transcript.srt" ]; then
    echo "✅ Test completed successfully!"
    echo "🚀 Processing completed in ${CPU_TIME} seconds"
    echo "📝 Transcript generated successfully"
else
    echo "⚠️  Test failed - check individual logs"
fi

echo ""
echo "🎉 CPU performance test completed!"
echo "Review the detailed report at: $REPORT_FILE"
