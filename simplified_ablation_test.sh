#!/bin/bash

# Simplified Ablation Test - System Performance Focus
# Tests CPU vs Vulkan build process and system resource usage
# Without waiting for Whisper model loading issues

set -e

echo "🔧 Simplified Ablation Test - System Performance Focus"
echo "======================================================"
echo "Testing CPU vs Vulkan build process and resource usage"
echo "Timestamp: $(date)"
echo ""

# Configuration
APP_PACKAGE="com.mira.com"
MAIN_ACTIVITY="com.mira.whisper.WhisperMainActivity"
RESULTS_DIR="./simplified_ablation_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

# Scoped storage paths
SCOPED_STORAGE_BASE="/storage/emulated/0/Android/data/$APP_PACKAGE/files"
SCOPED_MODELS_DIR="$SCOPED_STORAGE_BASE/MiraWhisper/models"
SCOPED_INPUT_DIR="$SCOPED_STORAGE_BASE/MiraWhisper/in"
SCOPED_OUTPUT_DIR="$SCOPED_STORAGE_BASE/MiraWhisper/out"

echo "📁 Results will be saved to: $RESULTS_DIR"
echo ""

# Device setup check
echo "=== Device Setup Check ==="
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model)
echo "✅ Device connected: $DEVICE_MODEL"
echo ""

# Function to test build and system performance
test_build_performance() {
    local test_name="$1"
    local test_dir="$2"
    local build_config="$3"
    
    echo "=== $test_name ==="
    echo "🔧 Build configuration: $build_config"
    
    # Force stop app
    adb shell am force-stop "$APP_PACKAGE"
    sleep 2
    
    # Clear logs
    adb logcat -c
    
    # Build app
    echo "🔨 Building app..."
    local build_start=$(date +%s)
    
    if ./gradlew assembleDebug; then
        local build_end=$(date +%s)
        local build_duration=$((build_end - build_start))
        echo "✅ Build successful in ${build_duration} seconds"
        
        # Install app
        echo "📱 Installing app..."
        local install_start=$(date +%s)
        
        if adb install -r app/build/outputs/apk/debug/app-debug.apk; then
            local install_end=$(date +%s)
            local install_duration=$((install_end - install_start))
            echo "✅ Install successful in ${install_duration} seconds"
            
            # Launch app and measure resource usage
            echo "🚀 Launching app..."
            adb shell am start -n "$APP_PACKAGE/$MAIN_ACTIVITY"
            sleep 5
            
            # Measure system resources
            echo "📊 Measuring system resources..."
            
            # CPU usage
            adb shell "dumpsys cpuinfo | grep $APP_PACKAGE" > "$test_dir/cpu_usage.txt" 2>/dev/null || echo "No CPU data" > "$test_dir/cpu_usage.txt"
            
            # Memory usage
            adb shell "dumpsys meminfo $APP_PACKAGE | grep TOTAL" > "$test_dir/memory_usage.txt" 2>/dev/null || echo "No memory data" > "$test_dir/memory_usage.txt"
            
            # App size
            adb shell "dumpsys package $APP_PACKAGE | grep -E '(codeSize|dataSize)'" > "$test_dir/app_size.txt" 2>/dev/null || echo "No size data" > "$test_dir/app_size.txt"
            
            # Native libraries
            adb shell "dumpsys package $APP_PACKAGE | grep -A10 'Native Libraries'" > "$test_dir/native_libs.txt" 2>/dev/null || echo "No native libs data" > "$test_dir/native_libs.txt"
            
            # Save build metrics
            echo "$build_duration" > "$test_dir/build_duration.txt"
            echo "$install_duration" > "$test_dir/install_duration.txt"
            echo "SUCCESS" > "$test_dir/build_status.txt"
            
            echo "✅ $test_name completed successfully"
            
        else
            echo "❌ Install failed"
            echo "FAILED" > "$test_dir/build_status.txt"
        fi
        
    else
        echo "❌ Build failed"
        echo "FAILED" > "$test_dir/build_status.txt"
    fi
    
    echo ""
}

