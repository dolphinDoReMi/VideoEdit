# Infrastructure Architecture Design and Control Knot

**Status: READY FOR VERIFICATION**

## Control Knots

### Core Infrastructure Controls
- **Hash-based detection**: SHA-256 content comparison
- **Duplicate strategy**: Keep most recent file
- **Global cleanup**: Comprehensive cleanup operations
- **Error handling**: Graceful degradation

### Advanced Infrastructure Control Knots

#### File Management Control Knots
- **Hash Algorithm**: SHA-256 for content-based detection
  - **Rationale**: Cryptographically secure, collision-resistant
  - **Control**: Configurable hash algorithm
  - **Impact**: Higher security vs processing cost

- **Duplicate Strategy**: Keep most recent vs keep original
  - **Rationale**: Balance between storage efficiency and data integrity
  - **Control**: Configurable duplicate handling strategy
  - **Impact**: Storage savings vs data preservation

#### Storage Optimization Control Knots
- **Cleanup Frequency**: Automatic vs manual cleanup
  - **Rationale**: Balance between automation and user control
  - **Control**: Configurable cleanup scheduling
  - **Impact**: Automatic cleanup vs user intervention

- **Storage Limits**: Configurable storage quotas
  - **Rationale**: Prevent storage overflow
  - **Control**: Configurable storage limits
  - **Impact**: Storage protection vs flexibility

#### Performance Control Knots
- **Chunk Size**: 8KB chunks for large file hashing
  - **Rationale**: Balance between memory usage and processing speed
  - **Control**: Configurable chunk size
  - **Impact**: Memory usage vs processing speed

- **Background Processing**: Asynchronous operations
  - **Rationale**: Non-blocking user experience
  - **Control**: Configurable background processing
  - **Impact**: Better UX vs resource usage

## Implementation

- **Deterministic hashing**: SHA-256 with 8KB chunks
- **Content-based detection**: 100% accuracy duplicate detection
- **Background processing**: Asynchronous operations with callbacks
- **Storage optimization**: Up to 40% space reduction

## Verification

Hash comparison script in `docs/infra/Scripts/Verification Ops/verify_all.sh`

### Verification Scripts
- **Comprehensive Verification**: `docs/infra/Scripts/Verification Ops/verify_all.sh`
- **Scoped Storage Verification**: `docs/infra/Scripts/Verification Ops/verify_A_scoped_storage.sh`
- **Capability Access Verification**: `docs/infra/Scripts/Verification Ops/verify_B_capability_access.sh`
- **Model Integrity Verification**: `docs/infra/Scripts/Verification Ops/verify_C_model_integrity.sh`
- **Memory Mapping Verification**: `docs/infra/Scripts/Verification Ops/verify_D_memory_mapping.sh`
- **Embedding Storage Verification**: `docs/infra/Scripts/Verification Ops/verify_E_embedding_storage.sh`
- **FD Processing Verification**: `docs/infra/Scripts/Verification Ops/verify_F_fd_processing.sh`
- **Output Generation Verification**: `docs/infra/Scripts/Verification Ops/verify_G_output_generation.sh`
- **Secure Sharing Verification**: `docs/infra/Scripts/Verification Ops/verify_H_secure_sharing.sh`
- **Cleanup Operations Verification**: `docs/infra/Scripts/Verification Ops/verify_I_cleanup_operations.sh`
- **Error Handling Verification**: `docs/infra/Scripts/Verification Ops/verify_J_error_handling.sh`
- **API Surface Verification**: `docs/infra/Scripts/Verification Ops/verify_K_api_surface.sh`
- **Dependency Management Verification**: `docs/infra/Scripts/Verification Ops/verify_L_dependency_management.sh`
- **Testing Coverage Verification**: `docs/infra/Scripts/Verification Ops/verify_M_testing_coverage.sh`

## Core Infrastructure Components

### GlobalFileManagementService
Centralized service providing:
- Hash-based duplicate detection using SHA-256
- Filename-based duplicate detection
- Original file removal after conversion
- Global cleanup operations
- Storage optimization

### GlobalFileManagementBackgroundService
Background service wrapper providing:
- Asynchronous execution of file management operations
- Non-blocking duplicate detection and removal
- Background cleanup operations
- Performance monitoring

