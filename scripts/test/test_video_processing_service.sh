#!/bin/bash

# CLIP4Clip Video Processing Service Test Script
# Tests the dedicated VideoProcessingService for CLIP4Clip video processing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PACKAGE_NAME="com.mira.videoeditor.debug"
SERVICE_NAME="com.mira.videoeditor.VideoProcessingService"
TEST_VIDEO_PATH="/sdcard/Android/data/com.mira.videoeditor.debug/files/video_v1.mp4"

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Test video processing service
test_video_processing_service() {
    log_info "Testing VideoProcessingService..."
    
    # Check if device is connected
    if ! adb devices | grep -q "device$"; then
        log_error "No Android device connected"
        return 1
    fi
    
    # Check if app is installed
    if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
        log_error "App not installed"
        return 1
    fi
    
    # Check if test video exists
    if ! adb shell test -f "$TEST_VIDEO_PATH"; then
        log_warning "Test video not found at $TEST_VIDEO_PATH"
        return 1
    fi
    
    log_info "Starting VideoProcessingService..."
    
    # Start the service
    adb shell am startservice -n "$PACKAGE_NAME/$SERVICE_NAME"
    
    if [ $? -eq 0 ]; then
        log_success "VideoProcessingService started successfully"
    else
        log_error "Failed to start VideoProcessingService"
        return 1
    fi
    
    # Wait a moment for service to initialize
    sleep 3
    
    # Send broadcast to trigger video processing
    log_info "Sending video processing broadcast..."
    adb shell am broadcast -a com.mira.videoeditor.PROCESS_VIDEO \
        --es video_path "$TEST_VIDEO_PATH" \
        --ei target_duration_ms 30000 \
        --ei segment_ms 2000 \
        --ei sample_ms 500 \
        --ei min_shot_ms 1500 \
        --ef threshold 0.28
    
    if [ $? -eq 0 ]; then
        log_success "Video processing broadcast sent successfully"
    else
        log_error "Failed to send video processing broadcast"
        return 1
    fi
    
    # Monitor for processing activity
    log_info "Monitoring video processing activity..."
    log_info "Check logs for: VideoProcessingService, ShotDetector, AutoCutEngine"
    
    return 0
}

# Monitor service logs
monitor_service_logs() {
    log_info "Starting service log monitoring..."
    
    # Clear logs first
    adb logcat -c
    
    # Monitor for service activity
    adb logcat | grep -E "(VideoProcessingService|ShotDetector|AutoCutEngine|VideoScorer|CLIP4Clip)" &
    MONITOR_PID=$!
    
    log_info "Log monitoring started (PID: $MONITOR_PID)"
    log_info "Press Ctrl+C to stop monitoring"
    
    # Wait for user to stop monitoring
    wait $MONITOR_PID
}

# Check service status
check_service_status() {
    log_info "Checking VideoProcessingService status..."
    
    # Check if service is running
    local service_running=$(adb shell ps | grep "$PACKAGE_NAME" | grep "video_processing" | wc -l)
    
    if [ "$service_running" -gt 0 ]; then
        log_success "VideoProcessingService is running"
        adb shell ps | grep "$PACKAGE_NAME" | grep "video_processing"
    else
        log_warning "VideoProcessingService is not running"
    fi
    
    # Check recent logs
    log_info "Recent service logs:"
    adb logcat -d | grep -E "(VideoProcessingService|ShotDetector|AutoCutEngine)" | tail -10
}

# Main function
main() {
    log_info "CLIP4Clip Video Processing Service Test"
    log_info "Time: $(date)"
    log_info "Target: video_v1.mp4"
    log_info "Service: VideoProcessingService"
    
    case "${1:-test}" in
        "test")
            test_video_processing_service
            ;;
        "monitor")
            monitor_service_logs
            ;;
        "status")
            check_service_status
            ;;
        "all")
            test_video_processing_service
            sleep 5
            check_service_status
            ;;
        *)
            echo "Usage: $0 [test|monitor|status|all]"
            echo ""
            echo "Available commands:"
            echo "  test     - Test video processing service (default)"
            echo "  monitor  - Monitor service logs in real-time"
            echo "  status   - Check service status and recent logs"
            echo "  all      - Run test, wait, then check status"
            exit 1
            ;;
    esac
}

# Run main function
main "$@"
