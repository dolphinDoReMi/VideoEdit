# CLIP Feature Cursor Rules

## Overview

This document defines the cursor rules and development guidelines for the CLIP feature implementation, ensuring consistent code quality, proper testing, and maintainable architecture.

## Development Rules

### 1. CLIP Engine Development

#### When modifying CLIP engines or model loading:
```bash
# Run instrumented tests for CLIP functionality
./gradlew :app:connectedDebugAndroidTest --tests '*ClipEngineInstrumentedTest*'
./gradlew :app:connectedDebugAndroidTest --tests '*ClipEnginesInstrumentedTest*'
```

#### Code Quality Requirements:
- All CLIP model loading must include proper error handling
- Embedding generation must be deterministic (no randomness)
- Memory management must be optimized for mobile devices
- GPU acceleration must be properly configured

### 2. CLIP Database and Storage

#### When modifying CLIP database entities or DAOs:
```bash
# Run unit tests for database layer
./gradlew :app:testDebugUnitTest --tests '*ClipDatabaseTest*'
./gradlew :app:testDebugUnitTest --tests '*ClipDaoTest*'
```

#### Database Requirements:
- All CLIP embeddings must be stored with proper indexing
- Database migrations must be backward compatible
- Vector storage must be optimized for similarity search
- Encryption must be enabled for sensitive data

### 3. CLIP Preprocessing and Sampling

#### When modifying CLIP preprocessing or frame sampling:
```bash
# Run comprehensive CLIP tests
./gradlew :app:connectedDebugAndroidTest --tests '*ClipPreprocessInstrumentedTest*'
./gradlew :app:connectedDebugAndroidTest --tests '*FrameSamplerInstrumentedTest*'
```

#### Preprocessing Requirements:
- All preprocessing must be deterministic (center crop, fixed normalization)
- Frame sampling must be configurable and reproducible
- Image preprocessing must maintain CLIP compatibility
- Performance must be optimized for real-time processing

### 4. CLIP Verification and Testing

#### When modifying CLIP verification system:
```bash
# Run verification tests
./ops/verify_all.sh
./ops/verify_all.sh representation
./ops/verify_all.sh retrieval
./ops/verify_all.sh reproducibility
```

#### Verification Requirements:
- All CLIP processing must be deterministic
- Hash comparison must be implemented for verification
- Performance benchmarks must be maintained
- Device-specific optimizations must be validated

### 5. CLIP Service Integration

#### When modifying CLIP service or API:
```bash
# Run service integration tests
./gradlew :app:connectedDebugAndroidTest --tests '*ClipServiceInstrumentedTest*'
./gradlew :app:connectedDebugAndroidTest --tests '*Clip4ClipServiceInstrumentedTest*'
```

#### Service Requirements:
- All CLIP services must be properly isolated
- Background processing must use WorkManager
- Service APIs must be thread-safe
- Error handling must be comprehensive

## Code Style Guidelines

### 1. CLIP-Specific Naming Conventions

```kotlin
// CLIP engine classes
class ClipEngines { }
class ClipPreprocessor { }
class ClipVerification { }

// CLIP configuration
object ClipConfig { }
object ClipSamplingConfig { }
object ClipPreprocessingConfig { }

// CLIP entities
data class VideoEmbedding { }
data class TextEmbedding { }
data class SimilarityResult { }
```

### 2. Error Handling Patterns

```kotlin
// CLIP engine initialization
suspend fun initialize(): Boolean {
    return try {
        imageEncoder = PytorchLoader.loadImageEncoder(context)
        textEncoder = PytorchLoader.loadTextEncoder(context)
        tokenizer = ClipBPETokenizer(vocabPath, mergesPath)
        true
    } catch (e: Exception) {
        Log.e("ClipEngines", "Failed to initialize CLIP engines", e)
        false
    }
}

// CLIP processing with proper error handling
suspend fun processVideo(videoUri: Uri): VideoProcessingResult {
    return try {
        val frameTimestamps = frameSampler.sampleFrames(videoUri, frameCount)
        val embeddings = processFrames(frameTimestamps)
        VideoProcessingResult.Success(embeddings)
    } catch (e: Exception) {
        Log.e("ClipService", "Video processing failed", e)
        VideoProcessingResult.Error(e.message ?: "Unknown error")
    }
}
```

### 3. Performance Optimization Patterns

```kotlin
// Memory-efficient frame processing
suspend fun processFrames(frameTimestamps: List<Long>): List<FloatArray> {
    return frameTimestamps.map { timestamp ->
        val frame = extractFrameAtTimestamp(timestamp)
        val embedding = encodeFrame(frame)
        frame.recycle() // Important: recycle bitmap to prevent memory leaks
        embedding
    }
}

// Batch processing for efficiency
suspend fun processBatch(frames: List<Bitmap>): List<FloatArray> {
    return frames.chunked(batchSize).flatMap { batch ->
        val embeddings = encodeBatch(batch)
        batch.forEach { it.recycle() }
        embeddings
    }
}
```

## Testing Requirements

### 1. Unit Tests

All CLIP components must have comprehensive unit tests:

