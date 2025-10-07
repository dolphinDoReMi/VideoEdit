# Cursor Rules for VideoEdit Project

## Project Overview

**VideoEdit** is a comprehensive video processing platform with AI-powered speech recognition (Whisper) and visual understanding (CLIP) capabilities, designed for cross-platform deployment on Android, iOS, and macOS web.

## Consolidated Documentation Structure

### Documentation Organization
```
docs/
├── clip/                                    # CLIP Visual Understanding
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── Device Deployment.md
│   ├── README.md
│   ├── Release (iOS, Android and MacOS Web Version).md
│   └── scripts/                            # CLIP-specific scripts
├── whisper/                                 # Whisper Speech Recognition
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── Device Deployment.md
│   ├── README.md
│   ├── Release (iOS, Android and MacOS Web Version).md
│   └── scripts/                            # Whisper-specific scripts
├── ui/                                      # UI System
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── Device Deployment.md
│   ├── README.md
│   ├── Release (iOS, Android and MacOS Web Version).md
│   └── scripts/                            # UI-specific scripts
├── Release (iOS, Android and macOS Web Version).md
└── cursor_rule.md                          # This file
```

### Multi-Lens Expert Communication
Each feature README.md provides expert-level explanations for:
1. **Plain-text**: Step-by-step how it works
2. **Recommendation System Expert**: Indexing, ANN, similarity search
3. **Deep Learning Expert**: Model architecture, numerical hygiene
4. **Content Understanding Expert**: Primitive outputs, diagnostics
5. **Audio/Video Expert**: Domain-specific implementation details

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
├── docs/                         # Consolidated documentation
└── scripts/                      # Build and test scripts
```

## Architecture Design and Control Knots

### CLIP Control Knots
**Status: READY FOR VERIFICATION**

**Control knots:**
- Deterministic frame sampling: uniform timestamps at 1.0 fps
- Fixed preprocessing: 224x224 center crop, ImageNet normalization
- Consistent model assets: CLIP ViT-B/32 with L2 normalization

**Implementation:**
- Deterministic sampling: uniform frame extraction with configurable intervals
- Fixed preprocessing: center-crop, no augmentation, ImageNet mean/std
- Re-ingest verification: SHA-256 hash comparison for embedding consistency

**Verification:** Hash comparison script in `docs/clip/scripts/verify_clip_embeddings.sh`

### Whisper Control Knots
**Status: READY FOR VERIFICATION**

**Control knots:**
- Audio front-end: deterministic resampling to 16kHz mono
- Model quantization: GGUF Q5_1 for optimal speed/accuracy
- Streaming chunking: 30-second chunks for large files

**Implementation:**
- Deterministic preprocessing: downmix to mono, resample to 16kHz
- Streaming processing: automatic chunking for files >100MB
- Memory management: prevents OOM on devices with limited RAM

**Verification:** Processing validation script in `docs/whisper/scripts/validate_audio_processing.sh`

### UI Control Knots
**Status: READY FOR VERIFICATION**

**Control knots:**
- WebView framework: Chrome WebView 120+ compatibility
- JavaScript bridge: @JavascriptInterface for native calls
- State management: Centralized with broadcast updates
- Accessibility: Full WCAG compliance with screen reader support

**Implementation:**
- WebView integration: Embedded HTML/CSS/JS assets
- JavaScript bridge: Direct native method calls with error handling
- State synchronization: Broadcast system for real-time updates
- Accessibility: ARIA labels, semantic HTML, keyboard navigation

**Verification:** UI automation tests in `docs/ui/scripts/test_ui_automation.sh`

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

## Performance Requirements

### Whisper ASR
- **RTF**: < 1.0 (real-time factor)
- **Memory**: < 500MB peak for base model
- **Accuracy**: > 95% on standard benchmarks
- **Language Detection**: > 85% accuracy for Chinese
- **Chunking Overhead**: <5% processing time increase

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

## Release Process

### Version Management
- **Semantic Versioning**: MAJOR.MINOR.PATCH
- **Release Notes**: Detailed changelog for each release
- **Testing**: Comprehensive testing before release
- **Deployment**: Automated deployment pipeline

### Platform Releases
- **Android**: Play Store with internal testing
- **iOS**: App Store Connect with TestFlight
- **Web**: Static hosting with CDN distribution

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

## Quick Start Commands

### CLIP (Visual Understanding)
```bash
cd docs/clip/scripts
./deploy_clip_model.sh
./test_frame_sampling.sh
./work_through_clip_xiaomi.sh
```

### Whisper (Speech Recognition)
```bash
cd docs/whisper/scripts
./deploy_multilingual_models.sh
./test_lid_pipeline.sh
./work_through_video_v1.sh
```

### UI (User Interface)
```bash
cd docs/ui/scripts
./test_ui_automation.sh
./test_responsive_design.sh
./test_accessibility.sh
```

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

---

**Last Updated**: October 7, 2025  
**Version**: 1.0  
**Status**: Production Ready