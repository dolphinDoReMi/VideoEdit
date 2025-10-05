#!/bin/bash

# Xiaomi Pad Ultra Deployment Script
# Deploys the complete Whisper Step Flow app to Xiaomi Pad Ultra

set -e  # Exit on any error

echo "🚀 Xiaomi Pad Ultra Deployment"
echo "=============================="
echo ""

# Configuration
APP_ID="com.mira.com"
MAIN_ACTIVITY="com.mira.whisper.WhisperMainActivity"
DEVICE_NAME="Xiaomi Pad Ultra"
BUILD_TYPE="debug"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if ADB is available
    if ! command -v adb &> /dev/null; then
        log_error "ADB not found. Please install Android SDK platform-tools."
        exit 1
    fi
    
    # Check if device is connected
    if ! adb devices | grep -q "device$"; then
        log_error "No Android device connected. Please connect your Xiaomi Pad Ultra and enable USB debugging."
        exit 1
    fi
    
    # Get device info
    DEVICE_MODEL=$(adb shell getprop ro.product.model)
    DEVICE_VERSION=$(adb shell getprop ro.build.version.release)
    DEVICE_API=$(adb shell getprop ro.build.version.sdk)
    
    log_success "Device connected: $DEVICE_MODEL"
    log_info "Android Version: $DEVICE_VERSION (API $DEVICE_API)"
    
    # Check if it's a Xiaomi Pad
    if [[ "$DEVICE_MODEL" != *"Pad"* ]]; then
        log_warning "Device doesn't appear to be a Xiaomi Pad. Proceeding anyway..."
    fi
    
    # Check if Gradle is available
    if ! command -v ./gradlew &> /dev/null; then
        log_error "Gradle wrapper not found. Please run from project root."
        exit 1
    fi
    
    log_success "Prerequisites check completed"
}

# Clean and build the project
build_project() {
    log_info "Building project for $BUILD_TYPE..."
    
    # Clean previous builds
    log_info "Cleaning previous builds..."
    ./gradlew clean
    
    # Build the APK
    log_info "Building $BUILD_TYPE APK..."
    if [ "$BUILD_TYPE" = "release" ]; then
        ./gradlew assembleRelease
        APK_PATH="app/build/outputs/apk/release/app-release.apk"
    else
        ./gradlew assembleDebug
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    fi
    
    if [ ! -f "$APK_PATH" ]; then
        log_error "APK build failed. APK not found at $APK_PATH"
        exit 1
    fi
    
    # Get APK info
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    log_success "APK built successfully: $APK_PATH ($APK_SIZE)"
}

# Install the app using hands-free script
install_app() {
    log_info "Installing app on $DEVICE_NAME using hands-free script..."
    
    # Use the new hands-free install script
    if [ -f "scripts/xiaomi_hands_free_install.sh" ]; then
        log_info "Using hands-free installation script..."
        ./scripts/xiaomi_hands_free_install.sh "$APK_PATH" "$APP_ID"
        log_success "App installed successfully with hands-free script"
    else
        log_warning "Hands-free script not found, falling back to manual install..."
        
        # Uninstall existing version if it exists
        if adb shell pm list packages | grep -q "$APP_ID"; then
            log_info "Uninstalling existing version..."
            adb uninstall "$APP_ID" || true
        fi
        
        # Install new version with auto-grant permissions
        log_info "Installing new version with auto-grant permissions..."
        adb install -r -t -g "$APK_PATH"
        
        if [ $? -eq 0 ]; then
            log_success "App installed successfully"
        else
            log_error "App installation failed"
            exit 1
        fi
    fi
}

