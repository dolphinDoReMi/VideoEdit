# Whisper CI/CD Guide & Release

## Android Release Pipeline

### Build Configuration

**Debug Variant:**
```gradle
android {
    buildTypes {
        debug {
            applicationIdSuffix ".debug"
            debuggable true
            minifyEnabled false
        }
    }
}
```

**Release Variant:**
```gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
        }
    }
}
```

### Build Commands

**Debug Build:**
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

**Release Build:**
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Signing Configuration

**Debug Signing (automatic):**
- Uses debug keystore from Android SDK
- No additional configuration required

**Release Signing:**
```gradle
android {
    signingConfigs {
        release {
            storeFile file('keystore/release.keystore')
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }
}
```

### Testing Pipeline

**Unit Tests:**
```bash
./gradlew testDebugUnitTest
```

**Instrumented Tests:**
```bash
./gradlew connectedDebugAndroidTest
```

**E2E Testing:**
```bash
# Install both variants
adb install app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/release/app-release.apk

# Test debug variant
adb shell am broadcast -a com.mira.videoeditor.debug.action.DECODE_URI \
  --es uri "file:///sdcard/test_audio.wav"

# Test release variant  
adb shell am broadcast -a com.mira.videoeditor.action.DECODE_URI \
  --es uri "file:///sdcard/test_audio.wav"
```

### Deployment

**Internal Distribution:**
```bash
# Upload to internal distribution platform
curl -X POST "https://internal-distribution.com/api/upload" \
  -F "file=@app/build/outputs/apk/release/app-release.apk" \
  -F "version=${VERSION_NAME}" \
  -F "notes=${RELEASE_NOTES}"
```

**Play Store Release:**
```bash
# Bundle for Play Store
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

## iOS Release Pipeline

### Capacitor Configuration

**capacitor.config.ts:**
```typescript
import { CapacitorConfig } from '@capacitor/core';

const config: CapacitorConfig = {
  appId: 'com.mira.videoeditor',
  appName: 'VideoEdit',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  },
  ios: {
    scheme: 'VideoEdit'
  }
};

export default config;
```

### Build Commands

**Web Build:**
```bash
pnpm build
```

**iOS Sync:**
```bash
pnpm exec cap sync ios
cd ios/App && pod install --repo-update && cd -
```

**iOS Build:**
```bash
cd ios/App
agvtool next-version -all
cd -
xcodebuild -workspace ios/App/App.xcworkspace \
  -scheme App -configuration Release \
  -destination 'generic/platform=iOS' \
  -allowProvisioningUpdates \
  clean archive \
  -archivePath ios/build/App.xcarchive
```

### Code Signing

**Development Certificate:**
- Xcode automatically manages development certificates
- Requires Apple Developer account

**Distribution Certificate:**
- Create distribution certificate in Apple Developer portal
- Configure in Xcode project settings

### Testing

**XCTest Automation:**
```bash
# Run UI tests
xcodebuild test \
  -workspace ios/App/App.xcworkspace \
  -scheme App \
  -destination 'platform=iOS Simulator,name=iPhone 14'
```

**Device Testing:**
```bash
# Install on device
xcrun devicectl device install app --device [DEVICE_ID] ios/build/App.ipa
```

### Deployment

**TestFlight:**
```bash
# Export for TestFlight
xcodebuild -exportArchive \
  -archivePath ios/build/App.xcarchive \
  -exportOptionsPlist ios/build/ExportOptions.plist \
  -exportPath ios/build

# Upload to TestFlight
xcrun altool --upload-app -f ios/build/App.ipa -t ios \
  --apiKey "$ASC_API_KEY_ID" --apiIssuer "$ASC_API_ISSUER_ID"
```

**App Store:**
- Use Xcode Organizer to upload to App Store Connect
- Submit for review through App Store Connect portal

## macOS Web Version

### Build Configuration

**WebAssembly Build:**
```bash
# Build Whisper WASM
cd whisper.cpp
make clean
make wasm

