# Release Documentation (iOS, Android and macOS Web Version)

## CI/CD Guide & Release Pipeline

### Overview
This document outlines the complete CI/CD pipeline for releasing the VideoEdit application across iOS, Android, and macOS Web platforms. The pipeline ensures consistent, automated, and reliable releases with comprehensive testing and validation.

### Release Strategy
- **Android**: Primary platform with automated APK generation and testing
- **iOS**: Secondary platform with App Store Connect integration
- **macOS Web**: Tertiary platform with static hosting and PWA features
- **Versioning**: Semantic versioning (MAJOR.MINOR.PATCH)
- **Branching**: GitFlow with main, develop, and feature branches

## Android Release Pipeline

### Build Configuration
```gradle
// app/build.gradle.kts
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.mira.com"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }
    
    buildTypes {
        debug {
            applicationIdSuffix ".debug"
            debuggable true
        }
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
        }
    }
    
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

### Release Process
1. **Code Quality Checks**
   ```bash
   # Linting and static analysis
   ./gradlew detekt
   ./gradlew ktlintCheck
   
   # Unit tests
   ./gradlew testDebugUnitTest
   
   # Integration tests
   ./gradlew connectedDebugAndroidTest
   ```

2. **Build Generation**
   ```bash
   # Debug build
   ./gradlew assembleDebug
   
   # Release build
   ./gradlew assembleRelease
   
   # Bundle generation for Play Store
   ./gradlew bundleRelease
   ```

3. **Testing and Validation**
   ```bash
   # Install on test device
   adb install app/build/outputs/apk/release/app-release.apk
   
   # Run automated tests
   ./docs/android/scripts/test_release_validation.sh
   
   # Performance testing
   ./docs/android/scripts/test_performance.sh
   ```

4. **Deployment**
   ```bash
   # Upload to Play Store (internal testing)
   ./docs/android/scripts/upload_to_play_store.sh
   
   # Generate release notes
   ./docs/android/scripts/generate_release_notes.sh
   ```

### Android Release Scripts
- **Build Script**: `docs/android/scripts/build_release.sh`
- **Test Script**: `docs/android/scripts/test_release_validation.sh`
- **Upload Script**: `docs/android/scripts/upload_to_play_store.sh`
- **Performance Script**: `docs/android/scripts/test_performance.sh`

## iOS Release Pipeline

### Capacitor Configuration
```typescript
// capacitor.config.ts
import { CapacitorConfig } from '@capacitor/core';

const config: CapacitorConfig = {
  appId: 'com.mira.com',
  appName: 'VideoEdit',
  webDir: 'dist',
  server: {
    androidScheme: 'https'
  },
  ios: {
    scheme: 'VideoEdit',
    contentInset: 'automatic'
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      backgroundColor: "#000000"
    }
  }
};

