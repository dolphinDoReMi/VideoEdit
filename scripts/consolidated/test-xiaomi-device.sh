#!/bin/bash

# AutoCutPad Xiaomi Device Testing Script
# This script automates testing on Xiaomi devices with MIUI

set -e

echo "📱 AutoCutPad Xiaomi Device Testing"
echo "===================================="

# Set up environment
export JAVA_HOME=$(/opt/homebrew/bin/brew --prefix openjdk@17)
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Detect Xiaomi device
XIAOMI_DEVICE=""
echo "🔍 Detecting Xiaomi devices..."
for device in $(adb devices | grep -v "List of devices" | grep "device" | awk '{print $1}'); do
    if [[ $device == emulator* ]]; then
        continue
    fi
    
    # Check if it's a Xiaomi device
    model=$(adb -s $device shell getprop ro.product.manufacturer 2>/dev/null || echo "")
    if [[ $model == *"Xiaomi"* ]] || [[ $model == *"Redmi"* ]] || [[ $model == *"POCO"* ]]; then
        XIAOMI_DEVICE=$device
        echo "✅ Found Xiaomi device: $device"
        break
    fi
done

if [ -z "$XIAOMI_DEVICE" ]; then
    echo "❌ No Xiaomi device found. Please connect a Xiaomi device and enable USB debugging."
    exit 1
fi

# Get device information
echo "📊 Device Information:"
MODEL=$(adb -s $XIAOMI_DEVICE shell getprop ro.product.model)
ANDROID_VERSION=$(adb -s $XIAOMI_DEVICE shell getprop ro.build.version.release)
MIUI_VERSION=$(adb -s $XIAOMI_DEVICE shell getprop ro.miui.ui.version.name)
echo "   Model: $MODEL"
echo "   Android: $ANDROID_VERSION"
echo "   MIUI: $MIUI_VERSION"
echo ""

# Install AutoCutPad APK
echo "📦 Installing AutoCutPad APK..."
adb -s $XIAOMI_DEVICE install -r app/build/outputs/apk/debug/app-debug.apk

# Transfer test video
echo "📹 Transferring test video..."
adb -s $XIAOMI_DEVICE push test-videos/motion-test-video.mp4 /sdcard/Download/motion-test-video.mp4

# Grant permissions (MIUI specific)
echo "🔐 Granting permissions..."
adb -s $XIAOMI_DEVICE shell pm grant com.mira.videoeditor.debug android.permission.READ_EXTERNAL_STORAGE
adb -s $XIAOMI_DEVICE shell pm grant com.mira.videoeditor.debug android.permission.READ_MEDIA_VIDEO
adb -s $XIAOMI_DEVICE shell pm grant com.mira.videoeditor.debug android.permission.WRITE_EXTERNAL_STORAGE
adb -s $XIAOMI_DEVICE shell pm grant com.mira.videoeditor.debug android.permission.CAMERA
adb -s $XIAOMI_DEVICE shell pm grant com.mira.videoeditor.debug android.permission.RECORD_AUDIO

# Launch the app
echo "🚀 Launching AutoCutPad..."
adb -s $XIAOMI_DEVICE shell am start -n com.mira.videoeditor.debug/com.mira.videoeditor.MainActivity

echo "✅ Xiaomi device testing setup complete!"
echo ""
echo "📋 Manual Testing Steps:"
echo "1. 📱 Open AutoCutPad app on your Xiaomi device"
echo "2. 📁 Navigate to Downloads folder and select 'motion-test-video.mp4'"
echo "3. 🎬 Let the app analyze motion and create highlights"
echo "4. 📤 Export the processed video"
echo "5. 📊 Check the output quality and functionality"
echo ""
echo "🔧 MIUI-Specific Testing:"
echo "- Check if app appears in MIUI's app drawer"
echo "- Verify permissions are properly granted"
echo "- Test video processing performance on MIUI"
echo "- Check if exported videos are accessible"
echo ""
echo "📁 Test video location: /sdcard/Download/motion-test-video.mp4"
echo "📱 App package: com.mira.videoeditor.debug"
echo "🎯 Device: $MODEL (MIUI $MIUI_VERSION)"
echo ""
echo "💡 Tips for MIUI testing:"
echo "- Disable MIUI optimization for the app if needed"
echo "- Check battery optimization settings"
echo "- Verify storage permissions in MIUI settings"
echo "- Test with MIUI's file manager"
