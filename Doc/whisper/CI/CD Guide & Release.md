# Whisper CI/CD Guide & Release

## CI/CD Pipeline Overview

### Workflow Configuration
**File**: `.github/workflows/robust-lid-cicd.yml`

### Pipeline Jobs

#### 1. Code Quality
- **Detekt**: Static analysis for Kotlin code
- **Unit Tests**: `./gradlew :app:testDebugUnitTest`
- **Lint**: `./gradlew :app:lintDebug`

#### 2. Build & Test
- **Debug APK**: `./gradlew :app:assembleDebug`
- **Release APK**: `./gradlew :app:assembleRelease`
- **Artifact Upload**: Debug and Release APKs

#### 3. LID Validation
- **Core Implementation**: Verify LanguageDetectionService
- **Parameter Validation**: Check WhisperParams LID parameters
- **Pipeline Integration**: Validate TranscribeWorker LID pipeline
- **Model Selection**: Confirm multilingual model usage

#### 4. Documentation Validation
- **Implementation Docs**: ROBUST_LID_IMPLEMENTATION.md
- **Background Docs**: BACKGROUND_LID_IMPLEMENTATION.md
- **Deployment Scripts**: Validate script completeness

#### 5. Integration Tests
- **Device Testing**: Conditional on device availability
- **LID Pipeline**: End-to-end validation
- **Performance Metrics**: RTF and accuracy verification

#### 6. Deployment Preparation
- **Package Creation**: Robust LID deployment package
- **Script Validation**: Deployment and testing scripts
- **Documentation**: Implementation guides

#### 7. Auto Merge
- **Whisper → Main**: Automatic merge on whisper branch push
- **Release Creation**: Automated versioning and release notes

## Release Process

### Version Strategy
- **Major**: Breaking changes or new features
- **Minor**: New functionality, backward compatible
- **Patch**: Bug fixes and improvements

### Release Workflow

#### 1. Feature Development
```bash
# Create feature branch
git checkout -b feature/robust-lid

# Implement changes
# ... development work ...

# Commit with conventional format
git commit -m "feat: implement robust LID pipeline"

# Push to feature branch
git push origin feature/robust-lid
```

#### 2. Integration Testing
```bash
# Merge to whisper branch
git checkout whisper
git merge feature/robust-lid

# Push to trigger CI/CD
git push origin whisper
```

#### 3. Automated Release
- CI/CD pipeline automatically merges whisper → main
- Creates release with version tag
- Generates release notes
- Uploads deployment artifacts

### Release Artifacts

#### Android APKs
- **Debug APK**: `app-debug.apk`
- **Release APK**: `app-release.apk`
- **Architecture**: arm64-v8a optimized

#### Deployment Package
```
robust-lid-deployment/
├── LanguageDetectionService.kt
├── TranscribeWorker.kt
├── WhisperApi.kt
├── WhisperParams.kt
├── WhisperBridge.kt
├── deploy_multilingual_models.sh
├── test_lid_pipeline.sh
├── ROBUST_LID_IMPLEMENTATION.md
└── BACKGROUND_LID_IMPLEMENTATION.md
```

## Platform-Specific Releases

### Android Release

