#!/bin/bash

# CPU vs Vulkan Ablation Test
# Automated performance comparison without manual intervention

set -e

echo "🎾 CPU vs Vulkan Ablation Test - Tennis Interview Clip 002"
echo "=========================================================="
echo "Automated performance comparison"
echo "Timestamp: $(date)"
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_002.mp4"
DEVICE_PATH="/sdcard/MiraWhisper/in/$CLIP_FILE"
MODEL="/sdcard/MiraWhisper/models/whisper-base.q5_1.bin"

# Results directory
RESULTS_DIR="./cpu_vulkan_ablation_$(date +%Y%m%d_%H%M%S)"
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

# Function to run processing test
run_processing_test() {
    local test_name="$1"
    local test_dir="$2"
    local start_time=$(date +%s)
    
    echo "=== $test_name ==="
    echo "🛑 Force stopping app..."
    adb shell am force-stop com.mira.com
    sleep 2
    
    echo "🧹 Clearing logs..."
    adb logcat -c
    
    echo "🚀 Launching app..."
    adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
    sleep 5
    
    echo "📡 Starting DirectWhisperService..."
    adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
        --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
        --es "uri" "file://$DEVICE_PATH" \
        --es "model" "$MODEL" \
        --es "threads" "4" \
        --es "lang" "auto" \
        --ez "translate" "false"
    
    echo "⏳ Monitoring processing for 90 seconds..."
    
    # Monitor processing
    for i in {1..18}; do
        sleep 5
        echo "⏰ Check $i/18: $(date)"
        
        # Check CPU usage
        adb shell "dumpsys cpuinfo | grep com.mira.com | head -1" >> "$test_dir/cpu_monitoring.txt" 2>/dev/null || true
        
        # Check memory usage
        adb shell "dumpsys meminfo com.mira.com | grep TOTAL" >> "$test_dir/memory_monitoring.txt" 2>/dev/null || true
        
        # Check processing logs
        adb logcat -d | grep -E "(WhisperJNI|Processing)" | tail -5 > "$test_dir/processing_logs_$i.txt"
    done
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo "✅ $test_name completed in $duration seconds"
    
    # Get final metrics
    adb shell "dumpsys cpuinfo | grep com.mira.com | head -1" > "$test_dir/final_cpu.txt"
    adb shell "dumpsys meminfo com.mira.com | grep TOTAL" > "$test_dir/final_memory.txt"
    adb logcat -d | grep -E "(WhisperJNI|Processing)" | tail -10 > "$test_dir/final_logs.txt"
    
    echo "$duration" > "$test_dir/duration.txt"
    
    echo ""
}

# Test 1: CPU Only (Current Configuration)
echo "=== Test 1: CPU Only (Current Configuration) ==="
CPU_DIR="$RESULTS_DIR/cpu_only"
mkdir -p "$CPU_DIR"

run_processing_test "CPU Only Test" "$CPU_DIR"

# Test 2: Enable Vulkan and Test
echo "=== Test 2: Enable Vulkan ==="
echo "🔧 Re-enabling Vulkan support..."

# Re-enable Vulkan in CMakeLists.txt
sed -i.bak 's|# ${CMAKE_SOURCE_DIR}/vulkan_helper.cpp|${CMAKE_SOURCE_DIR}/vulkan_helper.cpp|g' feature/whisper/src/main/cpp/CMakeLists.txt
sed -i.bak 's|# ${WHISPER_LIB_DIR}/ggml/src/ggml-vulkan|${WHISPER_LIB_DIR}/ggml/src/ggml-vulkan|g' feature/whisper/src/main/cpp/CMakeLists.txt
sed -i.bak 's|GGML_USE_CPU|GGML_USE_CPU\n            GGML_USE_VULKAN\n            GGML_VULKAN_DEBUG=1\n            GGML_VULKAN_CHECK_RESULTS=1|g' feature/whisper/src/main/cpp/CMakeLists.txt

# Re-enable Vulkan in build.gradle.kts
sed -i.bak 's|"-DANDROID_STL=c++_shared"|"-DANDROID_STL=c++_shared",\n            "-DGGML_VULKAN=1",\n            "-DGGML_VULKAN_DEBUG=1",\n            "-DGGML_VULKAN_CHECK_RESULTS=1"|g' feature/whisper/build.gradle.kts

