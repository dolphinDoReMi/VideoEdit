# Whisper Speech Recognition System

## Multi-Lens Expert Communication

Got you. Here's the Whisper multi-lens explanation—compact, technical, and straight to the point.

⸻

### 1/ Plain-text: How it works (step-by-step)

- **Locate an input file (static)**: .wav or .mp4 (AAC inside container)
- **Decode to waveform**:
  - WAV → parse RIFF → PCM16
  - MP4 → MediaExtractor/MediaCodec → AAC→PCM16
- **Normalize front-end**: downmix to mono, resample to 16 kHz, clamp to int16 range
- **Feed PCM to whisper.cpp via JNI** with explicit config (threads, language or auto-LID, translate on/off, greedy/beam, temperature)
- **Whisper computes log-mel**, runs the Transformer decoder, and returns time-stamped segments (10 ms tick base): [t0Ms, t1Ms, text]
- **Optional**: enable word timestamps for word-level CTM
- **Serialize artifacts next to the audio**:
  - JSON sidecar: {segments[], model_variant, model_sha, decode_cfg, audio_sha256, created_at, rtf}
  - (Optional) SRT/VTT generated from segments
- **Persist run metadata in asr.db** (asr_files, asr_jobs, asr_segments) for audit/replay
- **Verify with sanity checks**: sample rate==16 kHz mono, non-empty segments, ordered times (t0≤t1), finite text, RTF < target, optional WER ≤ threshold on a reference clip

**Why this works**: End-to-end ASR (Whisper) maps normalized audio to subword text with learned alignment; strict front-end (mono/16k) removes domain mismatch; JSON+DB make outputs reproducible and time-addressable.

⸻

### 2/ For a Recommendation System Expert

- **Indexing contract**: one immutable transcript JSON per (asset, variant); path convention: {variant}/{audioId}.json (+ SHA of audio and model)
- **Online latency path**: user query → text retrieval over transcripts (BM25/ANN on text embeddings) with time-coded jumps back to media
- **ANN build**: store raw JSON for audit; build a serving index over text embeddings (e.g., E5/MPNet) or n-gram inverted index; keep Whisper confidence/timing as features
- **MIPS/cosine**: if using unit-norm text embeddings, cosine==dot; standard ANN (Faiss/ScaNN/HNSW) applies
- **Freshness & TTL**: decouple offline ASR ingest from online retrieval; sidecar has created_at, model_sha, decode_cfg for rollbacks and replays
- **Feature stability**: fixed resample/downmix and pinned decode params → deterministic transcripts (minus inherent stochasticity like temperature/beam)
- **Ranking fusion**: score = α·text_match(q, t) + β·ASR_quality(seg) + γ·user_personalization(u, asset) + δ·recency(asset); fuse at segment or asset level
- **Safety/observability**: metrics = recall@K, latency p99, RTF distribution, segment coverage (% voiced), WER on labeled panels; verify integrity via audio_sha256 and model_sha
- **AB discipline**: treat model change or decode config change (beam/temp) as new variant keys; support shadow deployments with side-by-side JSONs

⸻

### 3/ For a Deep Learning Expert

