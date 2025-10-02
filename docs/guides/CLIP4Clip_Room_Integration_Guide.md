# CLIP4Clip Room Database Integration Guide

## Overview

This guide provides comprehensive documentation for the production-ready CLIP4Clip video-text retrieval system integrated with Room database, PyTorch Mobile, and WorkManager for Android.

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Activities)                    │
├─────────────────────────────────────────────────────────────┤
│                  Use Cases (Business Logic)                │
├─────────────────────────────────────────────────────────────┤
│              Repository Layer (Data Access)                 │
├─────────────────────────────────────────────────────────────┤
│              Database Layer (Room + Entities)               │
├─────────────────────────────────────────────────────────────┤
│              ML Layer (PyTorch Mobile + CLIP)               │
├─────────────────────────────────────────────────────────────┤
│            Background Processing (WorkManager)               │
└─────────────────────────────────────────────────────────────┘
```

### Key Features

- ✅ **Room Database**: Persistent storage for videos, shots, embeddings, and text queries
- ✅ **PyTorch Mobile**: On-device CLIP model inference for image and text encoding
- ✅ **WorkManager**: Background video processing with progress tracking
- ✅ **Vector Serialization**: Little-endian float32 storage optimized for PyTorch Mobile
- ✅ **Multiple Variants**: Support for different embedding model versions
- ✅ **Comprehensive Testing**: Unit tests for all components
- ✅ **Production Ready**: ProGuard rules, error handling, and performance optimization

## Quick Start

### 1. Dependencies

The following dependencies are already configured in `app/build.gradle.kts`:

```kotlin
// Room database
implementation("androidx.room:room-runtime:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")

// PyTorch Mobile
implementation("org.pytorch:pytorch_android_lite:2.3.0")
implementation("org.pytorch:pytorch_android_torchvision:2.3.0")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.1")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.1")
```

### 2. Model Setup

#### Export CLIP Models

Run the model export script:

```bash
cd scripts
chmod +x export_clip_models.sh
./export_clip_models.sh
```

This creates:
- `mobile_models/clip_image_encoder.ptl` - PyTorch Mobile image encoder
- `mobile_models/clip_text_encoder.ptl` - PyTorch Mobile text encoder
- `mobile_models/model_info.json` - Model metadata

#### Add Model Files to Assets

Copy the generated files to your Android project:

```bash
cp mobile_models/clip_image_encoder.ptl app/src/main/assets/
cp mobile_models/clip_text_encoder.ptl app/src/main/assets/
```

#### Download BPE Tokenizer Files

Download from CLIP repository and place in `app/src/main/assets/`:
- `bpe_vocab.json` - BPE vocabulary
- `bpe_merges.txt` - BPE merge rules

### 3. Basic Usage

#### Initialize the System

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var videoIngestionUseCase: VideoIngestionUseCase
    private lateinit var videoSearchUseCase: VideoSearchUseCase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize use cases
        videoIngestionUseCase = VideoIngestionUseCase(this)
        videoSearchUseCase = VideoSearchUseCase(this)
    }
}
```

#### Process a Video

```kotlin
// Process video with background processing
lifecycleScope.launch {
    val videoId = videoIngestionUseCase.processVideo(
        videoUri = videoUri,
        variant = "clip_vit_b32_mean_v1",
        framesPerVideo = 32,
        framesPerShot = 12,
        useBackgroundProcessing = true
    )
    
    println("Video processing started: $videoId")
}
```

#### Search Videos by Text

```kotlin
// Search videos by text query
lifecycleScope.launch {
    val results = videoSearchUseCase.searchVideos(
        query = "person walking",
        variant = "clip_vit_b32_mean_v1",
        topK = 10,
        searchLevel = "video"
    )
    
    results.forEach { result ->
        println("Video: ${result.id}, Similarity: ${result.similarity}")
    }
}
```

## Detailed API Reference

### Use Cases

#### VideoIngestionUseCase

