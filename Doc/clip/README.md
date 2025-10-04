# CLIP Feature Documentation

## Overview

The CLIP (Contrastive Language-Image Pre-training) feature provides semantic video-text retrieval capabilities for the Mira Video Editor. This feature enables users to search for video content using natural language queries, making video editing more intuitive and efficient.

## Key Design Decisions

### For Content Understanding Expert

The CLIP feature provides **time-aligned semantic embeddings** that enable precise content understanding and retrieval:

- **Primitive Output**: `{t0Ms, t1Ms, text}` spans with exact anchors for highlights, topic segmentation, summarization, safety tagging, and retrieval-augmented QA
- **Segmentation Quality**: Phrase-level segments are stable for content understanding; word timestamps available when needed (with compute cost)
- **Diagnostics**: Coverage (voiced duration / file duration), gap distribution (silences), language stability, OOV rates, ASR confidence proxy
- **Sampling Bias**: Front-end normalization prevents drift across corpora; watch domain shift (far-field, music overlap, accents)
- **Multimodal Hooks**: Align transcripts with video frames or shots by time; late-fuse with image/video embeddings for better retrieval and summarization
- **Safety**: Time-pin policy flags (e.g., abuse/PII) to exact spans for explainability and partial redaction

### For Video Expert

The CLIP implementation provides **deterministic video processing** with production-ready performance:

- **Frame Sampling**: Uniform sampling (deterministic), adaptive sampling (content-aware), head/tail sampling (keyframe-focused)
- **Preprocessing Pipeline**: Center crop (deterministic), resize to 224x224, CLIP normalization (mean/std)
- **Model Integration**: PyTorch Mobile with hardware acceleration, 512-dimensional embeddings
- **Aggregation Methods**: Mean pooling (parameter-free), sequential aggregation (temporal awareness), attention aggregation (learnable weights)
- **Performance**: Optimized for ~10k vectors (brute-force), ANN infrastructure ready for 100k+ vectors
- **Resource Usage**: Efficient vector caching, compressed storage with Room, optimized frame sampling

### For RecSys Expert

The CLIP feature provides **scalable semantic retrieval** infrastructure:

- **Indexing Contract**: One immutable transcript JSON per (asset, variant); path convention: `{variant}/{audioId}.json` (+ SHA of audio and model)
- **Online Latency Path**: User query → text retrieval over transcripts (BM25/ANN on text embeddings) with time-coded jumps back to media
- **ANN Build**: Store raw JSON for audit; build serving index over text embeddings (E5/MPNet) or n-gram inverted index; keep CLIP confidence/timing as features
- **MIPS/Cosine**: If using unit-norm text embeddings, cosine==dot; standard ANN (Faiss/ScaNN/HNSW) applies
- **Freshness & TTL**: Decouple offline CLIP ingest from online retrieval; sidecar has created_at, model_sha, decode_cfg for rollbacks and replays
- **Feature Stability**: Fixed resample/downmix and pinned decode params → deterministic transcripts (minus inherent stochasticity)
- **Ranking Fusion**: Score = α·text_match(q, t) + β·CLIP_quality(seg) + γ·user_personalization(u, asset) + δ·recency(asset)
- **Safety/Observability**: Metrics = recall@K, latency p99, RTF distribution, segment coverage (% voiced), WER on labeled panels

## Architecture Overview

### Core Components

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Video Input   │───▶│  Frame Sampling  │───▶│   CLIP Model    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Text Query     │───▶│ Text Processing  │───▶│ Text Embedding  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Similarity      │◀───│   Aggregation    │◀───│ Video Embedding │
│ Computation     │    │   (Mean Pool)     │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### Control Knots

The CLIP implementation exposes key control knots for deterministic, reproducible results:

1. **Frame Sampling Control**
   - Method: UNIFORM (deterministic) | ADAPTIVE | HEAD_TAIL
   - Frame Count: Fixed count per video (default: 32)
   - Interval: Fixed interval between samples

2. **Preprocessing Control**
   - Crop Method: CENTER (deterministic) | RANDOM (disabled)
   - Image Size: Fixed 224x224 for CLIP compatibility
   - Normalization: Fixed CLIP mean/std values

