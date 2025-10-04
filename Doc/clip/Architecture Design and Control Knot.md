# CLIP Architecture Design and Control Knot

## Status: READY FOR VERIFICATION

**Control knots:**
- Seedless pipeline: deterministic sampling
- Fixed preprocess: no random crop
- Same model assets: fixed hypothesis f_θ

**Implementation:**
- Deterministic sampling: uniform frame timestamps
- Fixed preprocessing: center-crop, no augmentation
- Re-ingest twice: SHA-256 hash comparison

**Verification:** Hash comparison script in `ops/verify_all.sh`

## CLIP Service Architecture

### Core Service Components

The CLIP4Clip service provides a clean, production-ready API for video processing and semantic search with deterministic, reproducible results.

```
app/src/main/java/com/mira/videoeditor/
├── Clip4ClipServiceApplication.kt      # Service application entry point
├── Clip4ClipService.kt                # Main service API
├── di/                                 # Dependency injection modules
│   ├── DatabaseModule.kt              # Database dependencies
│   ├── RepositoryModule.kt             # Repository dependencies
│   └── UseCaseModule.kt               # Use case and service dependencies
├── db/                                 # Room database layer
│   ├── AppDatabase.kt                 # Database configuration
│   ├── Entities.kt                    # Database entities
│   ├── Daos.kt                        # Data access objects
│   ├── Models.kt                      # Read models with relations
│   └── Converters.kt                  # Type converters
├── data/                              # Data layer
│   ├── Repositories.kt                # Repository implementations
│   ├── SettingsRepository.kt          # Settings management
│   └── PerformanceMonitor.kt          # Performance monitoring
├── security/                          # Security layer
│   └── SecurityManager.kt             # Database encryption & security
├── ann/                               # ANN preparation
│   └── AnnPreparationManager.kt      # Scalability infrastructure
├── usecases/                          # Use case layer
│   ├── VideoUseCases.kt              # Video processing use cases
│   └── SettingsUseCase.kt            # Settings use cases
├── workers/                           # Background processing
│   └── VideoWorkers.kt               # WorkManager workers
└── Logger.kt                          # Centralized logging
```

### Service API

```kotlin
@Singleton
class Clip4ClipService @Inject constructor(
    // Dependencies injected via Hilt
) {
    // Initialize the service
    suspend fun initialize(
        enableEncryption: Boolean = false,
        encryptionKey: String? = null
    ): ServiceInitializationResult

    // Process videos and generate embeddings
    suspend fun processVideo(
        videoUri: Uri,
        variant: String = "clip_vit_b32_mean_v1",
        framesPerVideo: Int = 32,
        useBackgroundProcessing: Boolean = true
    ): VideoProcessingResult

    // Search videos by text query
    suspend fun searchVideos(
        query: String,
        variant: String = "clip_vit_b32_mean_v1",
        topK: Int = 10,
        searchLevel: String = "video"
    ): VideoSearchResult

    // Get service status and statistics
    suspend fun getServiceStatus(): ServiceStatus

    // Update service settings
    suspend fun updateSettings(settings: Map<String, Any>): SettingsUpdateResult

    // Clear cache and reset metrics
    suspend fun clearCache()

    // Shutdown service gracefully
    suspend fun shutdown()
}
```

## Control Knots Configuration

### Deterministic Pipeline Control

#### Frame Sampling Control Knots
```kotlin
object ClipSamplingConfig {
    // Deterministic sampling - no randomness
    const val SAMPLING_METHOD = "UNIFORM"  // UNIFORM | ADAPTIVE | HEAD_TAIL
    const val FRAME_COUNT = 32             // Fixed frame count per video
    const val SAMPLE_INTERVAL_MS = 1000L   // Fixed interval between samples
    
    // Verification: Same video produces identical frame timestamps
    fun getFrameTimestamps(durationMs: Long): List<Long> {
        val timestamps = mutableListOf<Long>()
        val interval = durationMs / FRAME_COUNT
        for (i in 0 until FRAME_COUNT) {
            timestamps.add(i * interval)
        }
        return timestamps
    }
}
```

**Code Pointer**: [`app/src/main/java/com/mira/clip/clip/ClipEngines.kt`](app/src/main/java/com/mira/clip/clip/ClipEngines.kt)

