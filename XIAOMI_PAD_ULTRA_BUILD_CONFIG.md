# Xiaomi Pad Ultra Build Configuration

## Build Variants for Xiaomi Pad Ultra

### Debug Build (Development)
```kotlin
buildTypes {
    create("xiaomiDebug") {
        initWith(getByName("debug"))
        applicationIdSuffix = ".xiaomi"
        versionNameSuffix = "-xiaomi-debug"
        
        // Xiaomi Pad Ultra specific debug settings
        buildConfigField("boolean", "XIAOMI_PAD_ULTRA", "true")
        buildConfigField("int", "MEMORY_BUDGET_MB", "1024")
        buildConfigField("boolean", "ENABLE_THERMAL_MONITORING", "true")
        buildConfigField("boolean", "ENABLE_GPU_BOOST", "true")
        buildConfigField("int", "DISPLAY_REFRESH_RATE", "120")
        buildConfigField("String", "DISPLAY_RESOLUTION", "\"2880x1920\"")
    }
}
```

### Release Build (Production)
```kotlin
buildTypes {
    create("xiaomiRelease") {
        initWith(getByName("release"))
        applicationIdSuffix = ".xiaomi"
        versionNameSuffix = "-xiaomi-release"
        
        // Xiaomi Pad Ultra specific release settings
        buildConfigField("boolean", "XIAOMI_PAD_ULTRA", "true")
        buildConfigField("int", "MEMORY_BUDGET_MB", "1024")
        buildConfigField("boolean", "ENABLE_THERMAL_MONITORING", "true")
        buildConfigField("boolean", "ENABLE_GPU_BOOST", "true")
        buildConfigField("int", "DISPLAY_REFRESH_RATE", "120")
        buildConfigField("String", "DISPLAY_RESOLUTION", "\"2880x1920\"")
        
        // Production optimizations
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules-xiaomi.pro"
        )
    }
}
```

## Xiaomi Pad Ultra Specific Dependencies

### Performance Libraries
```kotlin
dependencies {
    // Xiaomi Pad Ultra specific optimizations
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.concurrent:concurrent-futures:1.2.0")
    
    // Enhanced memory management
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    
    // Thermal monitoring
    implementation("androidx.core:core-ktx:1.12.0")
    
    // Display optimizations
    implementation("androidx.window:window:1.2.0")
}
```

## Build Commands

### Build Xiaomi Pad Ultra Debug
```bash
./gradlew :app:assembleXiaomiDebug
```

### Build Xiaomi Pad Ultra Release
```bash
./gradlew :app:assembleXiaomiRelease
```

### Install on Xiaomi Pad Ultra
```bash
# Debug version
adb install app/build/outputs/apk/xiaomiDebug/app-xiaomi-debug.apk

# Release version
adb install app/build/outputs/apk/xiaomiRelease/app-xiaomi-release.apk
```

## Performance Monitoring

### CPU Usage
```bash
adb shell top -n 1 | grep com.mira.com
```

### Memory Usage
```bash
adb shell dumpsys meminfo com.mira.com
```

### Thermal Status
```bash
adb shell cat /sys/class/thermal/thermal_zone*/temp
```

### Battery Usage
```bash
adb shell dumpsys batterystats com.mira.com
```

## Device-Specific Optimizations

### Snapdragon 8+ Gen 1 Optimizations
- **CPU Cores**: Utilize all 8 cores for CLIP processing
- **GPU**: Adreno 730 optimization for ML inference
- **Memory**: LPDDR5-6400 optimization
- **Storage**: UFS 3.1 sequential read optimization

### Display Optimizations
- **Resolution**: 2880x1920 native resolution
- **Refresh Rate**: 120Hz for smooth UI
- **Color Space**: P3 wide color gamut
- **HDR**: HDR10+ support for video preview

### Thermal Management
- **CPU Throttling**: Disabled during video processing
- **GPU Boost**: Enabled for CLIP inference
- **Fan Control**: Automatic fan speed adjustment
- **Thermal Zones**: Monitor all thermal sensors

## Testing Strategy

### Unit Tests
```bash
./gradlew :app:testXiaomiDebugUnitTest
```

### Instrumented Tests
```bash
./gradlew :app:connectedXiaomiDebugAndroidTest
```

### Performance Tests
```bash
./scripts/modules/xiaomi_pad_comprehensive_test.sh
```

### Stress Tests
```bash
./scripts/modules/xiaomi_performance_analyzer.sh
```

## Deployment Pipeline

### 1. Pre-deployment Checks
- [ ] Device connectivity verified
- [ ] ADB debugging enabled
- [ ] USB debugging authorized
- [ ] Developer options enabled

### 2. Build Process
- [ ] Clean build environment
- [ ] Build Xiaomi Pad Ultra variant
- [ ] Sign APK with release keystore
- [ ] Verify APK integrity

### 3. Installation
- [ ] Uninstall previous version
- [ ] Install new APK
- [ ] Verify installation
- [ ] Grant necessary permissions

### 4. Post-deployment Verification
- [ ] App launches successfully
- [ ] CLIP models load correctly
- [ ] Video processing works
- [ ] UI is responsive
- [ ] Performance metrics are acceptable

## Troubleshooting

### Common Issues
1. **Installation Failed**: Check USB debugging and authorization
2. **App Crashes**: Check logcat for error messages
3. **Performance Issues**: Monitor CPU and memory usage
4. **Thermal Throttling**: Check device temperature

### Debug Commands
```bash
# View logs
adb logcat | grep com.mira.com

# Check device info
adb shell getprop ro.product.model

# Monitor performance
adb shell top -n 1

# Check thermal status
adb shell cat /sys/class/thermal/thermal_zone*/temp
```
