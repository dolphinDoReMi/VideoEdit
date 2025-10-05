# Whisper Device Deployment

## Xiaomi Pad Deployment

### Target Device Specifications
- **Device**: Xiaomi Pad 6
- **OS**: Android 13+ (API Level 33+)
- **Architecture**: ARM64-v8a
- **Screen**: 11" 2560x1600 (WQXGA)
- **RAM**: 8GB LPDDR5
- **Storage**: 128GB/256GB UFS 3.1
- **GPU**: Adreno 650

### Prerequisites

**Development Environment:**
```bash
# Android SDK
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# ADB connection
adb devices
# Should show: List of devices attached
#              [DEVICE_ID]    device
```

**Device Setup:**
```bash
# Enable Developer Options
# Settings > About Tablet > Tap "MIUI Version" 7 times

# Enable USB Debugging
# Settings > Additional Settings > Developer Options > USB Debugging

# Enable Install via USB
# Settings > Additional Settings > Developer Options > Install via USB
```

### Build and Deploy

**Debug Build:**
```bash
# Build debug APK
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Verify installation
adb shell pm list packages | grep com.mira.videoeditor
```

**Release Build:**
```bash
# Build release APK
./gradlew assembleRelease

# Install on device
adb install app/build/outputs/apk/release/app-release.apk
```

### Testing

**Basic Functionality:**
```bash
# Launch app
adb shell am start -n com.mira.videoeditor/.MainActivity

# Test Whisper functionality
adb shell am broadcast -a com.mira.videoeditor.action.DECODE_URI \
  --es uri "file:///sdcard/test_audio.wav"

# Check logs
adb logcat | grep -E "(Whisper|VideoEdit)"
```

**Resource Monitoring:**
```bash
# Start resource monitoring service
adb shell am startservice -n com.mira.videoeditor/com.mira.resource.DeviceResourceService

# Monitor resource usage
adb shell dumpsys meminfo com.mira.videoeditor
adb shell top -p $(adb shell pidof com.mira.videoeditor)
```

**Performance Testing:**
```bash
# Run performance tests
./gradlew connectedDebugAndroidTest --tests "*PerformanceTest*"

# Benchmark Whisper processing
./scripts/test_whisper_performance.sh
```

### Troubleshooting

**Common Issues:**

1. **Installation Failed:**
```bash
# Clear previous installation
adb uninstall com.mira.videoeditor
adb install app/build/outputs/apk/debug/app-debug.apk
```

2. **Permission Denied:**
```bash
# Grant permissions manually
adb shell pm grant com.mira.videoeditor android.permission.RECORD_AUDIO
adb shell pm grant com.mira.videoeditor android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.mira.videoeditor android.permission.WRITE_EXTERNAL_STORAGE
```

3. **Service Not Starting:**
```bash
# Check service status
adb shell dumpsys activity services com.mira.videoeditor

# Restart service
adb shell am force-stop com.mira.videoeditor
adb shell am start -n com.mira.videoeditor/.MainActivity
```

## iPad Deployment

### Target Device Specifications
- **Device**: iPad Pro (12.9-inch, 6th generation)
- **OS**: iOS 16+ (iPadOS 16+)
- **Architecture**: ARM64 (Apple M2)
- **Screen**: 12.9" 2732x2048 (Retina)
- **RAM**: 8GB/16GB unified memory
- **Storage**: 128GB/256GB/512GB/1TB/2TB SSD
- **GPU**: Apple M2 GPU (10-core)

### Prerequisites

**Development Environment:**
```bash
# Xcode
xcode-select --install

# CocoaPods
sudo gem install cocoapods

# Capacitor CLI
npm install -g @capacitor/cli
```

**Device Setup:**
```bash
# Enable Developer Mode
# Settings > Privacy & Security > Developer Mode > On

# Trust Developer Certificate
# Settings > General > VPN & Device Management > Developer App
```

### Build and Deploy

**Web Build:**
```bash
# Build web assets
pnpm build
```

**iOS Sync:**
```bash
# Sync Capacitor
pnpm exec cap sync ios

# Install CocoaPods
cd ios/App && pod install --repo-update && cd -
```

**iOS Build:**
```bash
# Build for device
cd ios/App
agvtool next-version -all
cd -
xcodebuild -workspace ios/App/App.xcworkspace \
  -scheme App -configuration Release \
  -destination 'generic/platform=iOS' \
  -allowProvisioningUpdates \
  clean build
```

### Testing

**Basic Functionality:**
```bash
# Install on device
xcrun devicectl device install app --device [DEVICE_ID] ios/build/App.ipa

# Launch app
xcrun devicectl device launch --device [DEVICE_ID] com.mira.videoeditor
```

