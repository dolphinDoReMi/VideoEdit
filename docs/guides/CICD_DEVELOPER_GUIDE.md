# AutoCutPad CI/CD & Developer Guide

## 🚀 Overview
Complete CI/CD pipeline and developer guide for AutoCutPad - an AI-powered video editing application targeting Xiaomi Pad Ultra and Android devices. This guide covers development environment setup, automated testing, building, and deployment processes.

---

## 📋 Prerequisites & Environment Setup

### Development Environment
```bash
# macOS Development Stack
brew install --cask android-studio
brew install --cask android-commandlinetools
brew install --cask android-platform-tools
brew install openjdk@17
brew install gradle
brew install imagemagick
brew install scrcpy

# Environment Variables
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT=$ANDROID_HOME
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

### Android SDK Components
```bash
# Install required SDK components
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" "emulator"
sdkmanager "system-images;android-35;google_apis;arm64-v8a"

# Accept licenses
yes | sdkmanager --licenses

# Create AVD for testing
avdmanager create avd -n "XiaomiPadUltra" -k "system-images;android-35;google_apis;arm64-v8a" -d "pixel_c"
```

---

## 🏗️ Project Architecture

### Core Components
- **MainActivity.kt**: Jetpack Compose UI with Material 3 design
- **AutoCutEngine.kt**: Media3 Transformer video processing engine
- **VideoScorer.kt**: Motion analysis using frame difference algorithms
- **MediaStoreExt.kt**: Storage Access Framework utilities
- **AutoCutApplication.kt**: Application class with lifecycle management

### Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Video Processing**: Media3 Transformer 1.8.0
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Build System**: Gradle with Kotlin DSL

### Dependencies
```kotlin
// Media3 for video processing
implementation("androidx.media3:media3-transformer:1.8.0")
implementation("androidx.media3:media3-effect:1.8.0")
implementation("androidx.media3:media3-common:1.8.0")

// UI
implementation(platform("androidx.compose:compose-bom:2025.09.01"))
implementation("androidx.activity:activity-compose:1.9.2")
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

// Optional ML Kit face detection
implementation("com.google.mlkit:face-detection:16.1.7")
```

---

## 🔧 Local Development Workflow

### 1. Project Setup
```bash
# Clone and setup
git clone <repository-url>
cd VideoEdit

# Create Gradle wrapper
gradle wrapper

# Build project
./gradlew build
```

### 2. Device Testing
```bash
# Connect Xiaomi Pad Ultra
adb devices

# Build and install
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.autocutpad.videoeditor/.MainActivity

# Screen mirroring for testing
scrcpy --max-size 1920 --bit-rate 8M
```

### 3. MIUI Device Configuration
```bash
# Enable developer options on Xiaomi Pad Ultra
# Settings → About tablet → Tap "MIUI version" 7 times

# Enable USB debugging
# Settings → Additional settings → Developer options
# Enable: USB debugging, Install via USB, USB debugging (Security settings)

# Disable package verification
adb shell settings put global package_verifier_enable 0
```

---

## 🏗️ CI/CD Pipeline

### GitHub Actions Workflow
```yaml
# .github/workflows/android-ci.yml
name: Android CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: macos-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Setup Android SDK
      uses: android-actions/setup-android@v3
      with:
        api-level: 35
        build-tools: 35.0.0
        ndk-version: latest
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Run lint
      run: ./gradlew lint
    
    - name: Build debug APK
      run: ./gradlew assembleDebug
    
    - name: Build release APK
      run: ./gradlew assembleRelease
    
    - name: Upload APK artifacts
      uses: actions/upload-artifact@v3
      with:
        name: app-debug-apk
        path: app/build/outputs/apk/debug/app-debug.apk
    
    - name: Upload release APK
      uses: actions/upload-artifact@v3
      with:
        name: app-release-apk
        path: app/build/outputs/apk/release/app-release.apk
```

### Automated Testing Pipeline
```yaml
# .github/workflows/android-test.yml
name: Android Testing Pipeline

on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  workflow_dispatch:

jobs:
  emulator-test:
    runs-on: macos-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Setup Android SDK
      uses: android-actions/setup-android@v3
    
    - name: Create AVD
      run: |
        echo "y" | sdkmanager "system-images;android-35;google_apis;arm64-v8a"
        echo "no" | avdmanager create avd -n test -k "system-images;android-35;google_apis;arm64-v8a" -d "pixel_c"
    
    - name: Start Emulator
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 35
        target: google_apis
        arch: arm64-v8a
        profile: pixel_c
        script: |
          ./gradlew assembleDebug
          adb install app/build/outputs/apk/debug/app-debug.apk
          adb shell am start -n com.autocutpad.videoeditor/.MainActivity
          # Add UI tests here
```

---

## 📦 Build Configuration

### Build Variants
- **Debug**: `com.autocutpad.videoeditor.debug` - Full logging, no obfuscation
- **Internal**: `com.autocutpad.videoeditor.internal` - Logging enabled, no obfuscation  
- **Release**: `com.autocutpad.videoeditor` - Optimized, obfuscated, no logging

### Gradle Configuration
```kotlin
// build.gradle.kts (Project level)
plugins {
    id("com.android.application") version "8.12.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.21" apply false
}

