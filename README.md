# AutoCutPad - AI Video Editor for Android

![AutoCutPad Logo](app/src/main/res/mipmap-hdpi/ic_launcher.xml)

AutoCutPad is an AI-powered video editing application that automatically selects the most engaging clips from your videos using motion detection algorithms. Perfect for creating highlights from long videos, sports clips, or any content where motion indicates engagement.

## 🚀 Features

- **AI-Powered Clip Selection**: Automatically analyzes video segments for motion intensity
- **Smart Video Editing**: Uses Media3 Transformer for professional-grade video processing
- **Simple Interface**: Intuitive Jetpack Compose UI with Material 3 design
- **Export Options**: High-quality MP4 export with H.264 encoding
- **Xiaomi Optimized**: Specifically designed for Xiaomi devices and MIUI
- **No Watermark**: Clean exports without any branding

## 📱 Screenshots

*Screenshots will be added after UI implementation*

## 🛠️ Technical Details

### Architecture
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Video Processing**: Media3 Transformer
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Build System**: Gradle with Kotlin DSL

### Dependencies
- **Media3**: 1.8.0 (transformer, effect, common, exoplayer)
- **Compose**: BOM 2025.09.01
- **Kotlin Coroutines**: 1.9.0
- **ML Kit Face Detection**: 16.1.7 (optional)

## 🏗️ Project Structure

```
AutoCutPad/
├── app/
│   ├── build.gradle.kts          # App configuration
│   ├── proguard-rules.pro        # Obfuscation rules
│   └── src/main/
│       ├── AndroidManifest.xml   # App manifest
│       ├── java/com/autocutpad/videoeditor/
│       │   ├── MainActivity.kt      # Main UI activity
│       │   ├── AutoCutEngine.kt    # Video processing engine
│       │   ├── VideoScorer.kt      # Motion analysis
│       │   ├── MediaStoreExt.kt    # File utilities
│       │   └── AutoCutApplication.kt # Application class
│       └── res/                  # Resources
├── keystore/                     # Release signing
├── build-release.sh              # Build script
├── gradle.properties             # Configuration
└── docs/                        # Documentation
```

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest stable version)
- JDK 11 or higher
- Android SDK with API level 35

### Installation
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on device or emulator

### Building for Release
```bash
# Make build script executable
chmod +x build-release.sh

# Run release build
./build-release.sh
```

## 📦 Build Variants

### Debug
- Package: `com.autocutpad.videoeditor.debug`
- Version: `1.0.0-debug`
- Features: Full logging, no obfuscation

### Internal Testing
- Package: `com.autocutpad.videoeditor.internal`
- Version: `1.0.0-internal`
- Features: Logging enabled, no obfuscation

### Release
- Package: `com.autocutpad.videoeditor`
- Version: `1.0.0`
- Features: Optimized, obfuscated, no logging

## 🎯 How It Works

1. **Video Selection**: User selects a video file using Android's Storage Access Framework
2. **Motion Analysis**: VideoScorer analyzes segments for motion intensity using frame differences
3. **Clip Selection**: AutoCutEngine selects highest-scoring segments up to target duration
4. **Video Processing**: Media3 Transformer creates final edited video
5. **Export**: Processed video is saved and can be shared

## 🔧 Configuration

### Keystore Setup
```bash
# Generate release keystore
keytool -genkey -v -keystore keystore/autocutpad-release.keystore -alias autocutpad -keyalg RSA -keysize 2048 -validity 10000

# Update gradle.properties with credentials
```

### Build Configuration
Edit `gradle.properties`:
```properties
KEYSTORE_FILE=keystore/autocutpad-release.keystore
KEYSTORE_PASSWORD=your_secure_password
KEY_ALIAS=autocutpad
KEY_PASSWORD=your_secure_password
```

## 📋 Store Release

### Xiaomi Store Requirements
- [x] Target SDK 35
- [x] Proper permissions
- [x] App bundle format
- [x] Release signing
- [x] Obfuscation enabled
- [ ] Store listing information
- [ ] Screenshots
- [ ] Privacy policy

### Release Process
1. **Internal Testing**: Upload internal APK for testing
2. **Store Listing**: Complete app information and screenshots
3. **Public Release**: Submit final AAB for review
4. **Monitoring**: Track metrics and user feedback

## 🔒 Permissions

- **READ_EXTERNAL_STORAGE**: Access user's video files
- **READ_MEDIA_VIDEO**: Android 13+ video access
- **CAMERA**: Future video recording features
- **RECORD_AUDIO**: Future audio recording features
- **INTERNET**: Future cloud features

## 🐛 Troubleshooting

### Common Issues
- **Build failures**: Check Gradle version and Android SDK
- **Signing errors**: Verify keystore credentials
- **Video processing**: Ensure sufficient device storage
- **Permission issues**: Check Android version compatibility

### Debug Mode
Enable debug logging by building debug variant:
```bash
./gradlew assembleDebug
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

- **Issues**: Report bugs via GitHub Issues
- **Documentation**: See RELEASE_GUIDE.md for detailed instructions
- **Store Support**: Contact Xiaomi Developer Support

## 🗺️ Roadmap

### Version 1.1
- [ ] Face detection integration
- [ ] Custom segment length
- [ ] Video preview
- [ ] Batch processing

### Version 1.2
- [ ] Video transitions
- [ ] Audio processing
- [ ] Export quality options
- [ ] Cloud sync

## 🙏 Acknowledgments

- Android Media3 team for video processing capabilities
- Jetpack Compose team for modern UI framework
- Xiaomi for device optimization opportunities

---

**AutoCutPad** - Making video editing effortless with AI technology.