#!/bin/bash

# Vulkan Troubleshooting Script
# This script helps diagnose common Vulkan integration issues

set -e

echo "=== Vulkan Troubleshooting Script ==="
echo "Timestamp: $(date)"
echo

PACKAGE_NAME="com.mira.com.debug"

# Function to check device connectivity
check_device_connectivity() {
    echo "1. Checking device connectivity..."
    if ! adb devices | grep -q "device$"; then
        echo "❌ No Android device connected"
        echo "   Solution: Connect device via USB and enable USB debugging"
        return 1
    fi
    
    DEVICE_MODEL=$(adb shell getprop ro.product.model)
    echo "✅ Device connected: $DEVICE_MODEL"
    return 0
}

# Function to check Vulkan support
check_vulkan_support() {
    echo "2. Checking Vulkan support..."
    
    if ! adb shell which vulkaninfo >/dev/null 2>&1; then
        echo "❌ vulkaninfo not available on device"
        echo "   Solution: Install vulkaninfo APK"
        echo "   Download from: https://github.com/KhronosGroup/Vulkan-Tools/releases"
        return 1
    fi
    
    echo "✅ vulkaninfo available"
    
    # Get basic Vulkan info
    API_VERSION=$(adb shell vulkaninfo 2>/dev/null | grep -E "apiVersion" | head -1 | grep -o "0x[0-9a-fA-F]*" || echo "unknown")
    echo "   Vulkan API Version: $API_VERSION"
    
    # Check for required features
    VULKAN_INFO=$(adb shell vulkaninfo 2>/dev/null)
    
    if echo "$VULKAN_INFO" | grep -q "shaderFloat16.*true"; then
        echo "✅ FP16 shader support: Available"
    else
        echo "❌ FP16 shader support: Not available"
        echo "   This may cause ggml to fall back to CPU"
    fi
    
    if echo "$VULKAN_INFO" | grep -q "storageBuffer16BitAccess.*true"; then
        echo "✅ 16-bit storage buffer access: Available"
    else
        echo "❌ 16-bit storage buffer access: Not available"
        echo "   This may cause ggml to fall back to CPU"
    fi
    
    return 0
}

# Function to check app installation
check_app_installation() {
    echo "3. Checking app installation..."
    
    if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
        echo "❌ App not installed"
        echo "   Solution: Install the app first"
        echo "   Run: adb install -r app/build/outputs/apk/debug/app-debug.apk"
        return 1
    fi
    
    echo "✅ App installed: $PACKAGE_NAME"
    
    # Check app permissions
    PERMISSIONS=$(adb shell dumpsys package "$PACKAGE_NAME" | grep "android.permission" | wc -l)
    echo "   App permissions: $PERMISSIONS"
    
    return 0
}

