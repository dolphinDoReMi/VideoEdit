#!/bin/bash

# Xiaomi Pad Audio Processing Deployment Script
# Consolidated deployment with AudioIO fixes and optimizations

set -e

# Configuration
DEVICE_PATH="/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper"
APP_PACKAGE="com.mira.com"
MAIN_ACTIVITY="com.mira.whisper.WhisperMainActivity"
BROADCAST_ACTION="com.mira.com.feature.whisper.PROCESS_DIRECT"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[$(date +'%H:%M:%S')]${NC} $1"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Check device connection
check_device() {
    log "Checking Xiaomi Pad connection..."
    
    if ! adb devices | grep -q "device$"; then
        log_error "No Android device connected"
        echo "Please connect your Xiaomi Pad and enable USB debugging"
        exit 1
    fi
    
    local device_info=$(adb shell getprop ro.product.model 2>/dev/null || echo "Unknown")
    log "Connected device: $device_info"
}

# Setup scoped storage
setup_scoped_storage() {
    log "Setting up scoped storage directories..."
    
    # Create directory structure
    adb shell "mkdir -p $DEVICE_PATH/models"
    adb shell "mkdir -p $DEVICE_PATH/in"
    adb shell "mkdir -p $DEVICE_PATH/out"
    adb shell "mkdir -p $DEVICE_PATH/sidecars"
    adb shell "mkdir -p $DEVICE_PATH/temp"
    
    log "Scoped storage directories created"
}

# Deploy audio files
deploy_audio() {
    local audio_file="$1"
    
    log "Deploying audio files to Xiaomi Pad..."
    
    # Copy main audio file
    local device_audio="$DEVICE_PATH/in/processed_audio.wav"
    adb push "$audio_file" "$device_audio"
    
    if [ $? -eq 0 ]; then
        log "Audio file deployed: $device_audio"
    else
        log_error "Failed to deploy audio file"
        exit 1
    fi
    
    # Copy configuration
    local config_file="config/xiaomi_pad_config.json"
    if [ -f "$config_file" ]; then
        adb push "$config_file" "$DEVICE_PATH/"
        log "Configuration deployed"
    fi
}

# Launch Whisper app
launch_app() {
    log "Launching Whisper app..."
    
    adb shell am start -n "$APP_PACKAGE/$MAIN_ACTIVITY"
    
    # Wait for app initialization
    sleep 3
    
    log "App launched successfully"
}

# Start Whisper processing
start_processing() {
    local device_audio="$DEVICE_PATH/in/processed_audio.wav"
    
    log "Starting Whisper processing with AudioIO optimizations..."
    
    adb shell am broadcast -a "$BROADCAST_ACTION" \
        --es "uri" "file://$device_audio" \
        --es "model" "$DEVICE_PATH/models/small.en-q5_1.bin" \
        --es "threads" "4" \
        --es "lang" "en"
    
    log "Processing started"
}

# Monitor performance
monitor_performance() {
    log "Monitoring performance for 60 seconds..."
    
    for i in {1..60}; do
        # Get CPU usage
        local cpu_usage=$(adb shell "top -n 1 | grep $APP_PACKAGE" | awk '{print $1}' | head -1 || echo "0")
        
        # Get memory usage
        local memory_info=$(adb shell "dumpsys meminfo $APP_PACKAGE" | grep "TOTAL" | awk '{print $2}' || echo "0")
        local memory_mb=$((memory_info / 1024))
        
        # Get GPU usage (if available)
        local gpu_usage=$(adb shell "cat /sys/class/kgsl/kgsl-3d0/gpubusy" 2>/dev/null || echo "0")
        
        if [ $((i % 10)) -eq 0 ]; then
            log "Time ${i}s: CPU=${cpu_usage}%, Memory=${memory_mb}MB, GPU=${gpu_usage}%"
        fi
        
        sleep 1
    done
    
    log "Performance monitoring complete"
}

# Main execution
main() {
    local audio_file="$1"
    
    if [ -z "$audio_file" ]; then
        log_error "Usage: $0 <audio_file>"
        exit 1
    fi
    
    if [ ! -f "$audio_file" ]; then
        log_error "Audio file not found: $audio_file"
        exit 1
    fi
    
    log "Starting Xiaomi Pad deployment with AudioIO optimizations..."
    log "Audio file: $audio_file"
    
    check_device
    setup_scoped_storage
    deploy_audio "$audio_file"
    launch_app
    start_processing
    monitor_performance
    
    log "Deployment complete! Check device for results."
    log "Results location: $DEVICE_PATH/out/"
}

# Run main function with all arguments
main "$@"