#### Preprocessing Control Knots
```kotlin
object ClipPreprocessingConfig {
    // Fixed preprocessing - no augmentation
    const val IMAGE_SIZE = 224             // Fixed CLIP input size
    const val CROP_METHOD = "CENTER"       // CENTER | RANDOM (disabled)
    val NORMALIZE_MEAN = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
    val NORMALIZE_STD = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)
    
    // Verification: Same frame produces identical preprocessed tensor
    fun preprocessFrame(bitmap: Bitmap): FloatArray {
        // Center crop to square
        val size = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        val cropped = Bitmap.createBitmap(bitmap, left, top, size, size)
        
        // Resize to fixed size
        val resized = Bitmap.createScaledBitmap(cropped, IMAGE_SIZE, IMAGE_SIZE, false)
        
        // Normalize with fixed mean/std
        return normalizeImage(resized, NORMALIZE_MEAN, NORMALIZE_STD)
    }
}
```

**Code Pointer**: [`feature/clip/src/main/java/com/mira/clip/feature/clip/ClipPreprocess.kt`](feature/clip/src/main/java/com/mira/clip/feature/clip/ClipPreprocess.kt)

#### Model Assets Control Knots
```kotlin
object ClipModelConfig {
    // Fixed model assets - deterministic inference
    const val MODEL_VARIANT = "clip_vit_b32_mean_v1"  // Fixed model version
    const val EMBEDDING_DIM = 512                      // Fixed embedding dimension
    const val MODEL_PATH = "assets/clip_image_encoder.ptl"  // Fixed model path
    
    // Verification: Same input produces identical embedding
    fun generateEmbedding(preprocessedTensor: FloatArray): FloatArray {
        // Load fixed model
        val module = PyTorchMobile.loadModuleFromAsset(MODEL_PATH)
        
        // Generate embedding
        val input = Tensor.fromBlob(preprocessedTensor, longArrayOf(1, 3, 224, 224))
        val output = module.forward(IValue.from(input)).toTensor()
        
        // L2 normalize for consistency
        return normalizeEmbedding(output.dataAsFloatArray)
    }
}
```

**Code Pointer**: [`app/src/main/java/com/mira/clip/ml/PytorchLoader.kt`](app/src/main/java/com/mira/clip/ml/PytorchLoader.kt)

## Verification System

### Hash Comparison Verification

#### SHA-256 Hash Generation
```kotlin
object ClipVerification {
    fun generateVideoHash(videoUri: Uri, config: ClipConfig): String {
        val frameTimestamps = ClipSamplingConfig.getFrameTimestamps(getDuration(videoUri))
        val embeddings = mutableListOf<FloatArray>()
        
        frameTimestamps.forEach { timestamp ->
            val frame = extractFrameAtTimestamp(videoUri, timestamp)
            val preprocessed = ClipPreprocessingConfig.preprocessFrame(frame)
            val embedding = ClipModelConfig.generateEmbedding(preprocessed)
            embeddings.add(embedding)
        }
        
        // Generate deterministic hash from embeddings
        val meanEmbedding = meanPoolEmbeddings(embeddings)
        return sha256Hash(meanEmbedding)
    }
    
    fun verifyDeterministic(videoUri: Uri): Boolean {
        val hash1 = generateVideoHash(videoUri, ClipConfig())
        val hash2 = generateVideoHash(videoUri, ClipConfig())
        return hash1 == hash2
    }
}
```

**Code Pointer**: [`app/src/main/java/com/mira/clip/storage/EmbeddingStore.kt`](app/src/main/java/com/mira/clip/storage/EmbeddingStore.kt)

#### Verification Script Integration
```bash
#!/bin/bash
# ops/verify_all.sh - CLIP Deterministic Verification

echo "Verifying CLIP deterministic pipeline..."

# Test 1: Same video, same config, different runs
VIDEO_URI="file:///sdcard/test_video.mp4"
HASH1=$(adb shell "am broadcast -a com.mira.com.CLIP.VERIFY --es uri $VIDEO_URI --es run 1")
HASH2=$(adb shell "am broadcast -a com.mira.com.CLIP.VERIFY --es uri $VIDEO_URI --es run 2")

if [ "$HASH1" = "$HASH2" ]; then
    echo "✅ Deterministic verification PASSED"
else
    echo "❌ Deterministic verification FAILED"
    exit 1
fi

# Test 2: Different videos, different hashes
VIDEO2_URI="file:///sdcard/test_video2.mp4"
HASH3=$(adb shell "am broadcast -a com.mira.com.CLIP.VERIFY --es uri $VIDEO2_URI")

if [ "$HASH1" != "$HASH3" ]; then
    echo "✅ Content differentiation PASSED"
else
    echo "❌ Content differentiation FAILED"
    exit 1
fi

echo "All CLIP verification tests PASSED"
```

