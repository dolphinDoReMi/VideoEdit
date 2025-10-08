#!/bin/bash

# Micro Test 1: Basic Device Validation
# Following XiaoMi Pad Inference Optimization guide

echo "🔍 Micro Test 1: Device Validation"
echo "=================================="
echo "Timestamp: $(date)"
echo ""

# Check device specs as per guide
echo "📱 Device Specifications:"
echo "Model: $(adb shell getprop ro.product.model)"
echo "Manufacturer: $(adb shell getprop ro.product.manufacturer)"
echo "Android: $(adb shell getprop ro.build.version.release)"
echo "Architecture: $(adb shell getprop ro.product.cpu.abi)"
echo "RAM: $(adb shell getprop ro.product.ram)"
echo ""

# Check Vulkan support (as per guide)
echo "🔧 Vulkan Support Check:"
VULKAN_HARDWARE=$(adb shell "dumpsys SurfaceFlinger | grep -i vulkan" | head -1)
if [ -n "$VULKAN_HARDWARE" ]; then
    echo "✅ Vulkan hardware detected: $VULKAN_HARDWARE"
else
    echo "❌ No Vulkan hardware detected"
fi
echo ""

# Check GGUF models (as per guide - small.en-Q5_1)
echo "📁 GGUF Models Check:"
adb shell "ls -la /sdcard/MiraWhisper/models/ | grep -E 'small.*q5_1'"
echo ""

echo "✅ Micro Test 1 completed!"
