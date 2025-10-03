#!/bin/bash

# Xiaomi Pad Performance Analysis Script
# Provides detailed analysis of app performance and resource usage
# Designed for post-testing analysis and optimization

DEVICE="050C188041A00540"
PACKAGE="com.mira.videoeditor.debug"
ANALYSIS_FILE="xiaomi_performance_analysis_$(date +%Y%m%d_%H%M%S).txt"

echo "📊 Xiaomi Pad Performance Analysis"
echo "=================================="
echo "Device: $DEVICE"
echo "Package: $PACKAGE"
echo "Analysis File: $ANALYSIS_FILE"
echo "Analysis Time: $(date)"
echo ""

# Function to log analysis
log_analysis() {
    echo "$1" | tee -a $ANALYSIS_FILE
}

# Initialize analysis file
log_analysis "=== Xiaomi Pad Performance Analysis ==="
log_analysis "Device: $DEVICE"
log_analysis "Package: $PACKAGE"
log_analysis "Analysis Time: $(date)"
log_analysis ""

# Check if app is running
echo "🔍 Checking app status..."
APP_RUNNING=$(adb -s $DEVICE shell "ps | grep $PACKAGE" | wc -l)
if [ $APP_RUNNING -gt 0 ]; then
    log_analysis "✅ App Status: Running"
    echo "✅ App is currently running"
else
    log_analysis "❌ App Status: Not running"
    echo "❌ App is not running"
fi

echo ""
log_analysis "=== Current Resource Usage ==="

# Memory usage analysis
echo "🔍 Analyzing memory usage..."
log_analysis "Memory Usage Breakdown:"
MEMORY_INFO=$(adb -s $DEVICE shell "dumpsys meminfo $PACKAGE")
echo "$MEMORY_INFO" | grep -E "(TOTAL|Native|Dalvik|App|Unknown)" | while read line; do
    log_analysis "  $line"
done

# Get total memory usage
TOTAL_MEMORY=$(echo "$MEMORY_INFO" | grep "TOTAL PSS:" | awk '{print $3}')
if [ ! -z "$TOTAL_MEMORY" ]; then
    TOTAL_MEMORY_MB=$((TOTAL_MEMORY/1024))
    TOTAL_MEMORY_GB=$(echo "$TOTAL_MEMORY" | awk '{printf "%.2f", $1/1048576}')
    log_analysis "Total Memory: ${TOTAL_MEMORY}KB (${TOTAL_MEMORY_MB}MB / ${TOTAL_MEMORY_GB}GB)"
    
    # Memory assessment
    if [ $TOTAL_MEMORY_MB -lt 300 ]; then
        log_analysis "Memory Assessment: ✅ Excellent (<300MB)"
    elif [ $TOTAL_MEMORY_MB -lt 400 ]; then
        log_analysis "Memory Assessment: ✅ Good (300-400MB)"
    elif [ $TOTAL_MEMORY_MB -lt 600 ]; then
        log_analysis "Memory Assessment: ⚠️  Warning (400-600MB)"
    else
        log_analysis "Memory Assessment: ❌ Critical (>600MB)"
    fi
fi

echo ""

# CPU usage analysis
echo "🔍 Analyzing CPU usage..."
log_analysis "CPU Usage Analysis:"
CPU_INFO=$(adb -s $DEVICE shell "top -n 1 | grep $PACKAGE")
if [ ! -z "$CPU_INFO" ]; then
    log_analysis "  $CPU_INFO"
    CPU_USAGE=$(echo "$CPU_INFO" | awk '{print $9}')
    if [ ! -z "$CPU_USAGE" ]; then
        CPU_VAL=$(echo $CPU_USAGE | sed 's/%//')
        if [ $CPU_VAL -lt 30 ]; then
            log_analysis "CPU Assessment: ✅ Excellent (<30%)"
        elif [ $CPU_VAL -lt 50 ]; then
            log_analysis "CPU Assessment: ✅ Good (30-50%)"
        elif [ $CPU_VAL -lt 70 ]; then
            log_analysis "CPU Assessment: ⚠️  Warning (50-70%)"
        else
            log_analysis "CPU Assessment: ❌ Critical (>70%)"
        fi
    fi
else
    log_analysis "  No CPU usage data available"
fi

echo ""

