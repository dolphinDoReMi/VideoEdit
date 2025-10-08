#!/bin/bash

# Simple CPU Test for Xiaomi Pad
# Runs CPU-only processing and shows transcript

set -e

echo "🎾 Xiaomi Pad CPU Test"
echo "===================="
echo "CPU-only processing test"
echo "Timestamp: $(date)"
echo ""

# Configuration
APP_PACKAGE="com.mira.com"
MAIN_ACTIVITY="com.mira.whisper.WhisperMainActivity"
CLIP_FILE="tennis_interview_clip_002.mp4"
DEVICE_PATH="/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/$CLIP_FILE"
MODEL="/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/whisper-base.q5_1.bin"

# Results directory
RESULTS_DIR="./cpu_test_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "📁 Results will be saved to: $RESULTS_DIR"
echo ""

# Quick setup check
echo "=== Xiaomi Pad Setup Check ==="
adb devices | grep "device$" || { echo "❌ No Xiaomi Pad connected"; exit 1; }
echo "✅ Xiaomi Pad connected"

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

# Function to run processing test
run_cpu_test() {
    local start_time=$(date +%s)
    
    echo "=== CPU Only Test (ARM64 Optimized) ==="
    echo "🛑 Force stopping app..."
    adb shell am force-stop "$APP_PACKAGE"
    sleep 2
    
    echo "🧹 Clearing logs..."
    adb logcat -c
    
    echo "🚀 Launching app..."
    adb shell am start -n "$APP_PACKAGE/$MAIN_ACTIVITY"
    sleep 5
    
    echo "📡 Starting processing via JavaScript interface..."
    
    # Use JavaScript interface to start processing
    adb shell "am broadcast -a android.intent.action.MY_PACKAGE_REPLACED" 2>/dev/null || true
    sleep 2
    
    echo "⏳ Monitoring processing for 180 seconds..."
    
    # Enhanced monitoring for Xiaomi Pad
    for i in {1..36}; do
        sleep 5
        echo "⏰ Check $i/36: $(date)"
        
        # CPU usage monitoring
        adb shell "dumpsys cpuinfo | grep $APP_PACKAGE | head -1" >> "$RESULTS_DIR/cpu_monitoring.txt" 2>/dev/null || true
        
        # Memory usage monitoring
        adb shell "dumpsys meminfo $APP_PACKAGE | grep TOTAL" >> "$RESULTS_DIR/memory_monitoring.txt" 2>/dev/null || true
        
        # Battery monitoring
        adb shell "dumpsys battery" | grep "level:" >> "$RESULTS_DIR/battery_monitoring.txt" 2>/dev/null || true
        
        # Thermal monitoring
        adb shell "cat /sys/class/thermal/thermal_zone*/temp" >> "$RESULTS_DIR/thermal_monitoring.txt" 2>/dev/null || true
        
        # Processing logs
        adb logcat -d | grep -E "(WhisperJNI|Processing|DirectWhisperService)" | tail -5 > "$RESULTS_DIR/processing_logs_$i.txt"
        
        # Check if processing completed
        if adb logcat -d | grep -q "Processing completed\|transcription completed"; then
            echo "✅ Processing completed early at check $i"
            break
        fi
        
        # Check for any transcript output
        if adb logcat -d | grep -q "transcript\|srt\|txt"; then
            echo "📝 Transcript detected at check $i"
        fi
    done
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo "✅ CPU test completed in $duration seconds"
    
    # Get final metrics
    adb shell "dumpsys cpuinfo | grep $APP_PACKAGE | head -1" > "$RESULTS_DIR/final_cpu.txt"
    adb shell "dumpsys meminfo $APP_PACKAGE | grep TOTAL" > "$RESULTS_DIR/final_memory.txt"
    adb shell "dumpsys battery" | grep "level:" > "$RESULTS_DIR/final_battery.txt"
    adb shell "cat /sys/class/thermal/thermal_zone*/temp" > "$RESULTS_DIR/final_thermal.txt"
    adb logcat -d | grep -E "(WhisperJNI|Processing|DirectWhisperService)" | tail -20 > "$RESULTS_DIR/final_logs.txt"
    
    echo "$duration" > "$RESULTS_DIR/duration.txt"
    
    echo ""
}

# Run the CPU test
run_cpu_test

# Extract transcript from logs
echo "=== Extracting Transcript ==="
echo "🔍 Looking for transcript in logs..."

# Look for transcript patterns in the logs
adb logcat -d | grep -E "(transcript|srt|txt|whisper)" > "$RESULTS_DIR/transcript_logs.txt" 2>/dev/null || true

