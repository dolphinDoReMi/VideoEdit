#!/bin/bash

# App Launch Diagnostic Script
# Helps troubleshoot why the staging interface isn't showing

echo "🔍 App Launch Diagnostic"
echo "======================"

# Check device connection
echo "📱 Device Status:"
if adb devices | grep -q "device$"; then
    echo "✅ Device connected"
    adb shell getprop ro.product.model
    adb shell getprop ro.build.version.release
else
    echo "❌ No device connected"
    exit 1
fi

echo ""

# Check app installation
echo "📦 App Installation:"
if adb shell pm list packages | grep -q "com.mira.com.t.xi"; then
    echo "✅ App installed"
    
    # Get app info
    echo "App version:"
    adb shell dumpsys package com.mira.com.t.xi | grep "versionName" | head -1
else
    echo "❌ App not installed"
    exit 1
fi

echo ""

# Check app processes
echo "🔄 App Processes:"
if adb shell ps | grep -q "com.mira.com.t.xi"; then
    echo "✅ App is running"
    adb shell ps | grep "com.mira.com.t.xi"
else
    echo "❌ App not running"
fi

echo ""

# Check WebView
echo "🌐 WebView Status:"
if adb shell dumpsys webviewupdate | grep -q "com.mira.com.t.xi"; then
    echo "✅ WebView detected"
else
    echo "⚠️ WebView not detected"
fi

echo ""

# Check HTML file
echo "📄 HTML File Check:"
if adb shell ls /android_asset/web/whisper-step1.html > /dev/null 2>&1; then
    echo "✅ HTML file exists in assets"
else
    echo "❌ HTML file missing from assets"
fi

echo ""

# Try different launch methods
echo "🚀 Launch Methods:"
echo "1. Standard launch:"
adb shell am start -n com.mira.com.t.xi/com.mira.clip.Clip4ClipActivity

sleep 2

echo "2. Main action launch:"
adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.mira.com.t.xi/com.mira.clip.Clip4ClipActivity

sleep 2

echo "3. View HTML file:"
adb shell am start -a android.intent.action.VIEW -d "file:///android_asset/web/whisper-step1.html" -n com.mira.com.t.xi/com.mira.clip.Clip4ClipActivity

echo ""

# Take screenshot
echo "📸 Taking screenshot..."
adb shell screencap -p /sdcard/diagnostic_screen.png
adb pull /sdcard/diagnostic_screen.png diagnostic_screen.png
adb shell rm /sdcard/diagnostic_screen.png

if [ -f "diagnostic_screen.png" ]; then
    echo "✅ Screenshot saved as diagnostic_screen.png"
else
    echo "❌ Screenshot failed"
fi

echo ""

# Check recent logs
echo "📋 Recent App Logs:"
adb logcat -d | grep -i "com.mira.com.t.xi\|Clip4ClipActivity\|webview\|html\|error" | tail -10

echo ""
echo "🎯 Manual Steps:"
echo "1. Check the Xiaomi Pad screen"
echo "2. Look for the Mira app icon"
echo "3. Try tapping the app icon directly"
echo "4. Check if any error dialogs appear"
echo "5. Look for the staging interface with 'Add Folder' button"
echo ""
echo "✅ Diagnostic complete!"
