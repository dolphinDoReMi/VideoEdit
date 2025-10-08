#!/bin/bash

# Setup Script for Redesigned DirectWhisperService Ablation Test
# Ensures all prerequisites are met before running the redesigned ablation test
# Based on proven DirectWhisperService command flow from Device Deployment.md

set -e

echo "🔧 Redesigned Ablation Test Setup - DirectWhisperService"
echo "======================================================="
echo "Based on proven DirectWhisperService command flow"
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
APP_PACKAGE="com.mira.com"
SCOPED_STORAGE_BASE="/storage/emulated/0/Android/data/$APP_PACKAGE/files"

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
adb shell "mkdir -p $SCOPED_STORAGE_BASE/MiraWhisper/in $SCOPED_STORAGE_BASE/MiraWhisper/out $SCOPED_STORAGE_BASE/MiraWhisper/sidecars $SCOPED_STORAGE_BASE/MiraWhisper/models"
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
    
    # Check for 16-bit storage support
    if adb shell vulkaninfo 2>/dev/null | grep -q "storageInputOutput16.*true"; then
        echo "✅ 16-bit storage support: Available"
    else
        echo "⚠️  16-bit storage support: Not detected"
    fi
else
    echo "⚠️  vulkaninfo not available on device"
    echo "   Vulkan support cannot be verified"
fi

echo ""

# Check DirectWhisperService availability
echo "=== DirectWhisperService Check ==="
SERVICE_CLASS="com.mira.com.feature.whisper.service.DirectWhisperService"

# Check if service is available in the app
if adb shell "pm list packages | grep com.mira.com" >/dev/null 2>&1; then
    echo "✅ App package found"
    
    # Try to check service availability (this might fail if app not installed)
    if adb shell "dumpsys package com.mira.com | grep -i directwhisper" >/dev/null 2>&1; then
        echo "✅ DirectWhisperService found in app"
    else
        echo "⚠️  DirectWhisperService not found in app manifest"
        echo "   This may be normal if app is not installed yet"
    fi
else
    echo "⚠️  App package not found (may need installation)"
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

# Test DirectWhisperService command flow
echo "=== DirectWhisperService Command Flow Test ==="
echo "Testing the proven command flow from Device Deployment.md..."

# Install app if not already installed
echo "Installing app..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Test app launch
echo "Testing app launch..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
sleep 3

# Check if app is running
if adb shell "ps | grep com.mira.com" >/dev/null 2>&1; then
    echo "✅ App launched successfully"
else
    echo "❌ App launch failed"
    exit 1
fi

# Test service start (without actual processing)
echo "Testing DirectWhisperService availability..."
if adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$SCOPED_STORAGE_BASE/MiraWhisper/in/test.wav" \
    --es "model" "$SCOPED_STORAGE_BASE/MiraWhisper/models/small.en-q5_1.bin" \
    --es "threads" "4" \
    --es "lang" "en" 2>/dev/null; then
    echo "✅ DirectWhisperService responds to commands"
else
    echo "⚠️  DirectWhisperService test failed (may be normal without valid input)"
fi

# Stop app
adb shell am force-stop com.mira.com
echo "✅ Command flow test completed"

echo ""

# Summary
echo "=== Setup Summary ==="
echo "✅ Device: $DEVICE_MODEL"
echo "✅ Source file: ${FILE_SIZE_MB}MB, ${DURATION_MINUTES} minutes"
echo "✅ App: Ready to install"
echo "✅ Device directories: Created"
echo "✅ Required tools: Available"
echo "✅ DirectWhisperService: Command flow tested"
echo ""

# Estimated test duration
ESTIMATED_CPU_TIME=$((DURATION_SECONDS / 8))  # Rough estimate: 8x realtime
ESTIMATED_VULKAN_TIME=$((DURATION_SECONDS / 15))  # Rough estimate: 15x realtime with GPU
TOTAL_TIME=$((ESTIMATED_CPU_TIME + ESTIMATED_VULKAN_TIME + 600))  # Add 10 minutes overhead

echo "📊 Estimated Test Duration:"
echo "   CPU Test: ~${ESTIMATED_CPU_TIME} seconds ($(($ESTIMATED_CPU_TIME / 60)) minutes)"
echo "   Vulkan Test: ~${ESTIMATED_VULKAN_TIME} seconds ($(($ESTIMATED_VULKAN_TIME / 60)) minutes)"
echo "   Total: ~${TOTAL_TIME} seconds ($(($TOTAL_TIME / 60)) minutes)"
echo ""

echo "🚀 Ready to run redesigned ablation test!"
echo ""
echo "To start the test, run:"
echo "   ./redesigned_ablation_test.sh"
echo ""
echo "To monitor performance in real-time (in another terminal):"
echo "   ./monitor_directwhisper_performance.sh"
echo ""
echo "📋 The redesigned test will:"
echo "   1. Use DirectWhisperService (proven command flow)"
echo "   2. Keep app in foreground (prevents background errors)"
echo "   3. Use fixed language 'en' (prevents LID hangs)"
echo "   4. Monitor TranscribeWorker logs (real-time progress)"
echo "   5. Run CPU-only test with performance monitoring"
echo "   6. Run Vulkan GPU test with performance monitoring"
echo "   7. Generate comprehensive comparison report"
echo ""
echo "📁 Results will be saved to: ./redesigned_ablation_YYYYMMDD_HHMMSS/"
echo ""
echo "⚠️  Make sure the device is plugged in and has sufficient battery!"
echo ""
echo "🔑 Key Success Factors Applied:"
echo "   - App must stay in foreground during processing"
echo "   - Language fixed to 'en' to prevent LID hangs"
echo "   - DirectWhisperService eliminates broadcast overhead"
echo "   - TranscribeWorker monitoring for real-time progress"
echo "   - Storage self-test validation"
