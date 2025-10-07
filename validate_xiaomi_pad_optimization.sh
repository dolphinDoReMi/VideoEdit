#!/bin/bash

# Xiaomi Pad Inference Optimization Validation
# Based on XiaoMi Pad Inference Optimization.md
# Validates ARM64 optimization and Vulkan integration

set -e

echo "🔍 Xiaomi Pad Inference Optimization Validation"
echo "==============================================="
echo "Based on XiaoMi Pad Inference Optimization Guide"
echo "Timestamp: $(date)"
echo ""

# Configuration
APP_PACKAGE="com.mira.com"
VALIDATION_DIR="./xiaomi_pad_validation_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$VALIDATION_DIR"

echo "📁 Validation results will be saved to: $VALIDATION_DIR"
echo ""

# Quick setup check
echo "=== Xiaomi Pad Validation Setup ==="
adb devices | grep "device$" || { echo "❌ No Xiaomi Pad connected"; exit 1; }
echo "✅ Xiaomi Pad connected"

# Device information collection
echo "📱 Collecting device information..."
adb shell "getprop ro.product.model" > "$VALIDATION_DIR/device_model.txt"
adb shell "getprop ro.build.version.release" > "$VALIDATION_DIR/android_version.txt"
adb shell "getprop ro.product.cpu.abi" > "$VALIDATION_DIR/cpu_abi.txt"
adb shell "getprop ro.product.cpu.abilist" > "$VALIDATION_DIR/cpu_abilist.txt"
adb shell "getprop ro.hardware" > "$VALIDATION_DIR/hardware.txt"
adb shell "getprop ro.board.platform" > "$VALIDATION_DIR/platform.txt"

DEVICE_MODEL=$(cat "$VALIDATION_DIR/device_model.txt")
ANDROID_VERSION=$(cat "$VALIDATION_DIR/android_version.txt")
CPU_ABI=$(cat "$VALIDATION_DIR/cpu_abi.txt")
CPU_ABILIST=$(cat "$VALIDATION_DIR/cpu_abilist.txt")
HARDWARE=$(cat "$VALIDATION_DIR/hardware.txt")
PLATFORM=$(cat "$VALIDATION_DIR/platform.txt")

echo "📱 Device: $DEVICE_MODEL"
echo "🤖 Android: $ANDROID_VERSION"
echo "🔧 CPU ABI: $CPU_ABI"
echo "🔧 CPU ABI List: $CPU_ABILIST"
echo "🔧 Hardware: $HARDWARE"
echo "🔧 Platform: $PLATFORM"

# ARM64 optimization validation
echo ""
echo "=== ARM64 Optimization Validation ==="

# Check if ARM64 is supported
if echo "$CPU_ABILIST" | grep -q "arm64-v8a"; then
    echo "✅ ARM64-v8a support confirmed"
    ARM64_SUPPORT=true
else
    echo "❌ ARM64-v8a not supported"
    ARM64_SUPPORT=false
fi

# Check build configuration
echo "🔍 Checking build configuration..."
if grep -q "abiFilters.*arm64-v8a" feature/whisper/build.gradle.kts; then
    echo "✅ ARM64 build filter configured"
    ARM64_BUILD=true
else
    echo "❌ ARM64 build filter not configured"
    ARM64_BUILD=false
fi

# Check CMake configuration
if grep -q "GGML_USE_CPU" feature/whisper/src/main/cpp/CMakeLists.txt; then
    echo "✅ CPU backend configured"
    CPU_BACKEND=true
else
    echo "❌ CPU backend not configured"
    CPU_BACKEND=false
fi

# Vulkan support validation
echo ""
echo "=== Vulkan Support Validation ==="

# Check Vulkan library
if adb shell "test -f /system/lib64/libvulkan.so" 2>/dev/null; then
    echo "✅ Vulkan library found"
    VULKAN_LIBRARY=true
else
    echo "❌ Vulkan library not found"
    VULKAN_LIBRARY=false
fi

# Check Vulkan driver
adb shell "dumpsys SurfaceFlinger | grep -i vulkan" > "$VALIDATION_DIR/vulkan_driver.txt" 2>/dev/null || true
if [ -s "$VALIDATION_DIR/vulkan_driver.txt" ]; then
    echo "✅ Vulkan driver information found"
    VULKAN_DRIVER=true
