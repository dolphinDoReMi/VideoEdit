# Whisper Release (iOS, Android and macOS Web Version)

## Release Overview

This document outlines the release procedures for the Whisper ASR feature across all supported platforms: Android (Xiaomi Pad), iOS (iPad), and macOS Web.

## Android Release (Xiaomi Pad)

### Release Process

**1. Pre-Release Preparation:**
```bash
# Update version
./gradlew updateVersion -PversionName="1.0.0" -PversionCode=100

# Run full test suite
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest

# Security scan
./gradlew dependencyCheckAnalyze
```

**2. Build Release APK:**
```bash
# Clean build
./gradlew clean

# Build release
./gradlew assembleRelease

# Verify APK
aapt dump badging app/build/outputs/apk/release/app-release.apk
```

**3. Signing and Distribution:**
```bash
# Sign APK (if not using Gradle signing)
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore keystore/release.keystore \
  app/build/outputs/apk/release/app-release.apk \
  alias_name

# Align APK
zipalign -v 4 app/build/outputs/apk/release/app-release.apk \
  app/build/outputs/apk/release/app-release-aligned.apk
```

**4. Deployment:**
```bash
# Install on Xiaomi Pad
adb install app/build/outputs/apk/release/app-release-aligned.apk

# Verify installation
adb shell pm list packages | grep com.mira.videoeditor
```

### Release Notes Template

```markdown
## Whisper Android Release v1.0.0

### New Features
- Real-time speech-to-text transcription
- Background resource monitoring
- Multi-language support with automatic detection
- Batch processing capabilities

### Improvements
- Enhanced audio processing pipeline
- Optimized memory usage
- Improved error handling

### Bug Fixes
- Fixed audio format compatibility issues
- Resolved background service stability
- Fixed resource monitoring data accuracy

### Technical Details
- Target SDK: 34 (Android 14)
- Minimum SDK: 21 (Android 5.0)
- Architecture: ARM64-v8a
- Size: ~50MB
```

## iOS Release (iPad)

### Release Process

**1. Pre-Release Preparation:**
```bash
# Update version in package.json
npm version 1.0.0

# Build web assets
pnpm build

# Sync iOS
pnpm exec cap sync ios
```

**2. iOS Build:**
```bash
# Update build number
cd ios/App
agvtool next-version -all
cd -

# Build for device
xcodebuild -workspace ios/App/App.xcworkspace \
  -scheme App -configuration Release \
  -destination 'generic/platform=iOS' \
  -allowProvisioningUpdates \
  clean archive \
  -archivePath ios/build/App.xcarchive
```

**3. Export and Sign:**
```bash
# Create export options
cat > ios/build/ExportOptions.plist << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>app-store</string>
    <key>signingStyle</key>
    <string>automatic</string>
    <key>uploadSymbols</key>
    <true/>
</dict>
</plist>
EOF

# Export IPA
xcodebuild -exportArchive \
  -archivePath ios/build/App.xcarchive \
  -exportOptionsPlist ios/build/ExportOptions.plist \
  -exportPath ios/build
```

**4. TestFlight Distribution:**
```bash
# Upload to TestFlight
xcrun altool --upload-app -f ios/build/App.ipa -t ios \
  --apiKey "$ASC_API_KEY_ID" --apiIssuer "$ASC_API_ISSUER_ID" \
  --verbose
```

**5. App Store Submission:**
- Use Xcode Organizer to upload to App Store Connect
- Submit for review through App Store Connect portal
- Release when approved

### Release Notes Template

```markdown
## Whisper iOS Release v1.0.0

### New Features
- Native iOS speech-to-text integration
- Core ML optimized Whisper models
- iPad-optimized user interface
- Background processing capabilities

### Improvements
- Enhanced WebView performance
- Optimized for Apple Silicon
- Improved memory management
- Better accessibility support

### Bug Fixes
- Fixed WebView loading issues
- Resolved Core ML model loading
- Fixed background processing limitations
- Improved error handling

### Technical Details
- Target iOS: 16.0+
- Architecture: ARM64 (Apple Silicon)
- Size: ~80MB
- Requires: iPad Pro recommended
```

## macOS Web Version Release

### Release Process

**1. Pre-Release Preparation:**
```bash
# Update version
npm version 1.0.0

# Build web assets
pnpm build

# Build WebAssembly modules
cd whisper.cpp && make wasm && cd ..
cd clip && make wasm && cd ..
```

