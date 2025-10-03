#!/bin/bash

# Fix Android Studio Profiler "No Valid Process" Error
# This script helps resolve common profiler issues

set -e

echo "🔧 Android Studio Profiler Fix Script"
echo "====================================="

# Set up Java environment
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "✅ Java environment configured"

# Check if device is connected
echo "📱 Checking device connection..."
adb devices

if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected. Please:"
    echo "   1. Connect your Xiaomi device via USB"
    echo "   2. Enable USB Debugging in Developer Options"
    echo "   3. Run this script again"
    exit 1
fi

echo "✅ Device connected"

# Uninstall any existing versions
echo "🗑️  Uninstalling existing app versions..."
adb uninstall com.mira.videoeditor.debug 2>/dev/null || echo "Debug version not installed"
adb uninstall com.mira.videoeditor.internal 2>/dev/null || echo "Internal version not installed"
adb uninstall com.mira.videoeditor 2>/dev/null || echo "Release version not installed"

echo "✅ Cleaned existing installations"

# Install fresh debug APK
echo "📦 Installing fresh debug APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -eq 0 ]; then
    echo "✅ Debug APK installed successfully"
    echo "📱 Package: com.mira.videoeditor.debug"
    echo "🔧 Debuggable: true"
else
    echo "❌ Failed to install debug APK"
    exit 1
fi

# Launch the app
echo "🚀 Launching app..."
adb shell am start -n com.mira.videoeditor.debug/com.mira.videoeditor.MainActivity

echo ""
echo "🎯 Next Steps for Android Studio Profiler:"
echo "=========================================="
echo "1. Open Android Studio"
echo "2. Go to View → Tool Windows → Profiler"
echo "3. You should now see: com.mira.videoeditor.debug"
echo "4. Select the process and try profiling again"
echo ""
echo "💡 If still having issues:"
echo "   - Try 'Capture System Activities' first"
echo "   - Then try 'Java/Kotlin Method Recording'"
echo "   - Restart Android Studio if needed"
echo ""
echo "✅ Profiler fix complete!"
