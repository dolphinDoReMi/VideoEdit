#!/bin/bash

# Media3 AutoCutEngine Detailed Step-by-Step Test
# This test will monitor the actual processing when triggered

echo "🎥 Media3 AutoCutEngine - Detailed Step-by-Step Test"
echo "===================================================="
echo ""

DEVICE="050C188041A00540"
PACKAGE="com.mira.videoeditor.debug"

echo "📱 Device: Xiaomi Pad (25032RP42C)"
echo "📦 Package: $PACKAGE"
echo ""

# Function to monitor logs in real-time
monitor_realtime_logs() {
    local test_name="$1"
    local pattern="$2"
    local duration="$3"
    
    echo "🔍 Real-time monitoring: $test_name"
    echo "   Pattern: $pattern"
    echo "   Duration: ${duration}s"
    echo "   Starting monitoring..."
    
    local start_time=$(date +%s)
    local found_count=0
    
    while true; do
        current_time=$(date +%s)
        elapsed=$((current_time - start_time))
        
        if [ $elapsed -gt $duration ]; then
            echo "⏰ Monitoring completed after ${duration}s"
            break
        fi
        
        # Check for the pattern in recent logs
        result=$(adb -s $DEVICE logcat -d | grep "$pattern" | tail -1)
        if [ ! -z "$result" ]; then
            found_count=$((found_count + 1))
            echo "✅ Found ($found_count): $result"
        fi
        
        sleep 2
    done
    
    echo "📊 Total matches found: $found_count"
    echo ""
}

# Function to get detailed memory info
get_detailed_memory() {
    echo "📊 Detailed Memory Analysis:"
    adb -s $DEVICE shell "dumpsys meminfo $PACKAGE | grep -E '(TOTAL|Native|Unknown|TOTAL PSS)'"
    echo ""
}

# Function to check Media3 components
check_media3_components() {
    echo "🔍 Checking Media3 Components:"
    
    # Check for Media3 classes in memory
    local media3_classes=$(adb -s $DEVICE shell "dumpsys meminfo $PACKAGE | grep -i media3" | wc -l)
    echo "   - Media3 classes in memory: $media3_classes"
    
    # Check for Transformer classes
    local transformer_classes=$(adb -s $DEVICE shell "dumpsys meminfo $PACKAGE | grep -i transformer" | wc -l)
    echo "   - Transformer classes in memory: $transformer_classes"
    
    # Check for ExoPlayer classes
    local exoplayer_classes=$(adb -s $DEVICE shell "dumpsys meminfo $PACKAGE | grep -i exoplayer" | wc -l)
    echo "   - ExoPlayer classes in memory: $exoplayer_classes"
    
    echo ""
}

echo "🚀 Starting Detailed Media3 AutoCutEngine Test"
echo "=============================================="
echo ""

# Step 1: Pre-processing Analysis
echo "📋 Step 1: Pre-processing Analysis"
echo "---------------------------------"
get_detailed_memory
check_media3_components
echo ""

# Step 2: Start Real-time Monitoring
echo "📋 Step 2: Start Real-time Monitoring"
echo "-------------------------------------"
echo "⚠️  IMPORTANT: Now trigger video processing on your Xiaomi Pad!"
echo "   1. Select a video file (motion-test-video.mp4 recommended)"
echo "   2. Tap 'Auto Cut' button"
echo "   3. Watch the processing happen"
echo ""
echo "Starting 60-second monitoring window..."
echo ""

# Monitor different components simultaneously
echo "🔍 Monitoring AutoCutEngine initialization..."
monitor_realtime_logs "AutoCutEngine Init" "AutoCutEngine" 60 &

echo "🔍 Monitoring VideoScorer analysis..."
monitor_realtime_logs "VideoScorer Analysis" "VideoScorer" 60 &

echo "🔍 Monitoring Media3 Transformer..."
monitor_realtime_logs "Media3 Transformer" "Transformer" 60 &

echo "🔍 Monitoring Progress callbacks..."
monitor_realtime_logs "Progress Callbacks" "Progress callback" 60 &

echo "🔍 Monitoring MediaItem creation..."
monitor_realtime_logs "MediaItem Creation" "MediaItem" 60 &

# Wait for monitoring to complete
wait

