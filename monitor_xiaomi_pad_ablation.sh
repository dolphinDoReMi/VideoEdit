#!/bin/bash

# Xiaomi Pad Ablation Test Monitor
# Real-time monitoring during CPU vs Vulkan ablation tests

set -e

echo "📊 Xiaomi Pad Ablation Test Monitor"
echo "===================================="
echo "Real-time performance monitoring"
echo "Timestamp: $(date)"
echo ""

# Configuration
APP_PACKAGE="com.mira.com"
MONITOR_INTERVAL=5
MAX_MONITOR_TIME=300  # 5 minutes max

# Results directory
MONITOR_DIR="./ablation_monitor_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$MONITOR_DIR"

echo "📁 Monitoring data will be saved to: $MONITOR_DIR"
echo ""

# Quick setup check
echo "=== Monitor Setup Check ==="
adb devices | grep "device$" || { echo "❌ No Xiaomi Pad connected"; exit 1; }
echo "✅ Xiaomi Pad connected"

# Check if app is running
if ! adb shell "pgrep -f $APP_PACKAGE" >/dev/null 2>&1; then
    echo "❌ App not running. Please start the ablation test first."
    exit 1
else
    echo "✅ App is running"
fi

echo ""
echo "🔍 Starting real-time monitoring..."
echo "Press Ctrl+C to stop monitoring"
echo ""

# Function to get current metrics
get_metrics() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # CPU usage
    local cpu_usage=$(adb shell "dumpsys cpuinfo | grep $APP_PACKAGE | head -1" 2>/dev/null || echo "N/A")
    
    # Memory usage
    local memory_usage=$(adb shell "dumpsys meminfo $APP_PACKAGE | grep TOTAL" 2>/dev/null || echo "N/A")
    
    # Battery level
    local battery_level=$(adb shell "dumpsys battery" | grep "level:" | awk '{print $2}' 2>/dev/null || echo "N/A")
    
    # Thermal status
    local thermal_status=$(adb shell "cat /sys/class/thermal/thermal_zone*/temp" 2>/dev/null | head -3 || echo "N/A")
    
    # Processing status
    local processing_status=$(adb logcat -d | grep -E "(Processing|WhisperJNI)" | tail -1 2>/dev/null || echo "N/A")
    
    echo "$timestamp|$cpu_usage|$memory_usage|$battery_level|$thermal_status|$processing_status"
}

# Function to display metrics
display_metrics() {
    local timestamp="$1"
    local cpu_usage="$2"
    local memory_usage="$3"
    local battery_level="$4"
    local thermal_status="$5"
    local processing_status="$6"
    
    echo "⏰ $timestamp"
    echo "🖥️  CPU: $cpu_usage"
    echo "💾 Memory: $memory_usage"
    echo "🔋 Battery: $battery_level%"
    echo "🌡️  Thermal: $thermal_status"
    echo "⚙️  Processing: $processing_status"
    echo "---"
}

# Main monitoring loop
monitor_count=0
start_time=$(date +%s)

while [ $monitor_count -lt $((MAX_MONITOR_TIME / MONITOR_INTERVAL)) ]; do
    metrics=$(get_metrics)
    IFS='|' read -r timestamp cpu_usage memory_usage battery_level thermal_status processing_status <<< "$metrics"
    
    # Display metrics
    display_metrics "$timestamp" "$cpu_usage" "$memory_usage" "$battery_level" "$thermal_status" "$processing_status"
    
    # Save to file
    echo "$metrics" >> "$MONITOR_DIR/metrics.csv"
    
    # Check if processing completed
    if echo "$processing_status" | grep -q "completed\|finished\|done"; then
        echo "✅ Processing completed detected!"
        break
    fi
    
    sleep $MONITOR_INTERVAL
    monitor_count=$((monitor_count + 1))
done

end_time=$(date +%s)
duration=$((end_time - start_time))

echo ""
echo "📊 Monitoring completed after $duration seconds"
echo "📁 Data saved to: $MONITOR_DIR/metrics.csv"

# Generate monitoring report
REPORT_FILE="$MONITOR_DIR/monitoring_report.md"

cat > "$REPORT_FILE" << EOF
# Xiaomi Pad Ablation Test Monitoring Report

**Monitor Date:** $(date)  
**Duration:** $duration seconds  
**Monitor Interval:** $MONITOR_INTERVAL seconds  
**Data Points:** $monitor_count  

## Summary

- **Start Time:** $(date -d "@$start_time")
- **End Time:** $(date -d "@$end_time")
- **Total Duration:** $duration seconds
- **Data Points Collected:** $monitor_count

## Metrics Overview

The monitoring captured the following metrics:
- CPU Usage
- Memory Usage  
- Battery Level
- Thermal Status
- Processing Status

## Data Files

- **Raw Metrics:** \`metrics.csv\`
- **Report:** \`monitoring_report.md\`

## Usage

To analyze the monitoring data:

\`\`\`bash
# View raw metrics
cat $MONITOR_DIR/metrics.csv

# Plot CPU usage over time
awk -F'|' '{print \$1, \$2}' $MONITOR_DIR/metrics.csv

# Plot memory usage over time  
awk -F'|' '{print \$1, \$3}' $MONITOR_DIR/metrics.csv
\`\`\`

---

*Monitoring report generated automatically*
EOF

echo "📋 Monitoring report generated: $REPORT_FILE"
echo ""
echo "🎯 Monitoring completed!"
