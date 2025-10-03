#!/bin/bash

# CLIP4Clip Manual Testing Script
# Tests CLIP4Clip components through existing app functionality

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

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if device is connected
    if ! adb devices | grep -q "device$"; then
        log_error "No Android device connected"
        exit 1
    fi
    
    # Check if app is installed
    if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
        log_error "App not installed. Please install the app first."
        exit 1
    fi
    
    # Check if test video exists
    if ! adb shell test -f "$TEST_VIDEO_PATH"; then
        log_warning "Test video not found. Copying test video..."
        if [ -f "test/assets/video_v1.mp4" ]; then
            adb push test/assets/video_v1.mp4 "$TEST_VIDEO_PATH"
            log_success "Test video copied successfully"
        else
            log_error "Test video not found locally. Please ensure test/assets/video_v1.mp4 exists"
            exit 1
        fi
    fi
    
    log_success "Prerequisites check passed"
}

# Clear app data and logs
cleanup() {
    log_info "Cleaning up app data and logs..."
    adb shell pm clear "$PACKAGE_NAME"
    adb logcat -c
    log_success "Cleanup completed"
}

# Launch main activity
launch_app() {
    log_info "Launching main activity..."
    
    adb shell am start -n "$PACKAGE_NAME/com.mira.videoeditor.MainActivity"
    
    # Wait for activity to start
    sleep 3
    
    # Check if activity is running
    if adb shell dumpsys activity activities | grep -q "com.mira.videoeditor.MainActivity"; then
        test_passed "Main activity launched successfully"
    else
        test_failed "Failed to launch main activity"
        return 1
    fi
}

# Test shot detection through existing pipeline
test_shot_detection() {
    log_info "Testing shot detection through existing pipeline..."
    
    # Clear logs
    adb logcat -c
    
    log_info "Please manually start video processing in the app..."
    log_info "This will trigger shot detection. Press Enter when processing starts..."
    read -p "Press Enter to continue..."
    
    # Wait for processing
    sleep 10
    
    # Check logs for shot detection
    if adb logcat -d | grep -q "Shot detection completed"; then
        test_passed "Shot detection test passed"
        
        # Extract shot count from logs
        SHOT_COUNT=$(adb logcat -d | grep "Shots detected:" | tail -1 | grep -o '[0-9]*' | head -1)
        if [ -n "$SHOT_COUNT" ]; then
            log_info "Detected $SHOT_COUNT shots"
        fi
    else
        test_failed "Shot detection test failed"
        log_error "Check logs for details: adb logcat | grep ShotDetector"
    fi
}

# Test CLIP4Clip components through logs
test_clip4clip_components() {
    log_info "Testing CLIP4Clip components..."
    
    # Clear logs
    adb logcat -c
    
    log_info "Looking for CLIP4Clip-related logs..."
    
    # Wait a bit for any CLIP4Clip processing
    sleep 5
    
    # Check for CLIP4Clip logs
    if adb logcat -d | grep -q "Clip4Clip"; then
        test_passed "CLIP4Clip components detected in logs"
        
        # Show CLIP4Clip logs
        log_info "CLIP4Clip logs found:"
        adb logcat -d | grep "Clip4Clip" | tail -5
    else
        log_warning "No CLIP4Clip logs found - components may not be active yet"
        log_info "This is expected if CLIP4Clip integration is not yet enabled"
    fi
}

# Test AutoCutEngine integration
test_autocut_integration() {
    log_info "Testing AutoCutEngine integration..."
    
    # Clear logs
    adb logcat -c
    
    log_info "Looking for AutoCutEngine logs..."
    
    # Wait for any AutoCutEngine processing
    sleep 5
    
    # Check for AutoCutEngine logs
    if adb logcat -d | grep -q "AutoCutEngine"; then
        test_passed "AutoCutEngine integration detected"
        
        # Show AutoCutEngine logs
        log_info "AutoCutEngine logs found:"
        adb logcat -d | grep "AutoCutEngine" | tail -5
    else
        log_warning "No AutoCutEngine logs found"
    fi
}