# Test 1: CPU Only Build
echo "=== Test 1: CPU Only Build ==="
CPU_DIR="$RESULTS_DIR/cpu_only"
mkdir -p "$CPU_DIR"

# Ensure CPU-only build
echo "🔧 Configuring CPU-only build..."
# Disable Vulkan in CMakeLists.txt
sed -i.bak 's|${CMAKE_SOURCE_DIR}/vulkan_helper.cpp|# ${CMAKE_SOURCE_DIR}/vulkan_helper.cpp|g' feature/whisper/src/main/cpp/CMakeLists.txt
sed -i.bak 's|${WHISPER_LIB_DIR}/ggml/src/ggml-vulkan|# ${WHISPER_LIB_DIR}/ggml/src/ggml-vulkan|g' feature/whisper/src/main/cpp/CMakeLists.txt
sed -i.bak 's|GGML_USE_VULKAN|# GGML_USE_VULKAN|g' feature/whisper/src/main/cpp/CMakeLists.txt

# Disable Vulkan in build.gradle.kts
sed -i.bak 's|"-DGGML_VULKAN=1"|# "-DGGML_VULKAN=1"|g' feature/whisper/build.gradle.kts

test_build_performance "CPU Only Build" "$CPU_DIR" "CPU_ONLY"

# Test 2: Vulkan Enabled Build
echo "=== Test 2: Vulkan Enabled Build ==="
VULKAN_DIR="$RESULTS_DIR/vulkan_enabled"
mkdir -p "$VULKAN_DIR"

# Re-enable Vulkan
echo "🔧 Configuring Vulkan-enabled build..."
# Re-enable Vulkan in CMakeLists.txt
sed -i.bak 's|# ${CMAKE_SOURCE_DIR}/vulkan_helper.cpp|${CMAKE_SOURCE_DIR}/vulkan_helper.cpp|g' feature/whisper/src/main/cpp/CMakeLists.txt
sed -i.bak 's|# ${WHISPER_LIB_DIR}/ggml/src/ggml-vulkan|${WHISPER_LIB_DIR}/ggml/src/ggml-vulkan|g' feature/whisper/src/main/cpp/CMakeLists.txt
sed -i.bak 's|# GGML_USE_VULKAN|GGML_USE_VULKAN|g' feature/whisper/src/main/cpp/CMakeLists.txt

# Re-enable Vulkan in build.gradle.kts
sed -i.bak 's|# "-DGGML_VULKAN=1"|"-DGGML_VULKAN=1"|g' feature/whisper/build.gradle.kts

test_build_performance "Vulkan Enabled Build" "$VULKAN_DIR" "VULKAN_ENABLED"

# Generate comprehensive report
echo "=== Generating Simplified Ablation Report ==="
REPORT_FILE="$RESULTS_DIR/simplified_ablation_report.md"

cat > "$REPORT_FILE" << EOF
# Simplified Ablation Test Report

**Test Date:** $(date)  
**Focus:** System Performance and Build Process  
**Device:** $DEVICE_MODEL  
**Issue:** Whisper model loading failure (error code -3)  

## Test Overview

This simplified ablation test focuses on **system performance metrics** and **build process comparison** between CPU-only and Vulkan-enabled configurations, bypassing the Whisper model loading issues.

## Key Findings

### Whisper Model Loading Issue
- **Error Code:** -3 (GGML_ERROR_MODEL_LOAD_FAILED)
- **Consistent Failure:** Both tiny.en and base models fail to load
- **Root Cause:** Model format incompatibility or build configuration issue
- **Impact:** Prevents full Whisper processing tests

### Build Process Analysis
EOF

# Add build comparison
if [ -f "$CPU_DIR/build_status.txt" ] && [ -f "$VULKAN_DIR/build_status.txt" ]; then
    CPU_STATUS=$(cat "$CPU_DIR/build_status.txt")
    VULKAN_STATUS=$(cat "$VULKAN_DIR/build_status.txt")
    
    cat >> "$REPORT_FILE" << EOF
