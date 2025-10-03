#!/bin/bash

# Media3 AutoCutEngine Step-by-Step Test Script
# Comprehensive testing of Media3 video processing pipeline

echo "🎥 Media3 AutoCutEngine - Step-by-Step Test"
echo "============================================"
echo ""

DEVICE="050C188041A00540"
PACKAGE="com.mira.videoeditor.debug"
TEST_VIDEO="motion-test-video.mp4"

echo "📱 Device: Xiaomi Pad (25032RP42C)"
echo "📦 Package: $PACKAGE"
echo "🎬 Test Video: $TEST_VIDEO"
echo ""

# Function to monitor logs for specific patterns
monitor_logs() {
    local test_name="$1"
    local pattern="$2"
    local timeout="$3"
    
    echo "🔍 Monitoring $test_name..."
    echo "   Pattern: $pattern"
    echo "   Timeout: ${timeout}s"
    
    local start_time=$(date +%s)
    while true; do
        current_time=$(date +%s)
        elapsed=$((current_time - start_time))
        
        if [ $elapsed -gt $timeout ]; then
            echo "⏰ Timeout after ${timeout}s"
            break
        fi
        
        # Check for the pattern
        result=$(adb -s $DEVICE logcat -d | grep "$pattern" | tail -1)
        if [ ! -z "$result" ]; then
            echo "✅ Found: $result"
            break
        fi
        
        sleep 1
    done
}

# Function to get current memory usage
get_memory() {
    local memory=$(adb -s $DEVICE shell "dumpsys meminfo $PACKAGE | grep TOTAL" | awk '{print $2}')
    if [ ! -z "$memory" ]; then
        echo "${memory}KB"
    else
        echo "N/A"
    fi
}

# Function to check app status
check_app_status() {
    local running=$(adb -s $DEVICE shell "ps | grep $PACKAGE" | wc -l)
    if [ $running -gt 0 ]; then
        echo "✅ App is running"
        return 0
    else
        echo "❌ App not running"
        return 1
    fi
}

echo "🚀 Starting Media3 AutoCutEngine Step-by-Step Test"
echo "=================================================="
echo ""

# Step 1: Verify App Status
echo "📋 Step 1: Verify App Status"
echo "---------------------------"
check_app_status
if [ $? -ne 0 ]; then
    echo "❌ App not running, launching..."
    adb -s $DEVICE shell am start -n $PACKAGE/com.mira.videoeditor.MainActivity
    sleep 3
    check_app_status
fi
echo ""

# Step 2: Test Media3 Transformer Initialization
echo "📋 Step 2: Test Media3 Transformer Initialization"
echo "------------------------------------------------"
echo "🔍 Looking for Media3 Transformer creation logs..."
monitor_logs "Media3 Transformer Init" "Transformer" 10
echo ""

# Step 3: Test MediaItem Creation
echo "📋 Step 3: Test MediaItem Creation"
echo "---------------------------------"
echo "🔍 Looking for MediaItem creation logs..."
monitor_logs "MediaItem Creation" "MediaItem" 10
echo ""

# Step 4: Test AutoCutEngine Instantiation
echo "📋 Step 4: Test AutoCutEngine Instantiation"
echo "-------------------------------------------"
echo "🔍 Looking for AutoCutEngine creation logs..."
monitor_logs "AutoCutEngine Init" "AutoCutEngine" 10
echo ""

# Step 5: Test Video Analysis Pipeline
echo "📋 Step 5: Test Video Analysis Pipeline"
echo "--------------------------------------"
echo "🔍 Looking for VideoScorer analysis logs..."
monitor_logs "VideoScorer Analysis" "VideoScorer" 15
echo ""

# Step 6: Test Segment Processing
echo "📋 Step 6: Test Segment Processing"
echo "---------------------------------"
echo "🔍 Looking for segment processing logs..."
monitor_logs "Segment Processing" "segment" 20
echo ""

# Step 7: Test Video Export
echo "📋 Step 7: Test Video Export"
echo "----------------------------"
echo "🔍 Looking for video export logs..."
monitor_logs "Video Export" "export" 30
echo ""

# Step 8: Test Progress Callback System
echo "📋 Step 8: Test Progress Callback System"
echo "---------------------------------------"
echo "🔍 Looking for progress callback logs..."
monitor_logs "Progress Callbacks" "Progress callback" 15
echo ""

# Step 9: Performance Analysis
echo "📋 Step 9: Performance Analysis"
echo "-------------------------------"
echo "📊 Current Memory Usage: $(get_memory)"
echo "📊 App Status: $(check_app_status && echo "Running" || echo "Not Running")"
echo ""

# Step 10: Final Analysis
echo "📋 Step 10: Final Analysis"
echo "-------------------------"
echo "🔍 Analyzing all Media3-related logs..."

# Count different types of logs
TRANSFORMER_LOGS=$(adb -s $DEVICE logcat -d | grep -i "transformer" | wc -l)
MEDIAITEM_LOGS=$(adb -s $DEVICE logcat -d | grep -i "mediaitem" | wc -l)
AUTOCUT_LOGS=$(adb -s $DEVICE logcat -d | grep "AutoCutEngine" | wc -l)
VIDEOSCORER_LOGS=$(adb -s $DEVICE logcat -d | grep "VideoScorer" | wc -l)
PROGRESS_LOGS=$(adb -s $DEVICE logcat -d | grep "Progress callback" | wc -l)
EXPORT_LOGS=$(adb -s $DEVICE logcat -d | grep -i "export" | wc -l)

echo "📈 Media3 Log Analysis:"
echo "   - Transformer logs: $TRANSFORMER_LOGS"
echo "   - MediaItem logs: $MEDIAITEM_LOGS"
echo "   - AutoCutEngine logs: $AUTOCUT_LOGS"
echo "   - VideoScorer logs: $VIDEOSCORER_LOGS"
echo "   - Progress callback logs: $PROGRESS_LOGS"
echo "   - Export logs: $EXPORT_LOGS"
echo ""

# Success criteria evaluation
echo "✅ Media3 AutoCutEngine Test Results:"
echo "====================================="

if [ $AUTOCUT_LOGS -gt 0 ]; then
    echo "✅ AutoCutEngine Initialization: PASSED"
else
    echo "❌ AutoCutEngine Initialization: FAILED"
fi

if [ $VIDEOSCORER_LOGS -gt 0 ]; then
    echo "✅ Video Analysis Pipeline: PASSED"
else
    echo "❌ Video Analysis Pipeline: FAILED"
fi

if [ $PROGRESS_LOGS -gt 0 ]; then
    echo "✅ Progress Callback System: PASSED"
else
    echo "❌ Progress Callback System: FAILED"
fi

if [ $EXPORT_LOGS -gt 0 ]; then
    echo "✅ Video Export Functionality: PASSED"
else
    echo "❌ Video Export Functionality: FAILED"
fi

echo ""
echo "🎯 Media3 AutoCutEngine Step-by-Step Test Complete!"
echo "==================================================="
