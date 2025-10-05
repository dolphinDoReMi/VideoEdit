# CLIP Full Scale Implementation Details

## Production-Ready Android/Kotlin Implementation

### Problem Disaggregation

- **Inputs**: Video files (MP4, AVI, MOV), image sequences
- **Outputs**: CLIP embeddings (512-dim vectors) + JSON sidecar with frame metadata
- **Runtime Surfaces**: Broadcast → WorkManager job → CLIP pipeline → Storage writer
- **Isolation**: 
  - Preserve existing applicationId
  - Debug variant uses applicationIdSuffix ".debug"
  - All actions/authorities use ${applicationId} placeholders

### Analysis with Trade-offs

- **CLIP vs Custom Models**: CLIP provides zero-shot capabilities vs task-specific fine-tuning
- **Frame Sampling**: Uniform vs keyframe detection vs dense sampling
- **Preprocessing**: Center-crop vs resize vs augmentation
- **Batch Processing**: Memory vs speed trade-offs
- **Quantization**: FP32 vs FP16 vs INT8 for mobile deployment

### Design

**Pipeline Flow:**
```
Broadcast (ACTION_CLIP_EMBED) → CLIPReceiver → CLIPApi → EmbedWorker 
→ CLIPModel → FrameSampler → ImagePreprocessor → EmbeddingGenerator → Sidecar
```

**Key Control Knots:**
- `FRAME_SAMPLING_RATE` (default 1.0 fps)
- `PREPROCESS_SIZE` (224x224)
- `BATCH_SIZE` (32)
- `MODEL_TYPE` ("clip-vit-base-patch32")
- `EMBEDDING_DIM` (512)
- `NORMALIZATION` ("l2")
- `QUANTIZATION` ("fp16")

### Implementation Architecture

#### 1. CLIP Model Integration
```kotlin
class CLIPModel {
    fun generateEmbeddings(
        frames: List<Bitmap>,
        batchSize: Int = 32
    ): List<FloatArray> {
        val embeddings = mutableListOf<FloatArray>()
        
        frames.chunked(batchSize).forEach { batch ->
            val batchEmbeddings = processBatch(batch)
            embeddings.addAll(batchEmbeddings)
        }
        
        return embeddings
    }
}
```

#### 2. Frame Sampling
```kotlin
class FrameSampler {
    fun sampleFrames(
        videoPath: String,
        samplingRate: Float = 1.0f
    ): List<Bitmap> {
        val frames = mutableListOf<Bitmap>()
        val duration = getVideoDuration(videoPath)
        val frameInterval = (1000 / samplingRate).toInt()
        
        for (timestamp in 0 until duration step frameInterval) {
            val frame = extractFrameAtTimestamp(videoPath, timestamp)
            frames.add(frame)
        }
        
        return frames
    }
}
```

#### 3. Embedding Worker
```kotlin
class EmbedWorker : Worker {
    override suspend fun doWork(): Result {
        return try {
            val videoPath = inputData.getString("video_path") ?: return Result.failure()
            val samplingRate = inputData.getFloat("sampling_rate", 1.0f)
            
            // Sample frames
            val frames = FrameSampler().sampleFrames(videoPath, samplingRate)
            
            // Generate embeddings
            val embeddings = CLIPModel().generateEmbeddings(frames)
            
            // Save results
            saveEmbeddings(videoPath, embeddings)
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Embedding generation failed", e)
            Result.failure()
        }
    }
}
```

### Scale-out Plan

#### Single (Default Configuration)
```json
{
  "preset": "SINGLE",
  "sampling": { "rate": 1.0, "method": "uniform" },
  "preprocessing": { "size": [224, 224], "method": "center_crop" },
  "model": { "type": "clip-vit-base-patch32", "quantization": "fp16" },
  "batch": { "size": 32, "memory_limit": "2GB" }
}
```

#### Ablations (Performance Variants)

**A. High-Density Sampling**
```json
{
  "preset": "HIGH_DENSITY",
  "sampling": { "rate": 2.0, "method": "uniform" },
  "batch": { "size": 16 }
}
```

**B. Keyframe Detection**
```json
{
  "preset": "KEYFRAME",
  "sampling": { "method": "keyframe", "threshold": 0.3 },
  "preprocessing": { "method": "resize" }
}
```

**C. Mobile Optimized**
```json
{
  "preset": "MOBILE",
  "model": { "quantization": "int8" },
  "batch": { "size": 8, "memory_limit": "512MB" }
}
```

**D. High Accuracy**
```json
{
  "preset": "HIGH_ACCURACY",
  "model": { "type": "clip-vit-large-patch14", "quantization": "fp32" },
  "preprocessing": { "size": [336, 336] }
}
```

### Code Pointers

- **CLIP Model**: `core/ml/clip/CLIPModel.kt`
- **Frame Sampling**: `core/media/FrameSampler.kt`
- **Image Preprocessing**: `core/ml/preprocessing/ImagePreprocessor.kt`
- **Embedding Generation**: `core/ml/embedding/EmbeddingGenerator.kt`
- **Embed Worker**: `core/ml/workers/EmbedWorker.kt`
- **CLIP API**: `core/ml/api/CLIPApi.kt`