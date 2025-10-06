# Cursor Rules for VideoEdit

## Project Overview

VideoEdit is a comprehensive video processing platform with AI-powered speech recognition (Whisper) and visual understanding (CLIP) capabilities, designed for cross-platform deployment on Android, iOS, and macOS web.

## Code Organization

### Directory Structure
```
VideoEdit/
├── app/                    # Main Android application
├── feature/whisper/        # Whisper ASR implementation
├── feature/clip/           # CLIP visual understanding  
├── core/                   # Shared core functionality
├── Doc/                    # Comprehensive documentation
│   ├── clip/              # CLIP feature documentation
│   ├── whisper/           # Whisper feature documentation
│   ├── ui/                # UI feature documentation
│   └── deployment/        # Deployment guides
└── scripts/               # Build and utility scripts
```

### Module Responsibilities
- **`app/`**: Android application shell, WebView integration, native bridges
- **`feature/whisper/`**: Speech recognition, audio processing, transcription
- **`feature/clip/`**: Visual understanding, image processing, similarity search
- **`core/`**: Shared utilities, database, media processing, ML infrastructure

## Coding Standards

### Language Preferences
- **Android**: Kotlin (primary), Java (legacy only)
- **iOS**: Swift (primary), Objective-C (legacy only)
- **Web**: TypeScript (primary), JavaScript (legacy only)
- **Scripts**: Bash (primary), Python (ML utilities)

### Code Style
- **Kotlin**: Follow Android Kotlin Style Guide
- **Swift**: Follow Swift API Design Guidelines
- **TypeScript**: Follow Airbnb TypeScript Style Guide
- **Indentation**: 4 spaces (Kotlin/Swift), 2 spaces (TypeScript/JavaScript)

### Naming Conventions
- **Classes**: PascalCase (`WhisperConnectorService`)
- **Functions**: camelCase (`processAudioFile`)
- **Constants**: UPPER_SNAKE_CASE (`TARGET_SAMPLE_RATE`)
- **Files**: snake_case (`whisper_connector_service.kt`)

## Architecture Principles

### Control Knots
All features must expose key control parameters:
- **Deterministic processing**: Fixed seeds, no random augmentation
- **Reproducible results**: SHA-256 hash verification
- **Configurable quality**: Speed vs accuracy trade-offs
- **Resource management**: Memory, CPU, battery optimization

### Cross-Platform Design
- **Unified API**: Consistent interfaces across platforms
- **Platform-specific optimization**: Native performance where possible
- **Shared business logic**: Common algorithms and data structures
- **Platform abstraction**: Clean separation of concerns

### Performance Requirements
- **Whisper ASR**: RTF < 0.1 (10x faster than real-time)
- **CLIP Vision**: < 100ms latency per frame
- **UI Responsiveness**: 60fps target with graceful degradation
- **Memory Usage**: < 200MB peak per feature

## Development Workflow

### Git Workflow
- **Branch naming**: `feature/description`, `fix/description`, `docs/description`
- **Commit messages**: Conventional Commits format
- **Pull requests**: Required for all changes
- **Code review**: Minimum 1 approval required

### Testing Requirements
- **Unit tests**: Required for all new features
- **Integration tests**: Required for cross-module functionality
- **E2E tests**: Required for user-facing features
- **Performance tests**: Required for ML features

### CI/CD Pipeline
- **Build validation**: All platforms must build successfully
- **Test execution**: All tests must pass
- **Code quality**: Lint checks must pass
- **Security scan**: Dependency vulnerabilities must be resolved

## Feature-Specific Guidelines

### Whisper ASR
- **Audio processing**: Always normalize to 16kHz mono PCM16
- **Model management**: Use GGUF quantization for mobile deployment
- **Language detection**: Implement automatic LID with manual override
- **Batch processing**: Use WorkManager for background processing
- **Resource monitoring**: Track CPU, memory, and battery usage

