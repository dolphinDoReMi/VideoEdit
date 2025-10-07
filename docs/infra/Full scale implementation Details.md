# Infrastructure - Full Scale Implementation Details

## Problem Disaggregation

### Inputs
- **File formats**: All supported media formats (MP4, AVI, MOV, MKV, WAV, MP3, etc.)
- **Storage locations**: Cache, Documents, temp, whisper, CLIP directories
- **File sizes**: 1KB to 2GB+ files with efficient processing
- **Metadata**: File timestamps, sizes, and content hashes

### Outputs
- **Duplicate detection**: Content-based and filename-based duplicate identification
- **Cleanup results**: Comprehensive cleanup with detailed metrics
- **Storage optimization**: Space savings through duplicate removal
- **Performance metrics**: Processing statistics and efficiency measurements

### Runtime Architecture
- **Pipeline**: File Discovery → Hash Calculation → Duplicate Detection → Cleanup → Optimization
- **Processing**: Background processing with configurable batch sizes
- **Storage**: Efficient temporary file management
- **Caching**: Intelligent caching for repeated operations

## Analysis with Trade-offs

### Hash Calculation Strategy
- **Algorithm**: SHA-256 vs. MD5 for security vs. speed
- **Chunk size**: 4KB vs. 8KB vs. 16KB for memory vs. efficiency
- **Streaming**: Memory-based vs. disk-based processing

### Duplicate Detection Strategy
- **Content-based**: Hash comparison for 100% accuracy
- **Filename-based**: Pattern matching for quick detection
- **Hybrid approach**: Combined strategies for optimal performance

### Cleanup Strategy
- **Scope**: Selective vs. comprehensive cleanup
- **Safety**: Conservative vs. aggressive removal
- **Recovery**: Manual vs. automatic error recovery

## Design Pipeline

### File Management Pipeline
```
File Discovery → Hash Calculation → Duplicate Detection → Removal Strategy → Cleanup Execution → Result Reporting
```

### Global Cleanup Pipeline
```
Directory Scanning → Hash-based Duplicate Detection → Selective Removal → Cache Cleanup → Storage Optimization → Performance Metrics
```

### Key Control Knots
- **HASH_ALGORITHM**: Hash algorithm for content comparison (default: SHA-256)
- **CHUNK_SIZE**: Processing chunk size (default: 8KB)
- **DUPLICATE_STRATEGY**: Duplicate removal strategy (keep_recent, keep_largest, manual)
- **CLEANUP_SCOPE**: Cleanup scope (selective, comprehensive, custom)
- **BATCH_SIZE**: Processing batch size (default: 100 files)
- **CACHE_SIZE**: Cache size for operations (default: 1000 entries)
- **ERROR_HANDLING**: Error handling strategy (fail_fast, graceful, retry)

## Prioritization & Rationale

### P0: Core Functionality
- **Hash calculation**: SHA-256 content hashing for duplicate detection
- **Duplicate detection**: Content-based and filename-based detection
- **Global cleanup**: Comprehensive cleanup operations
- **Error handling**: Robust error handling and recovery

### P1: Performance Optimization
- **Background processing**: Asynchronous execution of operations
- **Batch processing**: Multiple file processing capabilities
- **Memory optimization**: Efficient memory usage for large files
- **Caching system**: Intelligent caching for repeated operations

### P2: Advanced Features
- **Real-time monitoring**: Live performance monitoring
- **Custom strategies**: Configurable cleanup and removal strategies
- **Distributed processing**: Multi-device processing support
- **Analytics**: Detailed analytics and reporting

## Implementation

### Core Services

#### GlobalFileManagementService
```kotlin
object GlobalFileManagementService {
    fun calculateFileHash(file: File): String?
    fun findDuplicateFilesByHash(directory: File): Map<String, List<File>>
    fun removeDuplicateFilesByHash(directory: File): DuplicateRemovalResult
    fun findExistingConvertedFile(context: Context, inputUri: Uri, outputFileName: String? = null): Uri?
    fun removeOriginalFile(context: Context, inputUri: Uri): Boolean
    fun globalCleanup(context: Context): GlobalCleanupResult
}
```

#### GlobalFileManagementBackgroundService
```kotlin
class GlobalFileManagementBackgroundService {
    fun removeDuplicateFilesByHash(directory: File, onComplete: (DuplicateRemovalResult) -> Unit = {})
    fun findDuplicateFilesByHash(directory: File, onComplete: (Map<String, List<File>>) -> Unit = {})
    fun globalCleanup(context: Context, onComplete: (GlobalCleanupResult) -> Unit = {})
    fun performComprehensiveFileManagement(context: Context, directories: List<File>, onComplete: (ComprehensiveFileManagementResult) -> Unit = {})
}
```

