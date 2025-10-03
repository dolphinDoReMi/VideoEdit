# Release Scripts

This directory contains scripts for release management across iOS, Android, and macOS Web platforms.

## Contents

### Build Scripts
- **build-release.sh** - Release build automation

### Future Scripts
- iOS release scripts
- macOS Web release scripts
- Cross-platform release scripts
- App Store submission scripts
- Firebase distribution scripts

## Usage

### Android Release
```bash
# Build release APK
./scripts/release/build-release.sh

# Sign and align APK
./scripts/release/sign-and-align.sh

# Upload to Firebase App Distribution
./scripts/release/upload-to-firebase.sh
```

### iOS Release (Future)
```bash
# Build iOS release
./scripts/release/build-ios-release.sh

# Archive and upload to App Store Connect
./scripts/release/upload-to-app-store.sh
```

### macOS Web Release (Future)
```bash
# Build web version
./scripts/release/build-web-release.sh

# Deploy to hosting
./scripts/release/deploy-web.sh
```

## Release Process

### Android
1. **Build**: Compile release APK with release keystore
2. **Sign**: Sign APK with release certificate
3. **Align**: Optimize APK for distribution
4. **Test**: Internal testing via Firebase App Distribution
5. **Release**: Upload to Google Play Store

### iOS (Future)
1. **Build**: Compile iOS app with release configuration
2. **Archive**: Create App Store archive
3. **Upload**: Upload to App Store Connect
4. **Test**: TestFlight beta testing
5. **Release**: App Store submission

### macOS Web (Future)
1. **Build**: Compile web version with Capacitor
2. **Optimize**: Optimize assets and performance
3. **Deploy**: Deploy to web hosting
4. **Test**: Cross-browser testing
5. **Release**: Public web release

## Integration

These scripts integrate with:
- CI/CD pipeline for automated releases
- Firebase App Distribution for testing
- App Store Connect for iOS releases
- Web hosting for macOS Web releases
- Monitoring systems for release tracking
