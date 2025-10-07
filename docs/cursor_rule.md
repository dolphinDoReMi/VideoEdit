# Cursor Rules for VideoEdit Project

## Project Overview

**VideoEdit** is a comprehensive video processing platform with AI-powered speech recognition (Whisper) and visual understanding (CLIP) capabilities, designed for cross-platform deployment on Android, iOS, and macOS web.

## Code Organization

### Directory Structure
```
VideoEdit/
├── app/                          # Main Android application
│   ├── src/main/java/com/mira/
│   │   ├── whisper/              # Whisper ASR implementation
│   │   ├── clip/                 # CLIP visual understanding
│   │   ├── ui/                   # UI components and WebView
│   │   └── resource/             # Resource monitoring
│   └── src/main/assets/web/      # WebView HTML/CSS/JS assets
├── feature/                      # Feature modules
│   ├── whisper/                  # Whisper feature module
│   └── clip/                     # CLIP feature module
├── core/                         # Shared core functionality
├── docs/                         # Comprehensive documentation
│   ├── clip/                     # CLIP documentation
│   ├── whisper/                  # Whisper documentation
│   ├── ui/                       # UI documentation
│   └── deployment/                # Deployment guides
└── scripts/                      # Build and test scripts
```

## Coding Standards

### Kotlin Development
- **Language**: Kotlin for Android development
- **Style**: Follow Kotlin coding conventions
- **Documentation**: Use KDoc for all public APIs
- **Null Safety**: Use nullable types appropriately
- **Coroutines**: Use suspend functions for async operations

### Architecture Patterns
- **MVVM**: Model-View-ViewModel for UI components
- **Repository Pattern**: Data access abstraction
- **Dependency Injection**: Manual DI with constructor injection
- **Observer Pattern**: LiveData/StateFlow for reactive updates

### Code Quality
- **Linting**: Detekt configuration in `detekt.yml`
- **Testing**: Unit tests for business logic, integration tests for features
- **Documentation**: Comprehensive README files for each feature
- **Error Handling**: Proper exception handling with logging

## Development Workflow