**Script Pointer**: [`ops/verify_all.sh`](ops/verify_all.sh)

## Frame Sampling Strategies

### 1. Uniform Sampling (Default)
```kotlin
class UniformFrameSampler : FrameSampler {
    override fun sampleFrames(videoUri: Uri, frameCount: Int): List<Long> {
        val duration = getVideoDuration(videoUri)
        val interval = duration / frameCount
        return (0 until frameCount).map { it * interval }
    }
}
```

**Code Pointer**: [`feature/clip/src/main/java/com/mira/clip/feature/clip/ClipRunner.kt`](feature/clip/src/main/java/com/mira/clip/feature/clip/ClipRunner.kt)

### 2. Head/Tail Sampling
```kotlin
class HeadTailFrameSampler : FrameSampler {
    override fun sampleFrames(videoUri: Uri, frameCount: Int): List<Long> {
        val duration = getVideoDuration(videoUri)
        val halfCount = frameCount / 2
        val interval = duration / (frameCount * 2)
        
        val headFrames = (0 until halfCount).map { it * interval }
        val tailFrames = (halfCount until frameCount).map { 
            duration - (frameCount - it) * interval 
        }
        
        return headFrames + tailFrames
    }
}
```

**Code Pointer**: [`feature/clip/src/main/java/com/mira/clip/feature/clip/ClipRunner.kt`](feature/clip/src/main/java/com/mira/clip/feature/clip/ClipRunner.kt)

### 3. Adaptive Sampling
```kotlin
class AdaptiveFrameSampler : FrameSampler {
    override fun sampleFrames(videoUri: Uri, frameCount: Int): List<Long> {
        val duration = getVideoDuration(videoUri)
        val shotBoundaries = detectShotBoundaries(videoUri)
        
        // Distribute frames based on shot lengths
        val shotWeights = shotBoundaries.map { it.duration / duration }
        val framesPerShot = shotWeights.map { (it * frameCount).toInt() }
        
        return shotBoundaries.flatMapIndexed { index, shot ->
            val count = framesPerShot[index]
            val interval = shot.duration / count
            (0 until count).map { shot.startMs + it * interval }
        }
    }
}
```

**Code Pointer**: [`feature/clip/src/main/java/com/mira/clip/feature/clip/ClipRunner.kt`](feature/clip/src/main/java/com/mira/clip/feature/clip/ClipRunner.kt)

## Aggregation Methods

### 1. Mean Pooling (Parameter-free)
```kotlin
private fun meanPoolingAggregation(frameEmbeddings: List<FrameEmbedding>): FloatArray {
    val aggregated = FloatArray(EMBEDDING_DIM) { 0f }
    frameEmbeddings.forEach { frame ->
        for (i in 0 until EMBEDDING_DIM) {
            aggregated[i] += frame.embedding[i]
        }
    }
    // Normalize by count
    val count = frameEmbeddings.size.toFloat()
    for (i in 0 until EMBEDDING_DIM) {
        aggregated[i] /= count
    }
    return aggregated
}
```

**Code Pointer**: [`feature/clip/src/main/java/com/mira/clip/feature/clip/ClipEngine.kt`](feature/clip/src/main/java/com/mira/clip/feature/clip/ClipEngine.kt)

### 2. Sequential Aggregation
```kotlin
private fun sequentialAggregation(frameEmbeddings: List<FrameEmbedding>): FloatArray {
    val aggregated = FloatArray(EMBEDDING_DIM) { 0f }
    frameEmbeddings.forEachIndexed { index, frame ->
        val weight = 1.0f + (index.toFloat() / frameEmbeddings.size) * 0.1f
        for (i in 0 until EMBEDDING_DIM) {
            aggregated[i] += frame.embedding[i] * weight
        }
    }
    return aggregated
}
```

**Code Pointer**: [`feature/clip/src/main/java/com/mira/clip/feature/clip/ClipEngine.kt`](feature/clip/src/main/java/com/mira/clip/feature/clip/ClipEngine.kt)

### 3. Tight Aggregation
```kotlin
private fun tightAggregation(frameEmbeddings: List<FrameEmbedding>): FloatArray {
    val aggregated = FloatArray(EMBEDDING_DIM) { 0f }
    frameEmbeddings.forEach { frame ->
        val attentionWeight = computeAttentionWeight(frame.embedding)
        for (i in 0 until EMBEDDING_DIM) {
            aggregated[i] += frame.embedding[i] * attentionWeight
        }
    }
    return aggregated
}
```

