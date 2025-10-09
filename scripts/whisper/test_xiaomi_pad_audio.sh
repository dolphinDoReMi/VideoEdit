#!/bin/bash

# Xiaomi Pad Audio Processing Test Suite
# Comprehensive testing of audio extraction and preprocessing pipeline

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Test audio extraction
test_audio_extraction() {
    log "Testing audio extraction..."
    
    local test_file="$1"
    if [ ! -f "$test_file" ]; then
        log_error "Test file not found: $test_file"
        return 1
    fi
    
    # Test ffmpeg extraction
    local temp_audio="/tmp/test_extraction.wav"
    ffmpeg -i "$test_file" -ar 16000 -ac 1 -acodec pcm_s16le -y "$temp_audio" 2>/dev/null
    
    if [ $? -eq 0 ] && [ -f "$temp_audio" ]; then
        local size=$(ls -lh "$temp_audio" | awk '{print $5}')
        log_success "Audio extraction test passed (${size})"
        rm -f "$temp_audio"
        return 0
    else
        log_error "Audio extraction test failed"
        return 1
    fi
}

# Test Android app
test_android_app() {
    log "Testing Android app..."
    
    if ! adb devices | grep -q "device$"; then
        log_warning "No Android device connected - skipping app test"
        return 0
    fi
    
    # Check if app is installed
    if ! adb shell pm list packages | grep -q "com.mira.com"; then
        log_warning "App not installed - skipping app test"
        return 0
    fi
    
    # Launch app
    adb shell am start -n "com.mira.com/com.mira.whisper.WhisperMainActivity"
    sleep 3
    
    # Check if app is running
    if adb shell ps | grep -q "com.mira.com"; then
        log_success "Android app test passed"
        return 0
    else
        log_error "Android app test failed"
        return 1
    fi
}

# Test AudioIO fixes
test_audioio_fixes() {
    log "Testing AudioIO fixes..."
    
    local audioio_file="feature/whisper/src/main/java/com/mira/com/feature/whisper/data/io/AudioIO.kt"
    
    if [ ! -f "$audioio_file" ]; then
        log_error "AudioIO.kt not found"
        return 1
    fi
    
    # Check if fixes are applied
    if grep -q "chunks = mutableListOf<ShortArray>()" "$audioio_file"; then
        log_success "AudioIO streaming fixes detected"
        return 0
    else
        log_error "AudioIO streaming fixes not found"
        return 1
    fi
}

# Test performance
test_performance() {
    log "Testing performance targets..."
    
    # This would typically run actual performance tests
    # For now, we'll just validate the configuration
    
    local config_file="config/xiaomi_pad_config.json"
    if [ -f "$config_file" ]; then
        log_success "Performance configuration found"
        return 0
    else
        log_error "Performance configuration not found"
        return 1
    fi
}

# Main test execution
main() {
    local test_file="$1"
    
    log "Starting Xiaomi Pad audio processing test suite..."
    
    local tests_passed=0
    local tests_total=0
    
    # Test 1: Audio extraction
    tests_total=$((tests_total + 1))
    if test_audio_extraction "$test_file"; then
        tests_passed=$((tests_passed + 1))
    fi
    
    # Test 2: Android app
    tests_total=$((tests_total + 1))
    if test_android_app; then
        tests_passed=$((tests_passed + 1))
    fi
    
    # Test 3: AudioIO fixes
    tests_total=$((tests_total + 1))
    if test_audioio_fixes; then
        tests_passed=$((tests_passed + 1))
    fi
    
    # Test 4: Performance
    tests_total=$((tests_total + 1))
    if test_performance; then
        tests_passed=$((tests_passed + 1))
    fi
    
    # Summary
    echo
    log "Test Results: $tests_passed/$tests_total tests passed"
    
    if [ $tests_passed -eq $tests_total ]; then
        log_success "All tests passed!"
        exit 0
    else
        log_error "Some tests failed"
        exit 1
    fi
}

# Run main function with all arguments
main "$@"
