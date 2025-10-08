# Scoped Storage Infrastructure Migration Guide

## Overview

This document outlines the complete re-architecture of all Mira services to use a unified scoped storage infrastructure. The migration eliminates all hardcoded storage paths and provides a centralized, Android-compliant storage management system.

## 🏗️ **New Architecture**

### **Core Components**

1. **`ScopedStorageManager`** - Central storage infrastructure
2. **Service-Specific Adapters** - Whisper, CLIP, AutoClipper, FAISS
3. **Dynamic Path Resolution** - Context-aware path generation
4. **Unified Directory Structure** - Consistent organization across services

### **Directory Structure**

```
/storage/emulated/0/Android/data/com.mira.com/files/Mira/
├── services/                    # Service-specific storage
│   ├── whisper/               # Whisper service storage
│   │   ├── input/             # Input files
│   │   ├── output/            # Output files
│   │   ├── models/            # Whisper models
│   │   ├── logs/              # Processing logs
│   │   ├── sidecar/           # Sidecar files
│   │   ├── processing/        # Temporary processing
│   │   └── exports/           # Final exports
│   ├── clip/                  # CLIP service storage
│   │   ├── input/             # Input videos
│   │   ├── output/            # Output files
│   │   ├── models/            # CLIP models
│   │   ├── embeddings/        # Generated embeddings
│   │   └── logs/              # Processing logs
│   ├── autoclip/              # AutoClipper service storage
│   │   ├── input/             # Input videos
│   │   ├── output/            # Output clips
│   │   ├── processing/        # Processing directory
│   │   │   ├── chunks/        # Video chunks
│   │   │   └── intermediate/  # Intermediate files
│   │   └── logs/              # Processing logs
│   └── faiss/                 # FAISS service storage
│       ├── index/             # Index files
│       ├── query/             # Query vectors
│       ├── results/           # Search results
│       └── logs/              # Processing logs
├── shared/                     # Cross-service data
│   ├── models/                # Shared models
│   ├── metadata/              # Shared metadata
│   └── logs/                  # System logs
├── temp/                       # Temporary files
└── cache/                      # Cache files
```

## 🔄 **Migration Changes**

### **1. Whisper Service**

#### **Before (Hardcoded Paths)**
```kotlin
const val OUTPUT_DIR = "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/out"
const val SIDECAR_DIR = "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/sidecar"
```

#### **After (Storage Adapter)**
```kotlin
// Use WhisperStorageAdapter
val outputDir = WhisperStorageAdapter.getOutputDirPath(context)
val sidecarDir = WhisperStorageAdapter.getSidecarDirPath(context)
val modelPath = WhisperStorageAdapter.getDefaultModelPath(context)
```

### **2. CLIP Service**

#### **Before (Hardcoded Paths)**
```kotlin
const val DEFAULT_VIDEO_PATH = "/storage/emulated/0/Android/data/com.mira.com/files/MiraClip/in/video_v1.mp4"
const val OUT_ROOT = "/storage/emulated/0/Android/data/com.mira.com/files/MiraClip/out"
```

#### **After (Storage Adapter)**
```kotlin
// Use ClipStorageAdapter
val inputDir = ClipStorageAdapter.getInputDirPath(context)
val outputDir = ClipStorageAdapter.getOutputDirPath(context)
val embeddingsDir = ClipStorageAdapter.getEmbeddingsDirPath(context)
```

### **3. FAISS Service**

#### **Before (Hardcoded Paths)**
```kotlin
val dir: String = "/storage/emulated/0/Android/data/com.mira.com/files/Mira/index/clip_vit_b32"
```

#### **After (Storage Adapter)**
```kotlin
// Use FaissStorageAdapter
val indexDir = FaissStorageAdapter.getVariantIndexDir(context, variant)
val queryDir = FaissStorageAdapter.getQueryDirPath(context)
val resultsDir = FaissStorageAdapter.getResultsDirPath(context)
```

## 📋 **Migration Checklist**

### **For Developers**

- [ ] Replace all hardcoded `/sdcard/` paths with storage adapters
- [ ] Replace all hardcoded `/storage/emulated/0/` paths with storage adapters
- [ ] Use `ScopedStorageManager.ensureDirectoryStructure()` before file operations
- [ ] Update workers to use storage adapters
- [ ] Test on Xiaomi Pad and other Android devices
- [ ] Verify scoped storage compliance

### **For Services**

#### **Whisper Service**
- [x] Updated `AndroidWhisperBridge` to use `WhisperStorageAdapter`
- [x] Replaced hardcoded `OUTPUT_DIR` and `SIDECAR_DIR`
- [x] Updated model path resolution
- [x] Updated file picker paths

