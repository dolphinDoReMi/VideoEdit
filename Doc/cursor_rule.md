# Cursor Rules for Mira Video Editor

## Project Overview
This is a comprehensive video editing application with AI-powered features including CLIP-based video analysis and Whisper speech recognition. The project uses a hybrid Android-WebView architecture for cross-platform consistency.

## Architecture

### Core Modules
- **app/**: Main Android application with WebView-based UI
- **feature/clip/**: CLIP-based video analysis and processing
- **feature/whisper/**: Whisper speech recognition integration
- **core/media/**: Media processing and audio/video handling
- **core/ml/**: Machine learning model management
- **core/infra/**: Infrastructure and utility components

### UI Architecture
- **Hybrid Design**: Android MainActivity with WebView container
- **Cross-platform**: HTML5/CSS/JavaScript interface
- **Real-time Monitoring**: System resource tracking with moving averages
- **JavaScript Bridge**: Bidirectional communication between native and web layers

## Key Control Knots

### CLIP Feature Control Knots
- **Deterministic Sampling**: Uniform frame timestamps with fixed intervals
- **Fixed Preprocessing**: Center-crop video segments, no random augmentation
- **Model Consistency**: Fixed CLIP model with deterministic initialization

### Whisper Feature Control Knots
- **Seedless Pipeline**: Deterministic audio sampling without random variations
- **Fixed Preprocess**: Center-crop audio segments with fixed window sizes
- **Same Model Assets**: Fixed Whisper model file with deterministic initialization

### UI Control Knots
- **WebView-based UI**: Hybrid Android-WebView architecture
- **Real-time Resource Monitoring**: Deterministic system resource tracking
- **Processing Pipeline Visualization**: Step-by-step progress with real-time updates

## Development Guidelines

### Code Style
- **Kotlin**: Use modern Kotlin features, coroutines for async operations
- **JavaScript**: ES6+ features, efficient DOM manipulation
- **CSS**: Tailwind CSS with custom dark theme
- **HTML**: Semantic markup with proper accessibility

### Testing Requirements
- **Unit Tests**: Run `./gradlew :app:testDebugUnitTest` for database/ML changes
- **Instrumented Tests**: Run `./gradlew :app:connectedDebugAndroidTest` for workers/engines
- **Integration Tests**: Use provided test scripts in `scripts/test/`

### Performance Standards
- **Memory Usage**: < 500MB during normal operation
- **CPU Usage**: < 20% during idle, < 50% during processing
- **Battery Drain**: < 5% per hour during idle
- **Resource Accuracy**: ±5% accuracy for system monitoring

## File Organization

### Documentation Structure
```
Doc/
├── clip/
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── Device Deployment.md
│   ├── README.md
│   └── Release (iOS, Android and MacOS Web Version).md
├── whisper/
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── Device Deployment.md
│   ├── README.md
│   └── Release (iOS, Android and MacOS Web Version).md
├── ui/
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── Device Deployment.md
│   ├── README.md
│   └── Release (iOS, Android and MacOS Web Version).md
└── cursor_rule.md
```

### Key Implementation Files
- **MainActivity**: `app/src/main/java/mira/ui/MainActivity.kt`
- **Processing UI**: `app/src/main/assets/processing.html`
- **Whisper Bridge**: `app/src/main/java/com/mira/com/whisper/AndroidWhisperBridge.kt`
- **CLIP Engine**: `app/src/main/java/com/mira/videoeditor/AutoCutEngine.kt`

## Deployment

### Android (Xiaomi Pad Ultra)
```bash
# Build and install
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch and test
adb shell am start -n com.mira.videoeditor.debug/.MainActivity
adb logcat -s ResourceMonitor:V MainActivity:V
```

### iOS (iPad Pro)
```bash
# Build web assets
npm run build

# Sync Capacitor
npx cap sync ios

# Open in Xcode
npx cap open ios
```

### Web (macOS)
```bash
# Serve locally
python -m http.server 8000
# Open http://localhost:8000/processing.html
```

## Testing Scripts

### Available Test Scripts
- `scripts/test/whisper_logic_test.sh` - Whisper logic verification
- `scripts/test/whisper_real_integration_test.sh` - Real integration testing
- `scripts/test/video_transcription_test.sh` - Video transcription testing
- `scripts/test/whisper_step1_test.sh` - Basic WhisperEngine testing
- `scripts/test/whisper_step2_test.sh` - Audio processing testing

### Test Execution
```bash
# Run comprehensive tests
./test_pipeline_comprehensive.sh
./test_whisper_step_flow.sh
./test_staging_implementation.sh
```

## Performance Monitoring

### Resource Monitoring
- **CPU Usage**: Moving average calculation with outlier filtering
- **Memory Usage**: PSS-based memory tracking with percentage calculation
- **Battery Level**: Real-time battery monitoring with fallback methods
- **Temperature**: System temperature estimation with multiple detection methods

### Monitoring Configuration
- **Monitoring Interval**: 2 seconds
- **History Window**: 2 minutes (60 readings)
- **Outlier Threshold**: 50% CPU usage
- **System Memory**: 12GB (Xiaomi Pad Ultra)

## Troubleshooting

### Common Issues
- **WebView Not Loading**: Check JavaScript enabled, local asset paths
- **Resource Monitoring Inaccurate**: Verify system permissions, API access
- **JavaScript Bridge Not Working**: Check `@JavascriptInterface` annotations
- **Performance Issues**: Optimize monitoring frequency, memory usage

### Debug Commands
```bash
# Android Debug
adb logcat -s ResourceMonitor:V MainActivity:V
adb shell dumpsys meminfo com.mira.videoeditor.debug

# iOS Debug
# Use Safari Web Inspector for WKWebView debugging
# Use Xcode Instruments for performance monitoring
```

## Future Enhancements

### Planned Features
- **Accessibility**: Enhanced screen reader support
- **Internationalization**: Multi-language support
- **Performance**: Further optimization for low-end devices
- **Features**: Additional processing visualization options

### Scalability
- **Modular Design**: Easy to add new processing engines
- **Plugin System**: Extensible architecture for new features
- **Cross-platform**: Unified codebase for all platforms
- **Performance**: Optimized for high-end and low-end devices

## Code Pointers

### Key Functions
- **Resource Monitoring**: `updateResourceUsage()` - Real-time system monitoring
- **CPU Calculation**: `calculateCpuMovingAverage()` - Moving average with outlier filtering
- **Memory Tracking**: `getRealMemoryUsage()` - PSS-based memory calculation
- **JavaScript Bridge**: `JavaScriptInterface` - Bidirectional communication
- **Step Updates**: `updateStep()` - Processing pipeline visualization

### Key Configuration
- **Monitoring Interval**: 2 seconds
- **History Window**: 2 minutes (60 readings)
- **Outlier Threshold**: 50% CPU usage
- **System Memory**: 12GB (Xiaomi Pad Ultra)

This cursor rule provides comprehensive guidance for developing and maintaining the Mira Video Editor application with its AI-powered features and cross-platform UI architecture.