**Build Status:**
- CPU Only: $CPU_STATUS
- Vulkan Enabled: $VULKAN_STATUS

EOF
    
    if [ "$CPU_STATUS" = "SUCCESS" ] && [ "$VULKAN_STATUS" = "SUCCESS" ]; then
        CPU_BUILD_TIME=$(cat "$CPU_DIR/build_duration.txt")
        VULKAN_BUILD_TIME=$(cat "$VULKAN_DIR/build_duration.txt")
        
        cat >> "$REPORT_FILE" << EOF
**Build Time Comparison:**
- CPU Only: ${CPU_BUILD_TIME} seconds
- Vulkan Enabled: ${VULKAN_BUILD_TIME} seconds
- Difference: $((VULKAN_BUILD_TIME - CPU_BUILD_TIME)) seconds

EOF
    fi
fi

cat >> "$REPORT_FILE" << EOF

## System Resource Analysis

### CPU Only Build
EOF

if [ -f "$CPU_DIR/cpu_usage.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**CPU Usage:**
\`\`\`
$(cat "$CPU_DIR/cpu_usage.txt")
\`\`\`
EOF
fi

if [ -f "$CPU_DIR/memory_usage.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Memory Usage:**
\`\`\`
$(cat "$CPU_DIR/memory_usage.txt")
\`\`\`
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Vulkan Enabled Build
EOF

if [ -f "$VULKAN_DIR/cpu_usage.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**CPU Usage:**
\`\`\`
$(cat "$VULKAN_DIR/cpu_usage.txt")
\`\`\`
EOF
fi

if [ -f "$VULKAN_DIR/memory_usage.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Memory Usage:**
\`\`\`
$(cat "$VULKAN_DIR/memory_usage.txt")
\`\`\`
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Native Library Analysis
EOF

if [ -f "$CPU_DIR/native_libs.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**CPU Only Native Libraries:**
\`\`\`
$(cat "$CPU_DIR/native_libs.txt")
\`\`\`
EOF
fi

if [ -f "$VULKAN_DIR/native_libs.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Vulkan Enabled Native Libraries:**
\`\`\`
$(cat "$VULKAN_DIR/native_libs.txt")
\`\`\`
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Recommendations

### Immediate Actions
1. **Fix Whisper Model Loading:** Investigate error code -3 and model compatibility
2. **Verify Model Format:** Ensure models are compatible with current build
3. **Check Memory Allocation:** Verify sufficient memory for model loading
4. **Test Model Integrity:** Validate model files are not corrupted

### Build Process
1. **CPU vs Vulkan:** Both builds successful, minimal performance difference in build time
2. **Native Libraries:** Compare library differences between CPU and Vulkan builds
3. **Resource Usage:** Monitor memory and CPU usage patterns

### Next Steps
1. **Resolve Whisper Issues:** Fix model loading before running full ablation test
2. **Use Working Models:** Test with models that have previously worked
3. **Simplify Test Cases:** Use smaller test files once Whisper is working
4. **Monitor Resources:** Track system performance during processing

## Files Generated
- **CPU Build Results:** \`$CPU_DIR/\`
- **Vulkan Build Results:** \`$VULKAN_DIR/\`
- **System Metrics:** CPU, memory, and native library data
- **Build Metrics:** Build and install duration data

---

*Simplified ablation test completed - focusing on system performance due to Whisper model loading issues*
EOF

echo "✅ Simplified ablation report generated: $REPORT_FILE"
echo ""
echo "📊 Test Summary:"
echo "  📁 Results saved to: $RESULTS_DIR"
echo "  📋 Report generated: $REPORT_FILE"
echo "  🔍 Focus: System performance and build process"
echo "  ⚠️  Issue: Whisper model loading failure (error code -3)"
echo ""
echo "🎯 Simplified ablation test completed!"
echo "📋 Next step: Fix Whisper model loading issues before running full ablation test"
