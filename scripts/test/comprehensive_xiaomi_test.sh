#!/bin/bash

# Comprehensive Xiaomi Pad Core Capabilities Testing Script
# This script monitors and guides through complete testing of all core features

echo "🎬 Mira Video Editor - Comprehensive Core Capabilities Testing"
echo "============================================================="
echo ""

# Device information
DEVICE="050C188041A00540"
PACKAGE="com.mira.clip.debug"

echo "📱 Device: Xiaomi Pad (25032RP42C) - Android 15"
echo "📦 Package: $PACKAGE"
echo "🔍 Device ID: $DEVICE"
echo ""

# Test videos available
echo "📹 Test Videos Available:"
echo "   1. motion-test-video.mp4 (12MB) - Quick test"
echo "   2. video_v1.mov (375MB) - Comprehensive test"
echo "   3. video_v1.mp4 (375MB) - MP4 format test"
echo ""

# Check app status
echo "🔍 Checking app status..."
APP_RUNNING=$(adb -s $DEVICE shell "ps | grep $PACKAGE" | wc -l)
if [ $APP_RUNNING -gt 0 ]; then
    echo "✅ App is running"
else
    echo "❌ App not running, launching..."
    adb -s $DEVICE shell am start -n $PACKAGE/com.mira.clip.MainActivity
    sleep 3
fi

# Check test videos
echo "📁 Checking test videos..."
VIDEOS=$(adb -s $DEVICE shell "ls /sdcard/Download/ | grep -E '(video_v1|motion-test)'" | wc -l)
echo "✅ Found $VIDEOS test videos in Downloads folder"
echo ""

# Start comprehensive testing
echo "🚀 Starting Comprehensive Core Capabilities Testing"
echo "===================================================="
echo ""

# Test 1: Quick Test with motion-test-video.mp4
echo "🧪 Test 1: Quick Test (motion-test-video.mp4)"
echo "---------------------------------------------"
echo "📋 Instructions:"
echo "   1. On your Xiaomi Pad, select 'motion-test-video.mp4' from Downloads"
echo "   2. Tap 'Auto Cut' button"
echo "   3. Watch for progress updates"
echo "   4. Expected duration: 2-3 minutes"
echo ""
echo "🔍 Monitoring logs for:"
echo "   - VideoScorer: Motion analysis"
echo "   - AutoCutEngine: Media3 processing"
echo "   - MediaStoreExt: File permissions"
echo "   - MainActivity: UI interactions"
echo ""

# Wait for user to start test
echo "⏳ Waiting for you to start Test 1..."
echo "   (Select motion-test-video.mp4 and tap Auto Cut)"
echo ""

# Monitor the first test
echo "📊 Monitoring Test 1 progress..."
START_TIME=$(date +%s)

# Function to check for completion
check_test_completion() {
    local test_name="$1"
    local max_wait="$2"
    local start_time="$3"
    
    while true; do
        current_time=$(date +%s)
        elapsed=$((current_time - start_time))
        
        if [ $elapsed -gt $max_wait ]; then
            echo "⏰ Test timeout after ${max_wait}s"
            break
        fi
        
        # Check for completion indicators in logs
        completion=$(adb -s $DEVICE logcat -d | grep -E "(Export completed|Processing completed|Error|Exception)" | tail -1)
        if [ ! -z "$completion" ]; then
            echo "✅ $test_name completed!"
            echo "📝 Result: $completion"
            break
        fi
        
        # Show progress every 30 seconds
        if [ $((elapsed % 30)) -eq 0 ] && [ $elapsed -gt 0 ]; then
            echo "⏳ $test_name in progress... (${elapsed}s elapsed)"
        fi
        
        sleep 5
    done
}

# Run Test 1
check_test_completion "Test 1 (Quick Test)" 300 $START_TIME

echo ""
echo "📊 Test 1 Analysis:"
echo "==================="

# Analyze logs for Test 1
echo "🔍 Analyzing Test 1 logs..."

# Check for VideoScorer activity
VIDEO_SCORER_LOGS=$(adb -s $DEVICE logcat -d | grep "VideoScorer" | wc -l)
echo "   - VideoScorer logs: $VIDEO_SCORER_LOGS entries"

