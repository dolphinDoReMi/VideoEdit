# Cursor Rules

## Development Guidelines and Standards

This document outlines the development guidelines, coding standards, and best practices for the VideoEdit multi-modal AI video processing platform.

## Code Organization

### Project Structure
```
VideoEdit/
├── app/                    # Main Android application
├── feature/                # Feature modules
│   ├── whisper/           # Whisper ASR implementation
│   ├── clip/              # CLIP visual understanding
│   └── infra/             # Infrastructure services
├── core/                  # Shared core functionality
├── docs/                  # Comprehensive documentation
│   ├── whisper/          # Whisper documentation
│   ├── clip/             # CLIP documentation
│   ├── infra/            # Infrastructure documentation
│   └── CICD & Release Guide/ # CI/CD documentation
└── scripts/               # Utility scripts
```

### Module Dependencies
- **Core**: Shared utilities, data models, and common functionality
- **Feature Modules**: Self-contained feature implementations
- **App**: Main application with feature integration
- **Docs**: Comprehensive documentation and scripts

## Coding Standards

### Kotlin (Android)
```kotlin
// Use data classes for immutable data
data class WhisperResult(
    val segments: List<Segment>,
    val language: String,
    val confidence: Float
)

// Use sealed classes for state management
sealed class ProcessingState {
    object Idle : ProcessingState()
    data class Processing(val progress: Float) : ProcessingState()
    data class Completed(val result: WhisperResult) : ProcessingState()
    data class Error(val exception: Throwable) : ProcessingState()
}

// Use extension functions for utility functions
fun Context.getExternalFilesDir(): File = getExternalFilesDir(null) ?: filesDir

// Use coroutines for async operations
suspend fun processAudio(audioFile: File): WhisperResult = withContext(Dispatchers.IO) {
    // Processing logic
}
```

### TypeScript (Web)
```typescript
// Use interfaces for type definitions
interface WhisperConfig {
  modelPath: string;
  sampleRate: number;
  channels: number;
  language: string;
  translate: boolean;
}

// Use enums for constants
enum ProcessingState {
  Idle = 'idle',
  Processing = 'processing',
  Completed = 'completed',
  Error = 'error'
}

// Use async/await for async operations
async function processAudio(audioFile: File): Promise<WhisperResult> {
  const result = await whisperEngine.transcribe(audioFile);
  return result;
}
```

### Swift (iOS)
```swift
// Use structs for data models
struct WhisperResult {
    let segments: [Segment]
    let language: String
    let confidence: Float
}

// Use enums for state management
enum ProcessingState {
    case idle
    case processing(Float)
    case completed(WhisperResult)
    case error(Error)
}

// Use async/await for async operations
func processAudio(audioFile: URL) async throws -> WhisperResult {
    let result = try await whisperEngine.transcribe(audioFile: audioFile)
    return result
}
```

## Documentation Standards

### Architecture Documentation
- **Status**: Current implementation status
- **Control Knots**: Key configuration parameters
- **Implementation**: Technical implementation details
- **Verification**: Testing and validation procedures

### Code Documentation
```kotlin
/**
 * Processes audio file using Whisper ASR engine
 * 
 * @param audioFile Input audio file
 * @param config Whisper configuration parameters
 * @return WhisperResult with transcription segments
 * @throws ProcessingException if processing fails
 */
suspend fun processAudio(
    audioFile: File,
    config: WhisperConfig
): WhisperResult {
    // Implementation
}
```

### API Documentation
- **Endpoint**: API endpoint URL
- **Method**: HTTP method (GET, POST, PUT, DELETE)
- **Parameters**: Request parameters and types
- **Response**: Response format and status codes
- **Examples**: Usage examples

## Testing Standards

### Unit Tests
```kotlin
@Test
fun `processAudio should return valid result for valid input`() = runTest {
    // Given
    val audioFile = createTestAudioFile()
    val config = WhisperConfig.default()
    
    // When
    val result = whisperEngine.processAudio(audioFile, config)
    
    // Then
    assertThat(result.segments).isNotEmpty()
    assertThat(result.language).isEqualTo("en")
    assertThat(result.confidence).isGreaterThan(0.8f)
}
```

### Integration Tests
```kotlin
@Test
fun `whisper pipeline should process video file end-to-end`() = runTest {
    // Given
    val videoFile = createTestVideoFile()
    
    // When
    val result = processVideo(videoFile)
    
    // Then
    assertThat(result.transcript).isNotEmpty()
    assertThat(result.clips).isNotEmpty()
    assertThat(result.metadata).isNotNull()
}
```