# Function to check build configuration
check_build_configuration() {
    echo "4. Checking build configuration..."
    
    # Check if APK exists
    if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        echo "❌ Debug APK not found"
        echo "   Solution: Build the app first"
        echo "   Run: ./gradlew :app:assembleDebug"
        return 1
    fi
    
    echo "✅ Debug APK found"
    
    # Check APK size
    APK_SIZE=$(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')
    echo "   APK size: $APK_SIZE"
    
    # Check if native libraries are included
    NATIVE_LIBS=$(unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "lib/" | wc -l)
    echo "   Native libraries: $NATIVE_LIBS"
    
    if [ "$NATIVE_LIBS" -eq 0 ]; then
        echo "⚠️  No native libraries found in APK"
        echo "   This may indicate a build configuration issue"
    fi
    
    return 0
}

# Function to check environment variables
check_environment_variables() {
    echo "5. Checking environment variables..."
    
    # Check if Vulkan device is set
    VULKAN_DEVICE=$(adb shell "echo \$GGML_VULKAN_DEVICE" 2>/dev/null || echo "")
    if [ -n "$VULKAN_DEVICE" ]; then
        echo "✅ GGML_VULKAN_DEVICE: $VULKAN_DEVICE"
    else
        echo "⚠️  GGML_VULKAN_DEVICE not set"
        echo "   This will use default device (0)"
    fi
    
    # Check debug settings
    VULKAN_DEBUG=$(adb shell "echo \$GGML_VULKAN_DEBUG" 2>/dev/null || echo "")
    if [ -n "$VULKAN_DEBUG" ]; then
        echo "✅ GGML_VULKAN_DEBUG: $VULKAN_DEBUG"
    else
        echo "⚠️  GGML_VULKAN_DEBUG not set"
    fi
    
    return 0
}

# Function to check logs
check_logs() {
    echo "6. Checking application logs..."
    
    # Clear old logs
    adb logcat -c
    
    # Launch app
    echo "   Launching app..."
    adb shell am start -n "$PACKAGE_NAME/com.mira.whisper.WhisperMainActivity"
    
    # Wait for initialization
    sleep 5
    
    # Check for Vulkan-related logs
    VULKAN_LOGS=$(adb logcat -d | grep -i "vulkan\|ggml.*vulkan" | tail -20)
    
    if [ -n "$VULKAN_LOGS" ]; then
        echo "✅ Vulkan logs found:"
        echo "$VULKAN_LOGS"
    else
        echo "⚠️  No Vulkan logs found"
        
        # Check for general ggml logs
        GGML_LOGS=$(adb logcat -d | grep -i "ggml" | tail -10)
        if [ -n "$GGML_LOGS" ]; then
            echo "   GGML logs found:"
            echo "$GGML_LOGS"
        else
            echo "❌ No GGML logs found"
            echo "   This may indicate the native library is not loading"
        fi
    fi
    
    # Check for errors
    ERROR_LOGS=$(adb logcat -d | grep -i "error\|exception\|crash" | tail -10)
    if [ -n "$ERROR_LOGS" ]; then
        echo "⚠️  Error logs found:"
        echo "$ERROR_LOGS"
    fi
    
    return 0
}

# Function to check validation layers
check_validation_layers() {
    echo "7. Checking validation layers..."
    
    # Check if validation layers are enabled
    VALIDATION_ENABLED=$(adb shell settings get global enable_gpu_debug_layers 2>/dev/null || echo "0")
    if [ "$VALIDATION_ENABLED" = "1" ]; then
        echo "✅ GPU debug layers enabled"
        
        DEBUG_APP=$(adb shell settings get global gpu_debug_app 2>/dev/null || echo "")
        if [ -n "$DEBUG_APP" ]; then
            echo "   Debug app: $DEBUG_APP"
        fi
        
        DEBUG_LAYERS=$(adb shell settings get global gpu_debug_layers 2>/dev/null || echo "")
        if [ -n "$DEBUG_LAYERS" ]; then
            echo "   Debug layers: $DEBUG_LAYERS"
        fi
    else
        echo "⚠️  GPU debug layers not enabled"
        echo "   To enable: adb shell settings put global enable_gpu_debug_layers 1"
    fi
    
    return 0
}

# Function to run performance test
run_performance_test() {
    echo "8. Running performance test..."
    
    # Start a test transcription
    echo "   Starting test transcription..."
    adb shell am broadcast -a "$PACKAGE_NAME.whisper.RUN" --es "file_path" "/sdcard/test_audio.wav"
    
    # Monitor for 30 seconds
    echo "   Monitoring performance for 30 seconds..."
    for i in {1..30}; do
        CPU_USAGE=$(adb shell dumpsys cpuinfo | grep "$PACKAGE_NAME" | awk '{print $1}' | head -1 || echo "0")
        MEMORY_USAGE=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}' || echo "0")
        
        if [ "$i" -eq 1 ] || [ "$i" -eq 15 ] || [ "$i" -eq 30 ]; then
            echo "   Time ${i}s: CPU=${CPU_USAGE}%, Memory=${MEMORY_USAGE}KB"
        fi
        
        sleep 1
    done
    
    return 0
}

# Function to provide solutions
provide_solutions() {
    echo "9. Common solutions..."
    echo
    
    echo "If Vulkan is not working:"
    echo "1. Ensure device supports Vulkan 1.3: adb shell vulkaninfo"
    echo "2. Check FP16 support: adb shell vulkaninfo | grep shaderFloat16"
    echo "3. Enable debug output: adb shell setenv GGML_VULKAN_DEBUG 1"
    echo "4. Set device explicitly: adb shell setenv GGML_VULKAN_DEVICE 0"
    echo "5. Enable validation layers: adb shell settings put global enable_gpu_debug_layers 1"
    echo
    echo "If app crashes:"
    echo "1. Check logs: adb logcat | grep -E 'AndroidRuntime|FATAL'"
    echo "2. Verify native library: adb shell ls /data/app/*/lib/arm64/"
    echo "3. Check permissions: adb shell dumpsys package $PACKAGE_NAME"
    echo
    echo "If performance is poor:"
    echo "1. Check GPU usage: adb shell dumpsys gpu"
    echo "2. Monitor CPU: adb shell top | grep $PACKAGE_NAME"
    echo "3. Check memory: adb shell dumpsys meminfo $PACKAGE_NAME"
    echo "4. Verify Vulkan usage: adb logcat | grep 'ggml.*vulkan'"
}

# Main execution
main() {
    echo "Starting Vulkan troubleshooting..."
    echo
    
    # Run all checks
    check_device_connectivity || exit 1
    echo
    
    check_vulkan_support || exit 1
    echo
    
    check_app_installation || exit 1
    echo
    
    check_build_configuration || exit 1
    echo
    
    check_environment_variables
    echo
    
    check_logs
    echo
    
    check_validation_layers
    echo
    
    run_performance_test
    echo
    
    provide_solutions
    
    echo
    echo "=== Troubleshooting Complete ==="
    echo "Timestamp: $(date)"
}

# Run main function
main "$@"
