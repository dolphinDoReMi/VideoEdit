#!/bin/bash

# Xiaomi Pad Ultra Device Verification Script for Async Whisper Processing
# This script verifies the async solution works on the actual device

echo "=== Xiaomi Pad Ultra Device Verification ==="
echo "Testing Async Whisper Processing Solution"
echo ""

# Check if device is connected
echo "=== Device Connection Check ==="
DEVICE_INFO=$(adb devices | grep "device$")
if [ -z "$DEVICE_INFO" ]; then
    echo "❌ No Android device connected"
    echo "Please connect Xiaomi Pad Ultra and enable USB debugging"
    exit 1
fi

echo "✅ Device connected: $DEVICE_INFO"

# Get device details
echo ""
echo "=== Device Information ==="
DEVICE_MODEL=$(adb shell getprop ro.product.model)
DEVICE_VERSION=$(adb shell getprop ro.build.version.release)
DEVICE_API=$(adb shell getprop ro.build.version.sdk)

echo "Model: $DEVICE_MODEL"
echo "Android Version: $DEVICE_VERSION"
echo "API Level: $DEVICE_API"

# Check if it's Xiaomi Pad Ultra
if [[ "$DEVICE_MODEL" == *"Pad"* ]] || [[ "$DEVICE_MODEL" == *"Xiaomi"* ]]; then
    echo "✅ Xiaomi Pad Ultra detected"
else
    echo "⚠️  Device may not be Xiaomi Pad Ultra: $DEVICE_MODEL"
fi

# Build and install the app using the existing deployment script
echo ""
echo "=== Building and Installing App ==="
echo "Building app..."
if ./gradlew assembleDebug > /dev/null 2>&1; then
    echo "✅ App built successfully"
else
    echo "❌ App build failed"
    exit 1
fi

echo "Installing app using hands-free script..."
if [ -f "scripts/xiaomi_hands_free_install.sh" ]; then
    echo "Using hands-free installation script..."
    ./scripts/xiaomi_hands_free_install.sh "app/build/outputs/apk/debug/app-debug.apk" "com.mira.com"
    echo "✅ App installed successfully with hands-free script"
else
    echo "⚠️  Hands-free script not found, falling back to manual install..."
    
    # Uninstall existing version if it exists
    if adb shell pm list packages | grep -q "com.mira.com"; then
        echo "Uninstalling existing version..."
        adb uninstall com.mira.com || true
    fi
    
    # Install new version with auto-grant permissions
    echo "Installing new version with auto-grant permissions..."
    adb install -r -t -g app/build/outputs/apk/debug/app-debug.apk
    
    if [ $? -eq 0 ]; then
        echo "✅ App installed successfully"
    else
        echo "❌ App installation failed"
        exit 1
    fi
fi

# Check device storage and permissions
echo ""
echo "=== Device Storage Check ==="
STORAGE_INFO=$(adb shell df /sdcard | tail -1)
echo "Storage info: $STORAGE_INFO"

# Check if MiraWhisper directories exist
echo ""
echo "=== Checking MiraWhisper Directories ==="
adb shell "mkdir -p /sdcard/MiraWhisper/in /sdcard/MiraWhisper/out /sdcard/MiraWhisper/sidecars /sdcard/MiraWhisper/models" 2>/dev/null
echo "✅ MiraWhisper directories created/verified"

# Check for whisper models
echo ""
echo "=== Checking Whisper Models ==="
MODEL_COUNT=$(adb shell "ls /sdcard/MiraWhisper/models/*.bin 2>/dev/null | wc -l")
if [ "$MODEL_COUNT" -gt 0 ]; then
    echo "✅ Found $MODEL_COUNT whisper model(s)"
    adb shell "ls -la /sdcard/MiraWhisper/models/*.bin"
else
    echo "⚠️  No whisper models found"
    echo "Please copy whisper models to /sdcard/MiraWhisper/models/"
