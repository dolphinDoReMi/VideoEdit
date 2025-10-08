#!/bin/bash

# CI/CD Ablation Test Workflow
# Automated CPU vs Vulkan performance comparison with WIP status
# Focuses on DirectWhisperService performance analysis

set -e

echo "🎾 CI/CD Ablation Test Workflow - WIP Status"
echo "============================================="
echo "Automated CPU vs Vulkan performance comparison"
echo "Timestamp: $(date)"
echo "Branch: $(git rev-parse --abbrev-ref HEAD)"
echo ""

# WIP Configuration
WIP_STATUS="true"
ABLATION_FOCUS="cpu_vulkan_performance"
TEST_METHOD="DirectWhisperService"

# Configuration based on Device Deployment.md
CLIP_FILE="tennis_interview_clip_002.mp4"
CLIP_SOURCE="/Users/dennis/Movies/VideoEdit/tennis_clips/$CLIP_FILE"
APP_PACKAGE="com.mira.com"
MAIN_ACTIVITY="com.mira.whisper.WhisperMainActivity"
SERVICE_CLASS="com.mira.com.feature.whisper.service.DirectWhisperService"

# Scoped storage paths
SCOPED_STORAGE_BASE="/storage/emulated/0/Android/data/$APP_PACKAGE/files"
DEVICE_PATH="$SCOPED_STORAGE_BASE/MiraWhisper/in/$CLIP_FILE"
MODEL="$SCOPED_STORAGE_BASE/MiraWhisper/models/small.en-q5_1.bin"
OUTPUT_DIR="$SCOPED_STORAGE_BASE/MiraWhisper/out"
SIDECAR_DIR="$SCOPED_STORAGE_BASE/MiraWhisper/sidecars"

# Test parameters
THREADS=4
LANGUAGE="en"
TRANSLATE=false

# Results directory with WIP prefix
RESULTS_DIR="./cicd_ablation_wip_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "📁 WIP Results will be saved to: $RESULTS_DIR"
echo "🚧 Status: Work In Progress - Ablation Testing"
echo ""

# Create WIP status file
cat > "$RESULTS_DIR/wip_status.txt" << EOF
WIP Status: ACTIVE
Focus: CPU vs Vulkan Ablation Testing
Method: DirectWhisperService
Branch: $(git rev-parse --abbrev-ref HEAD)
Commit: $(git rev-parse --short HEAD)
Started: $(date)
Status: IN_PROGRESS
EOF

# Device setup check
echo "=== Device Setup Check ==="
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    echo "Please connect Xiaomi Pad Ultra and enable USB debugging"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model)
echo "✅ Device connected: $DEVICE_MODEL"

if [[ "$DEVICE_MODEL" != *"Pad"* ]]; then
    echo "⚠️  Warning: Device may not be Xiaomi Pad Ultra"
fi

echo ""

# Source file check
echo "=== Source File Check ==="
if [ ! -f "$CLIP_SOURCE" ]; then
    echo "❌ Source clip file not found: $CLIP_SOURCE"
    echo "Creating test clip from available sources..."
    
    # Look for alternative test files
    ALTERNATIVE_FILES=(
        "/Users/dennis/Movies/VideoEdit/tennis_clips/tennis_interview_clip_001.mp4"
        "/Users/dennis/Movies/VideoEdit/test_clips/sample.mp4"
        "/Users/dennis/Movies/VideoEdit/sample_videos/test.mp4"
    )
    
    for alt_file in "${ALTERNATIVE_FILES[@]}"; do
        if [ -f "$alt_file" ]; then
            echo "✅ Using alternative file: $alt_file"
            CLIP_SOURCE="$alt_file"
            CLIP_FILE=$(basename "$alt_file")
            break
        fi
    done
    
    if [ ! -f "$CLIP_SOURCE" ]; then
        echo "❌ No suitable test file found"
        echo "Please ensure test video files are available"
        exit 1
    fi
fi

FILE_SIZE=$(stat -f%z "$CLIP_SOURCE")
FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
echo "✅ Source file found: ${FILE_SIZE_MB}MB"

# Get video duration
DURATION=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$CLIP_SOURCE" 2>/dev/null || echo "unknown")
echo "📹 Video duration: ${DURATION}s"

echo ""