### HashBasedDuplicateDetectionService
Specialized service for:
- SHA-256 content hashing
- Duplicate file identification
- Selective removal strategies
- Performance optimization

## Integration Points

### MediaConverter Integration
- `feature/whisper/src/main/java/com/mira/com/feature/whisper/data/io/MediaConverter.kt`
- Uses `GlobalFileManagementService` for all duplicate detection and cleanup operations
- Provides backward compatibility through wrapper functions

### Orchestrator Integration
- `app/src/main/java/com/mira/clip/ops/Orchestrator.kt`
- Uses `GlobalFileManagementService` for global cleanup operations
- Provides unified cleanup across CLIP and Whisper modules

### Web Interface Integration
- `app/src/main/assets/web/whisper_unified.html`
- Uses core infrastructure services through Android bridge
- Provides background service execution

## Performance Metrics

- **Space Optimization**: Up to 40% reduction through duplicate removal
- **Processing Efficiency**: 8KB chunk-based hashing for large files
- **Error Resilience**: Graceful handling of edge cases
- **Background Processing**: Non-blocking operations with callbacks

## Testing Strategy

- **Unit Tests**: Individual service component testing
- **Integration Tests**: Cross-module integration testing
- **Performance Tests**: Hash calculation and cleanup efficiency
- **Error Tests**: Edge case and error condition testing
- **Device Tests**: Multi-device testing (Xiaomi Pad, iPad)

## Optimized Configuration Combinations

### High Performance Mode
**Configuration:**
- Hash Algorithm: SHA-256
- Chunk Size: 16KB
- Duplicate Strategy: Keep most recent
- Cleanup Frequency: Automatic

**Rationale:** Maximum performance with automatic optimization

### Storage Optimized Mode
**Configuration:**
- Hash Algorithm: SHA-256
- Chunk Size: 8KB
- Duplicate Strategy: Keep most recent
- Cleanup Frequency: Aggressive

**Rationale:** Maximum storage savings with frequent cleanup

### Balanced Mode
**Configuration:**
- Hash Algorithm: SHA-256
- Chunk Size: 8KB
- Duplicate Strategy: Keep most recent
- Cleanup Frequency: Moderate

**Rationale:** Balanced performance and storage optimization

## Troubleshooting Architecture

### Common Infrastructure Issues
1. **Hash Calculation Slow**: Increase chunk size, optimize algorithm
2. **Storage Issues**: Increase cleanup frequency, adjust storage limits
3. **Memory Pressure**: Reduce chunk size, enable garbage collection
4. **Service Failures**: Check background processing, verify permissions

### Debug Tools
- **Hash Calculator**: SHA-256 hash verification
- **Storage Monitor**: Storage usage analysis
- **Performance Profiler**: Infrastructure performance tracking
- **Log Analyzer**: Detailed infrastructure log analysis

## Future Enhancements

### Planned Optimizations
- **Advanced Hashing**: Parallel hash calculation
- **Cloud Integration**: Cloud-based duplicate detection
- **Machine Learning**: Intelligent duplicate detection
- **Real-time Monitoring**: Live infrastructure monitoring

### Performance Targets
- **Hash Speed**: <1ms per MB processed
- **Storage Savings**: 50% reduction through optimization
- **Error Rate**: <0.1% error rate
- **Background Impact**: <1% CPU usage

## Scoped Storage Architecture

### Storage Boundaries (Zero Raw Paths from Shared Storage)

#### App-Private Root Structure
```
/data/user/0/<pkg>/files/
├── models/          # Versioned, mmappable model files
├── cache/           # Decoded PCM/chunks, temporary processing
├── outputs/         # Transcripts, thumbnails, processed results
└── temp/            # Temporary files during processing
```

**No direct `File("/sdcard/...")`**. All user content enters via URI/FD.

### Core Components Design

#### 1. ModelStore (Idempotent, Verifiable)
**Purpose**: Manages model files in app-private storage with SHA-256 validation.

