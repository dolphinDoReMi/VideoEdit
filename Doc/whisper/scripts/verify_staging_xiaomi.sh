#!/bin/bash

# Xiaomi Pad Staging Step 0 Verification Script
# Tests the deployed staging functionality on the device

echo "🧪 Xiaomi Pad Staging Step 0 Verification"
echo "=========================================="

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ Error: No Android device connected"
    exit 1
fi

echo "✅ Android device connected"

# Check if app is installed
if ! adb shell pm list packages | grep -q "com.mira.com"; then
    echo "❌ Error: Mira app not installed"
    exit 1
fi

echo "✅ Mira app installed"

# Launch the app
echo "🚀 Launching Mira app..."
adb shell am start -n com.mira.com/com.mira.clip.Clip4ClipActivity

# Wait for app to load
echo "⏳ Waiting for app to load..."
sleep 5

# Check if app is running
if adb shell ps | grep -q "com.mira.com"; then
    echo "✅ App is running"
else
    echo "❌ App failed to start"
    exit 1
fi

# Take a screenshot for verification
echo "📸 Taking screenshot for verification..."
adb shell screencap -p /sdcard/staging_verification.png
adb pull /sdcard/staging_verification.png staging_verification.png
adb shell rm /sdcard/staging_verification.png

if [ -f "staging_verification.png" ]; then
    echo "✅ Screenshot saved as staging_verification.png"
else
    echo "⚠️ Screenshot failed"
fi

# Check app logs for any errors
echo "📋 Checking app logs..."
adb logcat -d | grep -i "mira\|staging\|bridge" | tail -20

echo ""
echo "🎯 Manual Verification Steps:"
echo "1. Look for 'Staging — Select & Inspect' section in the app"
echo "2. Test 'Add Folder' button (should open SAF picker)"
echo "3. Verify mock videos appear in the table"
echo "4. Test filtering by extension (.mp4, .mov, etc.)"
echo "5. Test duration filtering"
echo "6. Test individual file probing"
echo "7. Test selection checkboxes and summary updates"
echo ""
echo "🔌 Bridge Integration Test:"
echo "1. Check browser console for 'StagingBridge' availability"
echo "2. Test real SAF folder selection"
echo "3. Verify persisted permissions work"
echo ""
echo "✅ Staging Step 0 verification complete!"
echo "📱 Check the Xiaomi Pad screen for the staging interface"
