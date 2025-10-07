#!/bin/bash

# Build and Test Vulkan Integration Script
# This script builds the app with Vulkan support and runs comprehensive tests

set -e

echo "=== Vulkan Build and Test Script ==="
echo "Timestamp: $(date)"
echo

# Configuration
BUILD_TYPE="debug"
PACKAGE_NAME="com.mira.com.debug"
TEST_AUDIO_FILE="/sdcard/test_audio.wav"

# Step 1: Clean and build
echo "1. Cleaning and building with Vulkan support..."
./gradlew clean
./gradlew :app:assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
else
    echo "❌ Build failed"
    exit 1
fi

echo

# Step 2: Install app
echo "2. Installing app..."
adb install -r app/build/outputs/apk/debug/app-debug.apk
echo "✅ App installed"

echo

# Step 3: Setup test environment
echo "3. Setting up test environment..."

# Create test audio file if it doesn't exist
if ! adb shell test -f "$TEST_AUDIO_FILE"; then
    echo "   Creating test audio file..."
    # Create a simple test audio file (1 second of silence)
    adb shell "echo -e '\x52\x49\x46\x46\x24\x00\x00\x00\x57\x41\x56\x45\x66\x6d\x74\x20\x10\x00\x00\x00\x01\x00\x01\x00\x44\xac\x00\x00\x88\x58\x01\x00\x02\x00\x10\x00\x64\x61\x74\x61\x00\x00\x00\x00' > $TEST_AUDIO_FILE"
    echo "✅ Test audio file created"
else
    echo "✅ Test audio file exists"
fi

echo

# Step 4: Enable Vulkan validation layers
echo "4. Enabling Vulkan validation layers..."
adb shell settings put global enable_gpu_debug_layers 1
adb shell settings put global gpu_debug_app "$PACKAGE_NAME"
adb shell settings put global gpu_debug_layers VK_LAYER_KHRONOS_validation

echo "✅ Validation layers enabled"

echo

# Step 5: Run device validation
echo "5. Running device validation..."
if [ -f "scripts/validate_vulkan_device.sh" ]; then
    chmod +x scripts/validate_vulkan_device.sh
    ./scripts/validate_vulkan_device.sh
else
    echo "⚠️  Device validation script not found"
fi

echo

# Step 6: Test Vulkan integration
echo "6. Testing Vulkan integration..."

# Clear logs
adb logcat -c

# Launch app
echo "   Launching app..."
adb shell am start -n "$PACKAGE_NAME/com.mira.whisper.WhisperMainActivity"

# Wait for initialization
sleep 5

# Check Vulkan initialization logs
echo "   Checking Vulkan initialization..."
VULKAN_INIT_LOGS=$(adb logcat -d | grep -i "vulkan.*init\|ggml.*vulkan.*init" | tail -10)

if [ -n "$VULKAN_INIT_LOGS" ]; then
    echo "✅ Vulkan initialization logs found:"
    echo "$VULKAN_INIT_LOGS"
else
    echo "⚠️  No Vulkan initialization logs found"
fi

echo

# Step 7: Performance test
echo "7. Running performance test..."

# Start transcription
echo "   Starting Whisper transcription..."
adb shell am broadcast -a "$PACKAGE_NAME.whisper.RUN" --es "file_path" "$TEST_AUDIO_FILE"

# Monitor for 60 seconds
echo "   Monitoring performance for 60 seconds..."
START_TIME=$(date +%s)

for i in {1..60}; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))
    
    # Get performance metrics
    CPU_USAGE=$(adb shell dumpsys cpuinfo | grep "$PACKAGE_NAME" | awk '{print $1}' | head -1 || echo "0")
    MEMORY_USAGE=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}' || echo "0")
    
    # Check for Vulkan usage logs
    VULKAN_USAGE=$(adb logcat -d | grep -i "vulkan.*pipeline\|vulkan.*memory\|ggml.*vulkan" | wc -l)
    
    if [ $((i % 10)) -eq 0 ]; then
        echo "   Time ${ELAPSED}s: CPU=${CPU_USAGE}%, Memory=${MEMORY_USAGE}KB, Vulkan logs=${VULKAN_USAGE}"
    fi
    
    sleep 1
done

echo

# Step 8: Collect results
echo "8. Collecting test results..."

# Get final logs
FINAL_LOGS=$(adb logcat -d | grep -E "VulkanHelper|Vulkan|ggml.*vulkan" | tail -100)

# Save results
RESULTS_FILE="vulkan_test_results_$(date +%Y%m%d_%H%M%S).txt"
{
    echo "Vulkan Integration Test Results"
    echo "=============================="
    echo "Timestamp: $(date)"
    echo "Build Type: $BUILD_TYPE"
    echo "Package: $PACKAGE_NAME"
    echo
    echo "Vulkan Logs:"
    echo "$FINAL_LOGS"
    echo
    echo "Performance Summary:"
    echo "Final CPU Usage: $CPU_USAGE%"
    echo "Final Memory Usage: ${MEMORY_USAGE}KB"
    echo "Vulkan Log Count: $VULKAN_USAGE"
} > "$RESULTS_FILE"

echo "✅ Test results saved to: $RESULTS_FILE"

echo

# Step 9: Cleanup
echo "9. Cleaning up..."

# Disable validation layers
adb shell settings delete global enable_gpu_debug_layers
adb shell settings delete global gpu_debug_app
adb shell settings delete global gpu_debug_layers

echo "✅ Validation layers disabled"

echo

# Summary
echo "=== Test Summary ==="
echo "Build: ✅ Successful"
echo "Installation: ✅ Successful"
echo "Vulkan Integration: $(if [ -n "$VULKAN_INIT_LOGS" ]; then echo "✅ Detected"; else echo "⚠️  Not detected"; fi)"
echo "Performance: CPU=${CPU_USAGE}%, Memory=${MEMORY_USAGE}KB"
echo "Results: $RESULTS_FILE"
echo "Test completed: $(date)"
