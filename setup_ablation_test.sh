#!/bin/bash

# Quick Setup Script for Tennis Interview Clip 002 Ablation Test
# Ensures all prerequisites are met before running the ablation test

set -e

echo "🔧 Ablation Test Setup - Tennis Interview Clip 002"
echo "=================================================="
echo ""

# Check device connection
echo "=== Device Connection Check ==="
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

# Check source file
echo "=== Source File Check ==="
CLIP_FILE="tennis_interview_clip_002.mp4"
CLIP_SOURCE="/Users/dennis/Movies/VideoEdit/tennis_clips/$CLIP_FILE"

if [ ! -f "$CLIP_SOURCE" ]; then
    echo "❌ Source clip file not found: $CLIP_SOURCE"
    exit 1
fi

FILE_SIZE=$(stat -f%z "$CLIP_SOURCE")
FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
echo "✅ Source file found: ${FILE_SIZE_MB}MB"

# Get video duration
DURATION_SECONDS=$(ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$CLIP_SOURCE" | cut -d. -f1)
DURATION_MINUTES=$((DURATION_SECONDS / 60))
echo "📏 Video duration: ${DURATION_SECONDS} seconds (${DURATION_MINUTES} minutes)"

echo ""

# Check whisper model
echo "=== Whisper Model Check ==="
MODEL="whisper-base.q5_1.bin"
MODEL_LOCAL="whisper_models/$MODEL"

if [ -f "$MODEL_LOCAL" ]; then
    echo "✅ Whisper model found locally: $MODEL_LOCAL"
else
    echo "⚠️  Whisper model not found locally: $MODEL_LOCAL"
    echo "Please ensure whisper model is available"
fi

echo ""

# Check device storage
echo "=== Device Storage Check ==="
STORAGE_INFO=$(adb shell df /sdcard | tail -1)
echo "Storage info: $STORAGE_INFO"

# Create necessary directories
echo "Creating device directories..."
adb shell "mkdir -p /sdcard/MiraWhisper/in /sdcard/MiraWhisper/out /sdcard/MiraWhisper/sidecars /sdcard/MiraWhisper/models"
echo "✅ Device directories created"

echo ""

# Check if app is built
echo "=== App Build Check ==="
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ App APK found"
else
    echo "⚠️  App APK not found, building..."
    if ./gradlew assembleDebug > /dev/null 2>&1; then
        echo "✅ App built successfully"
    else
        echo "❌ App build failed"
        exit 1
    fi
fi

echo ""

# Check Vulkan support
echo "=== Vulkan Support Check ==="
if adb shell "which vulkaninfo" >/dev/null 2>&1; then
    echo "✅ vulkaninfo available on device"
    
    # Get basic Vulkan info
    VULKAN_API=$(adb shell vulkaninfo 2>/dev/null | grep -E "apiVersion" | head -1 | grep -o "0x[0-9a-fA-F]*" || echo "unknown")
    echo "   Vulkan API Version: $VULKAN_API"
    
    # Check for FP16 support
    if adb shell vulkaninfo 2>/dev/null | grep -q "shaderFloat16.*true"; then
        echo "✅ FP16 shader support: Available"
    else
        echo "⚠️  FP16 shader support: Not detected"
    fi
else
    echo "⚠️  vulkaninfo not available on device"
    echo "   Vulkan support cannot be verified"
fi

echo ""

# Check required tools
echo "=== Required Tools Check ==="
REQUIRED_TOOLS=("adb" "ffprobe" "bc")

for tool in "${REQUIRED_TOOLS[@]}"; do
    if command -v "$tool" >/dev/null 2>&1; then
        echo "✅ $tool: Available"
    else
        echo "❌ $tool: Not found"
        echo "   Please install $tool"
        exit 1
    fi
done

echo ""

# Summary
echo "=== Setup Summary ==="
echo "✅ Device: $DEVICE_MODEL"
echo "✅ Source file: ${FILE_SIZE_MB}MB, ${DURATION_MINUTES} minutes"
echo "✅ App: Ready to install"
echo "✅ Device directories: Created"
echo "✅ Required tools: Available"
echo ""

# Estimated test duration
ESTIMATED_CPU_TIME=$((DURATION_SECONDS / 10))  # Rough estimate: 10x realtime
ESTIMATED_VULKAN_TIME=$((DURATION_SECONDS / 20))  # Rough estimate: 20x realtime with GPU
TOTAL_TIME=$((ESTIMATED_CPU_TIME + ESTIMATED_VULKAN_TIME + 300))  # Add 5 minutes overhead

echo "📊 Estimated Test Duration:"
echo "   CPU Test: ~${ESTIMATED_CPU_TIME} seconds ($(($ESTIMATED_CPU_TIME / 60)) minutes)"
echo "   Vulkan Test: ~${ESTIMATED_VULKAN_TIME} seconds ($(($ESTIMATED_VULKAN_TIME / 60)) minutes)"
echo "   Total: ~${TOTAL_TIME} seconds ($(($TOTAL_TIME / 60)) minutes)"
echo ""

echo "🚀 Ready to run ablation test!"
echo ""
echo "To start the test, run:"
echo "   ./tennis_interview_clip_002_ablation_test.sh"
echo ""
echo "To monitor performance in real-time (in another terminal):"
echo "   ./monitor_ablation_performance.sh"
echo ""
echo "📋 The test will:"
echo "   1. Push the clip file to the device"
echo "   2. Install the app with Vulkan support"
echo "   3. Run CPU-only test with performance monitoring"
echo "   4. Run Vulkan GPU test with performance monitoring"
echo "   5. Generate comprehensive comparison report"
echo ""
echo "📁 Results will be saved to: ./ablation_results_YYYYMMDD_HHMMSS/"
echo ""
echo "⚠️  Make sure the device is plugged in and has sufficient battery!"