### Branch Strategy
- **main**: Production-ready code
- **feature/whisper**: Whisper feature development
- **feature/clip**: CLIP feature development
- **feature/ui**: UI feature development
- **hotfix/**: Critical bug fixes

### Commit Standards
- **Format**: Conventional Commits (`feat:`, `fix:`, `docs:`, etc.)
- **Scope**: Feature area (whisper, clip, ui, core)
- **Description**: Clear, concise description
- **Body**: Detailed explanation for complex changes

### Testing Requirements
- **Unit Tests**: All public APIs must have unit tests
- **Integration Tests**: Feature pipelines require integration tests
- **Device Tests**: Platform-specific validation tests
- **Performance Tests**: Benchmark tests for critical paths

## Architecture Decisions

### Control Knots
Each feature exposes key control knots for configuration:

#### Whisper Control Knots
- **Sample Rate**: 16 kHz (ASR-ready)
- **Channels**: Mono (downmix from stereo)
- **Model**: tiny.en/base.en/small.en variants
- **Language**: Auto-detection with manual override
- **Performance**: Configurable thread count and memory mode

#### CLIP Control Knots
- **Frame Rate**: Uniform sampling (default 1.0 fps)
- **Resolution**: 224x224 (CLIP standard)
- **Model**: ViT-B/32 (base), ViT-L/14 (large)
- **Batch Size**: Configurable for memory optimization
- **Quantization**: FP16 (default), INT8 (optimized)

#### UI Control Knots
- **WebView Version**: Chrome WebView 120+ compatibility
- **JavaScript Bridge**: @JavascriptInterface for native calls
- **State Management**: Centralized with broadcast updates
- **Accessibility**: Full WCAG compliance
- **Performance**: 60fps target with fallback

### Trade-offs and Rationale

#### Whisper vs Custom ASR
- **Choice**: Whisper for zero-shot multilingual capabilities
- **Trade-off**: Larger model size vs better accuracy
- **Rationale**: Whisper provides state-of-the-art accuracy with minimal fine-tuning

#### WebView vs Native UI
- **Choice**: WebView for cross-platform consistency
- **Trade-off**: Performance vs development speed
- **Rationale**: Rapid development and consistent UX across platforms

#### CLIP vs Custom Vision Models
- **Choice**: CLIP for zero-shot visual understanding
- **Trade-off**: General-purpose vs task-specific accuracy
- **Rationale**: CLIP provides strong baseline with minimal domain adaptation

## Testing Strategy

### Unit Testing
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run specific feature tests
./gradlew :feature:whisper:testDebugUnitTest
./gradlew :feature:clip:testDebugUnitTest
```

### Integration Testing
```bash
# Run integration tests
./gradlew connectedDebugAndroidTest

# Run specific integration tests
./gradlew connectedDebugAndroidTest --tests '*PipelineTest*'
```

### Device Testing
```bash
# Xiaomi Pad testing
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity

# iPad testing (requires macOS)
pnpm exec cap run ios
```

## Performance Requirements

### Whisper ASR
- **RTF**: < 1.0 (real-time factor)
- **Memory**: < 500MB peak for base model
- **Accuracy**: > 95% on standard benchmarks
- **Language Detection**: > 85% accuracy for Chinese

### CLIP Vision
- **Speed**: < 0.1s per frame on GPU
- **Memory**: < 2GB peak for batch processing
- **Accuracy**: > 95% on standard benchmarks
- **Embedding Consistency**: 99.9% hash match

### UI Components
- **Load Time**: < 1.5s First Contentful Paint
- **Bundle Size**: < 200KB gzipped
- **Animation**: 60fps smooth
- **Accessibility**: WCAG AA compliance

## Platform Support

### Android (Primary)
- **Target**: Xiaomi Pad Ultra (ARM64, 11.8GB RAM)
- **Minimum**: Android 5.0+ (API 21)
- **Architecture**: ARM64 with NEON support
- **WebView**: Chrome WebView 120+

### iOS (Secondary)
- **Target**: iPad Pro (M1/M2)
- **Minimum**: iOS 16+
- **Architecture**: ARM64 with Metal support
- **WebView**: WKWebView with Core ML integration

### Web (Tertiary)
- **Target**: macOS Chrome, Safari, Firefox
- **Minimum**: Browser 120+
- **Features**: Progressive Web App capabilities
- **Performance**: WebAssembly for ML models

## Security Considerations

### Data Protection
- **Audio Files**: Local processing only, no cloud upload
- **Model Weights**: SHA-256 hash verification
- **User Data**: No personal data collection
- **Storage**: Encrypted local storage for sensitive data

### WebView Security
- **CSP**: Content Security Policy enabled
- **JavaScript**: Restricted to necessary functions only
- **Network**: No external network access for UI
- **Permissions**: Minimal required permissions

## Documentation Standards

### Code Documentation
- **KDoc**: All public APIs documented
- **Examples**: Code examples for complex functions
- **Architecture**: Architecture decision records (ADRs)
- **README**: Feature-specific README files

### User Documentation
- **Multi-lens**: Expert perspectives (content, video, recsys, DL, audio/LLM)
- **Quick Start**: Step-by-step setup guides
- **Troubleshooting**: Common issues and solutions
- **API Reference**: Complete API documentation

## Deployment Guidelines

### Android Deployment
- **Build Variants**: Debug and release variants
- **Signing**: Debug keystore for development
- **Permissions**: Minimal required permissions
- **Testing**: Device-specific validation

### iOS Deployment
- **Capacitor**: Cross-platform development
- **Xcode**: iOS-specific configuration
- **TestFlight**: Beta testing distribution
- **App Store**: Production release

### Web Deployment
- **PWA**: Progressive Web App features
- **Hosting**: Static hosting with CDN
- **Performance**: WebAssembly optimization
- **Compatibility**: Cross-browser testing

## Monitoring and Observability

### Performance Monitoring
- **RTF Tracking**: Real-time factor monitoring
- **Memory Usage**: Peak memory tracking
- **Battery Impact**: Power consumption monitoring
- **Thermal Throttling**: Device temperature monitoring

### Error Tracking
- **Crash Reporting**: Automatic crash reporting
- **Error Logging**: Comprehensive error logging
- **Performance Metrics**: Real-time performance metrics
- **User Analytics**: Anonymous usage analytics

## Future Roadmap

### Planned Features
- **Speaker Diarization**: Multi-speaker identification
- **Real-time Processing**: Live audio/video streaming
- **Custom Models**: Fine-tuned domain-specific models
- **Advanced UI**: Gesture recognition and voice control

### Performance Improvements
- **GPU Acceleration**: OpenCL/Metal support
- **Model Optimization**: Further quantization options
- **Pipeline Optimization**: Parallel processing
- **Memory Optimization**: Advanced caching strategies

## Contributing Guidelines

### Getting Started
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Follow coding standards and testing requirements
4. Update documentation for new features
5. Submit a pull request with clear description

### Code Review Process
- **Automated Checks**: Linting, testing, and security scanning
- **Manual Review**: Architecture and design review
- **Performance Review**: Performance impact assessment
- **Documentation Review**: Documentation completeness check

### Release Process
- **Version Bumping**: Semantic versioning (MAJOR.MINOR.PATCH)
- **Changelog**: Detailed changelog for each release
- **Testing**: Comprehensive testing before release
- **Deployment**: Automated deployment pipeline

---

**Last Updated**: October 6, 2025  
**Version**: 1.0  
**Status**: Production Ready