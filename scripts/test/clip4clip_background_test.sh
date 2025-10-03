#!/bin/bash

# CLIP4Clip Background Testing Script for Xiaomi Device
# Runs all CLIP4Clip tasks as background processes without UI interaction

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

# Test results tracking
TESTS_PASSED=0
TESTS_FAILED=0
TOTAL_TESTS=0

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

test_passed() {
    TESTS_PASSED=$((TESTS_PASSED + 1))
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    log_success "$1"
}

test_failed() {
    TESTS_FAILED=$((TESTS_FAILED + 1))
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    log_error "$1"
}

# Background task 1: Shot Detection Testing
run_shot_detection_test() {
    log_info "Starting background shot detection test..."
    
    # Clear logs
    adb logcat -c
    
    # Trigger shot detection through existing pipeline
    adb shell am broadcast -a com.mira.videoeditor.TEST_SHOT_DETECTION \
        --es video_path "$TEST_VIDEO_PATH" \
        --ei sample_ms 500 \
        --ei min_shot_ms 1500 \
        --ef threshold 0.28 &
    
    # Monitor for shot detection logs
    local timeout=30
    local start_time=$(date +%s)
    
    while [ $(($(date +%s) - start_time)) -lt $timeout ]; do
        if adb logcat -d | grep -q "Shot detection completed"; then
            test_passed "Shot detection test completed"
            
            # Extract shot count
            local shot_count=$(adb logcat -d | grep "Shots detected:" | tail -1 | grep -o '[0-9]*' | head -1)
            if [ -n "$shot_count" ]; then
                log_info "Detected $shot_count shots"
            fi
            return 0
        fi
        sleep 2
    done
    
    test_failed "Shot detection test timeout"
    return 1
}

# Background task 2: Video Processing Pipeline Test
run_video_processing_test() {
    log_info "Starting background video processing test..."
    
    # Clear logs
    adb logcat -c
    
    # Trigger video processing
    adb shell am broadcast -a com.mira.videoeditor.TEST_VIDEO_PROCESSING \
        --es video_path "$TEST_VIDEO_PATH" \
        --ei target_duration_ms 30000 \
        --ei segment_ms 2000 &
    
    # Monitor for processing logs
    local timeout=60
    local start_time=$(date +%s)
    
    while [ $(($(date +%s) - start_time)) -lt $timeout ]; do
        if adb logcat -d | grep -q "AutoCutEngine completed successfully"; then
            test_passed "Video processing test completed"
            return 0
        fi
        sleep 3
    done
    
    test_failed "Video processing test timeout"
    return 1
}

# Background task 3: Performance Monitoring
run_performance_monitor() {
    log_info "Starting background performance monitoring..."
    
    # Get initial memory usage
    local initial_memory=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}')
    log_info "Initial memory usage: ${initial_memory}KB"
    
    # Monitor for 30 seconds
    local start_time=$(date +%s)
    local peak_memory=$initial_memory
    
    while [ $(($(date +%s) - start_time)) -lt 30 ]; do
        local current_memory=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}')
        if [ "$current_memory" -gt "$peak_memory" ]; then
            peak_memory=$current_memory
        fi
        sleep 5
    done
    
    local memory_increase=$((peak_memory - initial_memory))
    log_info "Peak memory usage: ${peak_memory}KB"
    log_info "Memory increase: ${memory_increase}KB"
    
    if [ "$peak_memory" -lt 512000 ]; then
        test_passed "Memory usage within acceptable limits"
    else
        test_failed "Memory usage exceeds acceptable limits"
    fi
}

# Background task 4: Log Analysis
run_log_analysis() {
    log_info "Starting background log analysis..."
    
    # Analyze recent logs
    local shot_logs=$(adb logcat -d | grep -c "ShotDetector")
    local engine_logs=$(adb logcat -d | grep -c "AutoCutEngine")
    local scorer_logs=$(adb logcat -d | grep -c "VideoScorer")
    local error_logs=$(adb logcat -d | grep -c "ERROR")
    
    log_info "Log analysis results:"
    log_info "  ShotDetector logs: $shot_logs"
    log_info "  AutoCutEngine logs: $engine_logs"
    log_info "  VideoScorer logs: $scorer_logs"
    log_info "  Error logs: $error_logs"
    
    if [ "$error_logs" -eq 0 ]; then
        test_passed "No errors found in logs"
    else
        test_warning "Found $error_logs error logs"
    fi
    
    if [ "$shot_logs" -gt 0 ] && [ "$engine_logs" -gt 0 ]; then
        test_passed "Core components are active"
    else
        test_failed "Core components may not be active"
    fi
}

# Background task 5: System Resource Monitoring
run_system_monitor() {
    log_info "Starting background system monitoring..."
    
    # Check CPU usage
    local cpu_usage=$(adb shell dumpsys cpuinfo | grep "$PACKAGE_NAME" | awk '{print $1}' | head -1)
    if [ -n "$cpu_usage" ]; then
        log_info "CPU usage: ${cpu_usage}%"
    fi
    
    # Check battery level
    local battery_level=$(adb shell dumpsys battery | grep "level:" | awk '{print $2}')
    log_info "Battery level: ${battery_level}%"
    
    # Check device temperature
    local temperature=$(adb shell dumpsys battery | grep "temperature:" | awk '{print $2}')
    if [ -n "$temperature" ]; then
        log_info "Device temperature: ${temperature}°C"
    fi
    
    test_passed "System monitoring completed"
}

# Background task 6: Comprehensive Testing
run_comprehensive_test() {
    log_info "Starting comprehensive background testing..."
    
    # Run all tests in parallel
    run_shot_detection_test &
    run_video_processing_test &
    run_performance_monitor &
    run_log_analysis &
    run_system_monitor &
    
    # Wait for all background tasks
    wait
    
    log_info "Comprehensive testing completed"
}

# Main execution
main() {
    log_info "Starting CLIP4Clip background testing on Xiaomi device..."
    log_info "========================================================"
    
    # Check device connection
    if ! adb devices | grep -q "device$"; then
        log_error "No Android device connected"
        exit 1
    fi
    
    # Check app installation
    if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
        log_error "App not installed"
        exit 1
    fi
    
    # Run comprehensive testing
    run_comprehensive_test
    
    # Generate final report
    log_info "========================================================"
    log_info "CLIP4Clip Background Testing Complete"
    log_info "Total Tests: $TOTAL_TESTS"
    log_info "Passed: $TESTS_PASSED"
    log_info "Failed: $TESTS_FAILED"
    
    if [ "$TESTS_FAILED" -eq 0 ]; then
        log_success "All background tests passed!"
    else
        log_warning "Some tests failed. Check logs for details."
    fi
}

# Run main function
main "$@"
