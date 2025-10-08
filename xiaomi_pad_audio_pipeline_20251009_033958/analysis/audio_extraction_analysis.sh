#!/bin/bash

echo "🔍 Audio Extraction Analysis for Xiaomi Pad"
echo "=========================================="

# Configuration
CLIP_FILE="[0;34m[2025-10-09 03:39:58][0m Preprocessing audio for Xiaomi Pad Whisper processing...
[0;31m❌ Audio preprocessing failed[0m"
APP_PACKAGE="com.mira.com"

echo "📁 Analyzing file: "
echo ""

# File information
echo "📊 File Information:"
ls -lh ""
echo ""

# Audio properties
echo "🎵 Audio Properties:"
ffprobe -v quiet -show_entries stream=sample_rate,channels,codec_name -of csv=p=0 ""
echo ""

# Check if device is connected for device-specific analysis
if adb devices | grep -q "device$"; then
    echo "📱 Device Analysis:"
    
    # Device memory
    echo "💾 Device Memory Status:"
    adb shell "cat /proc/meminfo | grep -E '(MemTotal|MemFree|MemAvailable)'"
    echo ""
    
    # App memory usage
    echo "📱 App Memory Usage:"
    adb shell "dumpsys meminfo  | grep -E '(TOTAL|Native Heap|Java Heap)'" 2>/dev/null || echo "App not running"
    echo ""
    
    # Check recent logs for audio extraction errors
    echo "📋 Recent Audio Extraction Errors:"
    adb logcat -d | grep -E "(AudioIO|FileSource|extractor|OutOfMemoryError)" | tail -5
    echo ""
    
    # Check for any successful audio extractions
    echo "✅ Recent Successful Audio Extractions:"
    adb logcat -d | grep -E "(TECHNICAL.*Samples.*[1-9])" | tail -3
    echo ""
else
    echo "⚠️  No Android device connected - skipping device analysis"
fi

echo "🔍 Analysis Complete"