```kotlin
class VideoIngestionUseCase(private val context: Context) {
    
    // Process a single video
    suspend fun processVideo(
        videoUri: Uri,
        variant: String = "clip_vit_b32_mean_v1",
        framesPerVideo: Int = 32,
        framesPerShot: Int = 12,
        aggregationType: AggregationType = AggregationType.MEAN_POOLING,
        useBackgroundProcessing: Boolean = true
    ): String
    
    // Process multiple videos in batch
    suspend fun processVideosBatch(
        videoUris: List<Uri>,
        variant: String = "clip_vit_b32_mean_v1",
        framesPerVideo: Int = 32,
        framesPerShot: Int = 12
    ): Int
    
    // Check processing status
    suspend fun getProcessingStatus(videoUri: Uri): ProcessingStatus
    
    // Delete video and all associated data
    suspend fun deleteVideo(videoId: String)
}
```

#### VideoSearchUseCase

```kotlin
class VideoSearchUseCase(private val context: Context) {
    
    // Search videos by text query
    suspend fun searchVideos(
        query: String,
        variant: String = "clip_vit_b32_mean_v1",
        topK: Int = 10,
        searchLevel: String = "video"
    ): List<SearchResult>
    
    // Get all videos with embeddings
    fun getAllVideosWithEmbeddings(variant: String = "clip_vit_b32_mean_v1"): Flow<List<VideoWithEmbedding>>
    
    // Get shots for a video with embeddings
    suspend fun getShotsWithEmbeddings(
        videoId: String,
        variant: String = "clip_vit_b32_mean_v1"
    ): List<ShotWithEmbedding>
    
    // Get embedding statistics
    suspend fun getEmbeddingStats(variant: String = "clip_vit_b32_mean_v1"): Map<String, Int>
}
```

### Database Entities

#### VideoEntity

```kotlin
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val uri: String,                 // content:// or file://
    val durationMs: Long,
    val fps: Float,
    val width: Int,
    val height: Int,
    val metadataJson: String = "{}", // exif, labels, etc.
    val createdAt: Long = System.currentTimeMillis()
)
```

#### EmbeddingEntity

```kotlin
@Entity(tableName = "embeddings")
data class EmbeddingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerType: String,          // "video" | "shot" | "text"
    val ownerId: String,             // videoId | shotId | textId
    val dim: Int,                    // e.g., 512 or 768
    val variant: String,             // "clip_vit_b32_mean_v1"
    val vec: ByteArray,              // little-endian float32 blob
    val createdAt: Long = System.currentTimeMillis()
)
```

### PyTorch Mobile Integration

#### PyTorchClipEngine

```kotlin
class PyTorchClipEngine(private val context: Context) {
    
    // Initialize models
    suspend fun initialize(): Boolean
    
    // Encode single image
    fun encodeImage(bitmap: Bitmap): FloatArray
    
    // Encode multiple images in batch
    fun encodeImages(bitmaps: List<Bitmap>): List<FloatArray>
    
    // Encode text
    fun encodeText(text: String): FloatArray
    
    // Compute cosine similarity
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float
    
    // Check if models are ready
    fun isReady(): Boolean
}
```

## Background Processing

### WorkManager Integration

The system uses WorkManager for background video processing:

```kotlin
// Enqueue video processing
VideoWorkManager.enqueueVideoIngest(
    context = context,
    videoUri = videoUri,
    variant = "clip_vit_b32_mean_v1",
    framesPerVideo = 32,
    framesPerShot = 12
)

// Enqueue batch processing
VideoWorkManager.enqueueBatchVideoIngest(
    context = context,
    videoUris = videoUris,
    variant = "clip_vit_b32_mean_v1"
)
```

### Progress Tracking

Monitor processing progress:

```kotlin
// In your activity or fragment
WorkManager.getInstance(this)
    .getWorkInfoByIdLiveData(workRequest.id)
    .observe(this) { workInfo ->
        val progress = workInfo.progress
        val stage = progress.getString("stage")
        val percent = progress.getInt("percent", 0)
        
        updateProgressBar(percent)
        updateStatusText("$stage: $percent%")
    }
```

## Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test classes
./gradlew test --tests "Clip4ClipDatabaseTest"
./gradlew test --tests "PyTorchClipEngineTest"
./gradlew test --tests "Clip4ClipUseCaseTest"
```

### Test Coverage

The test suite covers:
- ✅ Database CRUD operations
- ✅ Entity relationships and queries
- ✅ Vector serialization
- ✅ PyTorch Mobile integration
- ✅ Use case workflows
- ✅ Error handling
- ✅ Performance metrics

## Performance Optimization

### Memory Management

- **Batch Processing**: Process frames in batches of 8-16 for optimal memory usage
- **Vector Normalization**: Embeddings are normalized at write-time for efficient similarity computation
- **Resource Cleanup**: Proper cleanup of MediaMetadataRetriever and other resources

### Database Optimization

- **Indexing**: Proper indices on frequently queried columns
- **Batch Operations**: Use batch inserts for multiple entities
- **Query Optimization**: Efficient queries with proper joins

### Model Optimization

- **Quantization**: Text encoder uses dynamic int8 quantization
- **Batch Inference**: Process multiple images simultaneously
- **Model Caching**: Models are loaded once and reused

## Troubleshooting

### Common Issues

#### 1. Model Loading Fails

**Problem**: `PyTorchClipEngine.initialize()` returns false

**Solutions**:
- Verify model files are in `app/src/main/assets/`
- Check file names match exactly: `clip_image_encoder.ptl`, `clip_text_encoder.ptl`
- Ensure models are exported correctly using the provided script

#### 2. Database Migration Errors

**Problem**: Room database migration fails

**Solutions**:
- Check migration version numbers
- Verify entity changes are compatible
- Test migrations with in-memory database

#### 3. Background Processing Not Working

**Problem**: WorkManager jobs don't execute

**Solutions**:
- Check WorkManager constraints (battery, storage)
- Verify WorkManager is properly initialized
- Check device battery optimization settings

#### 4. Memory Issues

**Problem**: OutOfMemoryError during processing

**Solutions**:
- Reduce `framesPerVideo` and `framesPerShot` parameters
- Process videos in smaller batches
- Monitor memory usage with Android Studio Profiler

### Debug Commands

```bash
# Monitor database
adb shell run-as com.mira.videoeditor.debug ls /data/data/com.mira.videoeditor.debug/databases/

# Check WorkManager jobs
adb shell dumpsys jobscheduler

# Monitor memory usage
adb shell dumpsys meminfo com.mira.videoeditor.debug

# View logs
adb logcat | grep -E "(Clip4Clip|PyTorch|Room)"
```

## Production Deployment

### Pre-deployment Checklist

- [ ] Model files are properly exported and included
- [ ] BPE tokenizer files are included
- [ ] ProGuard rules are configured
- [ ] Tests are passing
- [ ] Performance benchmarks meet requirements
- [ ] Error handling is comprehensive
- [ ] Logging is appropriate for production

### Release Configuration

```kotlin
// In build.gradle.kts
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### Monitoring

- Monitor embedding generation performance
- Track database growth and query performance
- Monitor WorkManager job success rates
- Track memory usage patterns

## Future Enhancements

### Planned Features

1. **ANN Indexing**: Add approximate nearest neighbor indexing for large collections
2. **Multi-modal Integration**: Combine with audio (Whisper) embeddings
3. **Advanced Aggregation**: Implement transformer-based temporal modeling
4. **Real-time Processing**: Support for live video analysis
5. **Cloud Sync**: Optional cloud backup and sync

### Research Directions

1. **Model Compression**: Further quantization and pruning
2. **Federated Learning**: On-device model updates
3. **Cross-modal Attention**: Advanced video-text interaction
4. **Temporal Modeling**: Better shot boundary understanding

## Support

For issues and questions:
- Check the test suite for usage examples
- Review the demo activity (`Clip4ClipRoomDemoActivity`)
- Examine the comprehensive logging output
- Refer to the PyTorch Mobile documentation

## License

This implementation follows the same license as the original CLIP model (MIT License).
