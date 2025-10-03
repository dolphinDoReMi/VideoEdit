# Whisper Cursor Rules

## Overview

This document defines the Cursor rules and development guidelines for the Whisper speech recognition integration in the Mira Video Editor. These rules ensure consistent development practices, code quality, and maintainability.

## Development Rules

### 1. Code Quality Standards

#### Kotlin Code Style
```kotlin
// ✅ Good: Clear naming and documentation
class WhisperEngine(private val context: Context) {
    /**
     * Initializes the Whisper model with specified size
     * @param modelSize The size of the model to load
     * @return true if initialization successful, false otherwise
     */
    fun initialize(modelSize: ModelSize = ModelSize.BASE): Boolean {
        // Implementation
    }
}

// ❌ Bad: Unclear naming, no documentation
class WE(ctx: Context) {
    fun init(sz: ModelSize = ModelSize.BASE): Boolean {
        // Implementation
    }
}
```

#### Error Handling
```kotlin
// ✅ Good: Comprehensive error handling
suspend fun transcribe(audioUri: Uri): TranscriptionResult {
    return try {
        val audioData = audioExtractor.extractAudioSegment(audioUri)
        val processedAudio = normalizer.normalizeAudio(audioData)
        val transcription = whisperBridge.transcribe(processedAudio)
        
        TranscriptionResult(
            text = transcription,
            confidence = calculateConfidence(transcription),
            processingTime = System.currentTimeMillis() - startTime
        )
    } catch (e: AudioExtractionException) {
        Log.e(TAG, "Audio extraction failed", e)
        throw TranscriptionException("Failed to extract audio", e)
    } catch (e: ProcessingException) {
        Log.e(TAG, "Audio processing failed", e)
        throw TranscriptionException("Failed to process audio", e)
    }
}

// ❌ Bad: Poor error handling
suspend fun transcribe(audioUri: Uri): String {
    val audioData = audioExtractor.extractAudioSegment(audioUri)
    val processedAudio = normalizer.normalizeAudio(audioData)
    return whisperBridge.transcribe(processedAudio)
}
```

### 2. Testing Requirements

#### Unit Testing
```kotlin
// ✅ Good: Comprehensive unit tests
class WhisperEngineTest {
    @Test
    fun `should initialize with base model`() {
        // Given
        val engine = WhisperEngine(mockContext)
        
        // When
        val result = engine.initialize(ModelSize.BASE)
        
        // Then
        assertTrue(result)
        assertTrue(engine.isInitialized())
    }
    
    @Test
    fun `should fail initialization with invalid model`() {
        // Given
        val engine = WhisperEngine(mockContext)
        
        // When
        val result = engine.initialize(ModelSize.INVALID)
        
        // Then
        assertFalse(result)
        assertFalse(engine.isInitialized())
    }
}

// ❌ Bad: Incomplete testing
class WhisperEngineTest {
    @Test
    fun testInit() {
        val engine = WhisperEngine(mockContext)
        assertTrue(engine.initialize())
    }
}
```

#### Integration Testing
```kotlin
// ✅ Good: Integration test with real data
@RunWith(AndroidJUnit4::class)
class WhisperIntegrationTest {
    @Test
    fun `should process real audio file`() {
        // Given
        val testAudioUri = Uri.parse("file:///sdcard/test_audio.wav")
        val engine = WhisperEngine(InstrumentationRegistry.getInstrumentation().context)
        
        // When
        val result = runBlocking { engine.transcribe(testAudioUri) }
        
        // Then
        assertNotNull(result)
        assertTrue(result.text.isNotEmpty())
        assertTrue(result.confidence > 0.0f)
    }
}
```

### 3. Performance Guidelines

#### Memory Management
```kotlin
// ✅ Good: Proper memory management
class WhisperEngine(private val context: Context) {
    private var model: WhisperModel? = null
    
    fun initialize(modelSize: ModelSize): Boolean {
        return try {
            if (model == null) {
                model = WhisperModel.load(context.assets, modelSize)
                Log.d(TAG, "Model loaded: $modelSize")
            }
            true
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Insufficient memory for model", e)
            cleanup()
            false
        }
    }
    
    fun cleanup() {
        model?.close()
        model = null
        System.gc() // Force garbage collection
    }
}

// ❌ Bad: Poor memory management
class WhisperEngine(private val context: Context) {
    private var model: WhisperModel? = null
    
    fun initialize(modelSize: ModelSize): Boolean {
        model = WhisperModel.load(context.assets, modelSize)
        return true
    }
}
```

#### Resource Management
```kotlin
// ✅ Good: Resource management with try-with-resources
class AudioExtractor(private val context: Context) {
    fun extractAudioSegment(uri: Uri, startMs: Long, endMs: Long): AudioData {
        val inputStream = context.contentResolver.openInputStream(uri)
        return inputStream?.use { stream ->
            val buffer = ByteArray(8192)
            val audioData = ByteArrayOutputStream()
            
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                audioData.write(buffer, 0, bytesRead)
            }
            
            AudioData(audioData.toByteArray())
        } ?: throw AudioExtractionException("Failed to open input stream")
    }
}
```