### CLIP Vision
- **Frame sampling**: Use uniform sampling for deterministic results
- **Preprocessing**: Always resize to 224x224 with center crop
- **Embedding storage**: Use efficient binary format with JSON metadata
- **Similarity search**: Implement cosine similarity with ANN optimization
- **GPU acceleration**: Use platform-specific acceleration (OpenCL/Metal)

### UI Components
- **Accessibility**: Full WCAG compliance required
- **Responsive design**: Support multiple screen sizes and orientations
- **Performance**: 60fps target with efficient resource management
- **Security**: Implement Content Security Policy and secure bridges
- **Cross-platform**: Consistent behavior across Android/iOS/web

## Documentation Standards

### Documentation Structure
Each feature must have:
- **Architecture Design and Control Knot**: Core architecture and parameters
- **Full scale implementation Details**: Complete implementation guide
- **Device Deployment**: Platform-specific deployment instructions
- **README.md**: Multi-lens expert communication
- **Release Guide**: Automated testing and release procedures
- **scripts/**: Feature-specific scripts and tools

### Code Documentation
- **Public APIs**: Comprehensive JSDoc/KDoc documentation
- **Complex algorithms**: Inline comments explaining logic
- **Configuration options**: Document all control parameters
- **Performance characteristics**: Document resource usage and timing

## Quality Assurance

### Code Quality
- **Linting**: All code must pass linting checks
- **Type safety**: Use strict typing (no `any` in TypeScript)
- **Error handling**: Comprehensive error handling and logging
- **Resource cleanup**: Proper resource management and cleanup

### Security
- **Input validation**: Validate all user inputs
- **Secure storage**: Use Android Keystore/iOS Keychain for secrets
- **Network security**: Use HTTPS and certificate pinning
- **Content Security**: Implement CSP and secure JavaScript bridges

### Performance
- **Memory management**: Avoid memory leaks and OOM conditions
- **CPU optimization**: Use efficient algorithms and data structures
- **Battery optimization**: Minimize background processing
- **Storage efficiency**: Use compressed formats and efficient storage

## Deployment Guidelines

### Android
- **Target SDK**: Latest stable version
- **Min SDK**: API 21 (Android 5.0)
- **Architecture**: ARM64-v8a, ARMv7, x86, x86_64
- **Signing**: Use release keystore for production builds

### iOS
- **Target**: iOS 13.0+
- **Architecture**: ARM64 (device), x86_64 (simulator)
- **Code signing**: Use Apple Developer certificates
- **App Store**: Follow Apple App Store guidelines

### Web
- **Browsers**: Chrome 90+, Safari 14+, Firefox 88+
- **PWA**: Implement service worker and manifest
- **Performance**: Lighthouse score > 90
- **Accessibility**: WCAG AA compliance

## Troubleshooting

### Common Issues
- **Build failures**: Check dependencies and environment setup
- **Performance issues**: Profile memory and CPU usage
- **Cross-platform bugs**: Test on multiple devices and platforms
- **Integration issues**: Verify API contracts and data formats

### Debug Tools
- **Android**: Android Studio Profiler, ADB logging
- **iOS**: Xcode Instruments, Console logging
- **Web**: Chrome DevTools, Lighthouse audits
- **General**: Logcat, crash reporting, performance monitoring

## Resources

### Documentation
- [Android Developer Guide](https://developer.android.com/)
- [iOS Developer Guide](https://developer.apple.com/ios/)
- [Web Platform Guide](https://web.dev/)
- [Whisper Documentation](./Doc/whisper/)
- [CLIP Documentation](./Doc/clip/)
- [UI Documentation](./Doc/ui/)

### Tools
- **Android**: Android Studio, Gradle, ADB
- **iOS**: Xcode, CocoaPods, Capacitor
- **Web**: Node.js, npm/pnpm, Capacitor CLI
- **ML**: Python, PyTorch, ONNX, Core ML

---

**Remember**: This is a production-ready platform serving real users. Always prioritize stability, performance, and user experience over new features.