# Function to run processing test
run_processing_test() {
    local test_name="$1"
    local test_dir="$2"
    local config_type="$3"
    
    echo "=== $test_name ==="
    echo "🔧 Configuration: $config_type"
    
    local start_time=$(date +%s)
    
    # Clean previous outputs
    adb shell "rm -rf $OUTPUT_DIR/*" 2>/dev/null || true
    adb shell "rm -rf $SIDECAR_DIR/*" 2>/dev/null || true
    
    # Copy test file to device
    echo "📱 Copying test file to device..."
    adb push "$CLIP_SOURCE" "$DEVICE_PATH"
    
    # Start CPU monitoring
    echo "📊 Starting CPU monitoring..."
    adb shell "top -d 1 | grep $APP_PACKAGE" > "$test_dir/cpu_monitoring.txt" &
    CPU_MONITOR_PID=$!
    
    # Start memory monitoring
    echo "💾 Starting memory monitoring..."
    adb shell "while true; do dumpsys meminfo $APP_PACKAGE | grep TOTAL; sleep 2; done" > "$test_dir/memory_monitoring.txt" &
    MEMORY_MONITOR_PID=$!
    
    # Start DirectWhisperService
    echo "🎯 Starting DirectWhisperService..."
    adb shell "am startservice -n $APP_PACKAGE/$SERVICE_CLASS --es input_file $DEVICE_PATH --es model_path $MODEL --es output_dir $OUTPUT_DIR --es sidecar_dir $SIDECAR_DIR --ei threads $THREADS --es language $LANGUAGE --ez translate $TRANSLATE"
    
    # Monitor processing
    echo "⏳ Monitoring processing..."
    local processing_start=$(date +%s)
    local timeout=300  # 5 minutes timeout
    
    while [ $(($(date +%s) - processing_start)) -lt $timeout ]; do
        if adb shell "test -f $OUTPUT_DIR/$CLIP_FILE.srt" 2>/dev/null; then
            echo "✅ Processing completed!"
            break
        fi
        
        # Check if service is still running
        if ! adb shell "ps | grep $SERVICE_CLASS" | grep -q "$SERVICE_CLASS"; then
            echo "⚠️  Service stopped, checking for output..."
            break
        fi
        
        sleep 2
    done
    
    # Stop monitoring
    kill $CPU_MONITOR_PID 2>/dev/null || true
    kill $MEMORY_MONITOR_PID 2>/dev/null || true
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo "✅ $test_name completed in $duration seconds"
    
    # Get final metrics
    adb shell "dumpsys cpuinfo | grep $APP_PACKAGE | head -1" > "$test_dir/final_cpu.txt" 2>/dev/null || echo "No CPU data" > "$test_dir/final_cpu.txt"
    adb shell "dumpsys meminfo $APP_PACKAGE | grep TOTAL" > "$test_dir/final_memory.txt" 2>/dev/null || echo "No memory data" > "$test_dir/final_memory.txt"
    adb logcat -s TranscribeWorker:V -d | tail -20 > "$test_dir/final_transcribe_logs.txt" 2>/dev/null || echo "No logs" > "$test_dir/final_transcribe_logs.txt"
    
    # Check for output files
    if adb shell "test -f $OUTPUT_DIR/$CLIP_FILE.srt" 2>/dev/null; then
        echo "SUCCESS" > "$test_dir/status.txt"
        echo "✅ Output file generated successfully"
        
        # Pull output files for analysis
        adb pull "$OUTPUT_DIR/$CLIP_FILE.srt" "$test_dir/" 2>/dev/null || true
        adb pull "$SIDECAR_DIR/$CLIP_FILE.json" "$test_dir/" 2>/dev/null || true
    else
        echo "FAILED" > "$test_dir/status.txt"
        echo "❌ No output file generated"
    fi
    
    echo "$duration" > "$test_dir/duration.txt"
    
    echo ""
}

# Test 1: CPU Only (Current Configuration)
echo "=== Test 1: CPU Only (Current Configuration) ==="
CPU_DIR="$RESULTS_DIR/cpu_only"
mkdir -p "$CPU_DIR"

# Ensure CPU-only build
echo "🔧 Ensuring CPU-only build configuration..."
# Disable Vulkan in CMakeLists.txt
sed -i.bak 's|${CMAKE_SOURCE_DIR}/vulkan_helper.cpp|# ${CMAKE_SOURCE_DIR}/vulkan_helper.cpp|g' feature/whisper/src/main/cpp/CMakeLists.txt
sed -i.bak 's|${WHISPER_LIB_DIR}/ggml/src/ggml-vulkan|# ${WHISPER_LIB_DIR}/ggml/src/ggml-vulkan|g' feature/whisper/src/main/cpp/CMakeLists.txt
sed -i.bak 's|GGML_USE_VULKAN|# GGML_USE_VULKAN|g' feature/whisper/src/main/cpp/CMakeLists.txt

# Disable Vulkan in build.gradle.kts
sed -i.bak 's|"-DGGML_VULKAN=1"|# "-DGGML_VULKAN=1"|g' feature/whisper/build.gradle.kts

echo "🔨 Building CPU-only app..."
if ./gradlew assembleDebug; then
    echo "✅ CPU-only build successful"
    
    echo "📱 Installing CPU-only app..."
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    
    run_processing_test "CPU Only Test" "$CPU_DIR" "CPU_ONLY"
    
    CPU_SUCCESS=true
else
    echo "❌ CPU-only build failed"
    CPU_SUCCESS=false
fi

# Test 2: Enable Vulkan and Test
echo "=== Test 2: Enable Vulkan ==="
echo "🔧 Re-enabling Vulkan support..."