### 4. Configuration Management

#### Build Configuration
```kotlin
// ✅ Good: Centralized configuration
object WhisperConfig {
    const val DEFAULT_SAMPLE_RATE = 16000
    const val DEFAULT_CHANNELS = 1
    const val DEFAULT_MODEL_SIZE = "tiny"
    const val DEFAULT_THREADS = 4
    
    val MODEL_SIZES = mapOf(
        "tiny" to ModelSize.TINY,
        "base" to ModelSize.BASE,
        "small" to ModelSize.SMALL,
        "medium" to ModelSize.MEDIUM,
        "large" to ModelSize.LARGE
    )
}

// ❌ Bad: Scattered configuration
class WhisperEngine {
    private val sampleRate = 16000
    private val channels = 1
    private val modelSize = "tiny"
    private val threads = 4
}
```

#### Runtime Configuration
```kotlin
// ✅ Good: Flexible runtime configuration
data class WhisperParams(
    val modelSize: ModelSize = ModelSize.BASE,
    val language: String = "auto",
    val translate: Boolean = false,
    val temperature: Float = 0.0f,
    val beamSize: Int = 1,
    val threads: Int = 4,
    val enableWordTimestamps: Boolean = false
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("model_size", modelSize.name)
            put("language", language)
            put("translate", translate)
            put("temperature", temperature)
            put("beam_size", beamSize)
            put("threads", threads)
            put("word_timestamps", enableWordTimestamps)
        }.toString()
    }
}
```

### 5. Documentation Standards

#### Code Documentation
```kotlin
// ✅ Good: Comprehensive documentation
/**
 * WhisperEngine provides speech-to-text transcription capabilities using whisper.cpp
 * 
 * Features:
 * - Multiple model sizes (tiny, base, small, medium, large)
 * - Configurable language detection
 * - Translation support
 * - Word-level timestamps
 * - Deterministic processing
 * 
 * @param context Android context for resource access
 * @param config Configuration parameters for the engine
 * 
 * @see WhisperParams for configuration options
 * @see TranscriptionResult for output format
 */
class WhisperEngine(
    private val context: Context,
    private val config: WhisperParams = WhisperParams()
) {
    /**
     * Transcribes audio from the given URI
     * 
     * @param audioUri URI pointing to the audio file
     * @param startMs Start time in milliseconds (optional)
     * @param endMs End time in milliseconds (optional)
     * @return TranscriptionResult containing text, confidence, and metadata
     * @throws TranscriptionException if transcription fails
     */
    suspend fun transcribe(
        audioUri: Uri,
        startMs: Long? = null,
        endMs: Long? = null
    ): TranscriptionResult {
        // Implementation
    }
}
```

### 6. Security Guidelines

#### Data Privacy
```kotlin
// ✅ Good: Privacy-conscious implementation
class WhisperEngine {
    private val privacyManager = PrivacyManager()
    
    suspend fun transcribe(audioUri: Uri): TranscriptionResult {
        // Check privacy settings
        if (!privacyManager.isTranscriptionAllowed()) {
            throw PrivacyException("Transcription not allowed")
        }
        
        // Process audio locally only
        val audioData = extractAudioLocally(audioUri)
        val result = processLocally(audioData)
        
        // Log privacy-compliant metrics only
        privacyManager.logTranscriptionMetrics(result)
        
        return result
    }
    
    private fun extractAudioLocally(uri: Uri): AudioData {
        // Ensure audio is processed locally, never sent to external services
        return localAudioExtractor.extract(uri)
    }
}
```

#### Input Validation
```kotlin
// ✅ Good: Input validation
class AudioExtractor {
    fun extractAudioSegment(uri: Uri, startMs: Long, endMs: Long): AudioData {
        // Validate URI
        require(uri.scheme in listOf("file", "content")) {
            "Unsupported URI scheme: ${uri.scheme}"
        }
        
        // Validate time range
        require(startMs >= 0) { "Start time must be non-negative" }
        require(endMs > startMs) { "End time must be greater than start time" }
        require(endMs - startMs <= MAX_SEGMENT_DURATION_MS) {
            "Segment duration exceeds maximum allowed"
        }
        
        // Extract audio
        return performExtraction(uri, startMs, endMs)
    }
}
```

### 7. Error Handling Standards

#### Exception Hierarchy
```kotlin
// ✅ Good: Structured exception hierarchy
sealed class WhisperException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ModelLoadException(message: String, cause: Throwable? = null) : WhisperException(message, cause)
class AudioExtractionException(message: String, cause: Throwable? = null) : WhisperException(message, cause)
class TranscriptionException(message: String, cause: Throwable? = null) : WhisperException(message, cause)
class ConfigurationException(message: String, cause: Throwable? = null) : WhisperException(message, cause)
class PrivacyException(message: String, cause: Throwable? = null) : WhisperException(message, cause)
```

