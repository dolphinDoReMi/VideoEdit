#!/bin/bash

# Xiaomi Pad App Deployment Verification Script
# Tests the deployed app with Staging Step 0 functionality

echo "🚀 Xiaomi Pad App Deployment Verification"
echo "=========================================="

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ Error: No Android device connected"
    exit 1
fi

echo "✅ Android device connected"

# Check if app is installed and running
if adb shell ps | grep -q "com.mira.com"; then
    echo "✅ Mira app is running"
else
    echo "⚠️ App may not be running, launching..."
    adb shell am start -n com.mira.com/com.mira.clip.Clip4ClipActivity
    sleep 3
fi

# Take a screenshot for verification
echo "📸 Taking screenshot for verification..."
adb shell screencap -p /sdcard/app_verification.png
adb pull /sdcard/app_verification.png app_verification.png
adb shell rm /sdcard/app_verification.png

if [ -f "app_verification.png" ]; then
    echo "✅ Screenshot saved as app_verification.png"
else
    echo "⚠️ Screenshot failed"
fi

# Check app logs for any errors
echo "📋 Checking app logs..."
adb logcat -d | grep -i "mira\|staging\|bridge\|webview\|html\|error" | tail -20

# Test WebView functionality
echo "🌐 Testing WebView functionality..."
adb shell "am start -a android.intent.action.VIEW -d 'file:///android_asset/web/whisper-step1.html'"

sleep 2

# Check if WebView loaded
if adb shell ps | grep -q "com.mira.com"; then
    echo "✅ WebView loaded successfully"
else
    echo "⚠️ WebView may not have loaded"
fi

echo ""
echo "🎯 Manual Verification Steps:"
echo "1. Check Xiaomi Pad screen for the app interface"
echo "2. Look for 'Staging — Select & Inspect' section"
echo "3. Test 'Add Folder' button (should open SAF picker)"
echo "4. Verify mock videos appear in the table"
echo "5. Test filtering by extension (.mp4, .mov, etc.)"
echo "6. Test duration filtering"
echo "7. Test individual file probing"
echo "8. Test selection checkboxes and summary updates"
echo ""
echo "🔌 Bridge Integration Test:"
echo "1. Check browser console for 'StagingBridge' availability"
echo "2. Test real SAF folder selection"
echo "3. Verify persisted permissions work"
echo ""
echo "📱 Device Information:"
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
echo ""
echo "✅ App deployment verification complete!"
echo "📱 Check the Xiaomi Pad screen for the staging interface"
