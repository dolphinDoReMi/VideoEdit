#!/bin/bash

# Real-time Performance Monitor for DirectWhisperService Ablation Test
# Monitors CPU, Memory, GPU, Temperature, and TranscribeWorker progress
# Based on proven DirectWhisperService command flow

set -e

echo "📊 DirectWhisperService Performance Monitor"
echo "==========================================="
echo "Monitoring device performance during DirectWhisperService ablation test"
echo "Press Ctrl+C to stop monitoring"
echo ""

# Configuration
PACKAGE_NAME="com.mira.com"
SERVICE_CLASS="com.mira.com.feature.whisper.service.DirectWhisperService"
MONITOR_INTERVAL=2  # seconds
LOG_FILE="./directwhisper_performance_monitor_$(date +%Y%m%d_%H%M%S).log"

# Create log file with headers
echo "Timestamp,CPU_Usage,Memory_Usage,GPU_Usage,Temperature,Service_Status,TranscribeWorker_Status,Processing_Stage" > "$LOG_FILE"

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
    
    # Get service status
    local service_status="STOPPED"
    if adb shell "ps | grep $PACKAGE_NAME" >/dev/null 2>&1; then
        service_status="RUNNING"
    fi
    
    # Get TranscribeWorker status
    local transcribe_status="IDLE"
    local processing_stage="UNKNOWN"
    
    # Check TranscribeWorker logs for processing stage
    local latest_logs=$(adb logcat -s TranscribeWorker:V -d | tail -3)
    if echo "$latest_logs" | grep -q "Storage self-test"; then
        transcribe_status="PROCESSING"
        processing_stage="STORAGE_TEST"
    elif echo "$latest_logs" | grep -q "Audio loaded"; then
        transcribe_status="PROCESSING"
        processing_stage="AUDIO_LOADED"
    elif echo "$latest_logs" | grep -q "LID"; then
        transcribe_status="PROCESSING"
        processing_stage="LANGUAGE_DETECTION"
    elif echo "$latest_logs" | grep -q "Whisper"; then
        transcribe_status="PROCESSING"
        processing_stage="WHISPER_INFERENCE"
    elif echo "$latest_logs" | grep -q "Processing"; then
        transcribe_status="PROCESSING"
        processing_stage="PROCESSING"
    elif echo "$latest_logs" | grep -q "Completed"; then
        transcribe_status="COMPLETED"
        processing_stage="COMPLETED"
    elif echo "$latest_logs" | grep -q "Failed"; then
        transcribe_status="FAILED"
        processing_stage="FAILED"
    fi
    
    # Check if output file exists
    if adb shell "test -f /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
        transcribe_status="COMPLETED"
        processing_stage="OUTPUT_GENERATED"
    fi
    
    echo "$timestamp,$cpu_usage,$memory_usage,$gpu_usage,$temperature,$service_status,$transcribe_status,$processing_stage"
}

# Function to display metrics
display_metrics() {
    local metrics="$1"
    local timestamp=$(echo "$metrics" | cut -d, -f1)
    local cpu=$(echo "$metrics" | cut -d, -f2)
    local memory=$(echo "$metrics" | cut -d, -f3)
    local gpu=$(echo "$metrics" | cut -d, -f4)
    local temp=$(echo "$metrics" | cut -d, -f5)
    local service_status=$(echo "$metrics" | cut -d, -f6)
    local transcribe_status=$(echo "$metrics" | cut -d, -f7)
    local processing_stage=$(echo "$metrics" | cut -d, -f8)
    
    # Convert memory to MB
    local memory_mb=$((memory / 1024))
    
    # Format timestamp
    local time_str=$(date -r "$timestamp" "+%H:%M:%S")
    
    # Clear screen and display
    clear
    echo "📊 DirectWhisperService Performance Monitor - $time_str"
    echo "======================================================="
    echo ""
    echo "📱 Device: $(adb shell getprop ro.product.model)"
    echo "📦 Package: $PACKAGE_NAME"
    echo "🔧 Service: $SERVICE_CLASS"
    echo ""
    echo "⚡ CPU Usage:    ${cpu}%"
    echo "💾 Memory:      ${memory_mb}MB"
    echo "🎮 GPU Usage:   ${gpu}%"
    echo "🌡️  Temperature: ${temp}°C"
    echo ""
    echo "🔄 Service Status: $service_status"
    echo "📝 TranscribeWorker: $transcribe_status"
    echo "🎯 Processing Stage: $processing_stage"
    echo ""
    echo "📝 Log file: $LOG_FILE"
    echo "Press Ctrl+C to stop monitoring"
    echo ""
    
    # Show recent TranscribeWorker logs
    echo "📋 Recent TranscribeWorker Logs:"
    echo "--------------------------------"
    adb logcat -s TranscribeWorker:V -d | tail -3 | while read line; do
        echo "   $line"
    done
    echo ""
}

# Function to check for service errors
check_service_errors() {
    local error_logs=$(adb logcat -d | grep -E "(app is in background|Service not found|Failed to start)" | tail -3)
    if [ -n "$error_logs" ]; then
        echo "⚠️  Service Errors Detected:"
        echo "$error_logs" | while read line; do
            echo "   $line"
        done
        echo ""
        echo "💡 Solution: Ensure app stays in foreground"
        echo "   adb shell am start -n $PACKAGE_NAME/com.mira.whisper.WhisperMainActivity"
        echo ""
    fi
}

# Main monitoring loop
echo "Starting DirectWhisperService performance monitoring..."
echo "Logging to: $LOG_FILE"
echo ""

# Trap Ctrl+C to cleanup
trap 'echo ""; echo "🛑 Monitoring stopped"; echo "📊 Log saved to: $LOG_FILE"; exit 0' INT

# Monitor loop
while true; do
    metrics=$(get_metrics)
    display_metrics "$metrics"
    echo "$metrics" >> "$LOG_FILE"
    
    # Check for service errors every 10 seconds
    if [ $(($(date +%s) % 10)) -eq 0 ]; then
        check_service_errors
    fi
    
    sleep $MONITOR_INTERVAL
done