echo ""
echo "📋 Step 3: Post-processing Analysis"
echo "----------------------------------"
get_detailed_memory
check_media3_components
echo ""

# Step 4: Detailed Log Analysis
echo "📋 Step 4: Detailed Log Analysis"
echo "-------------------------------"

# Count all relevant logs
AUTOCUT_LOGS=$(adb -s $DEVICE logcat -d | grep "AutoCutEngine" | wc -l)
VIDEOSCORER_LOGS=$(adb -s $DEVICE logcat -d | grep "VideoScorer" | wc -l)
TRANSFORMER_LOGS=$(adb -s $DEVICE logcat -d | grep -i "transformer" | wc -l)
MEDIAITEM_LOGS=$(adb -s $DEVICE logcat -d | grep -i "mediaitem" | wc -l)
PROGRESS_LOGS=$(adb -s $DEVICE logcat -d | grep "Progress callback" | wc -l)
MAINACTIVITY_LOGS=$(adb -s $DEVICE logcat -d | grep "MainActivity" | wc -l)

echo "📈 Detailed Log Counts:"
echo "   - AutoCutEngine logs: $AUTOCUT_LOGS"
echo "   - VideoScorer logs: $VIDEOSCORER_LOGS"
echo "   - Transformer logs: $TRANSFORMER_LOGS"
echo "   - MediaItem logs: $MEDIAITEM_LOGS"
echo "   - Progress callback logs: $PROGRESS_LOGS"
echo "   - MainActivity logs: $MAINACTIVITY_LOGS"
echo ""

# Step 5: Show Recent Logs
echo "📋 Step 5: Recent Processing Logs"
echo "--------------------------------"
echo "🔍 Recent AutoCutEngine logs:"
adb -s $DEVICE logcat -d | grep "AutoCutEngine" | tail -5
echo ""

echo "🔍 Recent VideoScorer logs:"
adb -s $DEVICE logcat -d | grep "VideoScorer" | tail -5
echo ""

echo "🔍 Recent MainActivity logs:"
adb -s $DEVICE logcat -d | grep "MainActivity" | tail -5
echo ""

# Step 6: Performance Analysis
echo "📋 Step 6: Performance Analysis"
echo "-------------------------------"

# Check if output file was created
OUTPUT_FILE=$(adb -s $DEVICE shell "ls -la /storage/emulated/0/Android/data/$PACKAGE/files/ | grep mira_output")
if [ ! -z "$OUTPUT_FILE" ]; then
    echo "✅ Output file created:"
    echo "$OUTPUT_FILE"
else
    echo "❌ No output file found"
fi
echo ""

# Check app stability
APP_RUNNING=$(adb -s $DEVICE shell "ps | grep $PACKAGE" | wc -l)
if [ $APP_RUNNING -gt 0 ]; then
    echo "✅ App stability: PASSED (still running)"
else
    echo "❌ App stability: FAILED (crashed)"
fi
echo ""

# Step 7: Final Assessment
echo "📋 Step 7: Final Assessment"
echo "--------------------------"

echo "✅ Media3 AutoCutEngine Test Results:"
echo "====================================="

if [ $AUTOCUT_LOGS -gt 0 ]; then
    echo "✅ AutoCutEngine Initialization: PASSED ($AUTOCUT_LOGS logs)"
else
    echo "❌ AutoCutEngine Initialization: FAILED (no logs)"
fi

if [ $VIDEOSCORER_LOGS -gt 0 ]; then
    echo "✅ Video Analysis Pipeline: PASSED ($VIDEOSCORER_LOGS logs)"
else
    echo "❌ Video Analysis Pipeline: FAILED (no logs)"
fi

if [ $PROGRESS_LOGS -gt 0 ]; then
    echo "✅ Progress Callback System: PASSED ($PROGRESS_LOGS logs)"
else
    echo "❌ Progress Callback System: FAILED (no logs)"
fi

if [ $MAINACTIVITY_LOGS -gt 0 ]; then
    echo "✅ MainActivity Integration: PASSED ($MAINACTIVITY_LOGS logs)"
else
    echo "❌ MainActivity Integration: FAILED (no logs)"
fi

echo ""
echo "🎯 Detailed Media3 AutoCutEngine Test Complete!"
echo "==============================================="