#### Error Recovery
```kotlin
// ✅ Good: Graceful error recovery
class WhisperEngine {
    suspend fun transcribeWithFallback(audioUri: Uri): TranscriptionResult {
        return try {
            transcribe(audioUri)
        } catch (e: ModelLoadException) {
            Log.w(TAG, "Model load failed, trying fallback", e)
            transcribeWithFallbackModel(audioUri)
        } catch (e: AudioExtractionException) {
            Log.w(TAG, "Audio extraction failed, trying alternative method", e)
            transcribeWithAlternativeExtraction(audioUri)
        } catch (e: TranscriptionException) {
            Log.e(TAG, "Transcription failed completely", e)
            throw e
        }
    }
}
```

### 8. Testing Standards

#### Test Coverage Requirements
- **Unit Tests**: Minimum 80% code coverage
- **Integration Tests**: All public APIs must have integration tests
- **Performance Tests**: Critical paths must have performance benchmarks
- **Error Tests**: All error conditions must be tested

#### Test Naming Conventions
```kotlin
// ✅ Good: Descriptive test names
@Test
fun `should initialize with base model when valid context provided`() {
    // Test implementation
}

@Test
fun `should throw ModelLoadException when insufficient memory available`() {
    // Test implementation
}

@Test
fun `should transcribe audio successfully with high confidence`() {
    // Test implementation
}

// ❌ Bad: Unclear test names
@Test
fun testInit() {
    // Test implementation
}

@Test
fun testError() {
    // Test implementation
}
```

### 9. Performance Standards

#### Memory Usage
- **Peak Memory**: Must not exceed 200MB for base model
- **Memory Leaks**: Zero tolerance for memory leaks
- **Garbage Collection**: Proper cleanup of resources

#### Processing Time
- **Real-time Factor**: Must be < 1.0 for real-time processing
- **Latency**: Audio-to-text latency must be < 5 seconds for 30-second segments
- **Throughput**: Must handle at least 10 concurrent transcription requests

#### Battery Impact
- **Thermal Management**: Automatic throttling when device temperature > 45°C
- **Battery Optimization**: Respect device battery optimization settings
- **Background Processing**: Efficient background processing with WorkManager

### 10. Deployment Standards

#### Build Variants
```kotlin
// ✅ Good: Proper build variant configuration
android {
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
        }
        
        internal {
            initWith(release)
            applicationIdSuffix = ".internal"
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
        }
    }
}
```

#### ProGuard Rules
```proguard
# ✅ Good: Proper ProGuard configuration
-keep class com.mira.com.feature.whisper.** { *; }
-keep class whisper.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI bridge
-keep class com.mira.com.feature.whisper.engine.WhisperBridge { *; }
```

## Code Review Checklist

### Before Submitting Code
- [ ] All tests pass
- [ ] Code coverage meets requirements
- [ ] Documentation is complete
- [ ] Error handling is comprehensive
- [ ] Performance benchmarks are met
- [ ] Security guidelines are followed
- [ ] Privacy requirements are met
- [ ] Memory management is proper
- [ ] Resource cleanup is implemented

### During Code Review
- [ ] Code follows style guidelines
- [ ] Logic is correct and efficient
- [ ] Error cases are handled
- [ ] Tests are comprehensive
- [ ] Documentation is accurate
- [ ] Performance impact is acceptable
- [ ] Security implications are considered

## Repository-Specific Rules

### When touching WhisperEngine or TranscribeWorker
- **Run instrumented tests**: `./gradlew :app:connectedDebugAndroidTest`
- **Verify deterministic processing**: Run hash comparison tests
- **Check memory usage**: Monitor for memory leaks

### When modifying database entities or DAOs
- **Run unit tests**: `./gradlew :app:testDebugUnitTest`
- **Verify data integrity**: Check for proper data validation
- **Test migration scripts**: Ensure database migrations work correctly

### When changing ML models or encoders
- **Run all tests**: `./gradlew :app:testDebugUnitTest` and `./gradlew :app:connectedDebugAndroidTest`
- **Verify model compatibility**: Check model loading and inference
- **Test performance impact**: Benchmark processing time and memory usage

### When modifying workers
- **Run instrumented tests**: `./gradlew :app:connectedDebugAndroidTest --tests '*IngestWorkerInstrumentedTest*'`
- **Verify background processing**: Test WorkManager integration
- **Check error handling**: Ensure proper error recovery

### When changing video processing
- **Run frame sampler tests**: `./gradlew :app:connectedDebugAndroidTest --tests '*SamplerInstrumentedTest*'`
- **Verify audio extraction**: Test MediaExtractor integration
- **Check processing pipeline**: Ensure end-to-end functionality

## Conclusion

These Cursor rules ensure consistent, high-quality development practices for the Whisper integration. Following these guidelines will result in maintainable, performant, and secure code that meets production requirements.

**Status**: ✅ **ACTIVE**  
**Compliance**: ✅ **REQUIRED**  
**Review Process**: ✅ **MANDATORY**  
**Quality Standards**: ✅ **ENFORCED**