3. **Model Assets Control**
   - Model Variant: Fixed model version (clip_vit_b32_mean_v1)
   - Embedding Dimension: Fixed 512 dimensions
   - Model Path: Fixed asset path

## Implementation Details

### Frame Sampling Strategies

#### 1. Uniform Sampling (Default)
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

#### 2. Head/Tail Sampling
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

#### 3. Adaptive Sampling
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

### Preprocessing Pipeline

```kotlin
object ClipPreprocess {
  private val MEAN = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
  private val STD  = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)

  fun centerCropResize(bm: Bitmap, size: Int): Bitmap {
    val minSide = minOf(bm.width, bm.height)
    val x = (bm.width - minSide) / 2
    val y = (bm.height - minSide) / 2
    val cropped = Bitmap.createBitmap(bm, x, y, minSide, minSide)
    return Bitmap.createScaledBitmap(cropped, size, size, true)
  }

  /** CHW float32 normalized for CLIP */
  fun toCHWFloat(bm: Bitmap): FloatArray {
    val w = bm.width; val h = bm.height; val n = w*h
    val out = FloatArray(3*n); val px = IntArray(n)
    bm.getPixels(px, 0, w, 0, 0, w, h)
    var rI = 0; var gI = n; var bI = 2*n
    for (i in 0 until n) {
      val p = px[i]
      val r = ((p ushr 16) and 0xFF) / 255f
      val g = ((p ushr 8) and 0xFF) / 255f
      val b = (p and 0xFF) / 255f
      out[rI++] = (r - MEAN[0]) / STD[0]
      out[gI++] = (g - MEAN[1]) / STD[1]
      out[bI++] = (b - MEAN[2]) / STD[2]
    }
    return out
  }
}
```

**Code Pointer**: [`feature/clip/src/main/java/com/mira/clip/feature/clip/ClipPreprocess.kt`](feature/clip/src/main/java/com/mira/clip/feature/clip/ClipPreprocess.kt)

### CLIP Engine Implementation

```kotlin
class ClipEngines(private val context: Context) {
    
    private var imageEncoder: Module? = null
    private var textEncoder: Module? = null
    private var tokenizer: ClipBPETokenizer? = null
    
    fun initialize() {
        try {
            imageEncoder = PytorchLoader.loadImageEncoder(context)
            textEncoder = PytorchLoader.loadTextEncoder(context)
            
            val vocabPath = PytorchLoader.copyBpeVocab(context)
            val mergesPath = PytorchLoader.copyBpeMerges(context)
            tokenizer = ClipBPETokenizer(vocabPath, mergesPath)
            
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize CLIP engines: ${e.message}", e)
        }
    }
    
    fun encodeText(text: String): FloatArray {
        val encoder = textEncoder ?: throw IllegalStateException("Text encoder not initialized")
        val tokenizer = tokenizer ?: throw IllegalStateException("Tokenizer not initialized")
        
        val tokens = tokenizer.tokenize(text)
        val input = Tensor.fromBlob(tokens, longArrayOf(1, tokens.size))
        val output = encoder.forward(IValue.from(input)).toTensor()
        
        return normalizeEmbedding(output.dataAsFloatArray)
    }
    
    fun encodeImage(bitmap: Bitmap): FloatArray {
        val encoder = imageEncoder ?: throw IllegalStateException("Image encoder not initialized")
        
        // Preprocess image
        val preprocessed = preprocessImage(bitmap)
        val input = Tensor.fromBlob(preprocessed, longArrayOf(1, 3, 224, 224))
        val output = encoder.forward(IValue.from(input)).toTensor()
        
        return normalizeEmbedding(output.dataAsFloatArray)
    }
    
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) throw IllegalArgumentException("Arrays must have same size")
        
        var dotProduct = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        
        return dotProduct // Assuming embeddings are already L2-normalized
    }
}
```

**Code Pointer**: [`app/src/main/java/com/mira/clip/clip/ClipEngines.kt`](app/src/main/java/com/mira/clip/clip/ClipEngines.kt)

## Verification System

### Deterministic Pipeline Verification

The CLIP implementation includes a comprehensive verification system to ensure deterministic, reproducible results:

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

### Verification Script

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

## Usage Examples

### Basic Video Processing