**2. Progressive Web App Setup:**
```bash
# Generate service worker
cat > public/sw.js << 'EOF'
const CACHE_NAME = 'videoedit-v1';
const urlsToCache = [
  '/',
  '/static/js/bundle.js',
  '/static/css/main.css',
  '/whisper.wasm',
  '/clip.wasm'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(urlsToCache))
  );
});
EOF

# Update manifest
cat > public/manifest.json << 'EOF'
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
    },
    {
      "src": "/icon-512.png",
      "sizes": "512x512",
      "type": "image/png"
    }
  ]
}
EOF
```

**3. Cross-Browser Testing:**
```bash
# Test Chrome
google-chrome --enable-webassembly --disable-web-security \
  --user-data-dir=/tmp/chrome-test http://localhost:3000

# Test Safari
/Applications/Safari.app/Contents/MacOS/Safari

# Test Firefox
firefox --enable-webassembly http://localhost:3000
```

**4. Deployment:**
```bash
# Deploy to Netlify
netlify deploy --prod --dir=dist

# Deploy to Vercel
vercel --prod

# Deploy to GitHub Pages
gh-pages -d dist
```

### Release Notes Template

```markdown
## Whisper macOS Web Release v1.0.0

### New Features
- WebAssembly-powered Whisper processing
- Progressive Web App capabilities
- Offline functionality
- Cross-browser compatibility

### Improvements
- Optimized WebAssembly performance
- Enhanced offline caching
- Improved responsive design
- Better error handling

### Bug Fixes
- Fixed WebAssembly loading issues
- Resolved offline functionality
- Fixed cross-browser compatibility
- Improved performance on macOS

### Technical Details
- WebAssembly: Whisper.cpp + CLIP models
- PWA: Service Worker + Manifest
- Browsers: Chrome 90+, Safari 14+, Firefox 88+
- Size: ~20MB (initial load)
```

## Release Coordination

### Release Schedule

**Phase 1: Android (Week 1)**
- Build and test Android release
- Deploy to Xiaomi Pad
- Internal testing and validation

**Phase 2: iOS (Week 2)**
- Build and test iOS release
- Submit to TestFlight
- Beta testing with select users

**Phase 3: Web (Week 3)**
- Build and test web release
- Deploy to staging environment
- Cross-browser testing

**Phase 4: Production (Week 4)**
- Release all platforms simultaneously
- Monitor deployment
- Collect user feedback

### Quality Assurance

**Pre-Release Testing:**
```bash
# Run full test suite
./scripts/test_full_pipeline.sh

# Performance benchmarks
./scripts/test_performance_benchmarks.sh

# Cross-platform validation
./scripts/test_cross_platform.sh
```

**Post-Release Monitoring:**
```bash
# Monitor crash reports
# Firebase Crashlytics dashboard

# Monitor performance
# Firebase Performance dashboard

# Monitor user feedback
# App Store reviews, TestFlight feedback
```

### Rollback Procedures

**Android Rollback:**
```bash
# Revert to previous version
adb install app/build/outputs/apk/release/app-release-previous.apk
```

**iOS Rollback:**
```bash
# Revert TestFlight build
# App Store Connect > TestFlight > Previous Build

# Revert App Store release
# App Store Connect > App Store > Previous Version
```

**Web Rollback:**
```bash
# Revert deployment
netlify rollback
# or
vercel rollback
```

## Release Metrics

### Success Criteria

**Android:**
- [ ] 95%+ crash-free sessions
- [ ] <2s app launch time
- [ ] <5s Whisper processing time
- [ ] 4.5+ star rating

**iOS:**
- [ ] 95%+ crash-free sessions
- [ ] <2s app launch time
- [ ] <5s Whisper processing time
- [ ] 4.5+ star rating

**Web:**
- [ ] 99%+ uptime
- [ ] <3s initial load time
- [ ] <5s Whisper processing time
- [ ] 90%+ user satisfaction

### Monitoring Dashboard

**Key Metrics:**
- Crash rate by platform
- Performance metrics by platform
- User engagement by platform
- Feature adoption by platform

**Alerting:**
- Crash rate >5%
- Performance degradation >20%
- User complaints >10%
- Feature failures >1%