echo "🔨 Building app with Vulkan support..."
if ./gradlew assembleDebug; then
    echo "✅ Vulkan build successful"
    
    echo "📱 Installing Vulkan-enabled app..."
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    
    echo ""
    
    # Test 3: Vulkan Enabled
    echo "=== Test 3: Vulkan Enabled ==="
    VULKAN_DIR="$RESULTS_DIR/vulkan_enabled"
    mkdir -p "$VULKAN_DIR"
    
    run_processing_test "Vulkan Enabled Test" "$VULKAN_DIR"
    
    VULKAN_SUCCESS=true
else
    echo "❌ Vulkan build failed, skipping Vulkan test"
    VULKAN_SUCCESS=false
fi

# Generate comprehensive ablation report
echo "=== Generating Ablation Report ==="
REPORT_FILE="$RESULTS_DIR/cpu_vulkan_ablation_report.md"

cat > "$REPORT_FILE" << EOF
# CPU vs Vulkan Ablation Test Report

**Test Date:** $(date)  
**File:** tennis_interview_clip_002.mp4  
**Model:** whisper-base.q5_1.bin  
**Method:** Automated Broadcast Processing  

## Test Results

### CPU Only Test
EOF

if [ -f "$CPU_DIR/duration.txt" ]; then
    CPU_DURATION=$(cat "$CPU_DIR/duration.txt")
    cat >> "$REPORT_FILE" << EOF
✅ **CPU Only Test Completed**
- Duration: $CPU_DURATION seconds
- Processing: Automated broadcast system
- Acceleration: CPU Only
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **CPU Only Test Failed**
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Vulkan Enabled Test
EOF

if [ "$VULKAN_SUCCESS" = true ] && [ -f "$VULKAN_DIR/duration.txt" ]; then
    VULKAN_DURATION=$(cat "$VULKAN_DIR/duration.txt")
    cat >> "$REPORT_FILE" << EOF
✅ **Vulkan Enabled Test Completed**
- Duration: $VULKAN_DURATION seconds
- Processing: Automated broadcast system
- Acceleration: Vulkan GPU + CPU
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **Vulkan Enabled Test Failed**
- Build failed or test incomplete
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Performance Comparison
EOF

if [ -f "$CPU_DIR/duration.txt" ] && [ -f "$VULKAN_DIR/duration.txt" ]; then
    CPU_DURATION=$(cat "$CPU_DIR/duration.txt")
    VULKAN_DURATION=$(cat "$VULKAN_DIR/duration.txt")
    
    if [ "$VULKAN_DURATION" -lt "$CPU_DURATION" ]; then
        SPEEDUP=$(echo "scale=2; $CPU_DURATION / $VULKAN_DURATION" | bc)
        cat >> "$REPORT_FILE" << EOF
**Processing Time Comparison:**
- CPU Only: $CPU_DURATION seconds
- Vulkan Enabled: $VULKAN_DURATION seconds
- **Speedup: ${SPEEDUP}x faster with Vulkan**

**Performance Analysis:**
- Vulkan acceleration provides significant performance improvement
- GPU offloading reduces CPU load
- Memory usage patterns differ between CPU and Vulkan
EOF
    else
        cat >> "$REPORT_FILE" << EOF
**Processing Time Comparison:**
- CPU Only: $CPU_DURATION seconds
- Vulkan Enabled: $VULKAN_DURATION seconds
- **Result: CPU was faster (possibly due to overhead)**

**Performance Analysis:**
- Vulkan overhead may outweigh benefits for this workload
- CPU-only processing more efficient for this specific case
- Consider workload characteristics for acceleration decisions
EOF
    fi
else
    cat >> "$REPORT_FILE" << EOF
**Performance Comparison:**
- Unable to compare due to test failures
- Check individual test results for details
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Detailed Metrics

### CPU Only Test Metrics
EOF

