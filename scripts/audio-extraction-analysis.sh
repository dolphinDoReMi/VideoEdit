#!/bin/bash

echo "🔍 Audio Extraction Issue Analysis"
echo "=================================="

# Configuration
CLIP_FILE="/data/local/tmp/tennis_test.mp4"
APP_PACKAGE="com.mira.com"

echo "📁 Analyzing file: $CLIP_FILE"
echo ""

# File information
echo "📊 File Information:"
adb shell "ls -lh $CLIP_FILE"
echo ""

# Device memory
echo "💾 Device Memory Status:"
adb shell "cat /proc/meminfo | grep -E '(MemTotal|MemFree|MemAvailable)'"
echo ""

# App memory usage
echo "📱 App Memory Usage:"
adb shell "dumpsys meminfo $APP_PACKAGE | grep -E '(TOTAL|Native Heap|Java Heap)'"
echo ""

# Check if file is accessible
echo "🔐 File Access Test:"
adb shell "test -r $CLIP_FILE && echo '✅ File is readable' || echo '❌ File is not readable'"
echo ""

# Check MediaExtractor capabilities
echo "🎬 MediaExtractor Test:"
adb shell "am start -a android.intent.action.VIEW -d file://$CLIP_FILE -t video/mp4" 2>/dev/null
sleep 2
adb shell "am force-stop com.android.gallery3d" 2>/dev/null
echo ""

# Check recent logs for audio extraction errors
echo "📋 Recent Audio Extraction Errors:"
adb logcat -d | grep -E "(AudioIO|FileSource|extractor|OutOfMemoryError)" | tail -5
echo ""

# Check for any successful audio extractions
echo "✅ Recent Successful Audio Extractions:"
adb logcat -d | grep -E "(TECHNICAL.*Samples.*[1-9])" | tail -3
echo ""

echo "🔍 Analysis Complete"
