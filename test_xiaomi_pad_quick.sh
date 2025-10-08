#!/bin/bash

# Quick Xiaomi Pad Test - Duplicate Check & Global Cleanup
# Run this to test the new functionality immediately

set -e

# Update this with your Xiaomi Pad IP
DEVICE="192.168.1.100:5555"

echo "🔍 Testing Duplicate Check & Global Cleanup on Xiaomi Pad"
echo "=================================================="

# Check connection
echo "📱 Checking device connection..."
if adb -s "$DEVICE" shell echo "Connected" > /dev/null 2>&1; then
    echo "✅ Xiaomi Pad connected: $DEVICE"
else
    echo "❌ Cannot connect to Xiaomi Pad"
    echo "Please update DEVICE variable with correct IP"
    exit 1
fi

# Build and install
echo "🔨 Building and installing app..."
./gradlew :app:installDebug

# Launch app
echo "🚀 Launching app..."
adb -s "$DEVICE" shell am start -n "com.mira.videoeditor/.MainActivity"
sleep 3

# Test 1: Check logs for duplicate detection
echo "📋 Testing duplicate check functionality..."
echo "Creating test scenario..."

# Create a test file
adb -s "$DEVICE" shell "mkdir -p /storage/emulated/0/Documents/ConvertedMedia"
adb -s "$DEVICE" shell "echo 'test converted file' > /storage/emulated/0/Documents/ConvertedMedia/test_video_converted.mp4"

# Navigate to whisper interface
adb -s "$DEVICE" shell "am start -n com.mira.videoeditor/.WhisperUnifiedActivity"
sleep 2

echo "📊 Checking recent logs for duplicate detection..."
adb -s "$DEVICE" logcat -d | grep -E "(Found existing converted file|Skipping conversion|TECHNICAL.*converted)" | tail -10

# Test 2: Check logs for global cleanup
echo "🧹 Testing global cleanup functionality..."
echo "Triggering cleanup operations..."

# Check current files
echo "Files before cleanup:"
adb -s "$DEVICE" shell "ls -la /storage/emulated/0/Documents/ConvertedMedia/"

# Take screenshot
echo "📸 Taking screenshot..."
adb -s "$DEVICE" shell "screencap -p /storage/emulated/0/screenshot_$(date +%s).png"
adb -s "$DEVICE" pull "/storage/emulated/0/screenshot_$(date +%s).png" ./xiaomi_pad_current_state.png 2>/dev/null || true

echo "📊 Checking recent logs for cleanup operations..."
adb -s "$DEVICE" logcat -d | grep -E "(Global cleanup|Deleted.*file|Cleanup completed|TECHNICAL.*cleanup)" | tail -10

# Test 3: Performance check
echo "⚡ Checking performance logs..."
adb -s "$DEVICE" logcat -d | grep -E "(CPU|Memory|Resource)" | tail -5

echo "✅ Test completed!"
echo "📁 Screenshot saved as: xiaomi_pad_current_state.png"
echo "📋 Check the logs above for duplicate check and cleanup activity"
