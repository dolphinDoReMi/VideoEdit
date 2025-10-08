#!/bin/bash

# Test Auto-Clipper with TennisInterview.mp4
# This script triggers the Auto-Clipper pipeline

set -e

echo "🚀 Testing Auto-Clipper with TennisInterview.mp4"
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

# Input and output URIs
INPUT_URI="file:///sdcard/Documents/TennisInterview_converted.mp4"
OUTPUT_URI="content://com.android.externalstorage.documents/tree/primary%3ADocuments"

echo "📁 Input: $INPUT_URI"
echo "📁 Output: $OUTPUT_URI"
echo ""

# Trigger Auto-Clipper
echo "📡 Triggering Auto-Clipper pipeline..."
adb shell "am broadcast -a com.mira.clip.PROCESS_TENNIS_INTERVIEW \
    --es 'input_uri' '$INPUT_URI' \
    --es 'output_uri' '$OUTPUT_URI'"

if [ $? -eq 0 ]; then
    echo "✅ Auto-Clipper broadcast sent successfully"
    echo ""
    echo "📊 Monitoring progress..."
    echo "   Check logs: adb logcat -d | grep -E '(AutoClipperWorker|WhisperChainWorker)' | tail -20"
    echo ""
    echo "⏳ The pipeline will:"
    echo "   1. Split TennisInterview.mp4 into 10-minute clips"
    echo "   2. Create manifest.json with clip metadata"
    echo "   3. Chain Whisper processing for each clip"
    echo "   4. Store results in /sdcard/MiraWhisper/clips/"
    echo ""
    echo "🔍 To monitor live progress:"
    echo "   adb logcat | grep -E '(AutoClipperWorker|WhisperChainWorker)'"
else
    echo "❌ Failed to send Auto-Clipper broadcast"
    exit 1
fi