#### HashBasedDuplicateDetectionService
```kotlin
object HashBasedDuplicateDetectionService {
    fun calculateFileHash(file: File): String?
    fun findDuplicateFilesByHash(directory: File): Map<String, List<File>>
    fun removeDuplicateFilesByHash(directory: File): DuplicateRemovalResult
    fun removeDuplicateFilesFromDirectories(directories: List<File>): GlobalDuplicateRemovalResult
}
```

### Configuration Management
```kotlin
object InfrastructureConfig {
    const val HASH_ALGORITHM = "SHA-256"
    const val CHUNK_SIZE = 8192 // 8KB
    const val DUPLICATE_STRATEGY = "keep_recent"
    const val CLEANUP_SCOPE = "comprehensive"
    const val BATCH_SIZE = 100
    const val CACHE_SIZE = 1000
    const val ERROR_HANDLING = "graceful"
}
```

## Key Control Knots Configuration

| Knot | BuildConfig | Purpose | Typical Values |
|------|-------------|---------|----------------|
| Hash algorithm | HASH_ALGORITHM | Content comparison | SHA-256 |
| Chunk size | CHUNK_SIZE | Processing efficiency | 8KB |
| Duplicate strategy | DUPLICATE_STRATEGY | Removal strategy | keep_recent |
| Cleanup scope | CLEANUP_SCOPE | Cleanup range | comprehensive |
| Batch size | BATCH_SIZE | Processing efficiency | 100 files |
| Cache size | CACHE_SIZE | Memory management | 1000 entries |
| Error handling | ERROR_HANDLING | Error strategy | graceful |

## Scale-out Plan

### Single Configuration (Default)
```json
{
  "preset": "SINGLE",
  "hash_algorithm": "SHA-256",
  "chunk_size": 8192,
  "duplicate_strategy": "keep_recent",
  "cleanup_scope": "comprehensive",
  "batch_size": 100,
  "cache_size": 1000,
  "error_handling": "graceful"
}
```

### Performance Optimizations

#### A. High-Speed Processing
```json
{
  "preset": "HIGH_SPEED",
  "chunk_size": 16384,
  "batch_size": 200,
  "cache_size": 2000,
  "error_handling": "fail_fast"
}
```

#### B. High-Accuracy Processing
```json
{
  "preset": "HIGH_ACCURACY",
  "hash_algorithm": "SHA-256",
  "chunk_size": 4096,
  "duplicate_strategy": "manual",
  "error_handling": "retry"
}
```

#### C. Memory-Optimized Processing
```json
{
  "preset": "MEMORY_OPTIMIZED",
  "chunk_size": 4096,
  "batch_size": 50,
  "cache_size": 500,
  "cleanup_scope": "selective"
}
```

#### D. Real-Time Processing
```json
{
  "preset": "REAL_TIME",
  "chunk_size": 8192,
  "batch_size": 25,
  "cache_size": 100,
  "error_handling": "graceful"
}
```

## Code Pointers

### Core Implementation Files
- **GlobalFileManagementService**: `core/infra/src/main/java/com/mira/com/core/infra/GlobalFileManagementService.kt`
- **GlobalFileManagementBackgroundService**: `core/infra/src/main/java/com/mira/com/core/infra/GlobalFileManagementBackgroundService.kt`
- **HashBasedDuplicateDetectionService**: `core/infra/src/main/java/com/mira/com/core/infra/HashBasedDuplicateDetectionService.kt`

### Integration Files
- **MediaConverter**: `feature/whisper/src/main/java/com/mira/com/feature/whisper/data/io/MediaConverter.kt`
- **Orchestrator**: `app/src/main/java/com/mira/clip/ops/Orchestrator.kt`
- **Web Interface**: `app/src/main/assets/web/whisper_unified.html`

### Testing and Verification
- **Verification script**: `ops/verify_infrastructure_consistency.sh`
- **Test utilities**: `scripts/test_infrastructure_consistency.sh`
- **Performance monitoring**: `scripts/monitor_infrastructure_performance.sh`
- **Integration tests**: `test-results/infrastructure_integration_tests/`

### Build Configuration
- **Gradle build**: `core/infra/build.gradle.kts`
- **Dependencies**: Core Android libraries, file system utilities
- **Native libraries**: JNI bindings for performance-critical operations