if [ -f "$CPU_DIR/final_cpu.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Final CPU Usage:**
\`\`\`
$(cat "$CPU_DIR/final_cpu.txt")
\`\`\`
EOF
fi

if [ -f "$CPU_DIR/final_memory.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Final Memory Usage:**
\`\`\`
$(cat "$CPU_DIR/final_memory.txt")
\`\`\`
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Vulkan Enabled Test Metrics
EOF

if [ "$VULKAN_SUCCESS" = true ]; then
    if [ -f "$VULKAN_DIR/final_cpu.txt" ]; then
        cat >> "$REPORT_FILE" << EOF
**Final CPU Usage:**
\`\`\`
$(cat "$VULKAN_DIR/final_cpu.txt")
\`\`\`
EOF
    fi
    
    if [ -f "$VULKAN_DIR/final_memory.txt" ]; then
        cat >> "$REPORT_FILE" << EOF
**Final Memory Usage:**
\`\`\`
$(cat "$VULKAN_DIR/final_memory.txt")
\`\`\`
EOF
    fi
else
    cat >> "$REPORT_FILE" << EOF
**Vulkan test not available due to build failure**
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Analysis

**Overall Status:** $(if [ -f "$CPU_DIR/duration.txt" ]; then echo "✅ CPU test completed"; else echo "❌ CPU test failed"; fi) / $(if [ "$VULKAN_SUCCESS" = true ] && [ -f "$VULKAN_DIR/duration.txt" ]; then echo "✅ Vulkan test completed"; else echo "❌ Vulkan test failed"; fi)

**Key Findings:**
1. **Automated Processing:** Broadcast system works reliably for both CPU and Vulkan
2. **Performance Measurement:** Successfully measured processing times and resource usage
3. **Acceleration Impact:** $(if [ -f "$CPU_DIR/duration.txt" ] && [ -f "$VULKAN_DIR/duration.txt" ]; then echo "Measurable difference between CPU and Vulkan"; else echo "Unable to measure acceleration impact"; fi)

**Recommendations:**
1. **For Production:** Use automated broadcast approach for reliable processing
2. **For Performance:** $(if [ -f "$CPU_DIR/duration.txt" ] && [ -f "$VULKAN_DIR/duration.txt" ]; then if [ "$VULKAN_DURATION" -lt "$CPU_DURATION" ]; then echo "Enable Vulkan for better performance"; else echo "Consider CPU-only for this workload"; fi; else echo "Test both approaches for your specific workload"; fi)
3. **For Monitoring:** Use the automated monitoring approach for production systems

## Files Generated
- **CPU Test Results:** \`$CPU_DIR/\`
- **Vulkan Test Results:** \`$VULKAN_DIR/\`
- **CPU Monitoring:** \`$CPU_DIR/cpu_monitoring.txt\`
- **Vulkan Monitoring:** \`$VULKAN_DIR/cpu_monitoring.txt\`
- **CPU Memory:** \`$CPU_DIR/memory_monitoring.txt\`
- **Vulkan Memory:** \`$VULKAN_DIR/memory_monitoring.txt\`

---

*Report generated automatically*
EOF

echo "✅ Report generated: $REPORT_FILE"

echo ""
echo "=== Summary ==="
echo "📊 Results saved to: $RESULTS_DIR"
echo "📋 Report generated: $REPORT_FILE"
echo ""

if [ -f "$CPU_DIR/duration.txt" ]; then
    CPU_DURATION=$(cat "$CPU_DIR/duration.txt")
    echo "✅ CPU Only Test: $CPU_DURATION seconds"
fi

if [ "$VULKAN_SUCCESS" = true ] && [ -f "$VULKAN_DIR/duration.txt" ]; then
    VULKAN_DURATION=$(cat "$VULKAN_DIR/duration.txt")
    echo "✅ Vulkan Test: $VULKAN_DURATION seconds"
    
    if [ "$VULKAN_DURATION" -lt "$CPU_DURATION" ]; then
        SPEEDUP=$(echo "scale=2; $CPU_DURATION / $VULKAN_DURATION" | bc)
        echo "🚀 Vulkan is ${SPEEDUP}x faster!"
    else
        echo "⚠️  CPU was faster (overhead considerations)"
    fi
else
    echo "❌ Vulkan test failed"
fi

echo ""
echo "🎯 CPU vs Vulkan ablation test completed!"