# Monitor performance
monitor_performance() {
    log_info "Monitoring performance..."
    
    # Get initial memory usage
    INITIAL_MEMORY=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}')
    log_info "Initial memory usage: ${INITIAL_MEMORY}KB"
    
    # Monitor during processing
    log_info "Please perform video processing in the app..."
    log_info "Monitoring for 30 seconds..."
    sleep 30
    
    # Get peak memory usage
    PEAK_MEMORY=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}')
    log_info "Peak memory usage: ${PEAK_MEMORY}KB"
    
    # Calculate memory increase
    MEMORY_INCREASE=$((PEAK_MEMORY - INITIAL_MEMORY))
    log_info "Memory increase: ${MEMORY_INCREASE}KB"
    
    # Check if memory usage is reasonable (< 500MB)
    if [ "$PEAK_MEMORY" -lt 512000 ]; then
        test_passed "Memory usage within acceptable limits"
    else
        test_failed "Memory usage exceeds acceptable limits"
    fi
}

# Generate test report
generate_report() {
    log_info "Generating test report..."
    
    REPORT_FILE="clip4clip_manual_test_report_$(date +%Y%m%d_%H%M%S).txt"
    
    cat > "$REPORT_FILE" << EOF
CLIP4Clip Manual Test Report
============================
Date: $(date)
Device: $(adb shell getprop ro.product.model)
Android Version: $(adb shell getprop ro.build.version.release)
App Version: $(adb shell dumpsys package "$PACKAGE_NAME" | grep versionName | awk '{print $2}')

Test Results:
- Total Tests: $TOTAL_TESTS
- Passed: $TESTS_PASSED
- Failed: $TESTS_FAILED
- Success Rate: $((TESTS_PASSED * 100 / TOTAL_TESTS))%

Recent Logs:
$(adb logcat -d | grep -E "(ShotDetector|Clip4Clip|AutoCutEngine|ERROR|SUCCESS)" | tail -20)

Memory Usage:
- Peak Memory: $(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}')KB

Notes:
- This is a manual test of existing components
- CLIP4Clip integration requires app rebuild
- Some tests may show warnings if components aren't active yet
EOF
    
    log_success "Test report generated: $REPORT_FILE"
}

# Main test execution
main() {
    log_info "Starting CLIP4Clip manual testing..."
    log_info "====================================="
    
    # Check prerequisites
    check_prerequisites
    
    # Cleanup
    cleanup
    
    # Launch app
    launch_app
    
    # Test shot detection
    test_shot_detection
    
    # Test CLIP4Clip components
    test_clip4clip_components
    
    # Test AutoCutEngine integration
    test_autocut_integration
    
    # Monitor performance
    monitor_performance
    
    # Generate report
    generate_report
    
    # Final summary
    log_info "====================================="
    log_info "CLIP4Clip Manual Testing Complete"
    log_info "Total Tests: $TOTAL_TESTS"
    log_info "Passed: $TESTS_PASSED"
    log_info "Failed: $TESTS_FAILED"
    
    if [ "$TESTS_FAILED" -eq 0 ]; then
        log_success "All tests passed! Ready for CLIP4Clip integration."
    else
        log_warning "Some tests failed. Check the report for details."
    fi
}

# Handle script arguments
case "${1:-}" in
    "shot-detection")
        check_prerequisites
        launch_app
        test_shot_detection
        ;;
    "components")
        check_prerequisites
        launch_app
        test_clip4clip_components
        ;;
    "integration")
        check_prerequisites
        launch_app
        test_autocut_integration
        ;;
    "performance")
        check_prerequisites
        launch_app
        monitor_performance
        ;;
    "all")
        main
        ;;
    "cleanup")
        cleanup
        ;;
    *)
        echo "Usage: $0 [shot-detection|components|integration|performance|all|cleanup]"
        echo ""
        echo "Available commands:"
        echo "  shot-detection  - Test shot detection through existing pipeline"
        echo "  components      - Test CLIP4Clip components (if available)"
        echo "  integration     - Test AutoCutEngine integration"
        echo "  performance     - Monitor performance and memory usage"
        echo "  all            - Run all tests (default)"
        echo "  cleanup        - Clean app data and logs"
        exit 1
        ;;
esac