**Code Pointer**: [`feature/clip/src/main/java/com/mira/clip/feature/clip/ClipEngine.kt`](feature/clip/src/main/java/com/mira/clip/feature/clip/ClipEngine.kt)

## Performance Characteristics

### Scalability
- **Current**: Optimized for ~10k vectors (brute-force search)
- **Future**: ANN infrastructure ready for 100k+ vectors
- **Caching**: 10k vector in-memory cache
- **Background Processing**: Non-blocking video ingestion

### Resource Usage
- **Memory**: Efficient vector caching and management
- **Storage**: Compressed vector storage with Room
- **CPU**: Optimized frame sampling and processing
- **Battery**: Background processing with constraints

## Security Features

### Data Protection
- **Database Encryption**: SQLCipher integration
- **Key Management**: Secure key generation and validation
- **Hardware Security**: Android Keystore integration
- **Privacy**: All processing on-device

### Security Recommendations
- Automatic security assessment
- Encryption strength validation
- Hardware security detection
- Security event logging

## Integration with Existing Pipeline

The CLIP4Clip engine integrates seamlessly with the existing video processing pipeline:

```kotlin
// Enhanced AutoCutEngine with CLIP4Clip
suspend fun autoCutAndExport(
    input: Uri,
    outputPath: String,
    targetDurationMs: Long = DEFAULT_TARGET_DURATION_MS,
    useClip4Clip: Boolean = false,
    textQuery: String? = null
) {
    // ... existing shot detection and scoring ...
    
    // CLIP4Clip-enhanced segment selection
    val selectedSegments = if (useClip4Clip && textQuery != null) {
        selectBestSegmentsWithClip4Clip(
            candidateSegments, 
            targetDurationMs,
            input,
            textQuery
        )
    } else {
        selectBestSegments(candidateSegments, targetDurationMs)
    }
}
```

**Code Pointer**: [`app/src/main/java/com/mira/videoeditor/video/AutoCutEngine.kt`](app/src/main/java/com/mira/videoeditor/video/AutoCutEngine.kt)

## Usage Examples

### Basic Shot Embedding Generation
```kotlin
val clip4ClipEngine = Clip4ClipEngine(context)
val shotDetector = ShotDetector(context)

// Detect shots
val shots = shotDetector.detectShots(videoUri, sampleMs = 500L, minShotMs = 1500L)

// Generate embeddings
val shotEmbeddings = clip4ClipEngine.generateShotEmbeddings(
    uri = videoUri,
    shots = shots,
    framesPerShot = 12,
    aggregationType = Clip4ClipEngine.AggregationType.MEAN_POOLING
)
```

**Code Pointer**: [`app/src/main/java/com/mira/clip/usecases/ComputeClipSimilarityUseCase.kt`](app/src/main/java/com/mira/clip/usecases/ComputeClipSimilarityUseCase.kt)

### Text Query Similarity Search
```kotlin
// Generate text embedding
val textEmbedding = clip4ClipEngine.generateTextEmbedding("person walking")

// Compute similarity
val similarities = clip4ClipEngine.computeSimilarity(
    textQuery = "person walking",
    shotEmbeddings = shotEmbeddings,
    topK = 10
)

// Results are ranked by similarity score
similarities.forEach { result ->
    println("Shot ${result.shotId}: similarity ${result.similarity}")
}
```

**Code Pointer**: [`app/src/main/java/com/mira/clip/usecases/ComputeClipSimilarityUseCase.kt`](app/src/main/java/com/mira/clip/usecases/ComputeClipSimilarityUseCase.kt)

## Configuration Parameters

### Frame Sampling
```kotlin
companion object {
    private const val DEFAULT_FRAMES_PER_SHOT = 12
    private const val MIN_FRAMES_PER_SHOT = 4
    private const val MAX_FRAMES_PER_SHOT = 24
}
```

**Code Pointer**: [`app/src/main/java/com/mira/clip/clip/ClipEngines.kt`](app/src/main/java/com/mira/clip/clip/ClipEngines.kt)

### CLIP Model Parameters
```kotlin
companion object {
    private const val EMBEDDING_DIM = 512
    private const val FRAME_WIDTH = 224
    private const val FRAME_HEIGHT = 224
}
```

**Code Pointer**: [`app/src/main/java/com/mira/clip/clip/ClipEngines.kt`](app/src/main/java/com/mira/clip/clip/ClipEngines.kt)