### E2E Tests
```bash
#!/bin/bash
# Test complete pipeline
cd docs/whisper/scripts
./test_comprehensive_pipeline.sh

# Test CLIP integration
cd ../clip/scripts
./test_clip_pipeline.sh

# Test infrastructure
cd ../infra/scripts
./test_infrastructure_consistency.sh
```

## Performance Standards

### Memory Management
- **Streaming Processing**: For files >100MB to prevent OOM
- **Model Loading**: Lazy loading of ML models
- **Cache Management**: Intelligent caching with size limits
- **Memory Monitoring**: Real-time memory usage tracking

### CPU Optimization
- **Threading**: Configurable thread count based on device capabilities
- **Background Processing**: WorkManager for background tasks
- **Resource Monitoring**: CPU usage tracking and optimization
- **Thermal Management**: Thread count optimization for device thermals

### Storage Optimization
- **App-Scoped Storage**: Use app-scoped storage instead of public directories
- **Atomic Writes**: Temp file + rename pattern for data integrity
- **Compression**: Use compressed model formats (GGUF)
- **Cleanup**: Automatic cleanup of temporary files

## Security Standards

### Data Protection
- **Encryption**: Encrypt sensitive data at rest and in transit
- **Authentication**: Secure authentication mechanisms
- **Authorization**: Proper access control and permissions
- **Audit Logging**: Comprehensive audit trails

### Code Security
- **Dependency Scanning**: Regular security scanning of dependencies
- **Code Analysis**: Static code analysis for security vulnerabilities
- **Secrets Management**: Secure handling of API keys and secrets
- **Input Validation**: Proper validation of user inputs

## Error Handling

### Exception Handling
```kotlin
try {
    val result = processAudio(audioFile, config)
    return Result.success(result)
} catch (e: SecurityException) {
    Log.e(TAG, "Security error: ${e.message}", e)
    return Result.retry()
} catch (e: IOException) {
    Log.e(TAG, "I/O error: ${e.message}", e)
    return Result.retry()
} catch (e: Exception) {
    Log.e(TAG, "Unexpected error: ${e.message}", e)
    return Result.failure()
}
```

### Error Recovery
- **Retry Logic**: Exponential backoff for transient failures
- **Fallback Mechanisms**: Graceful degradation when possible
- **User Feedback**: Clear error messages for users
- **Logging**: Comprehensive error logging for debugging

## Deployment Standards

### Build Configuration
- **Debug Builds**: Development and testing builds
- **Release Builds**: Production-ready builds with optimizations
- **Staging Builds**: Pre-production testing builds
- **Version Management**: Semantic versioning (MAJOR.MINOR.PATCH)

### Release Process
1. **Feature Development**: Develop features in feature branches
2. **Code Review**: Thorough code review process
3. **Testing**: Comprehensive testing including unit, integration, and E2E tests
4. **Release Preparation**: Update version numbers and documentation
5. **Deployment**: Automated deployment to staging and production
6. **Monitoring**: Post-deployment monitoring and validation

## Quality Assurance

### Code Quality
- **Linting**: ESLint, Prettier, Detekt for code quality
- **Type Checking**: TypeScript, Kotlin type checking
- **Code Coverage**: Minimum 80% test coverage
- **Performance**: Performance benchmarks and monitoring

### Documentation Quality
- **Accuracy**: Keep documentation up-to-date with code changes
- **Completeness**: Comprehensive documentation for all features
- **Clarity**: Clear and concise documentation
- **Examples**: Practical examples and use cases

## Best Practices

### Development
- **Small Commits**: Frequent, small commits with clear messages
- **Feature Branches**: Use feature branches for new development
- **Code Reviews**: Thorough code review process
- **Testing**: Write tests before implementing features (TDD)

### Maintenance
- **Regular Updates**: Keep dependencies and tools up-to-date
- **Performance Monitoring**: Monitor performance metrics
- **Error Tracking**: Track and analyze errors
- **User Feedback**: Collect and act on user feedback

### Collaboration
- **Communication**: Clear communication in PRs and issues
- **Documentation**: Keep documentation current and comprehensive
- **Knowledge Sharing**: Share knowledge through documentation and code reviews
- **Continuous Improvement**: Regular retrospectives and process improvements

---

**Last Updated**: October 8, 2025  
**Version**: 1.0  
**Status**: Production Ready