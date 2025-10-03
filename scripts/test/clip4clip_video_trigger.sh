#!/bin/bash

# CLIP4Clip Video Processing Trigger Script
# Triggers video processing directly through ADB commands without UI interaction

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PACKAGE_NAME="com.mira.videoeditor.debug"
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

# Trigger video processing through broadcast intent
trigger_video_processing() {
    log_info "Triggering video processing via broadcast intent..."
    
    # Clear logs first
    adb logcat -c
    
    # Send broadcast to trigger video processing
    adb shell am broadcast -a com.mira.videoeditor.PROCESS_VIDEO \
        --es video_path "$TEST_VIDEO_PATH" \
        --ei target_duration_ms 30000 \
        --ei segment_ms 2000 \
        --ei sample_ms 500 \
        --ei min_shot_ms 1500 \
        --ef threshold 0.28
    
    log_success "Video processing broadcast sent"
}

# Trigger video processing through activity intent
trigger_video_processing_intent() {
    log_info "Triggering video processing via activity intent..."
    
    # Clear logs first
    adb logcat -c
    
    # Launch activity with video processing intent
    adb shell am start -n "$PACKAGE_NAME/com.mira.videoeditor.WhisperTestActivity" \
        --es action "process_video" \
        --es video_path "$TEST_VIDEO_PATH" \
        --ei target_duration_ms 30000
    
    log_success "Video processing intent sent"
}

# Trigger video processing through service
trigger_video_processing_service() {
    log_info "Triggering video processing via service..."
    
    # Clear logs first
    adb logcat -c
    
    # Start service for video processing
    adb shell am startservice -n "$PACKAGE_NAME/com.mira.videoeditor.VideoProcessingService" \
        --es video_path "$TEST_VIDEO_PATH" \
        --ei target_duration_ms 30000
    
    log_success "Video processing service started"
}

# Trigger video processing through shell command
trigger_video_processing_shell() {
    log_info "Triggering video processing via shell command..."
    
    # Clear logs first
    adb logcat -c
    
    # Execute shell command in app context
    adb shell run-as "$PACKAGE_NAME" am broadcast -a com.mira.videoeditor.PROCESS_VIDEO \
        --es video_path "$TEST_VIDEO_PATH"
    
    log_success "Video processing shell command executed"
}

# Trigger video processing through monkey
trigger_video_processing_monkey() {
    log_info "Triggering video processing via monkey..."
    
    # Clear logs first
    adb logcat -c
    
    # Use monkey to trigger specific events
    adb shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1
    
    # Wait a bit then send key events
    sleep 2
    adb shell input keyevent KEYCODE_MENU
    sleep 1
    adb shell input keyevent KEYCODE_DPAD_DOWN
    sleep 1
    adb shell input keyevent KEYCODE_DPAD_CENTER
    
    log_success "Video processing monkey events sent"
}

# Trigger video processing through direct method call
trigger_video_processing_direct() {
    log_info "Triggering video processing via direct method call..."
    
    # Clear logs first
    adb logcat -c
    
    # Use app_process to call methods directly
    adb shell "am start -n $PACKAGE_NAME/com.mira.videoeditor.WhisperTestActivity --es action process_video --es video_path $TEST_VIDEO_PATH"
    
    log_success "Video processing direct method call executed"
}

# Main trigger function
main() {
    log_info "Starting CLIP4Clip video processing trigger..."
    log_info "Target: video_v1.mp4 (375MB)"
    log_info "Method: Script-based (no UI interaction)"
    
    # Check if device is connected
    if ! adb devices | grep -q "device$"; then
        log_error "No Android device connected"
        exit 1
    fi
    
    # Check if app is installed
    if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
        log_error "App not installed"
        exit 1
    fi
    
    # Check if test video exists
    if ! adb shell test -f "$TEST_VIDEO_PATH"; then
        log_warning "Test video not found at $TEST_VIDEO_PATH"
        log_info "Attempting to copy test video..."
        if [ -f "test/assets/video_v1.mp4" ]; then
            adb push test/assets/video_v1.mp4 "$TEST_VIDEO_PATH"
            log_success "Test video copied successfully"
        else
            log_error "Test video not found locally"
            exit 1
        fi
    fi
    
    # Try different trigger methods
    log_info "Attempting multiple trigger methods..."
    
    # Method 1: Broadcast intent
    trigger_video_processing
    sleep 3
    
    # Method 2: Activity intent
    trigger_video_processing_intent
    sleep 3
    
    # Method 3: Service
    trigger_video_processing_service
    sleep 3
    
    # Method 4: Shell command
    trigger_video_processing_shell
    sleep 3
    
    # Method 5: Direct method call
    trigger_video_processing_direct
    
    log_success "All trigger methods executed"
    log_info "Check logs for video processing activity"
}

# Handle script arguments
case "${1:-}" in
    "broadcast")
        trigger_video_processing
        ;;
    "intent")
        trigger_video_processing_intent
        ;;
    "service")
        trigger_video_processing_service
        ;;
    "shell")
        trigger_video_processing_shell
        ;;
    "monkey")
        trigger_video_processing_monkey
        ;;
    "direct")
        trigger_video_processing_direct
        ;;
    "all")
        main
        ;;
    *)
        echo "Usage: $0 [broadcast|intent|service|shell|monkey|direct|all]"
        echo ""
        echo "Available trigger methods:"
        echo "  broadcast  - Use broadcast intent"
        echo "  intent     - Use activity intent"
        echo "  service    - Use service"
        echo "  shell      - Use shell command"
        echo "  monkey     - Use monkey events"
        echo "  direct     - Use direct method call"
        echo "  all        - Try all methods (default)"
        exit 1
        ;;
esac