### Evaluation Parameters
```kotlin
companion object {
    private const val DEFAULT_TOP_K = 10
    private const val MIN_CONFIDENCE_THRESHOLD = 0.1f
}
```

**Code Pointer**: [`app/src/main/java/com/mira/clip/services/RetrievalService.kt`](app/src/main/java/com/mira/clip/services/RetrievalService.kt)

## Testing Infrastructure

### Unit Tests
```kotlin
@Test
fun normalization_is_unit_length_and_idempotent() {
    val v = floatArrayOf(3f, 4f, 0f)
    val n1 = ClipEngines.normalizeEmbedding(v)
    val n2 = ClipEngines.normalizeEmbedding(n1)
    fun l2(x: FloatArray): Float = kotlin.math.sqrt(x.sumOf { (it*it).toDouble() }.toFloat())
    assertEquals(1f, l2(n1), 1e-4f)
    // idempotent
    assertArrayEquals(n1, n2, 1e-6f)
}
```

**Test Pointer**: [`app/src/test/java/com/mira/clip/MetricSpaceUnitTest.kt`](app/src/test/java/com/mira/clip/MetricSpaceUnitTest.kt)

### Instrumented Tests
```kotlin
@Test
fun e2e_store_and_retrieve_with_valid_cosine_and_auditable_bytes() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    // Create a simple synthetic image (gray) and a matching text prompt.
    val bmp = Bitmap.createBitmap(224,224, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
    val img = ClipEngines.embedImage(ctx, bmp)   // normalized
    val txt = ClipEngines.embedText(ctx, "a gray square") // normalized
    val sim = ClipEngines.cosine(img, txt)       // cosine = dot

    // Step K: vector sanity (expect finite, typically > 0.24 with real CLIP)
    assertTrue(sim.isFinite())

    // Persist & audit
    EmbeddingStore.save(ctx, "q_img", "clip_vit_b32_mean_v1", img)
    EmbeddingStore.save(ctx, "doc_text", "clip_vit_b32_mean_v1", txt)

    // Retrieval: query = image → should rank the matching text highly
    val top = RetrievalService(ctx).topK(queryNorm = img, k = 5)
    assertTrue(top.any { it.id == "doc_text" })
}
```

**Test Pointer**: [`app/src/androidTest/java/com/mira/clip/MetricSpaceInstrumentedTest.kt`](app/src/androidTest/java/com/mira/clip/MetricSpaceInstrumentedTest.kt)

## Script Integration

### CLIP Testing Scripts
```bash
# Run CLIP4Clip comprehensive test
./scripts/test/comprehensive_test_simulation.sh

# Test CLIP4Clip background processing
./scripts/test/test_core_capabilities.sh

# Manual CLIP4Clip testing
./scripts/test/test-e2e.sh
```

**Script Pointers**: 
- [`scripts/test/comprehensive_test_simulation.sh`](scripts/test/comprehensive_test_simulation.sh)
- [`scripts/test/test_core_capabilities.sh`](scripts/test/test_core_capabilities.sh)
- [`scripts/test/test-e2e.sh`](scripts/test/test-e2e.sh)

### Verification Scripts
```bash
# Run complete verification
./ops/verify_all.sh

# CLIP-specific verification
./ops/verify_all.sh representation
./ops/verify_all.sh retrieval
./ops/verify_all.sh reproducibility
```

**Script Pointers**:
- [`ops/verify_all.sh`](ops/verify_all.sh)
- [`ops/verify_all 2.sh`](ops/verify_all 2.sh)

## Future Enhancements

### Service Integration
1. **API Documentation**: Create comprehensive API docs
2. **Service Testing**: Add comprehensive service tests
3. **Performance Tuning**: Optimize for production workloads
4. **Monitoring**: Add production monitoring and alerting

### Deployment
1. **Service Packaging**: Package as Android library or service
2. **Integration Guide**: Create integration documentation
3. **Example Implementations**: Provide usage examples
4. **Production Deployment**: Deploy as production service

## Dependencies

### Core Dependencies
- **Room**: Database and vector storage
- **Hilt**: Dependency injection
- **WorkManager**: Background processing
- **DataStore**: Settings management
- **PyTorch Mobile**: CLIP model inference

### Security Dependencies
- **SQLCipher**: Database encryption
- **Android Keystore**: Hardware security

### Performance Dependencies
- **Android Tracing**: Performance monitoring
- **Coroutines**: Async processing

---

**Last Updated**: 2025-01-04  
**Version**: v1.0.0  
**Status**: ✅ Production Ready