# Battery analysis
echo "🔍 Analyzing battery usage..."
log_analysis "Battery Analysis:"
BATTERY_INFO=$(adb -s $DEVICE shell "dumpsys battery")
echo "$BATTERY_INFO" | grep -E "(level|temperature|health)" | while read line; do
    log_analysis "  $line"
done

# Battery stats for the app
BATTERY_STATS=$(adb -s $DEVICE shell "dumpsys batterystats | grep $PACKAGE")
if [ ! -z "$BATTERY_STATS" ]; then
    log_analysis "App Battery Usage:"
    echo "$BATTERY_STATS" | while read line; do
        log_analysis "  $line"
    done
else
    log_analysis "  No app-specific battery data available"
fi

echo ""

# Thermal analysis
echo "🔍 Analyzing thermal state..."
log_analysis "Thermal Analysis:"
THERMAL_INFO=$(adb -s $DEVICE shell "dumpsys thermalservice")
echo "$THERMAL_INFO" | grep -E "(temperature|throttling|thermal)" | while read line; do
    log_analysis "  $line"
done

# Check for thermal throttling
THROTTLING=$(echo "$THERMAL_INFO" | grep -i throttling)
if [ ! -z "$THROTTLING" ]; then
    log_analysis "Thermal Assessment: ⚠️  Throttling detected"
else
    log_analysis "Thermal Assessment: ✅ No throttling detected"
fi

echo ""

# Storage analysis
echo "🔍 Analyzing storage usage..."
log_analysis "Storage Analysis:"
STORAGE_INFO=$(adb -s $DEVICE shell "df -h /storage/emulated/0")
echo "$STORAGE_INFO" | while read line; do
    log_analysis "  $line"
done

# App-specific storage usage
APP_STORAGE=$(adb -s $DEVICE shell "du -sh /storage/emulated/0/Android/data/$PACKAGE/ 2>/dev/null")
if [ ! -z "$APP_STORAGE" ]; then
    log_analysis "App Storage Usage: $APP_STORAGE"
else
    log_analysis "App Storage Usage: No data available"
fi

echo ""

# Network analysis (if applicable)
echo "🔍 Analyzing network usage..."
log_analysis "Network Analysis:"
NETWORK_INFO=$(adb -s $DEVICE shell "cat /proc/net/dev | grep wlan0")
if [ ! -z "$NETWORK_INFO" ]; then
    log_analysis "  $NETWORK_INFO"
else
    log_analysis "  No network data available"
fi

echo ""

# Performance logs analysis
echo "🔍 Analyzing performance logs..."
log_analysis "Performance Logs Analysis:"

# Check for ANR (Application Not Responding)
ANR_COUNT=$(adb -s $DEVICE logcat -d | grep -i "ANR" | wc -l)
log_analysis "ANR Count: $ANR_COUNT"
if [ $ANR_COUNT -gt 0 ]; then
    log_analysis "ANR Assessment: ❌ ANR detected"
    adb -s $DEVICE logcat -d | grep -i "ANR" | tail -3 | while read line; do
        log_analysis "  Recent ANR: $line"
    done
else
    log_analysis "ANR Assessment: ✅ No ANR detected"
fi

# Check for crashes
CRASH_COUNT=$(adb -s $DEVICE logcat -d | grep -i "FATAL" | wc -l)
log_analysis "Crash Count: $CRASH_COUNT"
if [ $CRASH_COUNT -gt 0 ]; then
    log_analysis "Crash Assessment: ❌ Crashes detected"
    adb -s $DEVICE logcat -d | grep -i "FATAL" | tail -3 | while read line; do
        log_analysis "  Recent Crash: $line"
    done
else
    log_analysis "Crash Assessment: ✅ No crashes detected"
fi

# Check for memory warnings
MEMORY_WARNINGS=$(adb -s $DEVICE logcat -d | grep -i "low memory" | wc -l)
log_analysis "Memory Warnings: $MEMORY_WARNINGS"
if [ $MEMORY_WARNINGS -gt 0 ]; then
    log_analysis "Memory Warning Assessment: ⚠️  Memory warnings detected"
else
    log_analysis "Memory Warning Assessment: ✅ No memory warnings"
fi

echo ""

# Hardware acceleration analysis
echo "🔍 Analyzing hardware acceleration..."
log_analysis "Hardware Acceleration Analysis:"

# Check GPU usage
GPU_INFO=$(adb -s $DEVICE shell "dumpsys gpu 2>/dev/null")
if [ ! -z "$GPU_INFO" ]; then
    echo "$GPU_INFO" | grep -E "(usage|memory)" | while read line; do
        log_analysis "  $line"
    done
