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

### Job Scheduling System Architecture

**Device-Level Job Scheduling:**
The Xiaomi Pad runs a comprehensive job scheduling system that manages audio/video processing tasks through WorkManager, with dedicated input/output directories and background service coordination.

**Core Scheduling Components:**
- **WorkManager**: Android's job scheduling system for background tasks
- **AutoClipperService**: Background video processing service
- **WhisperWorker**: Audio transcription processing worker
- **ResourceMonitor**: Real-time device resource tracking
- **JobQueue**: Priority-based task queuing system

**Input/Output Directory Structure:**
```
/sdcard/Mira/
├── inbox/                    # Input directory
│   ├── audio/               # Audio files (.wav, .mp4)
│   ├── video/               # Video files (.mp4, .mov)
│   └── batch/               # Batch processing files
├── processing/              # Active processing directory
│   ├── temp/               # Temporary files during processing
│   ├── chunks/             # Audio/video chunks
│   └── intermediate/       # Intermediate processing files
├── output/                  # Output directory
│   ├── transcripts/        # Whisper transcription results
│   ├── clips/              # AutoClipper video clips
│   ├── metadata/           # Processing metadata
│   └── exports/            # Exported files (JSON, SRT, TXT)
├── models/                  # ML models storage
│   ├── whisper/            # Whisper model files
│   └── clip/               # CLIP model files
└── logs/                    # System logs
    ├── whisper/            # Whisper processing logs
    ├── autoclip/           # AutoClipper service logs
    └── system/             # System resource logs
```

**Job Scheduling Workflow:**
1. **File Detection**: Monitor `/sdcard/Mira/inbox/` for new files
2. **Job Creation**: Create WorkManager jobs for detected files
3. **Resource Check**: Verify device resources before processing
4. **Processing**: Execute Whisper/AutoClipper processing
5. **Output Generation**: Save results to `/sdcard/Mira/output/`
6. **Cleanup**: Remove temporary files from `/sdcard/Mira/processing/`
7. **Notification**: Send completion notifications

### Job Scheduling Configuration

**WorkManager Job Configuration:**
```kotlin
// Whisper processing job
val whisperJob = OneTimeWorkRequestBuilder<WhisperWorker>()
    .setInputData(workDataOf(
        "input_file" to "/sdcard/Mira/inbox/audio/sample.wav",
        "output_dir" to "/sdcard/Mira/output/transcripts/",
        "model_path" to "/sdcard/Mira/models/whisper/base.en.bin"
    ))
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .setRequiresBatteryNotLow(true)
        .setRequiresStorageNotLow(true)
        .build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .build()

// AutoClipper video processing job
val autoclipJob = OneTimeWorkRequestBuilder<AutoClipperWorker>()
    .setInputData(workDataOf(
        "input_file" to "/sdcard/Mira/inbox/video/sample.mp4",
        "output_dir" to "/sdcard/Mira/output/clips/",
        "model_path" to "/sdcard/Mira/models/clip/vit-b32.onnx"
    ))
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .setRequiresBatteryNotLow(true)
        .setRequiresStorageNotLow(true)
        .build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .build()
```

**Directory Management:**
```bash
# Create directory structure
adb shell mkdir -p /sdcard/Mira/inbox/audio
adb shell mkdir -p /sdcard/Mira/inbox/video
adb shell mkdir -p /sdcard/Mira/inbox/batch
adb shell mkdir -p /sdcard/Mira/processing/temp
adb shell mkdir -p /sdcard/Mira/processing/chunks
adb shell mkdir -p /sdcard/Mira/processing/intermediate
adb shell mkdir -p /sdcard/Mira/output/transcripts
adb shell mkdir -p /sdcard/Mira/output/clips
adb shell mkdir -p /sdcard/Mira/output/metadata
adb shell mkdir -p /sdcard/Mira/output/exports
adb shell mkdir -p /sdcard/Mira/models/whisper
adb shell mkdir -p /sdcard/Mira/models/clip
adb shell mkdir -p /sdcard/Mira/logs/whisper
adb shell mkdir -p /sdcard/Mira/logs/autoclip
adb shell mkdir -p /sdcard/Mira/logs/system

# Set permissions
adb shell chmod 755 /sdcard/Mira
adb shell chmod 755 /sdcard/Mira/inbox
adb shell chmod 755 /sdcard/Mira/output
adb shell chmod 755 /sdcard/Mira/processing
```

**Job Monitoring and Management:**
```bash
# Check WorkManager job status
adb shell dumpsys jobscheduler | grep com.mira.com

# Monitor AutoClipper service
adb shell dumpsys activity services com.mira.com | grep AutoClipper

# Check directory contents
adb shell ls -la /sdcard/Mira/inbox/
adb shell ls -la /sdcard/Mira/output/
adb shell ls -la /sdcard/Mira/processing/

# Monitor resource usage
adb shell dumpsys meminfo com.mira.com
adb shell top -p $(adb shell pidof com.mira.com)
```

**Job Scheduling Commands:**
```bash
# Trigger Whisper processing
adb shell am broadcast -a com.mira.com.action.WHISPER_RUN \
  --es input_file "/sdcard/Mira/inbox/audio/sample.wav" \
  --es output_dir "/sdcard/Mira/output/transcripts/"

# Trigger AutoClipper processing
adb shell am broadcast -a com.mira.com.action.AUTOCLIP_RUN \
  --es input_file "/sdcard/Mira/inbox/video/sample.mp4" \
  --es output_dir "/sdcard/Mira/output/clips/"

# Check job queue status
adb shell am broadcast -a com.mira.com.action.JOB_STATUS

# Cancel all pending jobs
adb shell am broadcast -a com.mira.com.action.CANCEL_JOBS
```

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

**Job Scheduling System Testing:**
```bash
# Test directory structure creation
./scripts/setup_xiaomi_pad_directories.sh

# Test WorkManager job creation
adb shell am broadcast -a com.mira.com.action.TEST_JOB_CREATION

# Test file processing pipeline
adb shell am broadcast -a com.mira.com.action.TEST_PROCESSING_PIPELINE

# Test resource monitoring
adb shell am broadcast -a com.mira.com.action.TEST_RESOURCE_MONITORING

# Test job queue management
adb shell am broadcast -a com.mira.com.action.TEST_JOB_QUEUE
```

**Basic Functionality:**
```bash
# Launch app
adb shell am start -n com.mira.com/.MainActivity

# Test Whisper functionality
adb shell am broadcast -a com.mira.com.action.WHISPER_RUN \
  --es input_file "/sdcard/Mira/inbox/audio/test.wav" \
  --es output_dir "/sdcard/Mira/output/transcripts/"

# Test AutoClipper functionality
adb shell am broadcast -a com.mira.com.action.AUTOCLIP_RUN \
  --es input_file "/sdcard/Mira/inbox/video/test.mp4" \
  --es output_dir "/sdcard/Mira/output/clips/"

# Check logs
adb logcat | grep -E "(Whisper|AutoClipper|Mira)"
```

**Resource Monitoring:**
```bash
# Start resource monitoring service
adb shell am startservice -n com.mira.com/com.mira.resource.DeviceResourceService

# Start AutoClipper service
adb shell am startservice -n com.mira.com/com.mira.clip.autoclip.AutoClipperService

# Monitor resource usage
adb shell dumpsys meminfo com.mira.com
adb shell top -p $(adb shell pidof com.mira.com)

# Monitor job queue
adb shell dumpsys jobscheduler | grep com.mira.com

# Check service status
adb shell dumpsys activity services com.mira.com | grep -E "(AutoClipper|Resource)"
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