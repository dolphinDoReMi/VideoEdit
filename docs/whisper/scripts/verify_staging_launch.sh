#!/bin/bash

# App Launch Verification Script
# Verifies the staging interface is working

echo "🎯 Staging Interface Launch Verification"
echo "======================================="

# Check if app is running
if adb shell ps | grep -q "com.mira.com.t.xi"; then
    echo "✅ App is running"
else
    echo "❌ App not running, launching..."
    adb shell am start -n com.mira.com.t.xi/com.mira.clip.Clip4ClipActivity
    sleep 3
fi

# Take screenshot
echo "📸 Taking screenshot..."
adb shell screencap -p /sdcard/final_screen.png
adb pull /sdcard/final_screen.png final_screen.png
adb shell rm /sdcard/final_screen.png

if [ -f "final_screen.png" ]; then
    echo "✅ Screenshot saved as final_screen.png"
else
    echo "❌ Screenshot failed"
fi

# Check app logs
echo "📋 Checking app logs..."
adb logcat -d | grep -i "Clip4ClipActivity\|staging\|webview" | tail -5

echo ""
echo "🎯 Manual Verification Steps:"
echo "1. Look at your Xiaomi Pad screen"
echo "2. You should see the Mira app with a WebView interface"
echo "3. Look for the 'Staging — Select & Inspect' section"
echo "4. Look for the 'Add Folder' button"
echo "5. Look for video file listings"
echo ""
echo "🔧 If you don't see the staging interface:"
echo "1. Try tapping the Mira app icon on your home screen"
echo "2. Check if there are any error messages"
echo "3. Try swiping up to see recent apps and tap on Mira"
echo ""
echo "✅ Verification complete!"
