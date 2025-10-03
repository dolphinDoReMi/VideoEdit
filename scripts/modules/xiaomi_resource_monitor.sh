#!/bin/bash

# Xiaomi Pad Real-Time Resource Monitoring Script
# Monitors memory, CPU, battery, thermal, and storage usage
# Designed specifically for Mira Video Editor testing

DEVICE="050C188041A00540"
PACKAGE="com.mira.videoeditor.debug"
LOG_FILE="xiaomi_resource_log_$(date +%Y%m%d_%H%M%S).txt"

echo "📱 Xiaomi Pad Resource Monitoring Started"
echo "=========================================="
echo "Device: $DEVICE"
echo "Package: $PACKAGE"
echo "Log File: $LOG_FILE"
echo "Start Time: $(date)"
echo ""

# Function to log with timestamp
log_with_timestamp() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a $LOG_FILE
}

# Function to check if app is running
check_app_running() {
    local pid=$(adb -s $DEVICE shell "pidof $PACKAGE" | tr -d '\r')
    if [ ! -z "$pid" ]; then
        return 0
    else
        return 1
    fi
}

# Function to get memory usage
get_memory_usage() {
    local memory=$(adb -s $DEVICE shell "dumpsys meminfo $PACKAGE | grep 'TOTAL PSS:'" | awk '{print $3}')
    if [ ! -z "$memory" ] && [ "$memory" != "" ]; then
        # Convert KB to GB using awk (more reliable than bc)
        local memory_gb=$(echo "$memory" | awk '{printf "%.2f", $1/1048576}')
        echo "${memory_gb}GB"
    else
        echo "N/A"
    fi
}

# Function to get CPU usage
get_cpu_usage() {
    # Prefer dumpsys cpuinfo (more stable on MIUI/Android variants)
    local cpu_line=$(adb -s $DEVICE shell "dumpsys cpuinfo | grep $PACKAGE | head -n1")
    local cpu=$(echo "$cpu_line" | awk '{print $1}' | tr -d '%')
    if [ ! -z "$cpu" ]; then
        echo "${cpu}%"
        return
    fi

    # Fallback: use top filtered by PID to avoid truncated process names
    local pid=$(adb -s $DEVICE shell "pidof $PACKAGE" | tr -d '\r')
    if [ -z "$pid" ]; then
        echo "N/A"
        return
    fi

    cpu=$(adb -s $DEVICE shell "top -n 1 | grep -E '^[[:space:]]*$pid[[:space:]]' | awk '{print \\$9}' | head -n1" 2>/dev/null)
    if [ ! -z "$cpu" ]; then
        # Some top variants already include % sign
        if echo "$cpu" | grep -q '%'; then
            echo "$cpu"
        else
            echo "${cpu}%"
        fi
    else
        echo "N/A"
    fi
}

# Function to get battery level
get_battery_level() {
    local battery=$(adb -s $DEVICE shell "dumpsys battery | grep level" | awk '{print $2}')
    if [ ! -z "$battery" ]; then
        echo "${battery}%"
    else
        echo "N/A"
    fi
}

# Function to get temperature
get_temperature() {
    local temp=$(adb -s $DEVICE shell "dumpsys thermalservice | grep temperature" | head -1 | awk '{print $2}')
    if [ ! -z "$temp" ]; then
        echo "${temp}°C"
    else
        echo "N/A"
    fi
}

# Function to get available storage
get_storage_usage() {
    local storage=$(adb -s $DEVICE shell "df -h /storage/emulated/0" | tail -1 | awk '{print $4}')
    if [ ! -z "$storage" ]; then
        echo "${storage}"
    else
        echo "N/A"
    fi
}

# Function to check for alerts
check_alerts() {
    local memory=$1
    local cpu=$2
    local temp=$3
    
    # Memory alert (>400MB)
    if [[ "$memory" =~ ^[0-9]+KB$ ]]; then
        local mem_mb=$((memory/1024))
        if [ $mem_mb -gt 400 ]; then
            log_with_timestamp "⚠️  ALERT: High memory usage: ${memory}"
        fi
    fi
    
    # CPU alert (>50%)
    if [[ "$cpu" =~ ^[0-9]+%$ ]]; then
        local cpu_val=$(echo $cpu | sed 's/%//')
        if [ $cpu_val -gt 50 ]; then
            log_with_timestamp "⚠️  ALERT: High CPU usage: ${cpu}"
        fi
    fi
    
    # Temperature alert (>40°C)
    if [[ "$temp" =~ ^[0-9]+°C$ ]]; then
        local temp_val=$(echo $temp | sed 's/°C//')
        if [ $temp_val -gt 40 ]; then
            log_with_timestamp "⚠️  ALERT: High temperature: ${temp}"
        fi
    fi
}

# Initialize log file
log_with_timestamp "=== Xiaomi Pad Resource Monitoring Started ==="
log_with_timestamp "Device: $DEVICE"
log_with_timestamp "Package: $PACKAGE"
log_with_timestamp "Monitoring interval: 10 seconds"

# Main monitoring loop
monitor_count=0
while true; do
    monitor_count=$((monitor_count + 1))
    
    # Check if app is running
    if ! check_app_running; then
        log_with_timestamp "❌ App not running - waiting for app to start..."
        sleep 5
        continue
    fi
    
    log_with_timestamp "=== Resource Snapshot #$monitor_count ==="
    
    # Get resource metrics
    memory=$(get_memory_usage)
    cpu=$(get_cpu_usage)
    battery=$(get_battery_level)
    temp=$(get_temperature)
    storage=$(get_storage_usage)
    
    # Log metrics
    log_with_timestamp "Memory Usage: $memory"
    log_with_timestamp "CPU Usage: $cpu"
    log_with_timestamp "Battery Level: $battery"
    log_with_timestamp "Temperature: $temp"
    log_with_timestamp "Available Storage: $storage"
    
    # Check for alerts
    check_alerts "$memory" "$cpu" "$temp"
    
    # Show progress every 30 seconds
    if [ $((monitor_count % 3)) -eq 0 ]; then
        echo "📊 Monitoring... (${monitor_count} snapshots taken)"
    fi
    
    sleep 10
done