# Build CLIP WASM  
cd ../clip
make wasm
```

**Web Build:**
```bash
pnpm build
```

### Progressive Web App Features

**Service Worker:**
```javascript
// sw.js
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open('videoedit-v1').then(cache => {
      return cache.addAll([
        '/',
        '/static/js/bundle.js',
        '/static/css/main.css'
      ]);
    })
  );
});
```

**Manifest:**
```json
{
  "name": "VideoEdit",
  "short_name": "VideoEdit",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#ffffff",
  "theme_color": "#000000",
  "icons": [
    {
      "src": "/icon-192.png",
      "sizes": "192x192",
      "type": "image/png"
    }
  ]
}
```

### Testing

**Cross-Browser Testing:**
```bash
# Chrome
google-chrome --enable-webassembly

# Safari
/Applications/Safari.app/Contents/MacOS/Safari

# Firefox
firefox --enable-webassembly
```

**Responsive Design Testing:**
```bash
# Test different screen sizes
# Use browser dev tools to simulate various devices
```

### Deployment

**Static Hosting:**
```bash
# Deploy to Netlify
netlify deploy --prod --dir=dist

# Deploy to Vercel
vercel --prod

# Deploy to GitHub Pages
gh-pages -d dist
```

## CI/CD Pipeline

### GitHub Actions

**Android Build:**
```yaml
name: Android Build
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Setup Android SDK
        uses: android-actions/setup-android@v2
      - name: Build
        run: ./gradlew assembleDebug
      - name: Test
        run: ./gradlew testDebugUnitTest
```

**iOS Build:**
```yaml
name: iOS Build
on: [push, pull_request]
jobs:
  build:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: Install dependencies
        run: pnpm install
      - name: Build web
        run: pnpm build
      - name: Sync iOS
        run: pnpm exec cap sync ios
```

### Release Automation

**Version Bumping:**
```bash
# Update version in build.gradle
sed -i "s/versionName \".*\"/versionName \"$VERSION\"/" app/build.gradle

# Update version in package.json
npm version $VERSION
```

**Release Notes:**
```bash
# Generate release notes
git log --oneline $(git describe --tags --abbrev=0)..HEAD > RELEASE_NOTES.md
```

## Quality Assurance

### Code Quality

**Linting:**
```bash
# Android
./gradlew detekt

# Web
pnpm lint
```

**Security Scanning:**
```bash
# Android
./gradlew dependencyCheckAnalyze

# Web
npm audit
```

### Performance Testing

**Android Performance:**
```bash
# Memory profiling
adb shell dumpsys meminfo com.mira.videoeditor

# CPU profiling
adb shell top -p $(adb shell pidof com.mira.videoeditor)
```

**iOS Performance:**
```bash
# Memory usage
xcrun simctl spawn booted log stream --predicate 'process == "VideoEdit"'

# CPU usage
xcrun simctl spawn booted top -pid $(xcrun simctl spawn booted pgrep VideoEdit)
```

## Monitoring & Analytics

### Crash Reporting

**Android (Firebase Crashlytics):**
```gradle
dependencies {
    implementation 'com.google.firebase:firebase-crashlytics-gradle:2.9.9'
}
```

**iOS (Firebase Crashlytics):**
```swift
import FirebaseCrashlytics

Crashlytics.crashlytics().record(error: error)
```

### Performance Monitoring

**Android (Firebase Performance):**
```gradle
dependencies {
    implementation 'com.google.firebase:firebase-perf:20.4.1'
}
```

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

## Release Checklist

### Pre-Release
- [ ] All tests passing
- [ ] Code review completed
- [ ] Security scan passed
- [ ] Performance benchmarks met
- [ ] Documentation updated
- [ ] Release notes prepared

### Release
- [ ] Version bumped
- [ ] Build artifacts created
- [ ] Signing completed
- [ ] Uploaded to distribution platform
- [ ] Release notes published
- [ ] Team notified

### Post-Release
- [ ] Monitor crash reports
- [ ] Track performance metrics
- [ ] Collect user feedback
- [ ] Plan next release