else
    echo "❌ Vulkan driver information not found"
    VULKAN_DRIVER=false
fi

# Check Vulkan configuration in build files
if grep -q "GGML_USE_VULKAN" feature/whisper/src/main/cpp/CMakeLists.txt; then
    echo "✅ Vulkan backend configured in CMakeLists.txt"
    VULKAN_CMAKE=true
else
    echo "❌ Vulkan backend not configured in CMakeLists.txt"
    VULKAN_CMAKE=false
fi

if grep -q "GGML_VULKAN" feature/whisper/build.gradle.kts; then
    echo "✅ Vulkan flags configured in build.gradle.kts"
    VULKAN_GRADLE=true
else
    echo "❌ Vulkan flags not configured in build.gradle.kts"
    VULKAN_GRADLE=false
fi

# DirectWhisperService validation
echo ""
echo "=== DirectWhisperService Validation ==="

# Check if DirectWhisperService exists
if [ -f "feature/whisper/src/main/java/com/mira/com/feature/whisper/service/DirectWhisperService.kt" ]; then
    echo "✅ DirectWhisperService found"
    DIRECT_SERVICE=true
else
    echo "❌ DirectWhisperService not found"
    DIRECT_SERVICE=false
fi

# Check if service is registered in manifest
if grep -q "DirectWhisperService" app/src/main/AndroidManifest.xml; then
    echo "✅ DirectWhisperService registered in manifest"
    SERVICE_REGISTERED=true
else
    echo "❌ DirectWhisperService not registered in manifest"
    SERVICE_REGISTERED=false
fi

# Check for broadcast receiver removal
echo ""
echo "=== Broadcast Infrastructure Removal Validation ==="

# Check if broadcast receivers are removed
if ! grep -q "BroadcastReceiver" app/src/main/AndroidManifest.xml; then
    echo "✅ Broadcast receivers removed from manifest"
    BROADCAST_REMOVED=true
else
    echo "❌ Broadcast receivers still present in manifest"
    BROADCAST_REMOVED=false
fi

# Check if WhisperConnectorReceiver is removed
if [ ! -f "app/src/main/java/com/mira/whisper/WhisperConnectorReceiver.kt" ]; then
    echo "✅ WhisperConnectorReceiver removed"
    CONNECTOR_RECEIVER_REMOVED=true
else
    echo "❌ WhisperConnectorReceiver still present"
    CONNECTOR_RECEIVER_REMOVED=false
fi

# Check if ResourceUpdateReceiver is removed
if [ ! -f "app/src/main/java/com/mira/whisper/ResourceUpdateReceiver.kt" ]; then
    echo "✅ ResourceUpdateReceiver removed"
    RESOURCE_RECEIVER_REMOVED=true
else
    echo "❌ ResourceUpdateReceiver still present"
    RESOURCE_RECEIVER_REMOVED=false
fi

# Performance validation
echo ""
echo "=== Performance Validation ==="

# Check if app can be built
echo "🔨 Testing build process..."
if ./gradlew assembleDebug > "$VALIDATION_DIR/build_log.txt" 2>&1; then
    echo "✅ Debug build successful"
    BUILD_SUCCESS=true
else
    echo "❌ Debug build failed"
    BUILD_SUCCESS=false
fi

# Check if app can be installed
if [ "$BUILD_SUCCESS" = true ]; then
    echo "📱 Testing installation..."
    if adb install -r app/build/outputs/apk/debug/app-debug.apk > "$VALIDATION_DIR/install_log.txt" 2>&1; then
        echo "✅ App installation successful"
        INSTALL_SUCCESS=true
    else
        echo "❌ App installation failed"
        INSTALL_SUCCESS=false
    fi
else
    INSTALL_SUCCESS=false
fi

# Check if app can be launched
if [ "$INSTALL_SUCCESS" = true ]; then
    echo "🚀 Testing app launch..."
    adb shell am start -n "$APP_PACKAGE/com.mira.whisper.WhisperMainActivity" > "$VALIDATION_DIR/launch_log.txt" 2>&1
    sleep 3
    if adb shell "pgrep -f $APP_PACKAGE" >/dev/null 2>&1; then
        echo "✅ App launch successful"
        LAUNCH_SUCCESS=true
    else
        echo "❌ App launch failed"
        LAUNCH_SUCCESS=false
    fi
