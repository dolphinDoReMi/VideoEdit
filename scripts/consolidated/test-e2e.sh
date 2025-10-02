#!/bin/bash

# AutoCutPad E2E Test Script
# This script automates the end-to-end testing of AutoCutPad

set -e

echo "🚀 AutoCutPad E2E Test Script"
echo "============================="

# Set up environment
export JAVA_HOME=$(/opt/homebrew/bin/brew --prefix openjdk@17)
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Wait for emulator to be ready
echo "⏳ Waiting for emulator to be ready..."
timeout=60
while [ $timeout -gt 0 ]; do
    if adb devices | grep -q "emulator.*device"; then
        echo "✅ Emulator is ready!"
        break
    fi
    echo "Waiting... ($timeout seconds remaining)"
    sleep 5
    timeout=$((timeout - 5))
done

if [ $timeout -le 0 ]; then
    echo "❌ Emulator failed to start within 60 seconds"
    exit 1
fi

# Install the debug APK
echo "📱 Installing AutoCutPad debug APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Push test video to device
echo "📹 Uploading test video to device..."
adb push test-videos/motion-test-video.mp4 /sdcard/Download/motion-test-video.mp4

# Launch the app
echo "🚀 Launching AutoCutPad..."
adb shell am start -n com.autocutpad.videoeditor.debug/.MainActivity

echo "✅ E2E test setup complete!"
echo ""
echo "📋 Manual testing steps:"
echo "1. Open AutoCutPad app on the emulator"
echo "2. Select the test video from Downloads"
echo "3. Let the app analyze motion and create highlights"
echo "4. Export the processed video"
echo "5. Check the output quality and functionality"
echo ""
echo "📁 Test video location: /sdcard/Download/motion-test-video.mp4"
echo "📱 App package: com.autocutpad.videoeditor.debug"