```kotlin
@Test
fun clipEngines_initialization_success() {
    val context = mockk<Context>()
    val clipEngines = ClipEngines(context)
    
    val result = runBlocking { clipEngines.initialize() }
    
    assertTrue(result)
    assertNotNull(clipEngines.imageEncoder)
    assertNotNull(clipEngines.textEncoder)
    assertNotNull(clipEngines.tokenizer)
}

@Test
fun clipPreprocessing_deterministic() {
    val bitmap = createTestBitmap()
    val preprocessed1 = ClipPreprocessor.preprocessFrame(bitmap)
    val preprocessed2 = ClipPreprocessor.preprocessFrame(bitmap)
    
    assertArrayEquals(preprocessed1, preprocessed2, 1e-6f)
}
```

### 2. Instrumented Tests

All CLIP functionality must have instrumented tests:

```kotlin
@Test
fun clipEngines_e2e_processing() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val clipEngines = ClipEngines(context)
    
    runBlocking {
        clipEngines.initialize()
        
        val testBitmap = createTestBitmap()
        val embedding = clipEngines.encodeImage(testBitmap)
        
        assertEquals(512, embedding.size)
        assertTrue(embedding.all { it.isFinite() })
        
        val norm = sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
        assertEquals(1f, norm, 1e-4f) // L2 normalized
    }
}
```

### 3. Performance Tests

CLIP performance must be validated:

```kotlin
@Test
fun clipEngines_performance_benchmark() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val clipEngines = ClipEngines(context)
    
    runBlocking {
        clipEngines.initialize()
        
        val startTime = System.currentTimeMillis()
        repeat(100) {
            val testBitmap = createTestBitmap()
            clipEngines.encodeImage(testBitmap)
        }
        val endTime = System.currentTimeMillis()
        
        val avgTime = (endTime - startTime) / 100.0
        assertTrue(avgTime < 100) // Should be under 100ms per image
    }
}
```

## Documentation Requirements

### 1. Code Documentation

All CLIP classes and methods must be properly documented:

```kotlin
/**
 * CLIP engines for text and image encoding.
 * 
 * Provides encodeText() and encodeFrames() with L2-normalized outputs.
 * Output dimension = 512 (ViT-B/32) or 768 (ViT-L/14).
 * 
 * @param context Android context for model loading
 */
class ClipEngines(private val context: Context) {
    
    /**
     * Encode text to normalized embedding.
     * 
     * @param text Input text to encode
     * @return L2-normalized embedding vector of dimension 512
     * @throws IllegalStateException if engine not initialized
     */
    fun encodeText(text: String): FloatArray {
        // Implementation
    }
}
```

### 2. Architecture Documentation

All CLIP architecture decisions must be documented:

- Control knots and their purposes
- Performance characteristics
- Device-specific optimizations
- Integration points with existing systems

### 3. API Documentation

All CLIP service APIs must be documented:

- Input/output specifications
- Error handling behavior
- Performance characteristics
- Usage examples

## Security Requirements

### 1. Data Protection

- All CLIP embeddings must be encrypted at rest
- Sensitive data must not be logged
- User data must be processed locally
- No data must be sent to external services

### 2. Model Security

- CLIP models must be validated for integrity
- Model loading must include security checks
- No untrusted models must be loaded
- Model assets must be properly signed

## Performance Requirements

### 1. Memory Management

- CLIP processing must not exceed memory limits
- Bitmaps must be properly recycled
- Memory leaks must be prevented
- Cache sizes must be configurable

### 2. Processing Performance

- CLIP inference must be optimized for mobile
- GPU acceleration must be utilized when available
- Background processing must not block UI
- Performance must be monitored and logged

### 3. Battery Optimization

- CLIP processing must be battery-efficient
- Thermal management must be implemented
- Background processing must be optimized
- Power consumption must be monitored

## Device-Specific Requirements

### 1. Xiaomi Pad Optimization

- Frame count must be optimized for Snapdragon 870
- Memory usage must be within 4GB limit
- GPU acceleration must be configured for Adreno 650
- Thermal management must be implemented

### 2. iPad Optimization

- M2 Neural Engine must be utilized
- Metal Performance Shaders must be integrated
- Unified memory must be properly managed
- iOS-specific optimizations must be applied

## Integration Requirements

### 1. Existing Pipeline Integration

- CLIP must integrate seamlessly with video processing
- No breaking changes to existing APIs
- Backward compatibility must be maintained
- Performance must not degrade existing functionality

### 2. Database Integration

- CLIP data must be properly indexed
- Database migrations must be safe
- Vector storage must be optimized
- Query performance must be maintained

## Monitoring and Observability

### 1. Performance Monitoring

- CLIP processing times must be logged
- Memory usage must be tracked
- GPU utilization must be monitored
- Battery impact must be measured

### 2. Error Monitoring

- All CLIP errors must be logged
- Error rates must be tracked
- Performance degradation must be detected
- User experience must be monitored

## Release Requirements

### 1. Pre-Release Testing

- All CLIP tests must pass
- Performance benchmarks must be met
- Device compatibility must be verified
- Security audit must be completed

### 2. Release Validation

- CLIP functionality must be validated on target devices
- Performance must meet requirements
- User experience must be satisfactory
- Documentation must be complete

---

**Last Updated**: 2025-01-04  
**Version**: v1.0.0  
**Status**: ✅ Production Ready
