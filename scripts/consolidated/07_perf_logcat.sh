#!/bin/bash
# Performance monitoring script with logcat analysis
# Monitors latency and performance metrics during video processing

set -e

echo "=== CLIP4Clip Performance Monitoring ==="

# Configuration
PKG=${PKG:-"com.mira.videoeditor"}
VARIANT=${VARIANT:-"clip_vit_b32_mean_v1"}
DURATION=${DURATION:-60}  # Monitoring duration in seconds
LOG_FILE="performance.log"

# Performance thresholds
MAX_FRAME_ENCODE_TIME=2500  # 2.5 seconds per frame
MAX_BATCH_ENCODE_TIME=10000  # 10 seconds per batch
MAX_MEMORY_USAGE=500  # 500MB

echo "📊 Monitoring performance for $DURATION seconds..."
echo "📱 Package: $PKG"
echo "🔧 Variant: $VARIANT"
echo "📝 Log file: $LOG_FILE"

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

echo "✅ Android device connected"

# Clear logcat
adb logcat -c

# Start monitoring
echo "🔍 Starting performance monitoring..."
echo "Timestamp,Event,Value,Unit" > "$LOG_FILE"

# Monitor for specified duration
for i in $(seq 1 $DURATION); do
    # Get current memory usage
    MEMORY=$(adb shell "dumpsys meminfo $PKG | grep 'TOTAL' | awk '{print \$2}'" 2>/dev/null || echo "0")
    if [ "$MEMORY" != "0" ]; then
        echo "$(date +%s),memory_usage,$MEMORY,KB" >> "$LOG_FILE"
    fi
    
    # Get CPU usage
    CPU=$(adb shell "top -n 1 | grep $PKG | awk '{print \$9}'" 2>/dev/null || echo "0")
    if [ "$CPU" != "0" ]; then
        echo "$(date +%s),cpu_usage,$CPU,%" >> "$LOG_FILE"
    fi
    
    # Check for performance-related log entries
    PERF_LOGS=$(adb logcat -d -s "CLIP4Clip:P" | grep -E "(encode|process|frame)" | tail -5)
    if [ -n "$PERF_LOGS" ]; then
        echo "$PERF_LOGS" | while read -r line; do
            echo "$(date +%s),performance_log,\"$line\",log" >> "$LOG_FILE"
        done
    fi
    
    sleep 1
done

echo "✅ Performance monitoring completed"

# Analyze results
echo ""
echo "📊 Performance Analysis:"

# Check for frame encoding times
FRAME_TIMES=$(grep "frame_encode" "$LOG_FILE" | awk -F',' '{print $3}' | sort -n)
if [ -n "$FRAME_TIMES" ]; then
    MAX_FRAME_TIME=$(echo "$FRAME_TIMES" | tail -1)
    AVG_FRAME_TIME=$(echo "$FRAME_TIMES" | awk '{sum+=$1} END {print sum/NR}')
    
    echo "🎞️  Frame encoding times:"
    echo "   Max: ${MAX_FRAME_TIME}ms"
    echo "   Avg: ${AVG_FRAME_TIME}ms"
    
    if [ "$MAX_FRAME_TIME" -gt "$MAX_FRAME_ENCODE_TIME" ]; then
        echo "   ❌ Exceeds threshold ($MAX_FRAME_ENCODE_TIME ms)"
    else
        echo "   ✅ Within threshold ($MAX_FRAME_ENCODE_TIME ms)"
    fi
else
    echo "ℹ️  No frame encoding times recorded"
fi

# Check memory usage
MEMORY_USAGE=$(grep "memory_usage" "$LOG_FILE" | awk -F',' '{print $3}' | sort -n | tail -1)
if [ -n "$MEMORY_USAGE" ] && [ "$MEMORY_USAGE" != "0" ]; then
    MEMORY_MB=$((MEMORY_USAGE / 1024))
    echo "🧠 Peak memory usage: ${MEMORY_MB}MB"
    
    if [ "$MEMORY_MB" -gt "$MAX_MEMORY_USAGE" ]; then
        echo "   ❌ Exceeds threshold (${MAX_MEMORY_USAGE}MB)"
    else
        echo "   ✅ Within threshold (${MAX_MEMORY_USAGE}MB)"
    fi
else
    echo "ℹ️  No memory usage data recorded"
fi

# Check for errors
ERROR_COUNT=$(grep "ERROR" "$LOG_FILE" | wc -l)
if [ "$ERROR_COUNT" -gt 0 ]; then
    echo "❌ Found $ERROR_COUNT errors during monitoring"
    echo "Recent errors:"
    grep "ERROR" "$LOG_FILE" | tail -3
else
    echo "✅ No errors detected during monitoring"
fi

# Check for warnings
WARNING_COUNT=$(grep "WARN" "$LOG_FILE" | wc -l)
if [ "$WARNING_COUNT" -gt 0 ]; then
    echo "⚠️  Found $WARNING_COUNT warnings during monitoring"
    echo "Recent warnings:"
    grep "WARN" "$LOG_FILE" | tail -3
else
    echo "✅ No warnings detected during monitoring"
fi

# Performance summary
echo ""
echo "=== Performance Summary ==="

# Determine overall performance status
PERFORMANCE_OK=true

if [ -n "$MAX_FRAME_TIME" ] && [ "$MAX_FRAME_TIME" -gt "$MAX_FRAME_ENCODE_TIME" ]; then
    PERFORMANCE_OK=false
fi

if [ -n "$MEMORY_MB" ] && [ "$MEMORY_MB" -gt "$MAX_MEMORY_USAGE" ]; then
    PERFORMANCE_OK=false
fi

if [ "$ERROR_COUNT" -gt 0 ]; then
    PERFORMANCE_OK=false
fi

if [ "$PERFORMANCE_OK" = true ]; then
    echo "✅ Performance is within acceptable limits"
    echo "📊 Status: Good"
    EXIT_CODE=0
else
    echo "❌ Performance issues detected"
    echo "📊 Status: Poor"
    EXIT_CODE=1
fi

echo ""
echo "📈 Performance metrics saved to: $LOG_FILE"
echo "🎉 Performance monitoring completed with exit code: $EXIT_CODE"

exit $EXIT_CODE