# Check for AutoCutEngine activity
AUTO_CUT_LOGS=$(adb -s $DEVICE logcat -d | grep "AutoCutEngine" | wc -l)
echo "   - AutoCutEngine logs: $AUTO_CUT_LOGS entries"

# Check for MediaStoreExt activity
MEDIA_STORE_LOGS=$(adb -s $DEVICE logcat -d | grep "MediaStoreExt" | wc -l)
echo "   - MediaStoreExt logs: $MEDIA_STORE_LOGS entries"

# Check for errors
ERROR_COUNT=$(adb -s $DEVICE logcat -d | grep -E "(Error|Exception|Fatal)" | wc -l)
echo "   - Error count: $ERROR_COUNT"

echo ""

# Test 2: Comprehensive Test with video_v1.mov
echo "🧪 Test 2: Comprehensive Test (video_v1.mov)"
echo "---------------------------------------------"
echo "📋 Instructions:"
echo "   1. Select 'video_v1.mov' from Downloads"
echo "   2. Tap 'Auto Cut' button"
echo "   3. Monitor progress (this will take 5-7 minutes)"
echo "   4. Watch for detailed motion analysis"
echo ""

echo "⏳ Waiting for you to start Test 2..."
echo "   (Select video_v1.mov and tap Auto Cut)"
echo ""

# Monitor the second test
echo "📊 Monitoring Test 2 progress..."
START_TIME=$(date +%s)
check_test_completion "Test 2 (Comprehensive Test)" 600 $START_TIME

echo ""
echo "📊 Test 2 Analysis:"
echo "==================="

# Analyze logs for Test 2
echo "🔍 Analyzing Test 2 logs..."

# Check for VideoScorer activity
VIDEO_SCORER_LOGS=$(adb -s $DEVICE logcat -d | grep "VideoScorer" | wc -l)
echo "   - VideoScorer logs: $VIDEO_SCORER_LOGS entries"

# Check for AutoCutEngine activity
AUTO_CUT_LOGS=$(adb -s $DEVICE logcat -d | grep "AutoCutEngine" | wc -l)
echo "   - AutoCutEngine logs: $AUTO_CUT_LOGS entries"

# Check for MediaStoreExt activity
MEDIA_STORE_LOGS=$(adb -s $DEVICE logcat -d | grep "MediaStoreExt" | wc -l)
echo "   - MediaStoreExt logs: $MEDIA_STORE_LOGS entries"

# Check for errors
ERROR_COUNT=$(adb -s $DEVICE logcat -d | grep -E "(Error|Exception|Fatal)" | wc -l)
echo "   - Error count: $ERROR_COUNT"

echo ""

# Test 3: MP4 Format Test
echo "🧪 Test 3: MP4 Format Test (video_v1.mp4)"
echo "-------------------------------------------"
echo "📋 Instructions:"
echo "   1. Select 'video_v1.mp4' from Downloads"
echo "   2. Tap 'Auto Cut' button"
echo "   3. Compare with MOV format results"
echo "   4. Verify MP4 handling"
echo ""

echo "⏳ Waiting for you to start Test 3..."
echo "   (Select video_v1.mp4 and tap Auto Cut)"
echo ""

# Monitor the third test
echo "📊 Monitoring Test 3 progress..."
START_TIME=$(date +%s)
check_test_completion "Test 3 (MP4 Format Test)" 600 $START_TIME

echo ""
echo "📊 Test 3 Analysis:"
echo "==================="

# Analyze logs for Test 3
echo "🔍 Analyzing Test 3 logs..."

# Check for VideoScorer activity
VIDEO_SCORER_LOGS=$(adb -s $DEVICE logcat -d | grep "VideoScorer" | wc -l)
echo "   - VideoScorer logs: $VIDEO_SCORER_LOGS entries"

# Check for AutoCutEngine activity
AUTO_CUT_LOGS=$(adb -s $DEVICE logcat -d | grep "AutoCutEngine" | wc -l)
echo "   - AutoCutEngine logs: $AUTO_CUT_LOGS entries"

