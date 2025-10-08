#!/bin/bash

# 📱 Xiaomi Pad Full Folder Access Permission Prompt
# This script prompts the user to grant full folder access permissions on Xiaomi Pad

set -e

echo "📱 Xiaomi Pad Full Folder Access Permission Prompt"
echo "=================================================="
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

# Step 1: Check current permissions
echo "🔍 Step 1: Checking Current Permissions"
echo "======================================="
echo "📋 Current storage permissions:"
adb shell "dumpsys package com.mira.com | grep -A 10 'install permissions:'" | grep -E "(READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|MANAGE_EXTERNAL_STORAGE|READ_MEDIA)" || echo "No storage permissions found"
echo ""

# Step 2: Grant basic permissions via ADB
echo "🔍 Step 2: Granting Basic Permissions via ADB"
echo "=============================================="
echo "📋 Granting READ_EXTERNAL_STORAGE..."
adb shell "pm grant com.mira.com android.permission.READ_EXTERNAL_STORAGE" 2>/dev/null && echo "✅ READ_EXTERNAL_STORAGE granted" || echo "⚠️  READ_EXTERNAL_STORAGE already granted or not available"

echo "📋 Granting WRITE_EXTERNAL_STORAGE..."
adb shell "pm grant com.mira.com android.permission.WRITE_EXTERNAL_STORAGE" 2>/dev/null && echo "✅ WRITE_EXTERNAL_STORAGE granted" || echo "⚠️  WRITE_EXTERNAL_STORAGE already granted or not available"

# Android 13+ media permissions
echo "📋 Granting READ_MEDIA_VIDEO..."
adb shell "pm grant com.mira.com android.permission.READ_MEDIA_VIDEO" 2>/dev/null && echo "✅ READ_MEDIA_VIDEO granted" || echo "⚠️  READ_MEDIA_VIDEO already granted or not available"

echo "📋 Granting READ_MEDIA_AUDIO..."
adb shell "pm grant com.mira.com android.permission.READ_MEDIA_AUDIO" 2>/dev/null && echo "✅ READ_MEDIA_AUDIO granted" || echo "⚠️  READ_MEDIA_AUDIO already granted or not available"

echo "📋 Granting READ_MEDIA_IMAGES..."
adb shell "pm grant com.mira.com android.permission.READ_MEDIA_IMAGES" 2>/dev/null && echo "✅ READ_MEDIA_IMAGES granted" || echo "⚠️  READ_MEDIA_IMAGES already granted or not available"
echo ""

# Step 3: Launch app to trigger SAF permission requests
echo "🔍 Step 3: Launching App to Trigger SAF Permission Requests"
echo "=========================================================="
echo "📱 Launching Whisper app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
echo "✅ App launched - SAF permission dialogs should appear"
echo ""

# Wait for app to initialize
sleep 3

# Step 4: Monitor SAF permission dialogs
echo "🔍 Step 4: Monitoring SAF Permission Dialogs"
echo "==========================================="
SAF_ACTIVE=$(adb shell "dumpsys activity activities | grep -i 'documentsui\|filemanager\|files'" | wc -l)
if [ "$SAF_ACTIVE" -gt 0 ]; then
    echo "✅ SAF permission dialog is active"
    echo "📋 User needs to grant permission for folder access"
else
    echo "⚠️  SAF permission dialog not detected"
    echo "📋 Checking if app is requesting permissions..."
fi
echo ""

# Step 5: Show recent permission logs
echo "🔍 Step 5: Recent Permission Request Logs"
echo "========================================="
echo "Recent permission requests:"
adb logcat -d | grep -E "(Requesting SAF permission|Storage permissions|Permission.*granted|Permission.*denied)" | tail -10
echo ""

# Step 6: Manual permission instructions
echo "🔍 Step 6: Manual Permission Instructions"
echo "========================================"
echo "📋 If SAF dialogs don't appear automatically, follow these steps:"
echo ""
echo "1. 📱 On your Xiaomi Pad, go to:"
echo "   Settings → Apps → Manage Apps → Whisper Video Editor"
echo ""
echo "2. 🔐 Tap 'Permissions' and grant:"
echo "   ✅ Storage (Files and media)"
echo "   ✅ Camera (if needed)"
echo "   ✅ Microphone (if needed)"
echo ""
echo "3. 📁 For full folder access, you may need to:"
echo "   Settings → Apps → Special app access → All files access"
echo "   Find 'Whisper Video Editor' and enable it"
echo ""
echo "4. 🔄 Restart the app after granting permissions"
echo ""

# Step 7: Test folder access
echo "🔍 Step 7: Testing Folder Access"
echo "================================"
echo "📁 Testing access to common folders:"

TEST_FOLDERS=(
    "/sdcard/Documents"
    "/sdcard/Download"
    "/sdcard/Pictures"
    "/sdcard/Movies"
    "/sdcard/Music"
)

for folder in "${TEST_FOLDERS[@]}"; do
    if adb shell "test -d $folder" 2>/dev/null; then
        echo "✅ $folder - accessible"
    else
        echo "❌ $folder - not accessible"
    fi
done
echo ""

# Step 8: Check SAF permission status
echo "🔍 Step 8: SAF Permission Status"
echo "================================"
echo "📋 Checking SAF permission grants..."
SAF_PERMISSIONS=$(adb shell "content query --uri content://com.android.providers.downloads.documents/document" 2>/dev/null | wc -l)
if [ "$SAF_PERMISSIONS" -gt 0 ]; then
    echo "✅ SAF permissions detected"
else
    echo "⚠️  No SAF permissions detected"
fi
echo ""

# Step 9: Final instructions
echo "🔍 Step 9: Final Instructions"
echo "============================="
echo "📋 After granting permissions:"
echo ""
echo "1. 🔄 Restart the Whisper app"
echo "2. 📁 Try selecting files from different folders"
echo "3. 🎬 Test video processing functionality"
echo "4. 📊 Check logs for permission-related errors"
echo ""
echo "💡 If you still have issues:"
echo "   - Check Xiaomi Pad's MIUI security settings"
echo "   - Ensure USB debugging is enabled"
echo "   - Try granting permissions manually in Settings"
echo ""

echo "✅ Permission prompt completed!"
echo "📱 Check your Xiaomi Pad for permission dialogs"