// app/build.gradle.kts
android {
    namespace = "com.autocutpad.videoeditor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.autocutpad.videoeditor"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures { 
        compose = true
        buildConfig = true
    }
}
```

---

## 🚀 Deployment Pipeline

### Release Automation
```yaml
# .github/workflows/release.yml
name: Release Pipeline

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: macos-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Setup Android SDK
      uses: android-actions/setup-android@v3
    
    - name: Build Release APK
      run: ./gradlew assembleRelease
      env:
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
    
    - name: Sign APK
      run: |
        jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
          -keystore keystore/autocutpad-release.keystore \
          -storepass $KEYSTORE_PASSWORD \
          -keypass $KEY_PASSWORD \
          app/build/outputs/apk/release/app-release-unsigned.apk \
          autocutpad
    
    - name: Create Release
      uses: actions/create-release@v1
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      with:
        tag_name: ${{ github.ref }}
        release_name: Release ${{ github.ref }}
        draft: false
        prerelease: false
    
    - name: Upload Release Asset
      uses: actions/upload-release-asset@v1
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      with:
        upload_url: ${{ steps.create_release.outputs.upload_url }}
        asset_path: app/build/outputs/apk/release/app-release.apk
        asset_name: AutoCutPad-${{ github.ref_name }}.apk
        asset_content_type: application/vnd.android.package-archive
```

---

## 🧪 Testing Strategy

### Unit Tests
```bash
# Run unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.autocutpad.videoeditor.VideoScorerTest"
```

### Integration Tests
```bash
# Run instrumentation tests
./gradlew connectedAndroidTest

# Run on specific device
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.autocutpad.videoeditor.MainActivityTest
```

### UI Tests
```kotlin
// Example UI test
@Test
fun testVideoSelection() {
    onView(withId(R.id.video_selector))
        .perform(click())
    
    onView(withText("Select Video"))
        .check(matches(isDisplayed()))
}
```

### Core Capabilities Testing
The app includes comprehensive testing for:
- **AI-Powered Motion Analysis**: Three-frame difference analysis with 96x54 pixel scaling
- **Smart Segment Selection**: Motion intensity-based ranking and selection
- **Media3 Video Processing**: H.264 hardware-accelerated encoding
- **Android Integration**: Storage Access Framework (SAF) for secure file access

---

## 📱 Device-Specific Testing

### Xiaomi Pad Ultra Testing
```bash
# Device detection
adb devices

# Install and test
adb -s <device-id> install app/build/outputs/apk/debug/app-debug.apk
adb -s <device-id> shell am start -n com.autocutpad.videoeditor/.MainActivity

# Performance monitoring
adb -s <device-id> shell top | grep autocutpad
adb -s <device-id> shell dumpsys meminfo com.autocutpad.videoeditor
```

### Emulator Testing
```bash
# Start emulator
emulator -avd XiaomiPadUltra &

# Run tests
./gradlew connectedAndroidTest
```

---

## 🔐 Security & Signing

### Keystore Management
```bash
# Generate keystore
keytool -genkey -v -keystore keystore/autocutpad-release.keystore \
  -alias autocutpad -keyalg RSA -keysize 2048 -validity 10000

# Sign APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore keystore/autocutpad-release.keystore \
  app-release-unsigned.apk autocutpad
```

### Environment Variables
```bash
# GitHub Secrets
KEYSTORE_PASSWORD=<keystore-password>
KEY_PASSWORD=<key-password>
XIAOMI_APP_KEY=<xiaomi-store-key>
```

---

## 📊 Monitoring & Analytics

### Build Monitoring
```yaml
# Add to CI pipeline
- name: Build Analysis
  run: |
    ./gradlew build --scan
    ./gradlew dependencyCheckAnalyze
```

### Performance Monitoring
```kotlin
// Add to app
implementation("com.google.firebase:firebase-analytics:21.5.0")
implementation("com.google.firebase:firebase-crashlytics:18.6.1")
```

---

## 🚀 Quick Commands

### Development
```bash
# Build and run
./gradlew assembleDebug && adb install app/build/outputs/apk/debug/app-debug.apk

# Clean and rebuild
./gradlew clean build

# Run tests
./gradlew test connectedAndroidTest

# Generate release
./gradlew assembleRelease
```

### CI/CD
```bash
# Local CI simulation
./gradlew clean test lint assembleDebug assembleRelease

# Deploy to device
adb devices
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📋 Checklist

### Pre-commit
- [ ] Code compiles without errors
- [ ] Unit tests pass
- [ ] Lint checks pass
- [ ] APK builds successfully

### Pre-release
- [ ] All tests pass
- [ ] Performance benchmarks met
- [ ] Security scan completed
- [ ] APK signed and verified
- [ ] Xiaomi Pad Ultra compatibility tested

### Post-deployment
- [ ] App launches successfully
- [ ] Video processing works
- [ ] UI responsive on large screen
- [ ] No crashes or ANRs
- [ ] Performance metrics collected

---

## 🔧 Troubleshooting

### Common Issues

#### Build Failures
```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Check Gradle version
./gradlew --version

# Sync project
./gradlew --refresh-dependencies
```

#### Device Connection Issues
```bash
# Restart ADB
adb kill-server
adb start-server

# Check devices
adb devices

# Enable USB debugging on device
# Settings → Developer options → USB debugging
```

#### MIUI Installation Issues
```bash
# Disable package verification
adb shell settings put global package_verifier_enable 0

# Enable installation via USB
# Settings → Developer options → Install via USB
```

### Performance Optimization
```bash
# Monitor memory usage
adb shell dumpsys meminfo com.autocutpad.videoeditor

# Check CPU usage
adb shell top | grep autocutpad

# Monitor thermal status
adb shell cat /sys/class/thermal/thermal_zone*/temp
```

---

## 📚 Additional Resources

- [Android Developer Guide](https://developer.android.com/guide)
- [Media3 Documentation](https://developer.android.com/guide/topics/media/media3)
- [Xiaomi Developer Resources](https://dev.mi.com/)
- [GitHub Actions for Android](https://github.com/actions/android-actions)
- [Gradle Build Tool](https://gradle.org/)

---

This CI/CD pipeline ensures reliable, automated development and deployment of AutoCutPad video editing app for Xiaomi Pad Ultra and Android devices.