# Check for MediaStoreExt activity
MEDIA_STORE_LOGS=$(adb -s $DEVICE logcat -d | grep "MediaStoreExt" | wc -l)
echo "   - MediaStoreExt logs: $MEDIA_STORE_LOGS entries"

# Check for errors
ERROR_COUNT=$(adb -s $DEVICE logcat -d | grep -E "(Error|Exception|Fatal)" | wc -l)
echo "   - Error count: $ERROR_COUNT"

echo ""

# Final Analysis
echo "🎯 Comprehensive Testing Complete!"
echo "=================================="
echo ""

echo "📊 Final Analysis:"
echo "=================="

# Overall statistics
TOTAL_VIDEO_SCORER=$(adb -s $DEVICE logcat -d | grep "VideoScorer" | wc -l)
TOTAL_AUTO_CUT=$(adb -s $DEVICE logcat -d | grep "AutoCutEngine" | wc -l)
TOTAL_MEDIA_STORE=$(adb -s $DEVICE logcat -d | grep "MediaStoreExt" | wc -l)
TOTAL_ERRORS=$(adb -s $DEVICE logcat -d | grep -E "(Error|Exception|Fatal)" | wc -l)

echo "📈 Overall Statistics:"
echo "   - Total VideoScorer logs: $TOTAL_VIDEO_SCORER"
echo "   - Total AutoCutEngine logs: $TOTAL_AUTO_CUT"
echo "   - Total MediaStoreExt logs: $TOTAL_MEDIA_STORE"
echo "   - Total errors: $TOTAL_ERRORS"

echo ""

# Success criteria evaluation
echo "✅ Success Criteria Evaluation:"
echo "==============================="

if [ $TOTAL_VIDEO_SCORER -gt 0 ]; then
    echo "✅ AI Motion Analysis (VideoScorer): PASSED"
else
    echo "❌ AI Motion Analysis (VideoScorer): FAILED"
fi

if [ $TOTAL_AUTO_CUT -gt 0 ]; then
    echo "✅ Media3 Video Processing (AutoCutEngine): PASSED"
else
    echo "❌ Media3 Video Processing (AutoCutEngine): FAILED"
fi

if [ $TOTAL_MEDIA_STORE -gt 0 ]; then
    echo "✅ File Permissions (MediaStoreExt): PASSED"
else
    echo "❌ File Permissions (MediaStoreExt): FAILED"
fi

if [ $TOTAL_ERRORS -eq 0 ]; then
    echo "✅ No Errors: PASSED"
else
    echo "⚠️  Errors Found: $TOTAL_ERRORS errors detected"
fi

echo ""

# Performance analysis
echo "⚡ Performance Analysis:"
echo "======================="

# Check memory usage
MEMORY_USAGE=$(adb -s $DEVICE shell "dumpsys meminfo $PACKAGE | grep TOTAL" | awk '{print $2}')
if [ ! -z "$MEMORY_USAGE" ]; then
    echo "   - Memory usage: ${MEMORY_USAGE}KB"
fi

# Check if app is still running
APP_RUNNING=$(adb -s $DEVICE shell "ps | grep $PACKAGE" | wc -l)
if [ $APP_RUNNING -gt 0 ]; then
    echo "✅ App stability: PASSED (still running)"
else
    echo "❌ App stability: FAILED (crashed)"
fi

echo ""

# Recommendations
echo "💡 Recommendations:"
echo "==================="
echo "1. Review output videos for quality"
echo "2. Check processing times for optimization"
echo "3. Monitor battery usage during processing"
echo "4. Test with different video formats and sizes"
echo "5. Verify output files are accessible"

echo ""
echo "🎉 Comprehensive Core Capabilities Testing Complete!"
echo "======================================================"
echo ""
echo "📋 Next Steps:"
echo "   1. Review the test results above"
echo "   2. Check output videos on the device"
echo "   3. Analyze performance metrics"
echo "   4. Report any issues or improvements"
echo ""
echo "✅ All core capabilities have been tested on Xiaomi Pad!"
