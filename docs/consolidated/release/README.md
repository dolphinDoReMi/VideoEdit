# Release

## Release Strategy

### 1. **Android Release**
- **Target Platforms**: Android 8.0+ (API 26+)
- **Build Variants**: Debug, Internal, Release
- **Distribution**: Firebase App Distribution, Google Play Store, Xiaomi Store
- **Signing**: Production keystore for release builds

### 2. **iOS Release** (Planned)
- **Target Platforms**: iOS 14.0+
- **Build System**: Capacitor + Xcode
- **Distribution**: TestFlight, App Store
- **Signing**: Apple Developer certificates

### 3. **macOS Web Version** (Planned)
- **Target Platforms**: macOS 10.15+
- **Build System**: Capacitor + Electron
- **Distribution**: Mac App Store, Direct download
- **Signing**: Apple Developer certificates

## Current Release Status

### Android v0.7.0 (Current)
- **Status**: Development Complete
- **Features**: CLIP integration, Firebase setup, Policy guard system
- **Build Variants**: Debug, Internal, Release
- **Distribution**: Firebase App Distribution ready
- **Store Submission**: Ready for Play Store and Xiaomi Store

### Release Timeline
- **v0.7.0**: Firebase & Keystore Setup (2025-01-03)
- **v0.6.0**: Offline Index Build & Online Retrieval System (2025-01-03)
- **v0.5.0**: FAISS Vector Indexing System (2025-01-03)
- **v0.4.0**: Temporal Sampling System (2025-01-03)
- **v0.3.0**: Metric Space & Serialization Layer (2025-01-03)
- **v0.2.0**: CLIP ViT-B/32 Implementation (2025-01-03)
- **v0.1.0**: Initial Project Setup (2025-01-02)

## Build Configuration

### Android Build Variants
```kotlin
buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-debug"
        isDebuggable = true
    }
    
    create("internal") {
        applicationIdSuffix = ".internal"
        versionNameSuffix = "-internal"
        initWith(getByName("release"))
    }
    
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        signingConfig = signingConfigs.getByName("release")
    }
}
```

### Firebase Configuration
```kotlin
firebaseAppDistribution {
    appId = "1:384262830567:android:1960eb5e2470beb09ce542"
    groups = "internal-testers"
    releaseNotes = """..."""
}
```

### Keystore Configuration
```properties
KEYSTORE_FILE=../keystore/mira-release.keystore
KEYSTORE_PASSWORD=Mira2024!
KEY_ALIAS=mira
KEY_PASSWORD=Mira2024!
```

## Distribution Channels

### 1. **Firebase App Distribution**
- **Purpose**: Internal testing and beta distribution
- **Audience**: Internal testers and beta users
- **Features**: Automatic notifications, release notes, crash reporting
- **Setup**: Configured with app ID and tester groups

### 2. **Google Play Store**
- **Purpose**: Public Android distribution
- **Requirements**: AAB format, Play Console setup, store listing
- **Signing**: Production keystore required
- **Status**: Ready for submission

### 3. **Xiaomi Store**
- **Purpose**: Alternative Android distribution
- **Requirements**: APK format, Xiaomi Developer account
- **Signing**: Alternative keystore (autocutpad-release.keystore)
- **Status**: Ready for submission

## Release Process

### 1. **Development Phase**
- Feature development in feature branches
- Policy guard enforcement via git hooks
- Automated testing via GitHub Actions
- Code review and approval process

### 2. **Testing Phase**
- Internal testing via Firebase App Distribution
- Automated testing suite execution
- Performance and stability validation
- User acceptance testing

### 3. **Release Phase**
- Version bump and changelog update
- Production build generation
- Store submission preparation
- Release notes and documentation

### 4. **Distribution Phase**
- Firebase App Distribution for internal testing
- Store submission for public release
- Monitoring and crash reporting
- User feedback collection

## Quality Assurance

### 1. **Automated Testing**
- Unit tests for core functionality
- Instrumented tests for end-to-end validation
- Policy guard enforcement for code quality
- Build verification for all variants

### 2. **Manual Testing**
- Feature functionality validation
- Performance testing on target devices
- User experience testing
- Compatibility testing across Android versions

### 3. **Release Validation**
- Build artifact verification
- Signing certificate validation
- Firebase configuration validation
- Store submission requirements check

## Monitoring and Analytics

### 1. **Firebase Analytics** (Planned)
- User behavior tracking
- Feature usage analytics
- Performance monitoring
- Crash reporting

### 2. **Firebase Crashlytics** (Planned)
- Crash reporting and analysis
- Stability monitoring
- Error tracking and resolution
- User impact assessment

### 3. **Firebase Performance** (Planned)
- App performance monitoring
- Network performance tracking
- User experience metrics
- Performance optimization insights

## Future Releases

### 1. **iOS Release** (Q2 2025)
- Capacitor integration
- iOS-specific optimizations
- App Store submission
- TestFlight distribution

### 2. **macOS Web Version** (Q3 2025)
- Electron integration
- Desktop-specific features
- Mac App Store submission
- Direct download distribution

### 3. **Web Version** (Q4 2025)
- Progressive Web App (PWA)
- Browser-based video processing
- Cloud-based processing options
- Cross-platform compatibility