**Key Features**:
- Idempotent installation (won't re-download if already present)
- SHA-256 validation for integrity
- Versioned storage with hash-based naming
- App-private storage only (no external storage access)

**API Design**:
```kotlin
@Singleton
class ModelStore {
    suspend fun install(
        context: Context,
        srcUri: Uri,
        name: String,
        expectedSha256: String? = null
    ): File
    
    fun getModel(context: Context, name: String, sha256: String): File?
    fun listModels(context: Context): List<File>
    fun removeModel(context: Context, name: String, sha256: String): Boolean
    fun validateModel(file: File, expectedSha256: String): Boolean
}
```

#### 2. ContentIngestion (Capability-Based)
**Purpose**: Handles user content access through URI/FD patterns.

**Key Features**:
- Capability-based access to user content
- No raw external storage paths exposed
- All access through Android's content resolver system
- Optional caching for slow providers

**API Design**:
```kotlin
@Singleton
class ContentIngestion {
    suspend fun openFileDescriptor(
        context: Context,
        uri: Uri,
        mode: String = "r"
    ): Int
    
    suspend fun cacheContent(
        context: Context,
        uri: Uri,
        fileName: String
    ): File
    
    fun getCachedContent(context: Context, fileName: String): File?
    fun clearCache(context: Context)
    fun getCacheSize(context: Context): Long
}
```

#### 3. OutputStorage (Secure Sharing)
**Purpose**: Manages processed results in app-private storage and provides secure sharing capabilities.

**Key Features**:
- All outputs stored in app-private storage
- Secure sharing via FileProvider
- Timestamp-based file naming
- Organized subdirectories for different output types

**API Design**:
```kotlin
@Singleton
class OutputStorage {
    suspend fun createTranscriptFile(context: Context, prefix: String = "transcript"): File
    suspend fun createSrtFile(context: Context, prefix: String = "subtitle"): File
    suspend fun createThumbnailFile(context: Context, prefix: String = "thumb"): File
    suspend fun createEmbeddingsFile(context: Context, prefix: String = "embeddings"): File
    
    fun getShareableUri(context: Context, file: File): Uri
    fun createShareIntent(context: Context, file: File, mimeType: String, title: String? = null): Intent
    fun createTextShareIntent(text: String, title: String? = null): Intent
    
    fun listTranscripts(context: Context): List<File>
    fun listThumbnails(context: Context): List<File>
    fun listEmbeddings(context: Context): List<File>
}
```

#### 4. NativeStorageBridge (FD-First + Private-Path)
**Purpose**: JNI interface for scoped storage-compliant model loading and processing.

**Key Features**:
- Both FD-based and mmap-based access patterns
- No external storage paths exposed to native code
- Comprehensive error handling
- Model validation and hash calculation

**API Design**:
```kotlin
object NativeStorageBridge {
    external fun initModelFromAppFile(modelPath: String): Long
    external fun initModelFromBuffer(modelBuffer: ByteBuffer): Long
    external fun transcribeFromFd(contextPtr: Long, audioFd: Int, outputPath: String): Int
    external fun transcribeFromPcmData(contextPtr: Long, pcmData: ByteArray, sampleRate: Int, channels: Int, outputPath: String): Int
    external fun extractEmbeddingsFromFd(contextPtr: Long, audioFd: Int, outputPath: String): Int
    external fun generateThumbnailFromFd(videoFd: Int, timestampMs: Long, outputPath: String): Int
    external fun freeContext(contextPtr: Long)
    external fun getLastError(): String?
    external fun validateModelFile(modelPath: String): Boolean
    external fun getModelHash(modelPath: String): String?
}
```

### Compliance Checklist

Before shipping, ensure:
- [ ] No raw `/sdcard/...` reads/writes; all user content via URI/FD
- [ ] Models are only in app-private; load via mmap
- [ ] Results in app-private; sharing via FileProvider only
- [ ] Use `READ_MEDIA_*` narrowly; request at runtime
- [ ] Persist URI perms only when you truly need long-term access
- [ ] Foreground Service for visible long ops; WorkManager for retries
- [ ] Caches have TTL/LRU; user can clear in app
- [ ] JNI never calls `fopen("/storage/emulated/0/...")`

### Key Principles

1. **Models** → app-private + mmap
2. **Inputs** → URI/FD
3. **Outputs** → app-private → FileProvider
4. **Pass capabilities** (URI/FD/ByteBuffer) across layers—never external paths
5. **Wrap work** in FGS/WorkManager, minimize perms, and keep caches tidy
