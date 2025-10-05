# Cursor Rules for VideoEdit Project

## Project Overview
This is a multi-platform video editing application with three main feature areas:
- **CLIP**: Video understanding and embedding generation
- **Whisper**: Speech-to-text transcription and processing
- **UI**: Cross-platform user interface with real-time resource monitoring

## Development Guidelines

### Code Organization
- **Package Structure**: `com.mira.videoeditor` for main app, `com.mira.clip`, `com.mira.whisper` for features
- **Build Variants**: Debug and Release with `.debug` suffix for side-by-side installation
- **Namespacing**: All broadcast actions, work names, and file authorities use `${applicationId}` placeholders

### Architecture Patterns
- **Control Knots**: Expose key configuration points for deterministic behavior
- **Background Services**: Use foreground services for persistent operations
- **Broadcast System**: Inter-component communication via Android broadcasts
- **WebView Bridge**: `@JavascriptInterface` for native-WebView communication

### Resource Management
- **Memory**: Stream processing to avoid OOM on large files
- **CPU**: Background service for resource monitoring with 2-second intervals
- **Storage**: Sidecar JSON files for metadata and verification
- **Battery**: Efficient polling and event-driven updates

## Feature-Specific Rules

### CLIP Features
- **Model Assets**: Bundle CLIP models in APK assets
- **GPU Acceleration**: Use OpenCL for ARM Mali/Adreno, Metal for Apple Silicon
- **Frame Sampling**: Uniform sampling by default, scene-based as option
- **Embedding Storage**: JSON format with binary fallback

### Whisper Features
- **Audio Processing**: MediaCodec for hardware-accelerated decoding
- **Resampling**: Linear resampler by default, sinc as option
- **Language Detection**: Automatic LID with manual override
- **Timestamp Policy**: Hybrid PTS + sample-count with drift tracking

### UI Features
- **WebView Integration**: Chrome WebView 120+ compatibility
- **State Management**: Centralized state with broadcast updates
- **Accessibility**: WCAG compliance with screen reader support
- **Theme System**: Auto theme with manual override

## Testing Requirements

### Unit Tests
- **Database Changes**: Run `./gradlew :app:testDebugUnitTest` for DAO modifications
- **ML Models**: Run all tests for model or encoder changes
- **Workers**: Run instrumented tests for worker modifications

### Integration Tests
- **Video Processing**: Run frame sampler tests for video changes
- **Resource Monitoring**: Validate background service functionality
- **Cross-Platform**: Test on Xiaomi Pad and iPad

### Verification Scripts
- **Hash Comparison**: SHA-256 verification for deterministic outputs
- **Performance Benchmarks**: RTF, memory usage, processing speed
- **Accessibility Audit**: Screen reader compatibility validation

## Deployment Rules

### Android Deployment
- **Build Variants**: Debug and Release APKs
- **Signing**: Debug keystore for development, release keystore for production
- **Permissions**: Minimal required permissions with runtime requests
- **Testing**: ADB broadcast testing for background services

### iOS Deployment
- **Capacitor Integration**: WebView-based hybrid app
- **Code Signing**: Development and distribution certificates
- **Background Processing**: Core Audio and background tasks
- **Testing**: XCTest automation for core functionality

### macOS Web Deployment
- **WebAssembly**: CLIP and Whisper models compiled to WASM
- **Progressive Web App**: Offline capability and native-like experience
- **Cross-Browser**: Chrome, Safari, Firefox compatibility
- **Testing**: Browser automation and responsive design validation

## Code Quality Standards

### Kotlin/Java
- **Null Safety**: Use nullable types appropriately
- **Coroutines**: Use for async operations
- **Broadcast Receivers**: Use `RECEIVER_NOT_EXPORTED` for Android 13+
- **Foreground Services**: Required permissions for background operations

### JavaScript/TypeScript
- **ES6+**: Use modern JavaScript features
- **Type Safety**: TypeScript for complex logic
- **WebView Bridge**: Proper error handling and type checking
- **State Management**: Immutable state updates

### Documentation
- **Architecture**: Document control knots and design decisions
- **Implementation**: Detailed technical specifications
- **Deployment**: Device-specific deployment guides
- **Verification**: Testing and validation procedures

## Security Considerations

### Data Protection
- **File Access**: Use scoped storage for Android 10+
- **Permissions**: Request minimal required permissions
- **Broadcast Security**: Use `RECEIVER_NOT_EXPORTED` for internal broadcasts
- **Foreground Services**: Required permissions for background operations

### Code Security
- **Input Validation**: Validate all user inputs
- **File Handling**: Sanitize file paths and names
- **Network Security**: Use HTTPS for all network requests
- **Storage Security**: Encrypt sensitive data at rest

## Performance Optimization

### Memory Management
- **Stream Processing**: Avoid loading large files into memory
- **Resource Monitoring**: Efficient background service implementation
- **WebView**: Proper memory cleanup and garbage collection
- **Native Code**: JNI best practices for C++ integration

### CPU Optimization
- **Background Services**: Minimal CPU usage for monitoring
- **GPU Acceleration**: Use hardware acceleration where available
- **Threading**: Proper thread management for UI responsiveness
- **Caching**: Intelligent caching for frequently accessed data

### Battery Optimization
- **Polling Frequency**: Optimize update intervals
- **Background Processing**: Efficient background task scheduling
- **Resource Monitoring**: Minimal impact on battery life
- **Power Management**: Respect device power management policies

## Troubleshooting

### Common Issues
- **Build Failures**: Check Gradle version and dependency conflicts
- **Runtime Errors**: Verify permissions and service registration
- **Performance Issues**: Profile memory and CPU usage
- **Cross-Platform**: Test on target devices and browsers

### Debug Tools
- **Logging**: Use structured logging with appropriate levels
- **Profiling**: Android Studio profiler for performance analysis
- **WebView Debugging**: Chrome DevTools for WebView debugging
- **Resource Monitoring**: Built-in resource monitoring service

## Version Control

### Branch Strategy
- **Main Branch**: Stable, production-ready code
- **Feature Branches**: Isolated feature development
- **Resource Monitor**: Dedicated branch for resource monitoring features
- **Documentation**: Separate documentation updates

### Commit Messages
- **Format**: `type: description` (feat, fix, docs, test, refactor)
- **Scope**: Feature area (clip, whisper, ui, resource-monitor)
- **Description**: Clear, concise description of changes
- **Breaking Changes**: Document in commit message and PR

### Pull Requests
- **Description**: Detailed description of changes and rationale
- **Testing**: Include test results and verification steps
- **Documentation**: Update relevant documentation
- **Review**: Code review required for all changes