- **Front-end**: mono 16 kHz, log-mel computed inside Whisper; ensure amplitude in [−1,1]
- **Tokenizer/units**: BPE (Whisper's vocabulary); timestamps at 10 ms tick resolution if enabled
- **Search**: greedy (fast) vs. beam (beamSize, patience); temperature for exploration; translate toggles X→EN decoding; language can be forced to avoid LID flips
- **Chunking**: whisper.cpp internally handles ~30 s contexts; for long files, do windowed decode with overlap and stitch segments
- **Numerical hygiene**: check isFinite, no NaNs; verify RTF vs threads; keep resampler and downmix deterministic; hold temperature fixed in eval runs
- **Quantization**: GGUF quantization reduces RAM/latency but may raise WER; keep a float (or higher-precision) baseline for audits; report ΔWER/ΔRTF
- **Known limitations**: no diarization/speaker turns by default; far-field/noisy audio benefits from better resampling and optional VAD; cross-talk and code-switching can degrade unless language is forced
- **Upgrades**: band-limited resampler (SoX-style) for noisy domains; VAD pre-trim; long-form strategies (context carryover); optional speaker diarization for CU tasks

⸻

### 4/ For a Content Understanding Expert

- **Primitive you get**: {t0Ms, t1Ms, text} spans—exact anchors for highlights, topic segmentation, summarization, safety tagging, and retrieval-augmented QA
- **Segmentation quality**: phrase-level segments are stable for CU; enable word timestamps only when you need word-level alignment (costs compute)
- **Diagnostics**: coverage (voiced duration / file duration), gap distribution (silences), language stability, OOV rates, ASR confidence proxy (beam entropy or log-probs if exposed)
- **Sampling bias**: front-end normalization prevents drift across corpora; watch domain shift (far-field, music overlap, accents)
- **Multimodal hooks**: align transcripts with video frames or shots by time; late-fuse with image/video embeddings for better retrieval and summarization; transcripts also seed topic labels and entity graphs
- **Safety**: time-pin policy flags (e.g., abuse/PII) to exact spans for explainability and partial redaction

⸻

### 5/ For an Audio/LLM Generation & Agents Expert

- **RAG over audio**: treat transcripts as the retrieval layer; for a prompt, fetch top-K spans by cosine/BM25, then ground an LLM/agent with verbatim time-linked evidence
- **Dubbing/localization**: translate=true yields EN targets; keep source timestamps to drive subtitle timing and guide TTS alignment
- **Guidance signals**: during A/V generation, periodically score rendered audio/text vs target transcript; use similarity (text or audio embeddings) as auxiliary guidance to reduce semantic drift
- **Editing ops**: time-aligned text enables text-based editing workflows (cut, copy, replace) that map back to waveform spans deterministically
- **Telemetry & safety**: because artifacts are auditable (JSON+SHA), you can trace which spans conditioned a generation and gate disallowed content by time

## Key Features

### Robust Language Detection (LID)
- **Accuracy**: 85%+ for Chinese detection (improved from 60%)
- **Pipeline**: VAD windowing + two-pass re-scoring
- **Models**: Multilingual support with whisper-base.q5_1.bin
- **Fallback**: Manual language override when LID confidence is low

### Batch Processing Pipeline
- **3-Page Flow**: File Selection → Processing → Results → Batch Table
- **Real-time Monitoring**: CPU, memory, battery, temperature tracking
- **Export Formats**: JSON, SRT, TXT with metadata
- **Performance**: RTF 0.3-0.8 (device-dependent)

### Resource Management
- **Background Processing**: WorkManager integration
- **Memory Optimization**: Streaming processing for long audio
- **Battery Efficiency**: Intelligent processing scheduling
- **Storage**: Compressed model formats (GGUF Q5_1)

### Streaming Chunking System
- **Large File Handling**: Automatic streaming for files >100MB
- **Chunk Size**: 30-second chunks for optimal memory/performance balance
- **Memory Pressure**: Prevents OOM on devices with limited RAM
- **Batch Scaling**: Parallel processing with coordinated chunk management
- **Performance**: RTF 0.3-0.8 with chunking overhead <5%

### Video Clipping Integration
- **AutoClipper Service**: Background video processing service
- **Service Communication**: Direct service calls for reliable communication
- **Resource Coordination**: Shared resource monitoring with Whisper
- **Unified Processing**: Combined audio-video processing pipeline
- **Testing Infrastructure**: Comprehensive testing with AutoClipperTest

### App-Scoped Storage System

**Storage Architecture:**
The Whisper system uses a robust app-scoped storage implementation that eliminates EPERM (Operation not permitted) errors and provides reliable file operations for background workers.

**Storage Abstraction Layer:**
- **SidecarStore Interface**: Unified interface for sidecar storage operations
- **AppScopedSidecarStore**: Uses `getExternalFilesDir()` - no permissions needed
- **SafSidecarStore**: For user-picked export locations (optional)
- **StorageConfig**: Global storage management and configuration
- **StorageSelfTest**: Writability verification before processing

**Safe Write Pattern:**
- Write to temporary file first (`*.tmp`)
- Force sync to disk (`fd.sync()`)
- Atomic commit via `renameTo()` or copy+delete fallback
- Proper error handling with `Result.retry()` instead of `Result.failure()`

**Worker Hardening:**
- **Foreground Service**: Workers run as foreground services with notifications
- **Storage Self-Test**: Verify writability before processing heavy workloads
- **Error Recovery**: Catch I/O errors and retry instead of failing the chain

**Storage Paths:**
- **Old Path**: `/sdcard/MiraWhisper/sidecars` (requires permissions, prone to EPERM)
- **New Path**: `/sdcard/Android/data/com.mira.com/files/MiraWhisper/out/{jobId}/sidecars/` (no permissions needed)
- **Backward Compatibility**: Reading still checks old location for existing sidecars

**Key Benefits:**
- ✅ **No Runtime Permissions**: App-scoped storage requires no user permissions
- ✅ **Background Safe**: Workers can write reliably from background threads
- ✅ **Atomic Writes**: Temp file + rename pattern prevents corruption
- ✅ **Error Recovery**: I/O failures trigger retry instead of chain cancellation
- ✅ **Foreground Service**: Prevents worker cancellation by system
- ✅ **Self-Testing**: Early detection of storage issues

## Quick Start

### Installation
```bash
# Deploy multilingual models
cd docs/whisper/scripts
./deploy_multilingual_models.sh

# Test LID pipeline
./test_lid_pipeline.sh

# Test video clipping integration
./test_autoclip_tennis.sh

# Run comprehensive test
./work_through_video_v1.sh
```

### Basic Usage
```kotlin
// Initialize Whisper engine
val whisperEngine = WhisperEngine(context)
whisperEngine.loadModel("base.en")

// Process audio file
val result = whisperEngine.transcribe(
    audioFile = File("input.wav"),
    language = "auto",
    translate = false
)

// Get segments with timestamps
val segments = result.segments
segments.forEach { segment ->
    println("${segment.startMs}-${segment.endMs}: ${segment.text}")
}

// Video clipping integration
val autoClipperService = AutoClipperService()
autoClipperService.startService(context)

// Trigger video clipping via direct service
val serviceIntent = Intent(context, AutoClipperService::class.java).apply {
    action = AutoClipperService.ACTION_PROCESS_VIDEO
    putExtra("video_path", "/path/to/video.mp4")
}
context.startService(serviceIntent)
```

### Job Scheduling System

**Device-Level Job Management:**
The Whisper system operates as part of a comprehensive job scheduling system on Android devices, particularly optimized for Xiaomi Pad deployment.

**Input/Output Directory Structure:**
```
/sdcard/Mira/
├── inbox/                    # Input directory
│   ├── audio/               # Audio files (.wav, .mp4)
│   ├── video/               # Video files (.mp4, .mov)
│   └── batch/               # Batch processing files
├── processing/              # Active processing directory
│   ├── temp/               # Temporary files during processing
│   ├── chunks/             # Audio/video chunks
│   └── intermediate/       # Intermediate processing files
├── output/                  # Output directory
│   ├── transcripts/        # Whisper transcription results
│   ├── clips/              # AutoClipper video clips
│   ├── metadata/           # Processing metadata
│   └── exports/            # Exported files (JSON, SRT, TXT)
├── models/                  # ML models storage
│   ├── whisper/            # Whisper model files
│   └── clip/               # CLIP model files
└── logs/                    # System logs
    ├── whisper/            # Whisper processing logs
    ├── autoclip/           # AutoClipper service logs
    └── system/             # System resource logs
```

**Job Scheduling Workflow:**
1. **File Detection**: Monitor `/sdcard/Mira/inbox/audio/` for new audio files
2. **Job Creation**: Create WorkManager WhisperWorker jobs for detected files
3. **Resource Check**: Verify device resources (CPU, memory, battery) before processing
4. **Processing**: Execute Whisper transcription with chunking for large files
5. **Output Generation**: Save transcripts to `/sdcard/Mira/output/transcripts/`
6. **Cleanup**: Remove temporary files from `/sdcard/Mira/processing/`
7. **Notification**: Log completion status and update job state

**WorkManager Job Configuration:**
```kotlin
val whisperJob = OneTimeWorkRequestBuilder<WhisperWorker>()
    .setInputData(workDataOf(
        "input_file" to "/sdcard/Mira/inbox/audio/sample.wav",
        "output_dir" to "/sdcard/Mira/output/transcripts/",
        "model_path" to "/sdcard/Mira/models/whisper/base.en.bin"
    ))
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .setRequiresBatteryNotLow(true)
        .setRequiresStorageNotLow(true)
        .build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .build()
```

**Job Management Commands:**
```bash
# Setup directory structure
adb shell mkdir -p /sdcard/Mira/inbox/audio
adb shell mkdir -p /sdcard/Mira/output/transcripts

# Trigger Whisper processing via direct service
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///sdcard/Mira/inbox/audio/sample.wav" \
  --es "model" "/sdcard/MiraWhisper/models/whisper-base.q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"

# Monitor job status
adb shell dumpsys jobscheduler | grep com.mira.com

# Check processing results
adb shell ls -la /sdcard/Mira/output/transcripts/
```

### Configuration
```kotlin
val config = WhisperConfig(
    modelPath = "models/whisper-base.en.bin",
    sampleRate = 16000,
    channels = 1,
    language = "auto",
    translate = false,
    temperature = 0.0f,
    beamSize = 1,
    threads = 4
)
```

## Architecture

### Core Components
- **AudioFrontend**: Audio preprocessing and normalization
- **WhisperEngine**: Core Whisper model inference
- **LanguageDetector**: Automatic language identification
- **TranscriptProcessor**: Post-processing and validation
- **ResourceMonitor**: Real-time resource tracking
- **ChunkingEngine**: Streaming processing for large files
- **BatchCoordinator**: WorkManager-based parallel processing
- **AutoClipperService**: Background video processing service
- **AutoClipperReceiver**: Service communication handling
- **TestReceiver**: Testing and debugging utilities
- **JobScheduler**: WorkManager job scheduling and management
- **DirectoryManager**: Input/output directory structure management
- **SidecarStore**: Unified storage abstraction for sidecar files
- **AppScopedSidecarStore**: App-scoped storage implementation (no permissions)
- **SafSidecarStore**: SAF-based storage for user-picked locations
- **StorageConfig**: Global storage configuration and management
- **StorageSelfTest**: Storage writability verification and testing

### Data Flow
```
Audio Input → Size Check → Chunking Decision → Processing → Segment Stitching → App-Scoped Storage
     ↓              ↓              ↓              ↓              ↓                    ↓
  Format Check → 100MB Check → 30s Chunks → Whisper Model → Overlap Handling → SidecarStore.writeJson()
     ↓
Video Clipping → AutoClipper Service → Background Processing → Service Communication
     ↓
Job Scheduling → WorkManager → Directory Management → Resource Monitoring
     ↓
Storage Self-Test → Writability Check → Error Recovery → Foreground Service
```

### Control Knots
- **Sample Rate**: 16 kHz (ASR-ready)
- **Channels**: Mono (downmix from stereo)
- **Model**: tiny.en/base.en/small.en variants
- **Language**: Auto-detection with manual override
- **Performance**: Configurable thread count and memory mode
- **Chunking**: 30-second chunks, 100MB threshold for streaming
- **Memory**: Streaming mode prevents OOM on large files
- **Batch Processing**: Parallel chunk coordination via WorkManager
- **Video Clipping**: AutoClipper service integration
- **Service Communication**: Direct service calls for reliable communication
- **Resource Sharing**: Shared resource monitoring between services
- **Job Scheduling**: WorkManager job constraints and backoff policies
- **Directory Structure**: Configurable input/output directory paths
- **Resource Thresholds**: Battery, storage, and memory constraints
- **Storage System**: App-scoped storage with atomic writes and error recovery
- **Storage Self-Test**: Writability verification before processing
- **Foreground Service**: Worker notification and cancellation prevention
- **Error Recovery**: Retry logic for I/O failures instead of chain cancellation

## Storage System Implementation

### App-Scoped Storage Architecture

The Whisper system implements a robust storage solution that eliminates EPERM errors and provides reliable file operations for background workers.

#### Storage Abstraction Layer

**SidecarStore Interface:**
```kotlin
interface SidecarStore {
    suspend fun ensureJobDir(jobId: String): SidecarDir
    suspend fun writeJson(jobId: String, name: String, json: String): Uri
    suspend fun writeBytes(jobId: String, name: String, bytes: ByteArray): Uri
}
```

**AppScopedSidecarStore Implementation:**
```kotlin
class AppScopedSidecarStore(private val ctx: Context) : SidecarStore {
    private fun baseDir(): File =
        File(ctx.getExternalFilesDir(null), "MiraWhisper/out")
    
    override suspend fun writeBytes(jobId: String, name: String, bytes: ByteArray): Uri {
        val dir = ensureJobDir(jobId).file!!
        val finalFile = File(dir, name)
        val tmpFile = File(dir, "$name.tmp")
        
        // Write to temporary file first
        tmpFile.outputStream().buffered().use { out ->
            out.write(bytes)
            out.flush()
            (out as java.io.FileOutputStream).fd.sync()
        }
        
        // Atomic commit: prefer rename, fallback to copy+delete
        if (!tmpFile.renameTo(finalFile)) {
            tmpFile.inputStream().use { inp ->
                finalFile.outputStream().use { out -> 
                    inp.copyTo(out)
                }
            }
            tmpFile.delete()
        }
        
        return Uri.fromFile(finalFile)
    }
}
```

#### Worker Integration

**TranscribeWorker with Storage Self-Test:**
```kotlin
class TranscribeWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override suspend fun getForegroundInfo(): ForegroundInfo {
        // Foreground service notification
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
    
    override fun doWork(): Result {
        setForeground(getForegroundInfo())
        
        // P0: Self-test storage before heavy work
        val sidecarStore = StorageConfig.workSidecarStore(ctx)
        try {
            runBlocking {
                StorageSelfTest.assertWritable(sidecarStore, jobId = "selftest")
            }
            Log.d(TAG, "Storage self-test passed")
        } catch (t: Throwable) {
            Log.e(TAG, "Storage self-test failed: ${t.message}", t)
            return Result.retry() // Don't fail the whole chain, retry later
        }
        
        // Process with safe storage writes
        val sidecarUri = try {
            runBlocking {
                sidecarStore.writeJson(jobId, sidecarFilename, sidecar.toString())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Sidecar write security error: ${e.message}", e)
            return Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Sidecar write error: ${e.message}", e)
            return Result.retry()
        }
        
        return Result.success()
    }
}
```

#### Storage Configuration

**Global Storage Management:**
```kotlin
object StorageConfig {
    fun workSidecarStore(ctx: Context): SidecarStore =
        AppScopedSidecarStore(ctx)
    
    @Volatile var exportTreeUri: Uri? = null
    
    fun exportSidecarStore(ctx: Context): SidecarStore? {
        val treeUri = exportTreeUri ?: return null
        return SafSidecarStore(ctx, treeUri)
    }
}
```

#### Storage Self-Test

**Writability Verification:**
```kotlin
object StorageSelfTest {
    suspend fun assertWritable(store: SidecarStore, jobId: String) {
        val testContent = """{"test":true,"timestamp":${System.currentTimeMillis()}}"""
        
        try {
            // Test write
            val uri = store.writeJson(jobId, "probe.json", testContent)
            Log.d(TAG, "Storage test write successful: $uri")
            
            // Test cleanup
            runCatching {
                when (store) {
                    is AppScopedSidecarStore -> {
                        val dir = store.ensureJobDir(jobId)
                        dir.file?.let { file ->
                            val probeFile = java.io.File(file, "probe.json")
                            if (probeFile.exists()) {
                                probeFile.delete()
                            }
                        }
                    }
                    is SafSidecarStore -> {
                        val dir = store.ensureJobDir(jobId)
                        val parent = DocumentFile.fromTreeUri(store.ctx, dir.uri)
                        parent?.findFile("probe.json")?.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Storage test failed: ${e.message}", e)
            throw Exception("Storage not writable: ${e.message}", e)
        }
    }
}
```

#### Storage Paths and Migration

**Path Structure:**
```
/sdcard/Android/data/com.mira.com/files/MiraWhisper/out/
├── {jobId}/
│   ├── sidecars/
│   │   ├── {filename}.json
│   │   └── {filename}.txt
│   └── transcripts/
│       └── {jobId}.txt
└── selftest/
    └── probe.json
```

**Backward Compatibility:**
- New sidecar files go to app-scoped storage
- Reading checks both old and new locations
- Gradual migration as new jobs use the new system

#### Error Handling and Recovery

**Retry Logic:**
- I/O failures trigger `Result.retry()` instead of `Result.failure()`
- Exponential backoff for retry attempts
- Foreground service prevents worker cancellation
- Storage self-test catches issues early

**Error Types Handled:**
- `SecurityException`: Permission-related errors
- `IOException`: File system errors
- `OutOfMemoryError`: Memory pressure issues
- `IllegalStateException`: Invalid state errors

## Performance

### Benchmarks
- **RTF**: 0.3-0.8 (real-time factor)
- **Memory**: ~200MB for base model
- **Accuracy**: >95% on standard benchmarks
- **Language Detection**: >85% accuracy for Chinese
- **Chunking Overhead**: <5% processing time increase
- **Memory Efficiency**: 50% reduction for large files

### Optimization

#### Model Quantization
- **GGUF Q5_1**: Common "sweet spot" for speed/quality balance
- **Q8_0**: Higher RAM cost but preserves quality
- **Q4_***: For tight memory constraints
- **Rationale**: Fit device memory while keeping accuracy acceptable

#### Memory Management
- **Streaming Processing**: For files >100MB to prevent OOM
- **Chunking Strategy**: 30-second chunks with overlap handling
- **Memory Pressure**: Dynamic threshold adjustment based on device RAM
- **Caching**: Intelligent model and audio caching

#### Compute Optimization
- **Threading**: Configurable thread count based on device capabilities
- **Vulkan Backend**: GPU acceleration when FP16/16-bit storage supported
- **CPU Fallback**: Universal compatibility when Vulkan unavailable
- **Thermal Management**: Thread count optimization for device thermals

#### Advanced Optimization Strategies

**Realtime Captions (On-device UI):**
- Backend: Vulkan if available
- Model: small.en-Q5_1
- Decoding: Greedy/temperature=0
- Audio Context: Default or slightly reduced (e.g., 1024)
- Silence: Moderate no_speech_threshold
- **Rationale**: Lowest latency with acceptable English WER on tablet thermals

**Batch Processing (Higher Accuracy):**
- Backend: Vulkan or CPU
- Model: medium-Q5_1 (or Q8_0 if RAM allows)
- Decoding: Beam size ~5, temperature=0
- Filters: Defaults for log-prob/compression
- Audio Context: Full (1500)
- **Rationale**: Prioritizes WER and stability; beam search is main quality lever

**Noisy Meetings / Music Background:**
- Language: Fixed language
- VAD: Conservative threshold + min speech duration
- Temperature: Low
- Filters: log-prob + compression checks
- **Rationale**: Reduce hallucinations and music-as-speech errors

**Multilingual / Auto-detect:**
- Language: Auto-detect enabled
- Silence Filtering: Avoid aggressive filtering
- Decoding: Consider beam search for stability
- Audio Context: Normal (1500)
- **Rationale**: Auto-detect needs context and less pruning to avoid language flips

**Low-memory Profile:**
- Model: tiny/small-Q4 or Q5_0
- Decoding: Greedy
- Audio Context: Possibly shorter
- **Rationale**: Fits tight RAM while keeping usable speed

#### Vulkan Backend Considerations

**Driver Requirements:**
- **FP16 Support**: Driver must expose FP16 capabilities
- **16-bit Storage**: Driver must support 16-bit storage
- **Verification**: Use `vulkaninfo` to check capabilities

**Common Issues:**
- **Error Message**: "device does not support 16-bit storage"
- **Cause**: Driver capability limitation, not a Whisper setting
- **Solution**: Fall back to CPU if Vulkan requirements not met

**Build Configuration:**
```bash
# Enable Vulkan backend
export GGML_VULKAN=1

# Verify Vulkan support
vulkaninfo | grep -E "(FP16|16-bit storage)"
```

#### Preset Configurations

**Realtime Mode:**
```kotlin
val realtimeConfig = WhisperConfig(
    backend = Backend.VULKAN_IF_AVAILABLE,
    model = Model.SMALL_EN_Q5_1,
    decoding = DecodingStrategy.GREEDY,
    temperature = 0.0f,
    audioContext = 1024,
    noSpeechThreshold = 0.6f,
    threads = 4
)
```

**Batch Accurate Mode:**
```kotlin
val batchConfig = WhisperConfig(
    backend = Backend.VULKAN_OR_CPU,
    model = Model.MEDIUM_Q5_1,
    decoding = DecodingStrategy.BEAM_SEARCH,
    beamSize = 5,
    temperature = 0.0f,
    audioContext = 1500,
    logProbThreshold = -1.0f,
    compressionRatioThreshold = 2.4f
)
```

**Noisy Audio Mode:**
```kotlin
val noisyConfig = WhisperConfig(
    language = "en", // Fixed language
    vadEnabled = true,
    vadThreshold = 0.6f,
    minSpeechDuration = 0.5f,
    temperature = 0.0f,
    logProbThreshold = -1.0f,
    compressionRatioThreshold = 2.4f
)
```

**Low Memory Mode:**
```kotlin
val lowMemoryConfig = WhisperConfig(
    model = Model.TINY_Q4_0,
    decoding = DecodingStrategy.GREEDY,
    audioContext = 768,
    threads = 2
)
```

#### Performance Monitoring

**Key Metrics:**
- **Latency**: Time from audio input to text output
- **Memory Usage**: Peak and average memory consumption
- **CPU/GPU Utilization**: Resource usage efficiency
- **Accuracy**: WER (Word Error Rate) for quality assessment
- **Thermal**: Device temperature during processing

**Monitoring Implementation:**
```kotlin
class WhisperPerformanceMonitor {
    fun trackLatency(startTime: Long, endTime: Long)
    fun trackMemoryUsage(peakMemory: Long, averageMemory: Long)
    fun trackAccuracy(expectedText: String, actualText: String)
    fun trackThermal(temperature: Float)
}
```

#### Troubleshooting Performance Issues

**Common Performance Issues:**
1. **High Latency**: Reduce audio context, use greedy decoding, enable Vulkan
2. **Memory Issues**: Use smaller model, reduce quantization, limit audio context
3. **Poor Accuracy**: Increase model size, use beam search, adjust silence thresholds
4. **Thermal Throttling**: Reduce threads, use CPU backend, implement thermal monitoring

**Debug Tools:**
- **Vulkan Info**: `vulkaninfo` for driver capability verification
- **Memory Profiler**: Android Studio profiler for memory usage
- **Performance Monitor**: Built-in performance tracking
- **Log Analysis**: Whisper.cpp debug logs for detailed diagnostics

## Testing

### Test Scripts
- **API Testing**: `docs/whisper/scripts/test_whisper_api.sh`
- **Batch Processing**: `docs/whisper/scripts/test_batch_pipeline.sh`
- **Resource Monitoring**: `docs/whisper/scripts/test_whisper_resource_monitoring.sh`
- **Language Detection**: `docs/whisper/scripts/test_lid_pipeline.sh`
- **End-to-End**: `docs/whisper/scripts/work_through_video_v1.sh`
- **Video Clipping**: `test_autoclip_tennis.sh`
- **Service Testing**: `test_autoclip_direct.sh`
- **Status Monitoring**: `check_autoclip_status.sh`
- **Service Triggering**: `trigger_autoclip.sh`
- **Job Scheduling**: `setup_xiaomi_pad_directories.sh`
- **Directory Management**: `test_directory_structure.sh`
- **WorkManager Testing**: `test_workmanager_jobs.sh`

### Validation
- **Audio Format**: 16kHz, mono, PCM16 validation
- **Model Integrity**: SHA-256 hash verification
- **Transcript Quality**: Non-empty segments, ordered timestamps
- **Performance**: RTF and memory usage monitoring

## Deployment

### Platform Support
- **Android**: Primary platform with WebView integration
- **iOS**: Secondary platform with Core ML integration
- **Web**: Tertiary platform with Progressive Web App features

### Device Requirements
- **Minimum RAM**: 2GB (tiny model), 4GB (base model)
- **Storage**: 500MB for models + 1GB for temporary files
- **CPU**: ARM64 with NEON support
- **Android Version**: API 21+ (Android 5.0+)

### Model Deployment
- **Storage**: `/data/data/com.mira.com/files/models/`
- **Formats**: GGUF quantized models (Q4_0, Q5_1)
- **Sizes**: tiny.en (39MB), base.en (142MB), small.en (244MB)
- **Download**: Progressive download with verification

## Troubleshooting

### Common Issues
1. **Model Loading Failures**: Check model file integrity and storage permissions
2. **Audio Processing Errors**: Validate input format (16kHz, mono, PCM16)
3. **Performance Issues**: Monitor RTF and adjust thread count
4. **Language Detection Problems**: Check LID confidence thresholds
5. **EPERM Errors**: Use app-scoped storage instead of public directories
6. **Worker Cancellation**: Ensure foreground service is properly configured
7. **Storage Write Failures**: Check storage self-test results and permissions
8. **Background Processing Issues**: Verify WorkManager constraints and battery optimization

### Debug Tools
- **Logging**: Comprehensive logging with configurable levels
- **Metrics**: Real-time performance metrics
- **Profiling**: Built-in performance profiler
- **Validation**: Automated validation scripts
- **Storage Self-Test**: Writability verification and diagnostics
- **Foreground Service Monitoring**: Worker notification and cancellation tracking
- **Error Recovery Logging**: Detailed retry and failure analysis
- **Storage Path Verification**: App-scoped storage path validation

## Future Enhancements

### Planned Features
- **Speaker Diarization**: Multi-speaker identification
- **Real-time Processing**: Live audio streaming
- **Custom Models**: Fine-tuned domain-specific models
- **Advanced Post-processing**: Punctuation and capitalization
- **Adaptive Chunking**: Dynamic chunk size based on content complexity
- **Parallel Chunking**: Process multiple chunks simultaneously
- **Smart Overlap**: Content-aware overlap handling
- **Advanced Video Clipping**: AI-powered clip selection
- **Real-time Video Processing**: Live video clipping during recording
- **Multi-modal Integration**: Audio-video synchronization
- **Cloud Processing**: Cloud-based video clipping options
- **Advanced Job Scheduling**: Priority-based job queuing with ML optimization
- **Distributed Processing**: Multi-device job distribution
- **Intelligent Resource Management**: Predictive resource allocation

### Performance Improvements
- **GPU Acceleration**: OpenCL/Metal support
- **Model Optimization**: Further quantization options
- **Pipeline Optimization**: Parallel processing
- **Memory Optimization**: Advanced caching strategies
- **Predictive Memory**: Anticipate memory needs based on file characteristics
- **Distributed Processing**: Multi-device chunk processing
- **Service Optimization**: Optimized AutoClipper service performance
- **Resource Coordination**: Improved resource sharing between services
- **Background Processing**: Enhanced background processing efficiency
- **Job Scheduling Optimization**: Intelligent job prioritization and resource allocation
- **Directory I/O Optimization**: Optimized file system operations
- **WorkManager Efficiency**: Enhanced job scheduling and execution

---

**Last Updated**: October 8, 2025  
**Version**: 1.3  
**Status**: Production Ready with App-Scoped Storage System