export default config;
```

### Release Process
1. **Web Build**
   ```bash
   # Install dependencies
   corepack enable && pnpm --version
   pnpm install --frozen-lockfile
   
   # Build web assets
   pnpm build
   ```

2. **iOS Sync and Build**
   ```bash
   # Sync Capacitor iOS
   pnpm exec cap sync ios
   
   # Install CocoaPods
   cd ios/App && pod install --repo-update && cd -
   
   # Bump build number
   cd ios/App
   agvtool next-version -all
   cd -
   ```

3. **Archive and Export**
   ```bash
   # Archive Release build
   xcodebuild -workspace ios/App/App.xcworkspace \
     -scheme App -configuration Release \
     -destination 'generic/platform=iOS' \
     -allowProvisioningUpdates \
     clean archive \
     -archivePath ios/build/App.xcarchive
   
   # Export for App Store
   xcodebuild -exportArchive \
     -archivePath ios/build/App.xcarchive \
     -exportOptionsPlist ios/build/ExportOptions.plist \
     -exportPath ios/build
   ```

4. **Upload to App Store Connect**
   ```bash
   # Upload with API key
   IPA=$(ls ios/build/*.ipa | head -n1)
   xcrun altool --upload-app -f "$IPA" -t ios \
     --apiKey "$ASC_API_KEY_ID" --apiIssuer "$ASC_API_ISSUER_ID" \
     --verbose
   ```

### iOS Release Scripts
- **Build Script**: `docs/ios/scripts/build_ios_release.sh`
- **Test Script**: `docs/ios/scripts/test_ios_validation.sh`
- **Upload Script**: `docs/ios/scripts/upload_to_app_store.sh`
- **Performance Script**: `docs/ios/scripts/test_ios_performance.sh`

## macOS Web Release Pipeline

### Web Build Configuration
```json
// package.json
{
  "name": "videoedit-web",
  "version": "1.0.0",
  "scripts": {
    "build": "vite build",
    "preview": "vite preview",
    "test": "playwright test",
    "lighthouse": "lighthouse http://localhost:3000 --view"
  },
  "dependencies": {
    "@capacitor/core": "^5.0.0",
    "vite": "^5.0.0",
    "tailwindcss": "^3.0.0"
  }
}
```

### Release Process
1. **Web Build**
   ```bash
   # Install dependencies
   pnpm install --frozen-lockfile
   
   # Build web assets
   pnpm build
   
   # Generate PWA manifest
   pnpm exec vite build --mode production
   ```

2. **Testing and Validation**
   ```bash
   # Cross-browser testing
   pnpm exec playwright test --browser=all
   
   # Performance testing
   pnpm exec lighthouse http://localhost:3000 --view
   
   # Accessibility testing
   pnpm exec axe-cli http://localhost:3000
   ```

3. **Deployment**
   ```bash
   # Deploy to static hosting
   pnpm exec gh-pages -d dist
   
   # Update CDN cache
   pnpm exec cloudflare-cli purge-cache
   
   # Verify deployment
   pnpm exec curl -I https://videoedit.app
   ```

### Web Release Scripts
- **Build Script**: `docs/web/scripts/build_web_release.sh`
- **Test Script**: `docs/web/scripts/test_web_validation.sh`
- **Deploy Script**: `docs/web/scripts/deploy_web.sh`
- **Performance Script**: `docs/web/scripts/test_web_performance.sh`

## Automated Release Pipeline

### GitHub Actions Workflow
```yaml
# .github/workflows/release.yml
name: Release Pipeline

on:
  push:
    branches: [main]
    tags: ['v*']

jobs:
  android-release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run tests
        run: ./gradlew testDebugUnitTest
      - name: Build release
        run: ./gradlew assembleRelease
      - name: Upload to Play Store
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.GOOGLE_PLAY_SERVICE_ACCOUNT }}
          packageName: com.mira.com
          releaseFiles: app/build/outputs/apk/release/app-release.apk

  ios-release:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
      - name: Install dependencies
        run: pnpm install --frozen-lockfile
      - name: Build web assets
        run: pnpm build
      - name: Sync iOS
        run: pnpm exec cap sync ios
      - name: Build iOS
        run: |
          cd ios/App
          pod install
          xcodebuild -workspace App.xcworkspace -scheme App -configuration Release archive
      - name: Upload to App Store
        uses: apple-actions/upload-app-store@v1
        with:
          app-path: ios/build/App.xcarchive
          api-key: ${{ secrets.APP_STORE_API_KEY }}

  web-release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
      - name: Install dependencies
        run: pnpm install --frozen-lockfile
      - name: Build web assets
        run: pnpm build
      - name: Run tests
        run: pnpm exec playwright test
      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./dist
```

### Release Automation Scripts
- **Full Release**: `scripts/release_all_platforms.sh`
- **Android Only**: `scripts/release_android.sh`
- **iOS Only**: `scripts/release_ios.sh`
- **Web Only**: `scripts/release_web.sh`

## Quality Assurance

### Testing Strategy
1. **Unit Tests**: All business logic components
2. **Integration Tests**: Feature pipeline validation
3. **UI Tests**: Cross-platform UI automation
4. **Performance Tests**: Load time and resource usage
5. **Accessibility Tests**: WCAG compliance validation
6. **Security Tests**: Vulnerability scanning

### Release Criteria
- **Code Coverage**: >80% for critical components
- **Performance**: <2s load time, <100MB memory usage
- **Accessibility**: WCAG AA compliance
- **Security**: No high/critical vulnerabilities
- **Compatibility**: Support for target device versions

### Rollback Strategy
- **Automated Rollback**: Triggered by critical failures
- **Manual Rollback**: For performance or compatibility issues
- **Version Pinning**: Ability to revert to previous stable version
- **Feature Flags**: Disable problematic features without full rollback

## Monitoring and Observability

### Release Monitoring
- **Crash Reporting**: Automatic crash collection and analysis
- **Performance Metrics**: Real-time performance monitoring
- **User Analytics**: Anonymous usage analytics
- **Error Tracking**: Comprehensive error logging and alerting

### Success Metrics
- **Adoption Rate**: New user acquisition
- **Retention Rate**: User engagement over time
- **Performance**: Load time and responsiveness
- **Stability**: Crash rate and error frequency
- **User Satisfaction**: App store ratings and reviews

## Release Notes Template

### Version 1.0.0 Release Notes

**New Features:**
- Whisper speech recognition with multilingual support
- CLIP visual understanding with similarity search
- Cross-platform UI with accessibility compliance
- Background processing with resource monitoring

**Improvements:**
- 50% faster processing with streaming chunking
- 40% memory reduction with optimized models
- Enhanced accessibility with WCAG AA compliance
- Improved battery efficiency with intelligent scheduling

**Bug Fixes:**
- Fixed memory leaks in long-running processes
- Resolved audio format compatibility issues
- Fixed UI responsiveness on low-end devices
- Corrected accessibility navigation issues

**Technical Details:**
- Android: API 21+ support with WebView 120+
- iOS: iOS 16+ support with WKWebView integration
- Web: Progressive Web App with offline support
- Models: GGUF quantized Whisper and CLIP models

---

**Last Updated**: October 7, 2025  
**Version**: 1.0  
**Status**: Production Ready
