#!/bin/bash

# Test script for Whisper Step-2 UI integration
# This script builds the app and provides instructions for testing

echo "=== Whisper Step-2 UI Integration Test ==="
echo

# Check if we're in the right directory
if [ ! -f "gradlew" ]; then
    echo "Error: Please run this script from the project root directory"
    exit 1
fi

echo "1. Building the Android app..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✓ Build successful!"
else
    echo "✗ Build failed!"
    exit 1
fi

echo
echo "2. Installation and testing instructions:"
echo "   a) Install the app on your device:"
echo "      adb install app/build/outputs/apk/debug/app-debug.apk"
echo
echo "   b) Launch the app and test the Whisper Step-2 UI:"
echo "      - The app will start with the main processing UI"
echo "      - You can call AndroidInterface.openWhisperStep2() from the WebView console"
echo "      - Or modify the HTML to add a button that calls this method"
echo
echo "3. Testing the AndroidWhisper bridge:"
echo "   Once in the Whisper Step-2 UI, test these JavaScript calls:"
echo "   - window.AndroidWhisper.run('{\"uri\":\"file:///sdcard/test.mp4\",\"preset\":\"Single\",\"modelPath\":\"/sdcard/models/ggml-small-q5_1.bin\"}')"
echo "   - window.AndroidWhisper.listSidecars()"
echo "   - window.AndroidWhisper.verify('job_id_here')"
echo "   - window.AndroidWhisper.export('job_id_here')"
echo
echo "4. Expected behavior:"
echo "   - The UI should load without errors"
echo "   - The AndroidWhisper bridge should be available"
echo "   - Mock sidecar files should be created in /sdcard/MiraWhisper/sidecars/"
echo "   - Export operations should create files in /sdcard/MiraWhisper/out/"
echo
echo "5. Debugging:"
echo "   - Check logcat for 'AndroidWhisperBridge' and 'WhisperReceiver' logs"
echo "   - Use 'adb logcat | grep -E \"(AndroidWhisper|Whisper)\"' to filter logs"
echo
echo "=== Test setup complete ==="