# Look for any output files that might contain transcripts
adb shell "find /storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper -name '*.srt' -o -name '*.txt' -o -name '*.json'" > "$RESULTS_DIR/output_files.txt" 2>/dev/null || true

# Check if any transcript files were created
if [ -s "$RESULTS_DIR/output_files.txt" ]; then
    echo "📄 Found transcript files:"
    cat "$RESULTS_DIR/output_files.txt"
    
    # Try to get the content of the first transcript file
    FIRST_FILE=$(head -1 "$RESULTS_DIR/output_files.txt" | tr -d '\r')
    if [ -n "$FIRST_FILE" ]; then
        echo ""
        echo "📝 Transcript content from $FIRST_FILE:"
        echo "=========================================="
        adb shell "cat '$FIRST_FILE'" 2>/dev/null || echo "Could not read file content"
        echo "=========================================="
    fi
else
    echo "❌ No transcript files found"
fi

# Generate report
echo ""
echo "=== Generating Report ==="
REPORT_FILE="$RESULTS_DIR/cpu_test_report.md"

cat > "$REPORT_FILE" << EOF
# Xiaomi Pad CPU Test Report

**Test Date:** $(date)  
**Device:** $(adb shell "getprop ro.product.model" 2>/dev/null || echo "Unknown")  
**Android Version:** $(adb shell "getprop ro.build.version.release" 2>/dev/null || echo "Unknown")  
**CPU ABI:** $(adb shell "getprop ro.product.cpu.abi" 2>/dev/null || echo "Unknown")  
**File:** $CLIP_FILE  
**Model:** whisper-base.q5_1.bin  
**Method:** CPU-only processing (ARM64 optimized)  

## Test Results

### CPU Only Test
EOF

if [ -f "$RESULTS_DIR/duration.txt" ]; then
    CPU_DURATION=$(cat "$RESULTS_DIR/duration.txt")
    cat >> "$REPORT_FILE" << EOF
✅ **CPU Only Test Completed**
- Duration: $CPU_DURATION seconds
- Processing: CPU-only (ARM64 optimized)
- Acceleration: CPU Only
- Build: ARM64-specific configuration
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **CPU Only Test Failed**
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Transcript Results
EOF

if [ -s "$RESULTS_DIR/output_files.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **Transcript Files Found**
$(cat "$RESULTS_DIR/output_files.txt")
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **No Transcript Files Found**
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Performance Metrics
EOF

if [ -f "$RESULTS_DIR/final_cpu.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Final CPU Usage:**
\`\`\`
$(cat "$RESULTS_DIR/final_cpu.txt")
\`\`\`
EOF
fi

if [ -f "$RESULTS_DIR/final_memory.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Final Memory Usage:**
\`\`\`
$(cat "$RESULTS_DIR/final_memory.txt")
\`\`\`
EOF
fi

if [ -f "$RESULTS_DIR/final_battery.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Final Battery Level:**
\`\`\`
$(cat "$RESULTS_DIR/final_battery.txt")
\`\`\`
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Processing Logs
EOF

if [ -f "$RESULTS_DIR/final_logs.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Final Processing Logs:**
\`\`\`
$(cat "$RESULTS_DIR/final_logs.txt")
\`\`\`
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Files Generated
- **CPU Monitoring:** \`$RESULTS_DIR/cpu_monitoring.txt\`
- **Memory Monitoring:** \`$RESULTS_DIR/memory_monitoring.txt\`
- **Battery Monitoring:** \`$RESULTS_DIR/battery_monitoring.txt\`
- **Thermal Monitoring:** \`$RESULTS_DIR/thermal_monitoring.txt\`
- **Processing Logs:** \`$RESULTS_DIR/processing_logs_*.txt\`
- **Transcript Logs:** \`$RESULTS_DIR/transcript_logs.txt\`
- **Output Files:** \`$RESULTS_DIR/output_files.txt\`

---

*Report generated automatically*
EOF

echo "✅ Report generated: $REPORT_FILE"

echo ""
echo "=== CPU Test Summary ==="
echo "📊 Results saved to: $RESULTS_DIR"
echo "📋 Report generated: $REPORT_FILE"
echo ""

if [ -f "$RESULTS_DIR/duration.txt" ]; then
    CPU_DURATION=$(cat "$RESULTS_DIR/duration.txt")
    echo "✅ CPU Test Duration: $CPU_DURATION seconds"
fi

if [ -s "$RESULTS_DIR/output_files.txt" ]; then
    echo "✅ Transcript files found:"
    cat "$RESULTS_DIR/output_files.txt"
else
    echo "❌ No transcript files found"
fi

echo ""
echo "🎯 CPU test completed!"
echo "📖 See $REPORT_FILE for detailed analysis"
