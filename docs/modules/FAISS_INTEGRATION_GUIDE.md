# FAISS Integration Guide

This document explains how to use the newly implemented FAISS indexing layer in your video editing application.

## Overview

The FAISS integration provides a production-grade vector indexing system that replaces per-video `.f32` artifacts with immutable FAISS index segments. This enables fast approximate nearest neighbor (ANN) search while maintaining clean separation between build (indexing) and query (serving) operations.

## Architecture

### Core Components

```
app/src/main/java/com/mira/clip/index/faiss/
├── FaissConfig.kt               # Control knots (design config)
├── FaissPaths.kt                # Path & atomic publish
├── FaissManifest.kt             # Index manifest/segment metadata
├── FaissBridge.kt               # JNI wrapper
├── FaissSegmentBuildWorker.kt   # Build immutable FAISS segment from embeddings
├── FaissCompactionWorker.kt     # Merge segments → shard (optional)
├── FaissShardedSearch.kt        # Query-time sharded search + topK fuse
├── FaissReceiver.kt             # Broadcast entrypoint
└── FaissBroadcasts.kt           # Broadcast action constants

app/src/debug/java/com/mira/clip/index/faiss/debug/
├── DebugFaissReceiver.kt        # Debug-only receiver
└── FaissSelfTestWorker.kt       # Build→verify→search smoke test

app/src/main/cpp/
├── FaissBridge.cpp              # JNI (C++)
└── CMakeLists.txt               # NDK build; links FAISS
```

### Artifact Layout (Immutable)

```
/sdcard/MiraClip/out/faiss/{variant}/
├── MANIFEST.json
├── segments/
│   ├── seg-<ts>-<count>.faiss
│   └── seg-<ts>-<count>.ids.json
└── shards/                      # (optional, from compaction)
    ├── shard-00001.faiss
    └── shard-00001.ids.json
```

## Configuration

### Index Types

The system supports three FAISS index types:

1. **FlatIP** - Exact search, highest recall, slower
2. **IVF+PQ** - Trained once, fast + small, tunable recall↔speed/size
3. **HNSW** - No training, good recall/latency, memory-heavy

### Control Knots

Configure via `FaissConfig.update()`:

```kotlin
val config = FaissDesignConfig(
    indexType = FaissIndexType.IVF_PQ,
    dim = 512,
    nlist = 4096,
    nprobe = 16,
    pqM = 64,
    pqBits = 8,
    segmentTargetN = 512,
    compactionEnabled = false
)
FaissConfig.update(config)
```

## Usage

### 1. Building Index Segments

From your embedding step, broadcast the build action:

```kotlin
val intent = Intent(FaissBroadcasts.ACTION_FAISS_BUILD_SEGMENT).apply {
    putExtra(FaissBroadcasts.EXTRA_VIDEO_ID, videoId)
    putExtra(FaissBroadcasts.EXTRA_VARIANT, "base")
    putExtra(FaissBroadcasts.EXTRA_EMB_F32_PATH, tmpEmbPath) // N x dim float32, LE
    putExtra(FaissBroadcasts.EXTRA_DIM, dim)                 // e.g., 512
    putExtra(FaissBroadcasts.EXTRA_COUNT, frameCount)        // N
    putExtra(FaissBroadcasts.EXTRA_TS, System.currentTimeMillis())
}
context.sendBroadcast(intent)
```

### 2. Querying the Index

```kotlin
FaissShardedSearch(context, variant = "base").use { search ->
    val query = floatArrayOf(/* your query vector */)
    val results = search.searchTopK(query, k = 10)
    
    results.forEach { (id, score) ->
        // id is the stable 64-bit hash of videoId#frame
        // score is the similarity score
        println("ID: $id, Score: $score")
    }
}
```

### 3. Debug Self-Test

```kotlin
context.sendBroadcast(Intent("com.mira.clip.index.faiss.debug.ACTION_FAISS_SELFTEST"))
```

## Atomic Publishing

The system uses atomic publishing to ensure consistency:

1. Write to `.staging/` directory
2. Sync to disk with `fsync()`
3. Single `rename()` into `segments/` directory
4. Update manifest atomically

## Manifest Management

The `MANIFEST.json` file tracks:

- Schema version and dimensions
- Index type and parameters
- List of active segments
- Training status and provenance

## Pipeline Integration

### Video Ingestion Pipeline

The FAISS indexing is automatically integrated into the video ingestion pipeline:

1. **Frame Sampling**: Videos are sampled using `FrameSamplerCompat.sampleUniform()`
2. **Embedding Generation**: Each frame is embedded using `ClipEngines.embedImage()`
3. **Storage**: Embeddings are stored via `EmbeddingStore.save()` as `.f32` files
4. **FAISS Indexing**: A broadcast triggers `FaissSegmentBuildWorker` to build index segments