else
    log_analysis "  No GPU data available"
fi

# Check hardware decoder usage
CODEC_INFO=$(adb -s $DEVICE shell "dumpsys media.codec 2>/dev/null")
if [ ! -z "$CODEC_INFO" ]; then
    DECODER_COUNT=$(echo "$CODEC_INFO" | grep -i "decoder" | wc -l)
    log_analysis "Hardware Decoders Active: $DECODER_COUNT"
    
    if [ $DECODER_COUNT -gt 0 ]; then
        log_analysis "Hardware Acceleration Assessment: ✅ Hardware acceleration active"
    else
        log_analysis "Hardware Acceleration Assessment: ⚠️  No hardware acceleration detected"
    fi
else
    log_analysis "Hardware Acceleration Assessment: ❌ No codec data available"
fi

echo ""

# MIUI-specific analysis
echo "🔍 Analyzing MIUI-specific optimizations..."
log_analysis "MIUI Analysis:"

# Check MIUI battery optimization
MIUI_BATTERY=$(adb -s $DEVICE shell "dumpsys deviceidle | grep $PACKAGE 2>/dev/null")
if [ ! -z "$MIUI_BATTERY" ]; then
    log_analysis "MIUI Battery Optimization: $MIUI_BATTERY"
else
    log_analysis "MIUI Battery Optimization: No specific data"
fi

# Check MIUI memory management
MIUI_MEMORY=$(adb -s $DEVICE shell "dumpsys activity | grep -E '(MIUI|Xiaomi)' 2>/dev/null")
if [ ! -z "$MIUI_MEMORY" ]; then
    MIUI_COUNT=$(echo "$MIUI_MEMORY" | wc -l)
    log_analysis "MIUI Memory Management: $MIUI_COUNT entries found"
else
    log_analysis "MIUI Memory Management: No specific data"
fi

echo ""

# Performance recommendations
echo "🔍 Generating performance recommendations..."
log_analysis "=== Performance Recommendations ==="

# Memory recommendations
if [ ! -z "$TOTAL_MEMORY_MB" ]; then
    if [ $TOTAL_MEMORY_MB -gt 400 ]; then
        log_analysis "Memory: ⚠️  Consider optimizing memory usage (currently ${TOTAL_MEMORY_MB}MB)"
    else
        log_analysis "Memory: ✅ Memory usage is optimal (${TOTAL_MEMORY_MB}MB)"
    fi
fi

# CPU recommendations
if [ ! -z "$CPU_VAL" ]; then
    if [ $CPU_VAL -gt 50 ]; then
        log_analysis "CPU: ⚠️  Consider optimizing CPU usage (currently ${CPU_VAL}%)"
    else
        log_analysis "CPU: ✅ CPU usage is optimal (${CPU_VAL}%)"
    fi
fi

# Thermal recommendations
if [ ! -z "$THROTTLING" ]; then
    log_analysis "Thermal: ⚠️  Thermal throttling detected - consider reducing processing load"
else
    log_analysis "Thermal: ✅ No thermal issues detected"
fi

# Stability recommendations
if [ $ANR_COUNT -gt 0 ] || [ $CRASH_COUNT -gt 0 ]; then
    log_analysis "Stability: ❌ Stability issues detected - review error logs"
else
    log_analysis "Stability: ✅ App is stable"
fi

echo ""

# Summary
log_analysis "=== Analysis Summary ==="
log_analysis "Analysis completed at: $(date)"
log_analysis "Device: Xiaomi Pad ($DEVICE)"
log_analysis "Package: $PACKAGE"
log_analysis ""

echo "📊 Performance Analysis Complete!"
echo "Analysis saved to: $ANALYSIS_FILE"
echo ""
echo "📋 Key Findings:"
echo "================"

# Display key findings
if [ ! -z "$TOTAL_MEMORY_MB" ]; then
    echo "Memory Usage: ${TOTAL_MEMORY_MB}MB"
fi

if [ ! -z "$CPU_VAL" ]; then
    echo "CPU Usage: ${CPU_VAL}%"
fi

echo "ANR Count: $ANR_COUNT"
echo "Crash Count: $CRASH_COUNT"
echo "Memory Warnings: $MEMORY_WARNINGS"

echo ""
echo "📁 Full analysis available in: $ANALYSIS_FILE"
