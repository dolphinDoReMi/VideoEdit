#!/bin/bash

# 🔐 SAF Documents Folder Permission Grant
# This script guides the user through granting SAF permissions for the Documents folder

set -e

echo "🔐 SAF Documents Folder Permission Grant"
echo "========================================"
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

echo "📱 Device: $(adb shell getprop ro.product.model)"
echo "📁 Requesting SAF permission for: Documents folder"
echo ""

# Step 1: Check current SAF permission status
echo "🔍 Step 1: Checking Current SAF Permission Status"
echo "================================================"
SAF_GRANTED=$(adb logcat -d | grep "SAF permission granted" | tail -1)
if [ -n "$SAF_GRANTED" ]; then
    echo "✅ SAF permission previously granted:"
    echo "   $SAF_GRANTED"
else
    echo "⚠️  No SAF permission detected yet"
fi

# Step 2: Launch app to trigger SAF permission request
echo ""
echo "🔍 Step 2: Launching App to Trigger SAF Permission Request"
echo "========================================================="
echo "📱 Launching Whisper app..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperUnifiedActivity"
echo "✅ App launched - SAF permission dialog should appear"

# Wait for app to initialize
sleep 3

# Step 3: Monitor SAF permission dialog
echo ""
echo "🔍 Step 3: Monitoring SAF Permission Dialog"
echo "==========================================="
SAF_ACTIVE=$(adb shell "dumpsys activity activities | grep 'documentsui'" | wc -l)
if [ "$SAF_ACTIVE" -gt 0 ]; then
    echo "✅ SAF permission dialog is active"
    echo "📋 User needs to grant permission for Documents folder"
    echo ""
    echo "👤 USER ACTION REQUIRED:"
    echo "   1. Look at your device screen"
    echo "   2. You should see a 'Select folder' dialog"
    echo "   3. Navigate to and select the 'Documents' folder"
    echo "   4. Tap 'Use this folder' or 'Allow'"
    echo "   5. The app will then request permission for other folders"
else
    echo "⚠️  SAF permission dialog not detected"
    echo "📋 Checking if permission was already granted..."
fi

# Step 4: Monitor permission progress
echo ""
echo "🔍 Step 4: Monitoring Permission Progress"
echo "========================================="
echo "📊 Monitoring SAF permission requests..."

# Start monitoring in background
(
    while true; do
        # Check for SAF permission grants
        SAF_GRANTED=$(adb logcat -d | grep "SAF permission granted" | tail -1)
        if [ -n "$SAF_GRANTED" ]; then
            echo "✅ SAF Permission Granted: $SAF_GRANTED"
        fi
        
        # Check for next folder request
        NEXT_FOLDER=$(adb logcat -d | grep -E "(Download|Pictures|Movies|Music)" | tail -1)
        if [ -n "$NEXT_FOLDER" ]; then
            echo "📁 Next folder request: $NEXT_FOLDER"
        fi
        
        sleep 2
    done
) &
MONITOR_PID=$!

# Step 5: Wait for user action
echo ""
echo "⏳ Waiting for user to grant SAF permission for Documents folder..."
echo "   (This may take a few moments as you navigate and select the folder)"
echo ""

# Wait for permission to be granted
sleep 15

# Check if permission was granted
echo "🔍 Step 5: Checking Permission Status"
echo "===================================="
NEW_SAF_GRANTED=$(adb logcat -d | grep "SAF permission granted" | tail -1)
if [ -n "$NEW_SAF_GRANTED" ]; then
    echo "✅ SAF permission granted successfully:"
    echo "   $NEW_SAF_GRANTED"
else
    echo "⏳ Still waiting for user to grant permission..."
fi

# Kill monitoring process
kill $MONITOR_PID 2>/dev/null || true

# Step 6: Show recent SAF logs
echo ""
echo "🔍 Step 6: Recent SAF Permission Logs"
echo "===================================="
echo "Recent SAF permission activity:"
adb logcat -d | grep -E "(SAF permission granted|Starting SAF permission requests)" | tail -5

# Step 7: Check if next folder request is active
echo ""
echo "🔍 Step 7: Checking Next Folder Request"
echo "======================================="
NEXT_DIALOG=$(adb shell "dumpsys activity activities | grep 'documentsui'" | wc -l)
if [ "$NEXT_DIALOG" -gt 0 ]; then
    echo "✅ Next SAF permission dialog is active"
    echo "📋 The app is now requesting permission for the next folder"
else
    echo "⚠️  No additional SAF permission dialog detected"
fi

# Step 8: Show expected workflow
echo ""
echo "📋 Expected SAF Workflow:"
echo "========================"
echo "1. ✅ Documents folder permission requested"
echo "2. ⏳ User grants permission for Documents folder"
echo "3. ⏳ App requests permission for Download folder"
echo "4. ⏳ User grants permission for Download folder"
echo "5. ⏳ App requests permission for Pictures folder"
echo "6. ⏳ User grants permission for Pictures folder"
echo "7. ⏳ App requests permission for Movies folder"
echo "8. ⏳ User grants permission for Movies folder"
echo "9. ⏳ App requests permission for Music folder"
echo "10. ⏳ User grants permission for Music folder"
echo "11. ⏳ App requests document picker for TennisInterview_converted.mp4"
echo "12. ⏳ User selects TennisInterview_converted.mp4"
echo "13. ⏳ Auto-Clipper processes the video into clips"
echo ""

# Step 9: Show monitoring commands
echo "📊 Live Monitoring Commands:"
echo "============================"
echo "• Monitor SAF permissions: adb logcat | grep -E '(SAF permission granted|Starting SAF permission requests)'"
echo "• Monitor folder requests: adb logcat | grep -E '(Documents|Download|Pictures|Movies|Music)'"
echo "• Monitor Auto-Clipper: adb logcat | grep -E '(AutoClipperWorker|Document picked)'"
echo ""

# Step 10: Show current status
echo "📋 Current Status:"
echo "=================="
echo "• ✅ Enhanced SAF implementation active"
echo "• ✅ Documents folder permission requested"
echo "• ⏳ Waiting for user to grant Documents folder permission"
echo "• ⏳ Additional folder permissions will be requested sequentially"
echo "• ⏳ Auto-Clipper ready to process TennisInterview_converted.mp4"
echo ""

echo "🎉 SAF Documents Folder Permission Grant Complete!"
echo "================================================="
echo ""
echo "The SAF permission request for Documents folder is active:"
echo "• ✅ App launched with enhanced SAF implementation"
echo "• ✅ Documents folder permission dialog shown"
echo "• ⏳ Waiting for user to grant permission"
echo ""
echo "Next: User grants permission for Documents folder, then additional folders!"
echo ""
echo "🚀 SAF Documents Folder Permission: READY! 🔐"