# Launch and test the app
test_app() {
    log_info "Testing app functionality..."
    
    # Launch the app
    log_info "Launching app..."
    adb shell am start -n "$APP_ID/$MAIN_ACTIVITY"
    
    # Wait for app to load
    log_info "Waiting for app to load..."
    sleep 5
    
    # Check if app is running
    if adb shell ps | grep -q "$APP_ID"; then
        log_success "App is running"
    else
        log_error "App failed to start"
        exit 1
    fi
    
    # Take initial screenshot
    log_info "Taking initial screenshot..."
    adb shell screencap -p /sdcard/xiaomi_pad_deployed.png
    adb pull /sdcard/xiaomi_pad_deployed.png xiaomi_pad_deployed.png
    adb shell rm /sdcard/xiaomi_pad_deployed.png
    
    if [ -f "xiaomi_pad_deployed.png" ]; then
        log_success "Screenshot saved as xiaomi_pad_deployed.png"
    else
        log_warning "Screenshot failed"
    fi
}

# Test Whisper Step Flow
test_whisper_flow() {
    log_info "Testing Whisper Step Flow..."
    
    # Test Selection: Setup and Run
    log_info "Testing Selection (Setup & Run)..."
    
    # Navigate to whisper step 1 (this would be done via UI)
    log_info "Note: Manual testing required for UI navigation"
    log_info "1. Look for 'Whisper — File Selection' section in the app"
    log_info "2. Test 'Pick Media (URI)' button"
    log_info "3. Test 'Pick Model' button"
    log_info "4. Test preset selection (Single/Accuracy)"
    log_info "5. Test 'Run' button (should navigate to Step 2)"
    
    # Test Processing
    log_info "Testing Processing & Export..."
    log_info "1. Verify processing status display"
    log_info "2. Test 'Export All Data' button (should navigate to Step 3)"
    
    # Test Results
    log_info "Testing Results & Export..."
    log_info "1. Verify results display"
    log_info "2. Test export options (JSON, SRT, TXT)"
    log_info "3. Test 'New Analysis' button (should go back to Step 1)"
    
    log_success "Whisper Step Flow test instructions provided"
}

# Monitor app performance
monitor_performance() {
    log_info "Monitoring app performance..."
    
    # Get memory usage
    log_info "Memory usage:"
    adb shell dumpsys meminfo "$APP_ID" | grep -E "TOTAL|Native|Dalvik" || true
    
    # Get CPU usage
    log_info "CPU usage:"
    adb shell cat /proc/stat | head -1 || true
    
    # Get battery level
    log_info "Battery level:"
    adb shell cat /sys/class/power_supply/battery/capacity || true
    
    log_success "Performance monitoring completed"
}

# Check app logs
check_logs() {
    log_info "Checking app logs..."
    
    # Get recent logs
    log_info "Recent app logs:"
    adb logcat -d | grep -i "mira\|whisper\|clip" | tail -20 || true
    
    log_success "Log check completed"
}

# Main deployment process
main() {
    echo ""
    log_info "Starting deployment to $DEVICE_NAME..."
    echo ""
    
    # Step 1: Check prerequisites
    check_prerequisites
    echo ""
    
    # Step 2: Build project
    build_project
    echo ""
    
    # Step 3: Install app
    install_app
    echo ""
    
    # Step 4: Test app
    test_app
    echo ""
    
    # Step 5: Test Whisper Flow
    test_whisper_flow
    echo ""
    
    # Step 6: Monitor performance
    monitor_performance
    echo ""
    
    # Step 7: Check logs
    check_logs
    echo ""
    
    # Final success message
    log_success "Deployment completed successfully!"
    echo ""
    echo "🎯 Next Steps:"
    echo "1. Check the Xiaomi Pad Ultra screen for the app"
    echo "2. Test the Whisper Step Flow manually"
    echo "3. Verify all three steps work correctly"
    echo "4. Test export functionality"
    echo "5. Check performance and resource usage"
    echo ""
    echo "📱 App Package: $APP_ID"
    echo "📱 Main Activity: $MAIN_ACTIVITY"
    echo "📱 APK Location: $APK_PATH"
    echo ""
    
    if [ -f "xiaomi_pad_deployed.png" ]; then
        echo "📸 Screenshot saved: xiaomi_pad_deployed.png"
    fi
    
    log_success "Xiaomi Pad Ultra deployment complete!"
}

# Run main function
main "$@"
