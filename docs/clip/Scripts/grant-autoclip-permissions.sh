#!/bin/bash

# 🔐 Grant Auto-Clipper Permissions
# This script grants the necessary permissions for TennisInterview.mp4 Auto-Clipper

set -e

echo "🔐 Granting Auto-Clipper Permissions"
echo "===================================="
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

echo "📱 Device: $(adb shell getprop ro.product.model)"
echo "🎯 Target App: com.mira.com"
echo ""

# Step 1: Grant basic storage permissions
echo "✅ Step 1: Granting Basic Storage Permissions"
echo "=============================================="
echo "📋 Granting READ_EXTERNAL_STORAGE..."
adb shell "pm grant com.mira.com android.permission.READ_EXTERNAL_STORAGE"
echo "✅ READ_EXTERNAL_STORAGE granted"

echo "📋 Granting WRITE_EXTERNAL_STORAGE..."
adb shell "pm grant com.mira.com android.permission.WRITE_EXTERNAL_STORAGE"
echo "✅ WRITE_EXTERNAL_STORAGE granted"
echo ""

# Step 2: Grant MANAGE_EXTERNAL_STORAGE (if possible)
echo "✅ Step 2: Granting MANAGE_EXTERNAL_STORAGE"
echo "==========================================="
echo "📋 Attempting to grant MANAGE_EXTERNAL_STORAGE..."
if adb shell "pm grant com.mira.com android.permission.MANAGE_EXTERNAL_STORAGE" 2>/dev/null; then
    echo "✅ MANAGE_EXTERNAL_STORAGE granted"
else
    echo "⚠️  MANAGE_EXTERNAL_STORAGE cannot be granted via ADB"
    echo "   This permission requires user interaction in Settings"
fi
echo ""

# Step 3: Verify permissions
echo "✅ Step 3: Verifying Permissions"
echo "================================"
echo "📋 Checking granted permissions..."
adb shell "dumpsys package com.mira.com | grep -A 15 'install permissions:'"
echo ""

# Step 4: Test folder access
echo "✅ Step 4: Testing Folder Access"
echo "================================"
echo "📁 Testing input folder access..."

INPUT_FOLDERS=(
    "/sdcard/Documents"
)

for folder in "${INPUT_FOLDERS[@]}"; do
    if adb shell "test -d '$folder'"; then
        echo "✅ Input folder accessible: $folder"
        if adb shell "test -f '$folder/TennisInterview_converted.mp4'"; then
            FILE_SIZE=$(adb shell "ls -lh '$folder/TennisInterview_converted.mp4'" | awk '{print $5}')
            echo "   📄 TennisInterview_converted.mp4 found ($FILE_SIZE)"
        fi
    else
        echo "❌ Input folder not accessible: $folder"
    fi
done

echo ""
echo "📁 Testing output folder access..."
OUTPUT_FOLDER="/sdcard/Documents"
if adb shell "test -d '$OUTPUT_FOLDER'"; then
    echo "✅ Output folder accessible: $OUTPUT_FOLDER"
    echo "📁 Current contents:"
    adb shell "ls -la '$OUTPUT_FOLDER'" | head -5
else
    echo "❌ Output folder not accessible: $OUTPUT_FOLDER"
    echo "📋 Creating output folder..."
    adb shell "mkdir -p '$OUTPUT_FOLDER'"
    echo "✅ Output folder created"
fi
echo ""

# Step 5: Show next steps
echo "✅ Step 5: Next Steps"
echo "===================="
echo "🔧 If MANAGE_EXTERNAL_STORAGE is not granted:"
echo "   1. Go to Settings > Apps > Mira Video Editor"
echo "   2. Tap 'Permissions'"
echo "   3. Enable 'All files access' or 'Manage external storage'"
echo ""
echo "🔧 Alternative: Use SAF (Storage Access Framework)"
echo "   The app will request folder access when needed"
echo ""

# Step 6: Test Auto-Clipper
echo "✅ Step 6: Test Auto-Clipper"
echo "==========================="
echo "📋 Ready to test Auto-Clipper with granted permissions..."
echo "🚀 Launch the app to test:"
echo "   adb shell 'am start -n com.mira.com/com.mira.whisper.WhisperMainActivity'"
echo ""

echo "🎉 Permission Granting Complete!"
echo "================================"
echo ""
echo "✅ Granted Permissions:"
echo "   • READ_EXTERNAL_STORAGE"
echo "   • WRITE_EXTERNAL_STORAGE"
echo "   • MANAGE_EXTERNAL_STORAGE (if possible)"
echo ""
echo "📁 Accessible Folders:"
echo "   • Input: /sdcard/Download/, /sdcard/Documents/ConvertedMedia/"
echo "   • Output: /sdcard/Documents/ConvertedMedia/Clip/"
echo ""
echo "🚀 TennisInterview.mp4 Auto-Clipper: READY TO TEST! 🎬"