# Re-enable Vulkan in CMakeLists.txt
sed -i.bak 's|# ${CMAKE_SOURCE_DIR}/vulkan_helper.cpp|${CMAKE_SOURCE_DIR}/vulkan_helper.cpp|g' feature/whisper/src/main/cpp/CMakeLists.txt
sed -i.bak 's|# ${WHISPER_LIB_DIR}/ggml/src/ggml-vulkan|${WHISPER_LIB_DIR}/ggml/src/ggml-vulkan|g' feature/whisper/src/main/cpp/CMakeLists.txt
sed -i.bak 's|# GGML_USE_VULKAN|GGML_USE_VULKAN|g' feature/whisper/src/main/cpp/CMakeLists.txt

# Re-enable Vulkan in build.gradle.kts
sed -i.bak 's|# "-DGGML_VULKAN=1"|"-DGGML_VULKAN=1"|g' feature/whisper/build.gradle.kts

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
    
    run_processing_test "Vulkan Enabled Test" "$VULKAN_DIR" "VULKAN_ENABLED"
    
    VULKAN_SUCCESS=true
else
    echo "❌ Vulkan build failed, skipping Vulkan test"
    VULKAN_SUCCESS=false
fi

# Generate comprehensive ablation report
echo "=== Generating WIP Ablation Report ==="
REPORT_FILE="$RESULTS_DIR/cicd_ablation_wip_report.md"

cat > "$REPORT_FILE" << EOF
# CI/CD Ablation Test Report - WIP Status

**Test Date:** $(date)  
**Branch:** $(git rev-parse --abbrev-ref HEAD)  
**Commit:** $(git rev-parse --short HEAD)  
**Status:** 🚧 Work In Progress  
**Focus:** CPU vs Vulkan Performance Analysis  
**Method:** DirectWhisperService  

## Test Configuration

- **Test File:** $CLIP_FILE
- **File Size:** ${FILE_SIZE_MB}MB
- **Duration:** ${DURATION}s
- **Model:** small.en-q5_1.bin
- **Threads:** $THREADS
- **Language:** $LANGUAGE
- **Translate:** $TRANSLATE

## Test Results

### CPU Only Test
- **Status:** $([ "$CPU_SUCCESS" = true ] && echo "✅ SUCCESS" || echo "❌ FAILED")
- **Duration:** $(cat "$CPU_DIR/duration.txt" 2>/dev/null || echo "N/A") seconds
- **Output:** $(cat "$CPU_DIR/status.txt" 2>/dev/null || echo "N/A")

### Vulkan Enabled Test
- **Status:** $([ "$VULKAN_SUCCESS" = true ] && echo "✅ SUCCESS" || echo "❌ FAILED")
- **Duration:** $(cat "$VULKAN_DIR/duration.txt" 2>/dev/null || echo "N/A") seconds
- **Output:** $(cat "$VULKAN_DIR/status.txt" 2>/dev/null || echo "N/A")

## Performance Analysis

### CPU Usage
- **CPU Only:** $(cat "$CPU_DIR/final_cpu.txt" 2>/dev/null || echo "N/A")
- **Vulkan Enabled:** $(cat "$VULKAN_DIR/final_cpu.txt" 2>/dev/null || echo "N/A")

### Memory Usage
- **CPU Only:** $(cat "$CPU_DIR/final_memory.txt" 2>/dev/null || echo "N/A")
- **Vulkan Enabled:** $(cat "$VULKAN_DIR/final_memory.txt" 2>/dev/null || echo "N/A")

## WIP Status

This is a Work In Progress ablation test focused on:
- CPU vs Vulkan performance comparison
- DirectWhisperService optimization
- Automated CI/CD integration
- Performance monitoring and analysis

## Next Steps

1. Complete performance analysis
2. Optimize based on results
3. Integrate with CI/CD pipeline
4. Remove WIP status when ready

## Files Generated

- CPU test results: $CPU_DIR/
- Vulkan test results: $VULKAN_DIR/
- WIP status: $RESULTS_DIR/wip_status.txt
- This report: $REPORT_FILE

EOF

echo "✅ WIP Ablation report generated: $REPORT_FILE"

# Update WIP status
cat > "$RESULTS_DIR/wip_status.txt" << EOF
WIP Status: COMPLETED
Focus: CPU vs Vulkan Ablation Testing
Method: DirectWhisperService
Branch: $(git rev-parse --abbrev-ref HEAD)
Commit: $(git rev-parse --short HEAD)
Started: $(date)
Completed: $(date)
Status: ANALYSIS_READY
CPU Test: $([ "$CPU_SUCCESS" = true ] && echo "SUCCESS" || echo "FAILED")
Vulkan Test: $([ "$VULKAN_SUCCESS" = true ] && echo "SUCCESS" || echo "FAILED")
EOF

echo ""
echo "🎯 WIP Ablation Test Complete!"
echo "============================="
echo "Results saved to: $RESULTS_DIR"
echo "Report: $REPORT_FILE"
echo "Status: Analysis ready for CI/CD integration"
echo ""
echo "🚧 This is a Work In Progress test focused on ablation analysis"
echo "📊 Performance data collected and ready for review"
echo "🔄 Ready for CI/CD pipeline integration"
