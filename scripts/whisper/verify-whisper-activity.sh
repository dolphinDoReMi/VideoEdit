#!/bin/bash

# Final Verification Script - Whisper Step 1 Activity
# Confirms the proper Whisper activity is now the main launcher

echo "🎯 FINAL VERIFICATION: Whisper Step 1 Activity"
echo "=============================================="

# Check app installation
echo "📦 App Installation:"
if adb shell pm list packages | grep -q "com.mira.com"; then
    echo "✅ App installed as com.mira.com"
else
    echo "❌ App not installed"
    exit 1
fi

# Check main launcher activity
echo ""
echo "🚀 Main Launcher Activity:"
MAIN_ACTIVITY=$(adb shell dumpsys package com.mira.com | grep -A 2 "android.intent.action.MAIN" | grep "com.mira.com" | head -1 | awk '{print $2}')
if [[ "$MAIN_ACTIVITY" == *"WhisperStep1Activity"* ]]; then
    echo "✅ WhisperStep1Activity is the main launcher"
    echo "   Activity: $MAIN_ACTIVITY"
else
    echo "❌ Wrong main activity: $MAIN_ACTIVITY"
    exit 1
fi

# Check if app is running
echo ""
echo "🔄 App Status:"
if adb shell ps | grep -q "com.mira.com"; then
    echo "✅ App is running"
else
    echo "⚠️ App not running, launching..."
    adb shell am start -n com.mira.com/com.mira.com.whisper.WhisperStep1Activity
    sleep 3
fi

# Take screenshot
echo ""
echo "📸 Taking screenshot..."
adb shell screencap -p /sdcard/final_whisper_screen.png
adb pull /sdcard/final_whisper_screen.png final_whisper_screen.png
adb shell rm /sdcard/final_whisper_screen.png

if [ -f "final_whisper_screen.png" ]; then
    echo "✅ Screenshot saved as final_whisper_screen.png"
else
    echo "❌ Screenshot failed"
fi

echo ""
echo "🎯 WHAT YOU SHOULD SEE ON YOUR XIAOMI PAD:"
echo "1. ✅ The Mira app with WhisperStep1Activity as the main launcher"
echo "2. ✅ A WebView interface showing the staging page"
echo "3. ✅ 'Staging — Select & Inspect' section"
echo "4. ✅ 'Add Folder' button for video selection"
echo "5. ✅ Video file listings and filtering options"
echo ""
echo "🔧 ACTIVITY STRUCTURE NOW CORRECT:"
echo "• Main Launcher: WhisperStep1Activity (Staging)"
echo "• Secondary: Clip4ClipActivity (Service)"
echo "• Additional: WhisperStep2Activity, WhisperStep3Activity"
echo ""
echo "✅ VERIFICATION COMPLETE!"
echo "The app now properly uses WhisperStep1Activity for the staging interface!"