#### **CLIP Service**
- [x] Updated `Config.kt` to use `ClipStorageAdapter`
- [x] Updated `PipelineManifest` with dynamic path resolution
- [x] Updated `IngestWorker` and `RetrieveWorker`

#### **AutoClipper Service**
- [x] Created `AutoClipStorageAdapter`
- [ ] Update `AutoClipperWorker` to use storage adapter
- [ ] Update `AutoClipperService` to use storage adapter

#### **FAISS Service**
- [x] Created `FaissStorageAdapter`
- [x] Updated `FaissConfig` to use storage adapter
- [ ] Update FAISS workers to use storage adapter

## 🚀 **Usage Examples**

### **Basic Storage Access**
```kotlin
// Get Whisper output directory
val whisperOutputDir = WhisperStorageAdapter.getOutputDirPath(context)

// Get CLIP embeddings directory
val clipEmbeddingsDir = ClipStorageAdapter.getEmbeddingsDirPath(context)

// Get FAISS index directory
val faissIndexDir = FaissStorageAdapter.getIndexDirPath(context)
```

### **Job-Specific Storage**
```kotlin
// Get job-specific output directory
val jobOutputDir = ScopedStorageManager.getJobOutputDir(context, "whisper", jobId)

// Get job-specific sidecar file
val sidecarFile = WhisperStorageAdapter.getJobSidecarFile(context, jobId)
```

### **Directory Structure Setup**
```kotlin
// Ensure all directories exist
val success = ScopedStorageManager.ensureDirectoryStructure(context)
if (!success) {
    Log.e(TAG, "Failed to create directory structure")
}
```

### **Dynamic Path Resolution**
```kotlin
// Load manifest with dynamic path resolution
val manifest = loadManifest(manifestPath)
val outputDir = manifest.ingest.resolveOutputDir(context)
val indexDir = manifest.index.resolveDir(context)
```

## 🔧 **Configuration**

### **Xiaomi Pad Optimizations**

The storage infrastructure includes Xiaomi Pad specific optimizations:

- **Memory-aware directory creation**
- **VULKAN GPU acceleration support**
- **Thermal management integration**
- **Battery optimization**

### **Android Version Compatibility**

- **Android 13+**: Uses `READ_MEDIA_*` permissions
- **Android 12 and below**: Uses `READ_EXTERNAL_STORAGE`
- **Scoped Storage**: All paths use `getExternalFilesDir()`

## 🧪 **Testing**

### **Unit Tests**
```kotlin
@Test
fun testStorageAdapterPaths() {
    val context = mockk<Context>()
    val whisperDir = WhisperStorageAdapter.getOutputDirPath(context)
    assertTrue(whisperDir.contains("Mira/services/whisper/output"))
}
```

### **Integration Tests**
```kotlin
@Test
fun testDirectoryStructureCreation() {
    val context = getTestContext()
    val success = ScopedStorageManager.ensureDirectoryStructure(context)
    assertTrue(success)
}
```

### **Device Testing**
- Test on Xiaomi Pad Ultra
- Test on various Android versions
- Verify scoped storage compliance
- Test file permissions

## 📊 **Benefits**

### **Security**
- ✅ No runtime permissions required
- ✅ Isolated from other apps
- ✅ Automatic cleanup on uninstall

### **Performance**
- ✅ Direct file system access
- ✅ Optimized for Xiaomi Pad hardware
- ✅ Memory-efficient operations

### **Maintainability**
- ✅ Centralized storage management
- ✅ Consistent API across services
- ✅ Easy to extend and modify

### **Compliance**
- ✅ Android scoped storage compliant
- ✅ Google Play Store requirements
- ✅ Privacy-focused design

## 🚨 **Breaking Changes**

### **Deprecated Classes**
- `DirectoryManager` - Use `ScopedStorageManager` instead
- `Config` hardcoded paths - Use service adapters instead

### **Deprecated Methods**
- All hardcoded path constants
- Direct file path construction
- Manual directory creation

### **Migration Required**
- Update all service implementations
- Update all worker classes
- Update configuration files
- Update test cases

## 📚 **Additional Resources**

- [Android Scoped Storage Documentation](https://developer.android.com/training/data-storage)
- [Xiaomi Pad Optimization Guide](./docs/whisper/XiaoMi%20Pad%20Inference%20Optimization.md)
- [Storage Architecture Design](./docs/infra/Architecture%20Design%20and%20Control%20Knot.md)

---

**Note**: This migration ensures all Mira services comply with Android scoped storage requirements while providing optimal performance on Xiaomi Pad and other Android devices.