**Performance Testing:**
```bash
# Run XCTest
xcodebuild test \
  -workspace ios/App/App.xcworkspace \
  -scheme App \
  -destination 'platform=iOS Simulator,name=iPad Pro (12.9-inch) (6th generation)'
```

**Resource Monitoring:**
```bash
# Monitor memory usage
xcrun simctl spawn booted log stream --predicate 'process == "VideoEdit"'

# Monitor CPU usage
xcrun simctl spawn booted top -pid $(xcrun simctl spawn booted pgrep VideoEdit)
```

### Troubleshooting

**Common Issues:**

1. **Build Failed:**
```bash
# Clean build
cd ios/App
xcodebuild clean
pod install --repo-update
cd -
xcodebuild -workspace ios/App/App.xcworkspace -scheme App clean build
```

2. **Code Signing Issues:**
```bash
# Check certificates
security find-identity -v -p codesigning

# Update provisioning profiles
# Xcode > Preferences > Accounts > Download Manual Profiles
```

3. **WebView Issues:**
```bash
# Check WebView version
# Safari > Develop > [Device] > Web Inspector
```

## Cross-Platform Testing

### Test Matrix

| Feature | Xiaomi Pad | iPad | Status |
|---------|------------|------|--------|
| Whisper ASR | ✅ | ✅ | Verified |
| Resource Monitoring | ✅ | ✅ | Verified |
| WebView Bridge | ✅ | ✅ | Verified |
| File I/O | ✅ | ✅ | Verified |
| Background Services | ✅ | ⚠️ | iOS Limited |

### Automated Testing

**Android Testing:**
```bash
# Run all Android tests
./gradlew connectedDebugAndroidTest

# Run specific test suite
./gradlew connectedDebugAndroidTest --tests "*WhisperTest*"
```

**iOS Testing:**
```bash
# Run iOS tests
xcodebuild test \
  -workspace ios/App/App.xcworkspace \
  -scheme App \
  -destination 'platform=iOS Simulator,name=iPad Pro (12.9-inch) (6th generation)'
```

**Cross-Platform Validation:**
```bash
# Run cross-platform tests
./scripts/test_cross_platform.sh
```

### Performance Benchmarks

**Android Benchmarks:**
```bash
# Memory usage
adb shell dumpsys meminfo com.mira.videoeditor | grep "TOTAL"

# CPU usage
adb shell top -p $(adb shell pidof com.mira.videoeditor) -n 1

# Battery usage
adb shell dumpsys batterystats com.mira.videoeditor
```

**iOS Benchmarks:**
```bash
# Memory usage
xcrun simctl spawn booted log stream --predicate 'process == "VideoEdit"' | grep "Memory"

# CPU usage
xcrun simctl spawn booted top -pid $(xcrun simctl spawn booted pgrep VideoEdit) -l 1
```

## Deployment Validation

### Pre-Deployment Checklist

**Android:**
- [ ] APK builds successfully
- [ ] All permissions granted
- [ ] Background services start
- [ ] Resource monitoring active
- [ ] Whisper processing works
- [ ] File I/O operations successful
- [ ] Performance benchmarks met

**iOS:**
- [ ] App builds successfully
- [ ] Code signing configured
- [ ] WebView loads correctly
- [ ] Whisper processing works
- [ ] File I/O operations successful
- [ ] Performance benchmarks met

### Post-Deployment Validation

**Functional Testing:**
```bash
# Test core functionality
./scripts/test_deployment_validation.sh

# Verify resource monitoring
./scripts/test_resource_monitoring.sh

# Check performance metrics
./scripts/test_performance_benchmarks.sh
```

**User Acceptance Testing:**
- [ ] App launches successfully
- [ ] UI is responsive
- [ ] Whisper processing completes
- [ ] Resource monitoring displays data
- [ ] No crashes or errors
- [ ] Performance is acceptable

## Monitoring and Maintenance

### Crash Reporting

**Android (Firebase Crashlytics):**
```kotlin
// Automatic crash reporting
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
```

**iOS (Firebase Crashlytics):**
```swift
// Automatic crash reporting
Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
```

### Performance Monitoring

**Custom Metrics:**
```kotlin
// Track Whisper processing time
val startTime = System.currentTimeMillis()
// ... processing ...
val processingTime = System.currentTimeMillis() - startTime
FirebasePerformance.getInstance().newTrace("whisper_processing").apply {
    start()
    putMetric("processing_time_ms", processingTime)
    stop()
}
```

### Update Strategy

**Android Updates:**
```bash
# Incremental updates
adb install -r app/build/outputs/apk/release/app-release.apk

# Full reinstall
adb uninstall com.mira.videoeditor
adb install app/build/outputs/apk/release/app-release.apk
```

**iOS Updates:**
```bash
# TestFlight updates
# Upload new build to TestFlight
# Distribute to testers

# App Store updates
# Submit new version for review
# Release when approved
```