```kotlin
// In VideoIngestService.processVideo()
val meta = EmbeddingStore.save(context, videoId, variant, embedding)

// Trigger FAISS indexing
val intent = Intent("com.mira.clip.index.faiss.ACTION_FAISS_BUILD_SEGMENT").apply {
    putExtra("videoId", videoId)
    putExtra("variant", variant)
    putExtra("embF32Path", EmbeddingStore.f32Path(context, videoId).absolutePath)
    putExtra("dim", embedding.size)
    putExtra("count", frames.size)
    putExtra("ts", System.currentTimeMillis())
}
context.sendBroadcast(intent)
```

### Search Integration

To search the FAISS index from your application:

```kotlin
// Search for similar video frames
FaissShardedSearch(context, variant = "base").use { search ->
    val queryEmbedding = ClipEngines.embedImage(context, queryImage)
    val results = search.searchTopK(queryEmbedding, k = 10)
    
    results.forEach { (id, score) ->
        // Decode video ID and frame number from stable hash
        val videoFrame = decodeVideoFrame(id)
        println("Video: ${videoFrame.videoId}, Frame: ${videoFrame.frame}, Score: $score")
    }
}
```

## Performance Tuning

### Recall/Latency/Size Trade-offs

- **IVF+PQ**: Tune `nlist`, `nprobe`, `pqM`, `pqBits`
- **HNSW**: Tune `M`, `efConstruction`, `efSearch`
- **Segment size**: Increase `segmentTargetN` for better query performance

### Throughput Optimization

- Enable compaction to build shards for faster query load
- Use larger segments initially
- Consider pre-trained IVF centroids for large corpora

## Native Library Setup

### Required Files

Place FAISS libraries in:
```
app/src/main/cpp/third_party/faiss/
├── include/faiss/               # FAISS headers
└── lib/arm64-v8a/
    ├── libfaiss.a              # Main FAISS library
    └── libopenblas.a           # BLAS library (optional)
```

### Building FAISS for Android

```bash
# Clone and build FAISS for Android ARM64
git clone https://github.com/facebookresearch/faiss.git
cd faiss

mkdir build-android && cd build-android
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-21 \
  -DCMAKE_BUILD_TYPE=Release \
  -DFAISS_ENABLE_GPU=OFF \
  -DFAISS_ENABLE_PYTHON=OFF \
  -DBUILD_SHARED_LIBS=OFF

make -j$(nproc)
```

## Integration with Existing Pipeline

### Embedding Step Integration

Replace your current `.f32` file writing with:

```kotlin
// After generating embeddings
val tmpEmbFile = File.createTempFile("emb", ".f32", context.cacheDir)
tmpEmbFile.writeBytes(embeddings.toByteArray())

// Broadcast to FAISS
val intent = Intent(FaissBroadcasts.ACTION_FAISS_BUILD_SEGMENT).apply {
    putExtra(FaissBroadcasts.EXTRA_VIDEO_ID, videoId)
    putExtra(FaissBroadcasts.EXTRA_EMB_F32_PATH, tmpEmbFile.absolutePath)
    putExtra(FaissBroadcasts.EXTRA_DIM, embeddingDim)
    putExtra(FaissBroadcasts.EXTRA_COUNT, frameCount)
}
context.sendBroadcast(intent)
```

### Search Integration

Replace your current similarity search with:

```kotlin
// Query with FAISS
FaissShardedSearch(context).use { search ->
    val results = search.searchTopK(queryEmbedding, k = 10)
    // Process results...
}
```

## Debugging

### Debug Build Features

- Debug-only self-test worker
- Debug receiver for testing
- Manifest inspection tools

### Common Issues

1. **Native library not found**: Ensure FAISS libraries are in correct location
2. **Index training failures**: Check training data size and quality
3. **Memory issues**: Reduce segment size or use more efficient index types
4. **Performance problems**: Tune index parameters or enable compaction

## Scale-out Plan

### Growth Levers

- **Recall/Latency/Size**: Adjust index type and parameters
- **Throughput**: Increase segment size, enable compaction
- **Memory**: Use more efficient index types, reduce dimensions
- **Training**: Pre-train IVF centroids offline for large corpora

### Monitoring

- Track segment count and sizes
- Monitor query latency and recall
- Watch memory usage patterns
- Log training success/failure rates

## Security Considerations

- FAISS indexes are stored in external storage
- Consider encryption for sensitive embeddings
- Validate input dimensions and counts
- Sanitize broadcast intents

## Future Enhancements

- GPU acceleration support
- Distributed indexing across devices
- Real-time index updates
- Advanced compaction strategies
- Query result caching
- Index versioning and migration
