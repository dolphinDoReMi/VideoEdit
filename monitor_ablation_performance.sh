#!/bin/bash

# Real-time Performance Monitor for Tennis Interview Clip 002 Ablation Test
# Monitors CPU, Memory, GPU, and Temperature during processing

set -e

echo "📊 Real-time Performance Monitor"
echo "================================"
echo "Monitoring device performance during ablation test"
echo "Press Ctrl+C to stop monitoring"
echo ""

# Configuration
PACKAGE_NAME="com.mira.com"
MONITOR_INTERVAL=2  # seconds
LOG_FILE="./performance_monitor_$(date +%Y%m%d_%H%M%S).log"

# Create log file
echo "Timestamp,CPU_Usage,Memory_Usage,GPU_Usage,Temperature,Status" > "$LOG_FILE"

# Function to get performance metrics
get_metrics() {
    local timestamp=$(date +%s)
    
    # Get CPU usage
    local cpu_usage=$(adb shell "dumpsys cpuinfo | grep $PACKAGE_NAME | awk '{print \$1}' | head -1" 2>/dev/null || echo "0")
    
    # Get memory usage
    local memory_usage=$(adb shell "dumpsys meminfo $PACKAGE_NAME | grep TOTAL | awk '{print \$2}'" 2>/dev/null || echo "0")
    
    # Get GPU usage
    local gpu_usage=$(adb shell "dumpsys gpu | grep 'GPU utilization' | awk '{print \$3}'" 2>/dev/null || echo "0")
    
    # Get temperature
    local temperature=$(adb shell "cat /sys/class/thermal/thermal_zone*/temp 2>/dev/null | head -1" 2>/dev/null || echo "0")
    temperature=$((temperature / 1000))  # Convert from millidegrees
    
    # Get processing status
    local status="IDLE"
    if adb shell "ps | grep $PACKAGE_NAME" >/dev/null 2>&1; then
        if adb shell "test -f /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
            status="COMPLETED"
        else
            status="PROCESSING"
        fi
    fi
    
    echo "$timestamp,$cpu_usage,$memory_usage,$gpu_usage,$temperature,$status"
}

# Function to display metrics
display_metrics() {
    local metrics="$1"
    local timestamp=$(echo "$metrics" | cut -d, -f1)
    local cpu=$(echo "$metrics" | cut -d, -f2)
    local memory=$(echo "$metrics" | cut -d, -f3)
    local gpu=$(echo "$metrics" | cut -d, -f4)
    local temp=$(echo "$metrics" | cut -d, -f5)
    local status=$(echo "$metrics" | cut -d, -f6)
    
    # Convert memory to MB
    local memory_mb=$((memory / 1024))
    
    # Format timestamp
    local time_str=$(date -r "$timestamp" "+%H:%M:%S")
    
    # Clear screen and display
    clear
    echo "📊 Real-time Performance Monitor - $time_str"
    echo "=============================================="
    echo ""
    echo "📱 Device: $(adb shell getprop ro.product.model)"
    echo "📦 Package: $PACKAGE_NAME"
    echo "🔄 Status: $status"
    echo ""
    echo "⚡ CPU Usage:    ${cpu}%"
    echo "💾 Memory:      ${memory_mb}MB"
    echo "🎮 GPU Usage:   ${gpu}%"
    echo "🌡️  Temperature: ${temp}°C"
    echo ""
    echo "📝 Log file: $LOG_FILE"
    echo "Press Ctrl+C to stop monitoring"
    echo ""
}

# Main monitoring loop
echo "Starting performance monitoring..."
echo "Logging to: $LOG_FILE"
echo ""

# Trap Ctrl+C to cleanup
trap 'echo ""; echo "🛑 Monitoring stopped"; echo "📊 Log saved to: $LOG_FILE"; exit 0' INT

# Monitor loop
while true; do
    metrics=$(get_metrics)
    display_metrics "$metrics"
    echo "$metrics" >> "$LOG_FILE"
    sleep $MONITOR_INTERVAL
done
