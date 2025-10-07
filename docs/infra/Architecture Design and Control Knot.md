# Infrastructure - Architecture Design and Control Knot

**Status: READY FOR VERIFICATION**

## Control Knots

### Global File Management Pipeline
- **Deterministic hashing**: SHA-256 content hashing with 8KB chunks for consistent duplicate detection
- **Fixed cleanup strategy**: Comprehensive cleanup with hash-based duplicate removal
- **Model consistency**: Fixed file management algorithms with deterministic results

### Duplicate Detection System
- **Hash-based detection**: Content-based duplicate identification using SHA-256
- **Filename-based detection**: Pattern matching for converted file detection
- **Removal strategy**: Keep most recent file, remove older duplicates

### Global Cleanup Operations
- **Scope control**: Configurable cleanup across multiple directories
- **Safety mechanisms**: Error handling and graceful degradation
- **Performance monitoring**: Detailed metrics and result tracking

## Implementation

### Deterministic File Management
- SHA-256 content hashing: 8KB chunk-based processing for memory efficiency
- Fixed cleanup strategy: Comprehensive cleanup with hash-based duplicate removal
- Re-ingest verification: Hash comparison for consistency validation

### Core Infrastructure Services
- Fixed service architecture: Consistent service loading and execution
- Deterministic operations: Fixed random seeds for reproducible results
- Memory management: Consistent memory allocation patterns

### Background Processing
- Asynchronous execution: Non-blocking file management operations
- Error recovery: Graceful handling of processing errors
- Performance monitoring: Real-time metrics and result tracking

## Verification

### Hash Comparison Script
- **Location**: `ops/verify_infrastructure_consistency.sh`
- **Purpose**: Verify deterministic processing through hash comparison
- **Scope**: File management, duplicate detection, cleanup operations

### Consistency Tests
- **Re-ingest validation**: Process same files twice, compare SHA-256 hashes
- **Cross-module verification**: Verify consistent results across CLIP and Whisper modules
- **Service validation**: Ensure consistent service loading and execution

### Performance Monitoring
- **Processing time**: Monitor file management and cleanup times
- **Memory usage**: Track memory consumption during operations
- **Space optimization**: Verify space savings from duplicate removal

## Code Pointers

### Core Implementation
- **GlobalFileManagementService**: `core/infra/src/main/java/com/mira/com/core/infra/GlobalFileManagementService.kt`
- **GlobalFileManagementBackgroundService**: `core/infra/src/main/java/com/mira/com/core/infra/GlobalFileManagementBackgroundService.kt`
- **HashBasedDuplicateDetectionService**: `core/infra/src/main/java/com/mira/com/core/infra/HashBasedDuplicateDetectionService.kt`

### Integration Points
- **MediaConverter**: `feature/whisper/src/main/java/com/mira/com/feature/whisper/data/io/MediaConverter.kt`
- **Orchestrator**: `app/src/main/java/com/mira/clip/ops/Orchestrator.kt`
- **Web Interface**: `app/src/main/assets/web/whisper_unified.html`

### Testing Scripts
- **Verification script**: `ops/verify_infrastructure_consistency.sh`
- **Test utilities**: `scripts/test_infrastructure_consistency.sh`
- **Performance monitoring**: `scripts/monitor_infrastructure_performance.sh`

## Integration Points

### CLIP Module Integration
- **File management**: Coordinated file management with CLIP processing
- **Cleanup operations**: Unified cleanup across CLIP and infrastructure
- **Storage optimization**: Efficient storage management for video processing

### Whisper Module Integration
- **Duplicate detection**: Integration with Whisper file processing
- **Original file removal**: Coordinated removal after audio processing
- **Global cleanup**: Unified cleanup operations

### Web Interface Integration
- **Background services**: Non-blocking infrastructure operations
- **Real-time updates**: Progress monitoring and result streaming
- **Error handling**: Graceful error handling and user feedback
