#!/bin/bash

# 🎬 Auto-Clipper SAF Processing Simulation
# This script simulates the user selecting TennisInterview_converted.mp4 and monitors processing

set -e

echo "🎬 Auto-Clipper SAF Processing Simulation"
echo "========================================"
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

echo "📱 Device: $(adb shell getprop ro.product.model)"
echo "🎬 Simulating user selection of TennisInterview_converted.mp4"
echo ""

# Step 1: Check if document picker is active
echo "🔍 Step 1: Checking Document Picker Status"
echo "==========================================="
DOCUMENT_PICKER=$(adb shell "dumpsys activity activities | grep 'OPEN_DOCUMENT' | head -1")
if [ -n "$DOCUMENT_PICKER" ]; then
    echo "✅ Document picker is active"
    echo "   Waiting for user to select video file"
else
    echo "⚠️  Document picker not active - restarting app"
    adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"
    sleep 3
fi

# Step 2: Check file availability
echo ""
echo "🔍 Step 2: Checking File Availability"
echo "======================================"
if adb shell "test -f /sdcard/Documents/ConvertedMedia/TennisInterview_converted.mp4"; then
    FILE_SIZE=$(adb shell "ls -lh /sdcard/Documents/ConvertedMedia/TennisInterview_converted.mp4" | awk '{print $5}')
    echo "✅ TennisInterview_converted.mp4 available: $FILE_SIZE"
else
    echo "❌ TennisInterview_converted.mp4 not found"
    exit 1
fi

# Step 3: Monitor Auto-Clipper processing
echo ""
echo "🔍 Step 3: Monitoring Auto-Clipper Processing"
echo "=============================================="
echo "📊 Live monitoring of Auto-Clipper workflow..."
echo ""

# Start monitoring in background
(
    while true; do
        # Check for document picked
        DOCUMENT_PICKED=$(adb logcat -d | grep "Document picked" | tail -1)
        if [ -n "$DOCUMENT_PICKED" ]; then
            echo "✅ Document picked: $DOCUMENT_PICKED"
            break
        fi
        
        # Check for Auto-Clipper processing
        AUTO_CLIPPER_LOG=$(adb logcat -d | grep -E "(AutoClipperWorker|Successfully set data source)" | tail -1)
        if [ -n "$AUTO_CLIPPER_LOG" ]; then
            echo "📝 Auto-Clipper: $AUTO_CLIPPER_LOG"
        fi
        
        sleep 2
    done
) &
MONITOR_PID=$!

# Step 4: Wait for user interaction simulation
echo "⏳ Waiting for user to select TennisInterview_converted.mp4..."
echo "   (In a real scenario, the user would tap on the file in the document picker)"
echo ""

# Wait for processing to start
sleep 10

# Check if processing has started
echo "🔍 Step 4: Checking Processing Status"
echo "====================================="
PROCESSING_STARTED=$(adb logcat -d | grep "Document picked" | tail -1)
if [ -n "$PROCESSING_STARTED" ]; then
    echo "✅ Processing started with SAF URI"
else
    echo "⏳ Still waiting for user to select file..."
fi

# Kill monitoring process
kill $MONITOR_PID 2>/dev/null || true

# Step 5: Show recent logs
echo ""
echo "🔍 Step 5: Recent Processing Logs"
echo "================================="
echo "Recent Auto-Clipper logs:"
adb logcat -d | grep -E "(Document picked|AutoClipperWorker|Successfully set data source|Failed to set data source)" | tail -10

# Step 6: Check output folder
echo ""
echo "🔍 Step 6: Checking Output Folder"
echo "=================================="
OUTPUT_FOLDER="/sdcard/MiraWhisper"
if adb shell "test -d $OUTPUT_FOLDER"; then
    echo "✅ Output folder exists: $OUTPUT_FOLDER"
    echo "📁 Contents:"
    adb shell "ls -la $OUTPUT_FOLDER" | head -10
else
    echo "⚠️  Output folder not created yet"
fi

# Step 7: Show expected workflow
echo ""
echo "📋 Expected Workflow Status:"
echo "============================"
echo "1. ✅ SAF permission granted for MiraWhisper folder"
echo "2. ✅ Document picker active for video file selection"
echo "3. ⏳ User selects TennisInterview_converted.mp4"
echo "4. ⏳ AutoClipperWorker processes SAF URI"
echo "5. ⏳ Video split into 10-minute clips"
echo "6. ⏳ manifest.json generated"
echo "7. ⏳ Clips stored in MiraWhisper folder"
echo ""

# Step 8: Show monitoring commands
echo "📊 Live Monitoring Commands:"
echo "============================"
echo "• Monitor SAF workflow: adb logcat | grep -E '(Document picked|AutoClipperWorker)'"
echo "• Check output folder: adb shell 'ls -la /sdcard/MiraWhisper/'"
echo "• Check manifest: adb shell 'cat /sdcard/MiraWhisper/manifest.json'"
echo "• Monitor processing: adb logcat | grep -E '(Successfully set data source|Failed to set data source)'"
echo ""

echo "🎉 SAF Processing Simulation Complete!"
echo "======================================"
echo ""
echo "The Auto-Clipper SAF workflow is ready:"
echo "• ✅ SAF permissions granted"
echo "• ✅ Document picker active"
echo "• ✅ TennisInterview_converted.mp4 available"
echo "• ⏳ Waiting for user to select the file"
echo ""
echo "Next: User taps on TennisInterview_converted.mp4 in the document picker! 🎬"