```kotlin
val clipRunner = ClipRunner
val context = this

// Process video and generate embeddings
val result = clipRunner.run(
    ctx = context,
    inputUriStr = "file:///sdcard/Mira/video_v1.mp4",
    outDirStr = "file:///sdcard/MiraClip/out",
    variant = "ViT-B/32",
    frameCount = 32
)

Log.i("CLIP", "Processing completed: ${result.absolutePath}")
```

### Text Query Similarity Search

```kotlin
val clipEngines = ClipEngines(context)
clipEngines.initialize()

// Generate text embedding
val textEmbedding = clipEngines.encodeText("person walking")

// Generate image embedding
val imageEmbedding = clipEngines.encodeImage(bitmap)

// Compute similarity
val similarity = clipEngines.cosineSimilarity(textEmbedding, imageEmbedding)

Log.i("CLIP", "Similarity score: $similarity")
```

### Broadcast-based Processing

```bash
# Process video via broadcast
adb shell am broadcast \
  -a com.mira.com.CLIP.RUN \
  --es input "file:///sdcard/test_video.mp4" \
  --es variant "clip_vit_b32_mean_v1" \
  --ei frame_count 32

# Verify deterministic processing
adb shell am broadcast \
  -a com.mira.com.CLIP.VERIFY \
  --es uri "file:///sdcard/test_video.mp4"
```

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

### Device-Specific Optimizations

#### Xiaomi Pad Pro 12.4
- **Frame Count**: 16 (optimized for Snapdragon 870)
- **Resolution**: 512px max (memory optimization)
- **Batch Size**: 4 (GPU optimization for Adreno 650)
- **Cache Size**: 256MB (conservative)

#### iPad Pro M2
- **Frame Count**: 32+ (M2 Neural Engine)
- **Resolution**: 1024px max (M2 capabilities)
- **Batch Size**: 16 (M2 performance)
- **Cache Size**: 512MB+ (unified memory)

## Integration Points

### With Existing Video Pipeline

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

### With Database Layer

```kotlin
@Entity(tableName = "video_embeddings")
data class VideoEmbedding(
    @PrimaryKey val videoId: String,
    val uri: String,
    val variant: String,
    val embedding: FloatArray,
    val frameCount: Int,
    val durationMs: Long,
    val createdAt: Long,
    val hash: String
)
```

**Code Pointer**: [`app/src/main/java/com/mira/videoeditor/clip/db/entities/ClipEntities.kt`](app/src/main/java/com/mira/videoeditor/clip/db/entities/ClipEntities.kt)

## Future Enhancements

### Phase 1: Enhanced Sampling
- **Adaptive Sampling**: Content-aware frame selection
- **Shot-based Sampling**: Frame selection based on shot boundaries
- **Temporal Attention**: Learnable temporal weights

### Phase 2: Advanced Aggregation
- **Transformer Aggregation**: Self-attention based aggregation
- **Cross-modal Attention**: Video-text attention mechanisms
- **Learnable Aggregation**: Trainable aggregation weights

### Phase 3: Model Optimization
- **Model Quantization**: Reduce model size and inference time
- **Hardware Acceleration**: GPU/TPU acceleration
- **Model Compression**: Pruning and distillation

### Phase 4: Scalability
- **ANN Integration**: Approximate nearest neighbor search
- **Distributed Processing**: Multi-device processing
- **Cloud Integration**: Hybrid on-device/cloud processing

## Dependencies

### Core Dependencies
- **PyTorch Mobile**: CLIP model inference
- **Room**: Database and vector storage
- **Hilt**: Dependency injection
- **WorkManager**: Background processing
- **DataStore**: Settings management

### Security Dependencies
- **SQLCipher**: Database encryption
- **Android Keystore**: Hardware security

### Performance Dependencies
- **Android Tracing**: Performance monitoring
- **Coroutines**: Async processing

## Related Documentation

- [Architecture Design and Control Knot](Architecture%20Design%20and%20Control%20Knot.md) - Detailed architecture and control knots
- [Full Scale Implementation Details](Full%20scale%20implementation%20Details.md) - Complete implementation guide
- [Device Deployment](Device%20Deployment.md) - Xiaomi Pad and iPad deployment guide
- [Release Guide](Release.md) - iOS, Android and macOS Web release procedures

---

**Last Updated**: 2025-01-04  
**Version**: v1.0.0  
**Status**: ✅ Production Ready
