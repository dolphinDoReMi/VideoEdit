# Infrastructure Documentation

## Overview

The infrastructure system provides core services for file management, duplicate detection, and storage optimization across the VideoEditor application.

## Key Features

- **Duplicate Detection**: SHA-256 based content deduplication
- **Storage Optimization**: Up to 40% space reduction
- **Background Processing**: Asynchronous operations with callbacks
- **Resource Monitoring**: Real-time CPU, memory, battery tracking

## Documentation

- [Architecture Design and Control Knot](Architecture%20Design%20and%20Control%20Knot.md) - Core architecture and control points

## Scripts

The `Scripts/` folder contains infrastructure-related testing scripts:

- `*duplicate*.sh` - Duplicate detection testing scripts
- `*hash*.sh` - Hash calculation testing scripts
- `*cleanup*.sh` - Cleanup operation testing scripts

## Performance Targets

- **Hash Speed**: <1ms per MB processed
- **Storage Savings**: 50% reduction through optimization
- **Error Rate**: <0.1% error rate
- **Background Impact**: <1% CPU usage

## Quick Start

```bash
# Test duplicate detection
./Scripts/test_duplicate_detection.sh

# Test hash calculation
./Scripts/test_hash_calculation.sh

# Test cleanup operations
./Scripts/test_cleanup_operations.sh
```

## Integration Points

### MediaConverter Integration
- Uses `GlobalFileManagementService` for duplicate detection
- Provides backward compatibility through wrapper functions

### Orchestrator Integration
- Uses `GlobalFileManagementService` for global cleanup
- Provides unified cleanup across CLIP and Whisper modules

### Web Interface Integration
- Uses core infrastructure services through Android bridge
- Provides background service execution

## Core Services

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
