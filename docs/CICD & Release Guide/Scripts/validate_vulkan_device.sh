#!/bin/bash

# Vulkan Device Validation Script for Xiaomi Pad 7 Ultra
# This script validates Vulkan support and features on the target device

set -e

DEVICE_NAME="Xiaomi Pad 7 Ultra"
PACKAGE_NAME="com.mira.com.debug"
LOG_TAG="VulkanValidation"

echo "=== Vulkan Device Validation for $DEVICE_NAME ==="
echo "Timestamp: $(date)"
echo

# Check device connectivity
echo "1. Checking device connectivity..."
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model)
echo "✅ Device connected: $DEVICE_MODEL"

# Check if device is Xiaomi Pad 7 Ultra
if [[ "$DEVICE_MODEL" != *"Pad"* ]]; then
    echo "⚠️  Warning: Device may not be Xiaomi Pad 7 Ultra"
fi

echo

# Check Vulkan support
echo "2. Checking Vulkan support..."
if ! adb shell which vulkaninfo >/dev/null 2>&1; then
    echo "❌ vulkaninfo not available on device"
    echo "   Install vulkaninfo via: adb install vulkaninfo.apk"
    exit 1
fi

echo "✅ vulkaninfo available"

# Get Vulkan information
echo "3. Gathering Vulkan device information..."
VULKAN_INFO=$(adb shell vulkaninfo 2>/dev/null || echo "vulkaninfo failed")

# Check Vulkan API version
API_VERSION=$(echo "$VULKAN_INFO" | grep -E "apiVersion" | head -1 | grep -o "0x[0-9a-fA-F]*" || echo "unknown")
echo "   API Version: $API_VERSION"

# Check for required features
echo "4. Checking required Vulkan features..."

# Check for FP16 support
if echo "$VULKAN_INFO" | grep -q "shaderFloat16.*true"; then
    echo "✅ FP16 shader support: Available"
else
    echo "❌ FP16 shader support: Not available"
fi

# Check for 16-bit storage
if echo "$VULKAN_INFO" | grep -q "storageBuffer16BitAccess.*true"; then
    echo "✅ 16-bit storage buffer access: Available"
else
    echo "❌ 16-bit storage buffer access: Not available"
fi

# Check for 16-bit uniform buffer
if echo "$VULKAN_INFO" | grep -q "uniformAndStorageBuffer16BitAccess.*true"; then
    echo "✅ 16-bit uniform buffer access: Available"
else
    echo "❌ 16-bit uniform buffer access: Not available"
fi

echo

# Check GPU device properties
echo "5. Checking GPU device properties..."
GPU_NAME=$(echo "$VULKAN_INFO" | grep -A 5 "VkPhysicalDeviceProperties" | grep "deviceName" | head -1 | cut -d'"' -f2 || echo "unknown")
echo "   GPU Name: $GPU_NAME"

GPU_TYPE=$(echo "$VULKAN_INFO" | grep -A 5 "VkPhysicalDeviceProperties" | grep "deviceType" | head -1 | grep -o "VK_PHYSICAL_DEVICE_TYPE_[A-Z_]*" || echo "unknown")
echo "   GPU Type: $GPU_TYPE"

echo

# Test app installation and Vulkan usage
echo "6. Testing app Vulkan integration..."

# Install app if not already installed
if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo "   Installing app..."
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        adb install -r app/build/outputs/apk/debug/app-debug.apk
        echo "✅ App installed"
    else
        echo "❌ App APK not found. Build the app first."
        exit 1
    fi
else
    echo "✅ App already installed"
fi

# Launch app and check logs
echo "7. Testing Vulkan usage in app..."

# Clear logcat
adb logcat -c

# Launch app
echo "   Launching app..."
adb shell am start -n "$PACKAGE_NAME/com.mira.whisper.WhisperMainActivity"

# Wait for app to initialize
sleep 5

# Check for Vulkan-related logs
echo "   Checking Vulkan logs..."
VULKAN_LOGS=$(adb logcat -d | grep -i "vulkan\|ggml.*vulkan" | tail -20)

if [ -n "$VULKAN_LOGS" ]; then
    echo "✅ Vulkan logs found:"
    echo "$VULKAN_LOGS"
else
    echo "⚠️  No Vulkan logs found. Checking for ggml logs..."
    GGML_LOGS=$(adb logcat -d | grep -i "ggml" | tail -10)
    if [ -n "$GGML_LOGS" ]; then
        echo "   GGML logs found:"
        echo "$GGML_LOGS"
    else
        echo "❌ No GGML logs found"
    fi
fi

echo

# Performance test
echo "8. Running performance test..."
echo "   Starting Whisper processing test..."

# Start a test transcription
adb shell am broadcast -a "$PACKAGE_NAME.whisper.RUN" --es "file_path" "/sdcard/test_audio.wav"

# Monitor performance for 30 seconds
echo "   Monitoring performance for 30 seconds..."
for i in {1..30}; do
    CPU_USAGE=$(adb shell dumpsys cpuinfo | grep "$PACKAGE_NAME" | awk '{print $1}' | head -1 || echo "0")
    MEMORY_USAGE=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}' || echo "0")
    
    if [ "$i" -eq 1 ] || [ "$i" -eq 15 ] || [ "$i" -eq 30 ]; then
        echo "   Time ${i}s: CPU=${CPU_USAGE}%, Memory=${MEMORY_USAGE}KB"
    fi
    
    sleep 1
done

echo

# Summary
echo "=== Validation Summary ==="
echo "Device: $DEVICE_MODEL"
echo "GPU: $GPU_NAME ($GPU_TYPE)"
echo "Vulkan API: $API_VERSION"
echo "App Package: $PACKAGE_NAME"
echo "Validation completed: $(date)"

# Save detailed report
REPORT_FILE="vulkan_validation_$(date +%Y%m%d_%H%M%S).txt"
echo "Saving detailed report to: $REPORT_FILE"

{
    echo "Vulkan Device Validation Report"
    echo "=============================="
    echo "Device: $DEVICE_MODEL"
    echo "Timestamp: $(date)"
    echo
    echo "Vulkan Information:"
    echo "$VULKAN_INFO"
    echo
    echo "App Logs:"
    adb logcat -d | grep -E "VulkanHelper|Vulkan|ggml.*vulkan" | tail -50
} > "$REPORT_FILE"

echo "✅ Validation complete. Report saved to $REPORT_FILE"
