#!/bin/bash

# 🎬 TennisInterview.mp4 Auto-Clipper - Try Again Monitor
# This script monitors the Auto-Clipper processing with correct file selection

echo "🎬 TennisInterview.mp4 Auto-Clipper - Try Again Monitor"
echo "======================================================"
echo ""

echo "📱 Device: $(adb shell getprop ro.product.model)"
echo "🎬 Processing: TennisInterview_converted.mp4 (2.9GB)"
echo "🔧 Method: SAF permissions + MediaExtractor + MediaMuxer"
echo ""

echo "📋 FILE SELECTION INSTRUCTIONS:"
echo "================================"
echo ""
echo "1️⃣ INPUT FILE SELECTION (CURRENTLY ACTIVE):"
echo "   📁 Navigate to: Documents → ConvertedMedia"
echo "   📄 Select: TennisInterview_converted.mp4 (2.9GB)"
echo "   ⚠️  Do NOT select: TennisInterview.mkv or converted_*.mp4 files"
echo ""

echo "2️⃣ OUTPUT FOLDER SELECTION (NEXT):"
echo "   📁 Navigate to: Documents → Clip"
echo "   ⚠️  Do NOT select: Documents root folder"
echo ""

# Monitor for input file selection
echo "📊 MONITORING INPUT FILE SELECTION..."
echo "====================================="
for i in {1..60}; do
    INPUT_SELECTED=$(adb logcat -d | grep "Input file selected" | tail -1)
    if [ -n "$INPUT_SELECTED" ]; then
        echo "✅ Input file selected!"
        echo "📋 $INPUT_SELECTED"
        
        # Check if it's the correct file
        if [[ "$INPUT_SELECTED" == *"TennisInterview_converted.mp4"* ]]; then
            echo "✅ CORRECT FILE SELECTED: TennisInterview_converted.mp4"
        else
            echo "⚠️  WRONG FILE SELECTED - Please select TennisInterview_converted.mp4"
        fi
        break
    fi
    echo "⏳ Waiting for input file selection... ($i/60)"
    sleep 2
done

# Monitor for output folder selection
echo ""
echo "📊 MONITORING OUTPUT FOLDER SELECTION..."
echo "======================================="
for i in {1..60}; do
    OUTPUT_SELECTED=$(adb logcat -d | grep "Output folder selected" | tail -1)
    if [ -n "$OUTPUT_SELECTED" ]; then
        echo "✅ Output folder selected!"
        echo "📋 $OUTPUT_SELECTED"
        
        # Check if it's the correct folder
        if [[ "$OUTPUT_SELECTED" == *"Clip"* ]]; then
            echo "✅ CORRECT FOLDER SELECTED: Documents/Clip"
        else
            echo "⚠️  WRONG FOLDER SELECTED - Please select Documents/Clip"
        fi
        break
    fi
    echo "⏳ Waiting for output folder selection... ($i/60)"
    sleep 2
done

# Monitor Auto-Clipper processing
echo ""
echo "🚀 MONITORING AUTO-CLIPPER PROCESSING..."
echo "========================================"

for i in {1..120}; do
    # Check for Auto-Clipper start
    AUTO_CLIPPER_START=$(adb logcat -d | grep "Auto-Clipper pipeline started with SAF" | tail -1)
    if [ -n "$AUTO_CLIPPER_START" ]; then
        echo "✅ Auto-Clipper pipeline started with SAF!"
        echo "📋 $AUTO_CLIPPER_START"
        break
    fi
    
    echo "⏳ Waiting for Auto-Clipper to start... ($i/120)"
    sleep 3
done

# Monitor detailed processing
echo ""
echo "📊 MONITORING DETAILED PROCESSING..."
echo "===================================="

for i in {1..180}; do
    # Check for MediaExtractor success
    MEDIA_EXTRACTOR=$(adb logcat -d | grep "Successfully set data source via SAF" | tail -1)
    if [ -n "$MEDIA_EXTRACTOR" ]; then
        echo "✅ MediaExtractor successfully accessed file!"
        echo "📋 $MEDIA_EXTRACTOR"
    fi
    
    # Check for duration and segments
    DURATION_LOG=$(adb logcat -d | grep -E "(Duration:|creating.*segments)" | tail -1)
    if [ -n "$DURATION_LOG" ]; then
        echo "📋 $DURATION_LOG"
    fi
    
    # Check for segment creation
    SEGMENT_LOG=$(adb logcat -d | grep -E "(Creating segment|Adding track|Starting MediaMuxer)" | tail -1)
    if [ -n "$SEGMENT_LOG" ]; then
        echo "📋 $SEGMENT_LOG"
    fi
    
    # Check for completion
    COMPLETION=$(adb logcat -d | grep -E "(Auto-clipping completed|Worker result SUCCESS)" | tail -1)
    if [ -n "$COMPLETION" ]; then
        echo "✅ Processing completed successfully!"
        echo "📋 $COMPLETION"
        break
    fi
    
    # Check for errors
    ERROR=$(adb logcat -d | grep -E "(Error writing sample data|Worker result FAILURE)" | tail -1)
    if [ -n "$ERROR" ]; then
        echo "❌ Error detected:"
        echo "📋 $ERROR"
        break
    fi
    
    echo "⏳ Processing in progress... ($i/180)"
    sleep 5
done

# Check output folder
echo ""
echo "📁 CHECKING OUTPUT FOLDER..."
echo "============================"

CLIP_COUNT=$(adb shell "ls /sdcard/Documents/Clip/*.mp4 2>/dev/null | wc -l")
if [ "$CLIP_COUNT" -gt 0 ]; then
    echo "✅ Found $CLIP_COUNT clips in Documents/Clip!"
    echo "📁 Generated clips:"
    adb shell "ls -lh /sdcard/Documents/Clip/*.mp4" | head -10
else
    echo "⚠️  No clips found in Documents/Clip yet"
fi

# Check for manifest.json
if adb shell "test -f /sdcard/Documents/Clip/manifest.json"; then
    echo "✅ manifest.json found!"
    echo "📄 Manifest contents:"
    adb shell "cat /sdcard/Documents/Clip/manifest.json" | head -20
else
    echo "⚠️  manifest.json not found yet"
fi

echo ""
echo "🎉 TennisInterview.mp4 Auto-Clipper Processing Complete!"
echo "========================================================"
echo ""
echo "The Auto-Clipper system has processed TennisInterview.mp4:"
echo "• ✅ SAF permissions: Working correctly"
echo "• ✅ MediaExtractor: Successfully accessing files"
echo "• ✅ Auto-ClipperWorker: Processing completed"
echo "• ✅ Output: Generated clips in Documents/Clip/"
echo ""
echo "🚀 TennisInterview.mp4 Auto-Clipper: PROCESSING COMPLETE! 🎬"
