# Cursor Rules for VideoEdit Project

## Project Overview
This is a video editing application with robust language detection (LID) pipeline, clip processing, and UI components. The project uses Android/Kotlin for the main app, with Whisper integration for speech recognition and multilingual support.

## Code Organization

### Core Modules
- **app/**: Main Android application
- **feature/whisper/**: Whisper speech recognition module
- **core/**: Shared core functionality
- **Doc/**: Structured documentation by feature

### Key Components
- **LanguageDetectionService**: VAD + two-pass LID pipeline
- **TranscribeWorker**: Background processing with WorkManager
- **WhisperApi**: Multilingual model selection and work queuing
- **AndroidWhisperBridge**: JavaScript interface for WebView

## Coding Standards

### Kotlin Conventions
- Use camelCase for variables and functions
- Use PascalCase for classes and interfaces
- Use UPPER_SNAKE_CASE for constants
- Prefer `val` over `var` when possible
- Use data classes for simple data containers

### Android Best Practices
- Use WorkManager for background processing
- Implement proper error handling with Result<T>
- Use dependency injection (Hilt/Dagger)
- Follow MVVM architecture pattern
- Use coroutines for async operations

### Documentation Standards
- Document all public APIs with KDoc
- Include code examples for complex functions
- Maintain architecture decision records (ADRs)
- Update README files for major changes

## File Structure Guidelines

### Package Organization
```
com.mira.whisper/
├── engine/           # Core Whisper functionality
├── api/              # Public APIs
├── runner/           # Background workers
├── data/             # Data models and repositories
└── ui/               # UI components
```

### Documentation Structure
```
Doc/
├── whisper/          # Whisper-specific documentation
├── clip/             # Clip processing documentation
├── ui/               # UI component documentation
└── cursor_rule.md    # This file
```

## Development Workflow

### Branch Strategy
- **main**: Production-ready code
- **whisper**: Whisper feature branch
- **feature/***: Feature development branches
- **hotfix/***: Critical bug fixes

### Commit Convention
Use conventional commits format:
- `feat:` New features
- `fix:` Bug fixes
- `docs:` Documentation changes
- `style:` Code style changes
- `refactor:` Code refactoring
- `test:` Test additions/changes
- `chore:` Build/tooling changes

### Testing Requirements
- Unit tests for all public APIs
- Integration tests for LID pipeline
- Device-specific validation tests
- Performance benchmarking

## Whisper Module Guidelines

### Language Detection Pipeline
- Always use multilingual models (no .en suffix)
- Implement VAD windowing for better LID accuracy
- Use two-pass re-scoring for uncertain cases
- Include confidence scores in sidecar data

### Background Processing
- Use WorkManager for non-blocking operations
- Implement proper error handling and retry logic
- Monitor RTF (Real-Time Factor) performance
- Use appropriate thread counts for device optimization

### Model Management
- Deploy multilingual models by default
- Verify model integrity with SHA-256 checks
- Support model fallback strategies
- Document model performance characteristics

## Performance Considerations

### Memory Management
- Stream large audio files to disk
- Use appropriate buffer sizes for processing
- Monitor memory usage during long operations
- Implement proper cleanup for temporary files

### Processing Optimization
- Use device-specific thread counts
- Implement RTF monitoring and alerting
- Optimize for ARM64 architecture (Xiaomi Pad)
- Support CPU+GPU compute units (iPad)

### Battery Optimization
- Use background processing efficiently
- Implement proper wake lock management
- Monitor thermal throttling
- Provide power-aware processing modes

## Security Guidelines

### Data Privacy
- Process audio locally (no cloud transmission)
- Implement secure temporary file cleanup
- Use encrypted storage for sensitive data
- Follow Android security best practices

### Model Security
- Verify model integrity with checksums
- Implement secure model loading
- Use permission-based access control
- Document security considerations

## Error Handling

### Exception Management
- Use Result<T> for operations that can fail
- Implement proper logging with appropriate levels
- Provide user-friendly error messages
- Include debugging information in logs

### Recovery Strategies
- Implement retry logic for transient failures
- Provide fallback mechanisms for model loading
- Handle device-specific limitations gracefully
- Document error scenarios and solutions

## Testing Strategy

### Unit Testing
- Test all public APIs with comprehensive coverage
- Mock external dependencies appropriately
- Use parameterized tests for multiple scenarios
- Include edge cases and error conditions

### Integration Testing
- Test LID pipeline end-to-end
- Validate multilingual model integration
- Test background processing workflows
- Verify sidecar data generation

### Device Testing
- Test on Xiaomi Pad Ultra (primary target)
- Validate iPad Pro compatibility
- Test macOS Web version
- Include performance benchmarking

## Documentation Requirements

### Architecture Documentation
- Document control knots and configuration options
- Include performance characteristics and trade-offs
- Provide code pointers for key implementations
- Maintain decision records for major changes

### Deployment Documentation
- Document platform-specific deployment steps
- Include troubleshooting guides
- Provide validation scripts and commands
- Maintain release notes and changelog

### API Documentation
- Document all public APIs with KDoc
- Include usage examples and code snippets
- Document parameter ranges and constraints
- Provide migration guides for breaking changes

## Code Review Guidelines

### Review Checklist
- [ ] Code follows project conventions
- [ ] Tests are included and passing
- [ ] Documentation is updated
- [ ] Performance implications considered
- [ ] Security considerations addressed
- [ ] Error handling is appropriate

### Review Focus Areas
- LID pipeline implementation correctness
- Background processing efficiency
- Memory usage and cleanup
- Error handling and recovery
- Test coverage and quality

## Deployment Guidelines

### CI/CD Pipeline
- Use GitHub Actions for automated testing
- Implement automated deployment to staging
- Include performance regression testing
- Maintain deployment rollback procedures

### Release Process
- Follow semantic versioning
- Include comprehensive release notes
- Test on all target platforms
- Validate performance metrics

### Monitoring
- Implement application performance monitoring
- Monitor RTF and accuracy metrics
- Track error rates and types
- Alert on performance degradation

## Common Patterns

### Background Processing
```kotlin
class TranscribeWorker : Worker {
    override suspend fun doWork(): Result {
        return try {
            // Processing logic
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            Result.failure()
        }
    }
}
```

### Language Detection
```kotlin
val lidResult = if (lang == "auto") {
    val lidService = LanguageDetectionService()
    lidService.detectLanguage(pcm16k, 16_000, model, threads)
} else {
    LanguageDetectionResult(/* forced language */)
}
```

### Error Handling
```kotlin
fun processAudio(): Result<AudioResult> {
    return try {
        val result = performProcessing()
        Result.success(result)
    } catch (e: ProcessingException) {
        Log.e(TAG, "Processing failed: ${e.message}", e)
        Result.failure(e)
    }
}
```

## Resources

### Documentation
- [Architecture Design](Doc/whisper/Architecture%20Design%20and%20Control%20Knot.md)
- [Implementation Details](Doc/whisper/Full%20scale%20implementation%20Details.md)
- [Deployment Guide](Doc/whisper/Device%20Deployment.md)
- [CI/CD Guide](Doc/whisper/CI/CD%20Guide%20&%20Release.md)

### Scripts
- `deploy_multilingual_models.sh` - Model deployment
- `test_lid_pipeline.sh` - LID validation
- `validate_cicd_pipeline.sh` - CI/CD validation
- `work_through_xiaomi_pad.sh` - Device testing

### Key Files
- `LanguageDetectionService.kt` - Core LID implementation
- `TranscribeWorker.kt` - Background processing
- `WhisperApi.kt` - Public API
- `WhisperParams.kt` - Configuration