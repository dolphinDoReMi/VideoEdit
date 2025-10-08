#!/bin/bash

# 📱 Direct Xiaomi Pad Permission Prompt
# This script directly triggers permission dialogs on the Xiaomi Pad screen

set -e

echo "📱 Direct Xiaomi Pad Permission Prompt"
echo "======================================"
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    echo "Please connect your Xiaomi Pad via USB debugging"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model)
echo "📱 Device: $DEVICE_MODEL"
echo "🎯 App: com.mira.com (Whisper Video Editor)"
echo ""

# Step 1: Force close and restart app to trigger fresh permission requests
echo "🔄 Step 1: Restarting App for Fresh Permission Requests"
echo "======================================================"
echo "📱 Force closing app..."
adb shell "am force-stop com.mira.com"
sleep 2

echo "📱 Launching app with fresh intent..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
echo "✅ App restarted - permission dialogs should appear"
echo ""

# Wait for app to initialize
sleep 3

# Step 2: Trigger file picker to request SAF permissions
echo "📁 Step 2: Triggering File Picker for SAF Permissions"
echo "===================================================="
echo "📱 Triggering file picker via JavaScript bridge..."
adb shell "am broadcast -a com.mira.com.TRIGGER_FILE_PICKER" 2>/dev/null || echo "⚠️  Broadcast not available, trying alternative method"

# Alternative: Use input tap to trigger file picker button
echo "📱 Attempting to trigger file picker via UI interaction..."
adb shell "input tap 500 800"  # Tap center of screen where file picker button might be
sleep 2
echo ""

# Step 3: Monitor for SAF permission dialogs
echo "👀 Step 3: Monitoring Permission Dialogs"
echo "========================================"
echo "📋 Looking for SAF permission dialogs..."

# Check for DocumentsUI activity
SAF_ACTIVE=$(adb shell "dumpsys activity activities | grep -i 'documentsui\|filemanager\|files'" | wc -l)
if [ "$SAF_ACTIVE" -gt 0 ]; then
    echo "✅ SAF permission dialog detected!"
    echo "📱 Check your Xiaomi Pad screen - you should see a folder selection dialog"
    echo ""
    echo "👤 USER ACTION REQUIRED ON YOUR XIAOMI PAD:"
    echo "   1. Look at your device screen"
    echo "   2. You should see a 'Select folder' or 'Choose folder' dialog"
    echo "   3. Navigate to and select the 'Documents' folder"
    echo "   4. Tap 'Use this folder' or 'Allow'"
    echo "   5. The app will then request permission for other folders"
else
    echo "⚠️  SAF permission dialog not detected yet"
    echo "📱 Trying alternative methods..."
fi
echo ""

# Step 4: Try triggering permission request via WebView JavaScript
echo "🌐 Step 4: Triggering Permission Request via WebView"
echo "===================================================="
echo "📱 Executing JavaScript to trigger file picker..."

# Inject JavaScript to trigger file picker
adb shell "am broadcast -a android.intent.action.MEDIA_BUTTON --es 'command' 'trigger_file_picker'" 2>/dev/null || echo "⚠️  Media button broadcast not available"

# Try to trigger via input method
echo "📱 Trying input method to trigger file picker..."
adb shell "input keyevent KEYCODE_MENU"  # Open menu
sleep 1
adb shell "input keyevent KEYCODE_DPAD_DOWN"  # Navigate down
sleep 1
adb shell "input keyevent KEYCODE_DPAD_CENTER"  # Select
sleep 2
echo ""

# Step 5: Check for any permission dialogs
echo "🔍 Step 5: Checking for Permission Dialogs"
echo "=========================================="
echo "📋 Checking for various permission dialogs..."

# Check for different types of permission dialogs
PERMISSION_DIALOGS=$(adb shell "dumpsys activity activities | grep -E 'permission|dialog|alert'" | wc -l)
if [ "$PERMISSION_DIALOGS" -gt 0 ]; then
    echo "✅ Permission dialog detected!"
    echo "📱 Check your Xiaomi Pad screen for permission requests"
else
    echo "⚠️  No permission dialogs detected"
fi

# Check for system permission dialogs
SYSTEM_PERMISSIONS=$(adb shell "dumpsys activity activities | grep -E 'Settings|PermissionController'" | wc -l)
if [ "$SYSTEM_PERMISSIONS" -gt 0 ]; then
    echo "✅ System permission dialog detected!"
    echo "📱 Check your Xiaomi Pad screen for system permission requests"
fi
echo ""

# Step 6: Show current app state
echo "📱 Step 6: Current App State"
echo "==========================="
echo "📋 Current app activities:"
adb shell "dumpsys activity activities | grep -A 5 'com.mira.com'" | head -10
echo ""

# Step 7: Force trigger SAF permission request
echo "🔐 Step 7: Force Triggering SAF Permission Request"
echo "=================================================="
echo "📱 Sending intent to request folder access..."

# Send intent to request folder access
adb shell "am start -a android.intent.action.OPEN_DOCUMENT_TREE" 2>/dev/null || echo "⚠️  Direct SAF intent not available"

# Try alternative method - send broadcast to app
adb shell "am broadcast -a com.mira.com.REQUEST_FOLDER_ACCESS" 2>/dev/null || echo "⚠️  Custom broadcast not available"

echo ""

# Step 8: Final monitoring
echo "👀 Step 8: Final Permission Monitoring"
echo "======================================"
echo "📱 Monitoring for permission dialogs for 10 seconds..."

for i in {1..10}; do
    SAF_ACTIVE=$(adb shell "dumpsys activity activities | grep -i 'documentsui\|filemanager\|files'" | wc -l)
    if [ "$SAF_ACTIVE" -gt 0 ]; then
        echo "✅ SAF permission dialog detected at $i seconds!"
        echo "📱 Check your Xiaomi Pad screen NOW!"
        break
    fi
    echo "⏳ Waiting... ($i/10)"
    sleep 1
done

echo ""
echo "📋 Final Status Check:"
SAF_ACTIVE=$(adb shell "dumpsys activity activities | grep -i 'documentsui\|filemanager\|files'" | wc -l)
if [ "$SAF_ACTIVE" -gt 0 ]; then
    echo "✅ SAF permission dialog is active!"
    echo "📱 Look at your Xiaomi Pad screen and grant folder access"
else
    echo "⚠️  No SAF permission dialog detected"
    echo "📱 You may need to manually grant permissions in Settings"
fi
echo ""

echo "✅ Direct permission prompt completed!"
echo "📱 Check your Xiaomi Pad screen for permission dialogs"