#### Build Configuration
```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        debug {
            applicationIdSuffix ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

#### Signing Configuration
```kotlin
// keystore configuration
signingConfigs {
    create("release") {
        storeFile = file("keystore/release.keystore")
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

#### Release Process
```bash
# Build release APK
./gradlew :app:assembleRelease

# Sign APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
    -keystore keystore/release.keystore \
    app-release-unsigned.apk release

# Align APK
zipalign -v 4 app-release-unsigned.apk app-release.apk
```

### iOS Release

#### Build Configuration
```swift
// WhisperKit configuration
let config = WhisperKitConfig(
    modelFolder: Bundle.main.url(forResource: "whisper-small.q5_1", withExtension: "bin")!,
    computeUnits: .cpuAndGPU,
    verbose: true
)
```

#### Release Process
```bash
# Build iOS app
xcodebuild -workspace ios/App/App.xcworkspace \
    -scheme App -configuration Release \
    -destination 'generic/platform=iOS' \
    clean archive -archivePath ios/build/App.xcarchive

# Export for App Store
xcodebuild -exportArchive \
    -archivePath ios/build/App.xcarchive \
    -exportOptionsPlist ios/build/ExportOptions.plist \
    -exportPath ios/build
```

### macOS Web Version Release

#### WebAssembly Build
```bash
# Build whisper.cpp for WebAssembly
cd whisper.cpp
make wasm

# Generate optimized WebAssembly module
emcc -O3 -s WASM=1 -s EXPORTED_FUNCTIONS="['_whisper_init', '_whisper_decode']" \
     -s ALLOW_MEMORY_GROWTH=1 -s INITIAL_MEMORY=64MB \
     whisper.cpp -o whisper.wasm
```

#### Web Distribution
```bash
# Package web assets
mkdir -p web-dist/
cp whisper.wasm web-dist/
cp whisper.js web-dist/
cp index.html web-dist/

# Create distribution package
tar -czf whisper-web-v1.0.0.tar.gz web-dist/
```

## Quality Assurance

### Testing Strategy

#### Unit Tests
```kotlin
@Test
fun testLanguageDetection() {
    val lidService = LanguageDetectionService()
    val result = lidService.detectLanguage(pcm16, 16000, modelPath, 4)
    
    assertThat(result.chosen).isEqualTo("zh")
    assertThat(result.confidence).isGreaterThan(0.80)
}
```

#### Integration Tests
```bash
# Test LID pipeline
./test_lid_pipeline.sh

# Test multilingual support
./test_multilingual_lid.sh

# Test device deployment
./work_through_xiaomi_pad.sh
```

#### Performance Tests
```bash
# Benchmark RTF performance
./benchmark_rtf.sh

# Memory usage analysis
./profile_memory.sh

# Accuracy validation
./validate_accuracy.sh
```

### Code Quality Metrics

#### Static Analysis
- **Detekt**: Kotlin code analysis
- **Lint**: Android-specific checks
- **Code Coverage**: Unit test coverage

#### Performance Metrics
- **RTF**: Real-Time Factor < 1.0
- **Memory**: < 200MB for base model
- **Accuracy**: Chinese detection > 85%

### Security Considerations

#### Model Security
- **SHA-256 Verification**: Model integrity checks
- **Secure Storage**: Encrypted model storage
- **Access Control**: Permission-based model access

#### Data Privacy
- **Local Processing**: No cloud data transmission
- **Temporary Files**: Secure cleanup of audio files
- **Sidecar Data**: Encrypted metadata storage

## Deployment Automation

### CI/CD Triggers

#### Push Triggers
- **whisper branch**: Full CI/CD pipeline
- **main branch**: Release deployment
- **feature branches**: Code quality checks

#### Pull Request Triggers
- **Code Review**: Automated quality checks
- **Integration Tests**: Feature validation
- **Documentation**: Update verification

### Automated Deployment

#### Development Environment
```bash
# Auto-deploy to development
./deploy_dev.sh

# Run smoke tests
./smoke_tests.sh
```

#### Staging Environment
```bash
# Deploy to staging
./deploy_staging.sh

# Run integration tests
./integration_tests.sh
```

#### Production Environment
```bash
# Deploy to production
./deploy_production.sh

# Run validation tests
./validate_production.sh
```

## Monitoring and Observability

### Metrics Collection

#### Performance Metrics
- **RTF Distribution**: Real-time factor statistics
- **Memory Usage**: Peak and average memory consumption
- **Processing Time**: End-to-end processing duration
- **Accuracy Rates**: Language detection accuracy

#### Error Tracking
- **Processing Failures**: Error rate and types
- **Model Loading**: Model deployment success rate
- **Device Compatibility**: Platform-specific issues

### Alerting

#### Critical Alerts
- **Processing Failures**: > 5% error rate
- **Performance Degradation**: RTF > 2.0
- **Memory Leaks**: Memory usage > 500MB

#### Warning Alerts
- **Accuracy Drop**: Detection accuracy < 80%
- **Slow Processing**: RTF > 1.5
- **Resource Usage**: High CPU/memory usage

## Release Notes Template

### Version 1.0.0 - Robust LID Pipeline

#### 🎯 New Features
- **Robust Language Detection**: VAD windowing + two-pass re-scoring
- **Multilingual Model Support**: whisper-base.q5_1.bin integration
- **Background Processing**: WorkManager-based non-blocking architecture
- **Enhanced Logging**: LID data in sidecar files

#### 🔧 Technical Improvements
- **LanguageDetectionService**: VAD + two-pass LID implementation
- **TranscribeWorker**: Background LID pipeline integration
- **WhisperApi**: Multilingual model selection
- **Enhanced Sidecar**: LID data with confidence scores

#### 📊 Performance Improvements
- **Chinese Detection**: 60% → 85%+ accuracy
- **Code-switching**: Poor → Good detection
- **Processing**: UI-blocking → Background worker
- **RTF**: 0.3-0.8 (device-dependent)

#### 🚀 Deployment
- **Android**: Xiaomi Pad Ultra optimized
- **iOS**: iPad Pro with WhisperKit
- **Web**: macOS WebAssembly support

#### 📚 Documentation
- **Architecture Guide**: Complete implementation details
- **Deployment Guide**: Platform-specific instructions
- **Testing Framework**: Comprehensive validation scripts

## Troubleshooting

### Common Issues

#### Build Failures
```bash
# Clean and rebuild
./gradlew clean
./gradlew :app:assembleDebug

# Check dependencies
./gradlew :app:dependencies
```

#### Deployment Issues
```bash
# Verify device connection
adb devices

# Check permissions
adb shell "pm list permissions | grep mira"

# Monitor logs
adb logcat | grep -i whisper
```

#### Performance Issues
```bash
# Check device resources
adb shell "top -n 1"

# Monitor memory usage
adb shell "cat /proc/meminfo"

# Verify model deployment
adb shell "ls -la /sdcard/MiraWhisper/models/"
```

### Support Channels

#### Documentation
- **Architecture**: Doc/whisper/Architecture Design and Control Knot.md
- **Implementation**: Doc/whisper/Full scale implementation Details.md
- **Deployment**: Doc/whisper/Device Deployment.md

#### Scripts
- **Validation**: validate_cicd_pipeline.sh
- **Testing**: test_lid_pipeline.sh
- **Deployment**: deploy_multilingual_models.sh