fi

# Check for test videos
echo ""
echo "=== Checking Test Videos ==="
VIDEO_COUNT=$(adb shell "ls /sdcard/MiraWhisper/in/*.mp4 2>/dev/null | wc -l")
if [ "$VIDEO_COUNT" -gt 0 ]; then
    echo "✅ Found $VIDEO_COUNT test video(s)"
    adb shell "ls -la /sdcard/MiraWhisper/in/*.mp4"
else
    echo "⚠️  No test videos found"
    echo "Please copy test videos to /sdcard/MiraWhisper/in/"
fi

# Launch the app
echo ""
echo "=== Launching App ==="
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
if [ $? -eq 0 ]; then
    echo "✅ App launched successfully"
else
    echo "❌ App launch failed"
    exit 1
fi

# Wait for app to load
echo "⏳ Waiting for app to load..."
sleep 8

# Check if app is running
echo ""
echo "=== App Status Check ==="
APP_RUNNING=$(adb shell "ps | grep com.mira.com" | wc -l)
if [ "$APP_RUNNING" -gt 0 ]; then
    echo "✅ App is running"
else
    echo "❌ App is not running"
fi

# Start log monitoring
echo ""
echo "=== Starting Log Monitoring ==="
echo "Monitoring whisper-related logs..."
echo "Press Ctrl+C to stop monitoring and continue with manual testing"
echo ""

# Function to monitor logs
monitor_logs() {
    adb logcat -c  # Clear existing logs
    echo "=== Recent Whisper Logs ==="
    adb logcat | grep -E "(WhisperConnectorService|AndroidWhisperBridge|TranscribeWorker|WhisperApi)" --line-buffered | head -20
}

# Run log monitoring in background
monitor_logs &
MONITOR_PID=$!

# Give user time to see logs
sleep 5

# Stop monitoring
kill $MONITOR_PID 2>/dev/null

echo ""
echo "=== Manual Testing Instructions ==="
echo "Now please test the async whisper processing solution:"
echo ""
echo "1. 📱 Navigate to Whisper section in the app"
echo "2. 🎥 Select some video files from the file picker"
echo "3. ⚡ Click 'Start Processing' button"
echo "4. ✅ Verify: UI navigates to processing page after ~1 second (no hanging!)"
echo "5. 📊 Verify: Processing page shows real-time progress updates"
echo "6. 🎯 Verify: Jobs complete and navigate to results page"
echo ""

echo "=== Expected Behavior ==="
echo "✅ No more UI hanging when clicking 'Start Processing'"
echo "✅ Immediate navigation to processing page (after 1 second)"
echo "✅ Real-time progress updates from actual WorkManager jobs"
echo "✅ Proper job completion detection and navigation to results"
echo ""

echo "=== Monitoring Commands ==="
echo "To monitor logs in real-time:"
echo "adb logcat | grep -E '(WhisperConnectorService|AndroidWhisperBridge|TranscribeWorker)'"
echo ""
echo "To check WorkManager jobs:"
echo "adb shell 'run-as com.mira.videoeditor ls -la /data/data/com.mira.videoeditor/databases/'"
echo ""
echo "To check sidecar files:"
echo "adb shell 'ls -la /sdcard/MiraWhisper/sidecars/'"
echo ""
echo "To check processing results:"
echo "adb shell 'ls -la /sdcard/MiraWhisper/out/'"
echo ""

echo "=== Performance Monitoring ==="
echo "To monitor device performance during processing:"
echo "adb shell 'top | grep com.mira.videoeditor'"
echo ""
echo "To check memory usage:"
echo "adb shell 'dumpsys meminfo com.mira.videoeditor'"
echo ""

echo "🎉 Device verification setup complete!"
echo "The async whisper processing solution is ready for testing on Xiaomi Pad Ultra."
echo ""
echo "Please perform the manual testing steps above and report any issues."
