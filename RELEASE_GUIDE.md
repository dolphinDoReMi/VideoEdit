# AutoCutPad - Xiaomi Store Release Guide

## Overview
This guide will help you prepare and release AutoCutPad to the Xiaomi Store for both internal testing and public release.

## Prerequisites

### 1. Xiaomi Developer Account
- Register at [Xiaomi Developer Console](https://dev.mi.com/)
- Complete developer verification process
- Pay any required developer fees

### 2. Development Environment
- Android Studio (latest stable version)
- JDK 11 or higher
- Android SDK with API level 35
- Gradle 8.0+

### 3. Keystore Setup
```bash
# Generate release keystore
keytool -genkey -v -keystore keystore/autocutpad-release.keystore -alias autocutpad -keyalg RSA -keysize 2048 -validity 10000

# Update gradle.properties with your credentials
# Replace placeholder values with actual passwords
```

## Build Process

### Quick Build
```bash
# Make script executable
chmod +x build-release.sh

# Run release build
./build-release.sh
```

### Manual Build
```bash
# Clean project
./gradlew clean

# Build release APK
./gradlew assembleRelease

# Build release AAB (recommended for stores)
./gradlew bundleRelease

# Build internal testing version
./gradlew assembleInternal
```

## Release Workflow

### Phase 1: Internal Testing
1. **Build Internal Version**
   ```bash
   ./gradlew assembleInternal
   ```

2. **Test on Xiaomi Devices**
   - Install `app-internal-release.apk`
   - Test core functionality
   - Verify video processing works
   - Check UI/UX on different screen sizes

3. **Upload to Xiaomi Console**
   - Go to Xiaomi Developer Console
   - Create new app listing
   - Upload internal APK
   - Add internal testers (up to 100 users)

4. **Collect Feedback**
   - Monitor crash reports
   - Collect user feedback
   - Fix critical issues

### Phase 2: Public Release
1. **Final Build**
   ```bash
   ./gradlew bundleRelease
   ```

2. **Complete Store Listing**
   - App description (English & Chinese)
   - Screenshots (minimum 3)
   - App icon (512x512px)
   - Privacy policy
   - Age rating

3. **Submit for Review**
   - Upload final AAB
   - Complete all required information
   - Submit for Xiaomi review

4. **Monitor Release**
   - Track download metrics
   - Monitor user reviews
   - Plan updates based on feedback

## File Structure
```
AutoCutPad/
├── app/
│   ├── build.gradle.kts          # App configuration
│   ├── proguard-rules.pro        # Obfuscation rules
│   └── src/main/
│       ├── AndroidManifest.xml   # App manifest
│       ├── java/com/autocutpad/videoeditor/
│       │   ├── MainActivity.kt
│       │   ├── AutoCutEngine.kt
│       │   ├── VideoScorer.kt
│       │   └── MediaStoreExt.kt
│       └── res/                  # Resources
├── keystore/
│   └── autocutpad-release.keystore
├── build-release.sh              # Build script
├── gradle.properties             # Signing configuration
└── XIAOMI_STORE_CONFIG.md        # Store requirements
```

## Build Variants

### Debug
- **Package**: `com.autocutpad.videoeditor.debug`
- **Version**: `1.0.0-debug`
- **Features**: Full logging, no obfuscation
- **Use**: Development and testing

### Internal
- **Package**: `com.autocutpad.videoeditor.internal`
- **Version**: `1.0.0-internal`
- **Features**: Logging enabled, no obfuscation
- **Use**: Internal testing with testers

### Release
- **Package**: `com.autocutpad.videoeditor`
- **Version**: `1.0.0`
- **Features**: Optimized, obfuscated, no logging
- **Use**: Public store release

## Store Requirements Checklist

### Technical Requirements
- [x] Target SDK 35 (Android 15)
- [x] Minimum SDK 24 (Android 7.0)
- [x] Proper permissions with justification
- [x] App bundle (AAB) format
- [x] Signed with release keystore
- [x] ProGuard/R8 obfuscation enabled
- [x] No debug logging in release builds

### Store Listing Requirements
- [ ] App name: AutoCutPad
- [ ] App description (English & Chinese)
- [ ] Category: Video Players & Editors
- [ ] Screenshots (minimum 3)
- [ ] App icon (512x512px)
- [ ] Privacy policy URL
- [ ] Age rating: Everyone
- [ ] Keywords for search

### Compliance Requirements
- [ ] Privacy policy covers data collection
- [ ] Permissions properly justified
- [ ] No inappropriate content
- [ ] Follows Xiaomi store guidelines
- [ ] Compatible with MIUI

## Troubleshooting

### Build Issues
```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Check Gradle version
./gradlew --version

# Check Android SDK
sdkmanager --list
```

### Signing Issues
- Verify keystore file exists
- Check gradle.properties credentials
- Ensure keystore is not corrupted
- Verify key alias matches

### Store Submission Issues
- Check APK/AAB size limits
- Verify all required fields completed
- Ensure screenshots meet requirements
- Check app icon resolution

## Support Resources
- [Xiaomi Developer Documentation](https://dev.mi.com/docs/)
- [Android Developer Guide](https://developer.android.com/)
- [Media3 Documentation](https://developer.android.com/guide/topics/media/media3)

## Version History
- **v1.0.0**: Initial release with auto-cut functionality
- Future versions will include additional features based on user feedback

---

**Note**: Always test thoroughly on Xiaomi devices before release. The MIUI system may have specific behaviors that differ from stock Android.