else
    LAUNCH_SUCCESS=false
fi

# Generate comprehensive validation report
echo ""
echo "=== Generating Validation Report ==="
REPORT_FILE="$VALIDATION_DIR/xiaomi_pad_validation_report.md"

cat > "$REPORT_FILE" << EOF
# Xiaomi Pad Inference Optimization Validation Report

**Validation Date:** $(date)  
**Device:** $DEVICE_MODEL  
**Android Version:** $ANDROID_VERSION  
**CPU ABI:** $CPU_ABI  
**Hardware:** $HARDWARE  
**Platform:** $PLATFORM  

## Device Information

- **Model:** $DEVICE_MODEL
- **Android Version:** $ANDROID_VERSION
- **CPU Architecture:** $CPU_ABI
- **CPU ABI List:** $CPU_ABILIST
- **Hardware:** $HARDWARE
- **Platform:** $PLATFORM

## ARM64 Optimization Validation

| Component | Status | Details |
|-----------|--------|---------|
| ARM64 Support | $(if [ "$ARM64_SUPPORT" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | Device supports ARM64-v8a |
| ARM64 Build Filter | $(if [ "$ARM64_BUILD" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | Build configured for ARM64 |
| CPU Backend | $(if [ "$CPU_BACKEND" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | CPU backend configured |

## Vulkan Support Validation

| Component | Status | Details |
|-----------|--------|---------|
| Vulkan Library | $(if [ "$VULKAN_LIBRARY" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | Vulkan library present |
| Vulkan Driver | $(if [ "$VULKAN_DRIVER" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | Vulkan driver information |
| Vulkan CMake | $(if [ "$VULKAN_CMAKE" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | Vulkan configured in CMakeLists.txt |
| Vulkan Gradle | $(if [ "$VULKAN_GRADLE" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | Vulkan flags in build.gradle.kts |

## DirectWhisperService Validation

| Component | Status | Details |
|-----------|--------|---------|
| Service File | $(if [ "$DIRECT_SERVICE" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | DirectWhisperService.kt exists |
| Service Registration | $(if [ "$SERVICE_REGISTERED" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | Service registered in manifest |

## Broadcast Infrastructure Removal

| Component | Status | Details |
|-----------|--------|---------|
| Manifest Cleanup | $(if [ "$BROADCAST_REMOVED" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | Broadcast receivers removed from manifest |
| Connector Receiver | $(if [ "$CONNECTOR_RECEIVER_REMOVED" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | WhisperConnectorReceiver removed |
| Resource Receiver | $(if [ "$RESOURCE_RECEIVER_REMOVED" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | ResourceUpdateReceiver removed |

## Performance Validation

| Component | Status | Details |
|-----------|--------|---------|
| Build Process | $(if [ "$BUILD_SUCCESS" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | Debug build successful |
| Installation | $(if [ "$INSTALL_SUCCESS" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | App installation successful |
| App Launch | $(if [ "$LAUNCH_SUCCESS" = true ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi) | App launch successful |

## Overall Validation Status

**ARM64 Optimization:** $(if [ "$ARM64_SUPPORT" = true ] && [ "$ARM64_BUILD" = true ] && [ "$CPU_BACKEND" = true ]; then echo "✅ COMPLETE"; else echo "❌ INCOMPLETE"; fi)

**Vulkan Integration:** $(if [ "$VULKAN_LIBRARY" = true ] && [ "$VULKAN_CMAKE" = true ] && [ "$VULKAN_GRADLE" = true ]; then echo "✅ COMPLETE"; else echo "❌ INCOMPLETE"; fi)

**DirectWhisperService:** $(if [ "$DIRECT_SERVICE" = true ] && [ "$SERVICE_REGISTERED" = true ]; then echo "✅ COMPLETE"; else echo "❌ INCOMPLETE"; fi)

**Broadcast Removal:** $(if [ "$BROADCAST_REMOVED" = true ] && [ "$CONNECTOR_RECEIVER_REMOVED" = true ] && [ "$RESOURCE_RECEIVER_REMOVED" = true ]; then echo "✅ COMPLETE"; else echo "❌ INCOMPLETE"; fi)

**Performance:** $(if [ "$BUILD_SUCCESS" = true ] && [ "$INSTALL_SUCCESS" = true ] && [ "$LAUNCH_SUCCESS" = true ]; then echo "✅ COMPLETE"; else echo "❌ INCOMPLETE"; fi)

## Recommendations

EOF

# Add recommendations based on validation results
if [ "$ARM64_SUPPORT" = false ]; then
    cat >> "$REPORT_FILE" << EOF
- **ARM64 Support:** Device does not support ARM64-v8a. Consider using ARM32 or alternative architecture.
EOF
fi

if [ "$VULKAN_LIBRARY" = false ]; then
    cat >> "$REPORT_FILE" << EOF
- **Vulkan Support:** Vulkan library not found. Consider CPU-only processing or alternative GPU acceleration.
EOF
fi

if [ "$BUILD_SUCCESS" = false ]; then
    cat >> "$REPORT_FILE" << EOF
- **Build Issues:** Debug build failed. Check build logs for specific errors.
EOF
fi

if [ "$INSTALL_SUCCESS" = false ]; then
    cat >> "$REPORT_FILE" << EOF
- **Installation Issues:** App installation failed. Check device storage and permissions.
EOF
fi

if [ "$LAUNCH_SUCCESS" = false ]; then
    cat >> "$REPORT_FILE" << EOF
- **Launch Issues:** App launch failed. Check app configuration and dependencies.
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Next Steps

1. **Run Ablation Tests:** Execute \`xiaomi_pad_cpu_vulkan_ablation.sh\` for performance comparison
2. **Monitor Performance:** Use \`monitor_xiaomi_pad_ablation.sh\` for real-time monitoring
3. **Optimize Configuration:** Based on validation results, adjust build and runtime configuration
4. **Production Deployment:** Use validated configuration for production deployment

## Files Generated

- **Device Info:** \`device_model.txt\`, \`android_version.txt\`, \`cpu_abi.txt\`, \`cpu_abilist.txt\`, \`hardware.txt\`, \`platform.txt\`
- **Vulkan Info:** \`vulkan_driver.txt\`
- **Build Logs:** \`build_log.txt\`, \`install_log.txt\`, \`launch_log.txt\`
- **Validation Report:** \`xiaomi_pad_validation_report.md\`

---

*Validation report generated automatically based on XiaoMi Pad Inference Optimization Guide*
EOF

echo "✅ Validation report generated: $REPORT_FILE"

echo ""
echo "=== Xiaomi Pad Validation Summary ==="
echo "📊 Results saved to: $VALIDATION_DIR"
echo "📋 Report generated: $REPORT_FILE"
echo "📱 Device: $DEVICE_MODEL"
echo "🤖 Android: $ANDROID_VERSION"
echo "🔧 CPU ABI: $CPU_ABI"
echo ""

echo "🎯 Validation Results:"
echo "  ARM64 Optimization: $(if [ "$ARM64_SUPPORT" = true ] && [ "$ARM64_BUILD" = true ] && [ "$CPU_BACKEND" = true ]; then echo "✅ COMPLETE"; else echo "❌ INCOMPLETE"; fi)"
echo "  Vulkan Integration: $(if [ "$VULKAN_LIBRARY" = true ] && [ "$VULKAN_CMAKE" = true ] && [ "$VULKAN_GRADLE" = true ]; then echo "✅ COMPLETE"; else echo "❌ INCOMPLETE"; fi)"
echo "  DirectWhisperService: $(if [ "$DIRECT_SERVICE" = true ] && [ "$SERVICE_REGISTERED" = true ]; then echo "✅ COMPLETE"; else echo "❌ INCOMPLETE"; fi)"
echo "  Broadcast Removal: $(if [ "$BROADCAST_REMOVED" = true ] && [ "$CONNECTOR_RECEIVER_REMOVED" = true ] && [ "$RESOURCE_RECEIVER_REMOVED" = true ]; then echo "✅ COMPLETE"; else echo "❌ INCOMPLETE"; fi)"
echo "  Performance: $(if [ "$BUILD_SUCCESS" = true ] && [ "$INSTALL_SUCCESS" = true ] && [ "$LAUNCH_SUCCESS" = true ]; then echo "✅ COMPLETE"; else echo "❌ INCOMPLETE"; fi)"

echo ""
echo "🎯 Xiaomi Pad inference optimization validation completed!"
echo "📖 See $REPORT_FILE for detailed analysis"
