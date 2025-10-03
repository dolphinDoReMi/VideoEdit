# Offline Index Build & Online Retrieval System

## Overview

This is a production-style, background-only Android/Kotlin implementation for offline vector index building and online retrieval. The system is manifest-driven, broadcast-triggered, has no UI, and maintains the existing appId (`com.mira.clip`).

## Key Features

- **Background-only operation**: No UI components, runs entirely in background
- **Broadcast-driven**: Triggered via Android broadcast intents
- **Manifest-controlled**: Single JSON configuration file controls all parameters
- **Local isolation**: Separate debug package with `.debug` suffix
- **Control-knots exposed**: All tunable parameters in JSON config
- **AppId preserved**: No changes to existing `com.mira.clip` applicationId

## Architecture

### Core Components

```
app/src/main/java/com/mira/clip/retrieval/
├── Actions.kt                    # Broadcast action constants
├── Config.kt                     # Manifest schema and loading
├── PipelineReceiver.kt           # Broadcast receiver
├── work/
│   ├── IngestWorker.kt          # Video embedding worker
│   └── RetrieveWorker.kt        # Vector search worker
├── index/
│   ├── IndexBackend.kt          # Search interface
│   └── FlatIndexBackend.kt      # Pure Kotlin FLAT cosine search
├── io/
│   ├── EmbeddingStore.kt        # Vector I/O operations
│   └── ResultsWriter.kt         # Search results output
└── util/
    ├── FileIO.kt                # File utilities
    └── Maths.kt                 # Vector math operations
```

### Debug Components

```
app/src/debug/java/com/mira/clip/retrieval/debug/
├── DebugActions.kt              # Debug action constants
└── DebugReceiver.kt             # Debug broadcast receiver
```

## Configuration

### Manifest Schema

The system is controlled by a JSON manifest file with the following structure:

```json
{
  "variant": "clip_vit_b32",
  "frame_count": 32,
  "batch_size": 8,
  "index": {
    "dir": "/sdcard/Mira/index/clip_vit_b32",
    "type": "FLAT",
    "nlist": 4096,
    "pq_m": 16,
    "nprobe": 16
  },
  "ingest": {
    "videos": [
      "/sdcard/Mira/input/video_v1.mp4",
      "/sdcard/Mira/input/video_v2.mp4"
    ],
    "output_dir": "/sdcard/Mira/out/embeddings/clip_vit_b32"
  },
  "query": {
    "query_vec_path": "/sdcard/Mira/query/q1.f32",
    "top_k": 50,
    "output_path": "/sdcard/Mira/out/results/q1.json"
  }
}
```

### Control Parameters

- **variant**: CLIP model variant identifier
- **frame_count**: Number of frames to sample per video
- **batch_size**: Processing batch size for embeddings
- **index.type**: Index backend type (FLAT|FAISS_IVFPQ|HNSW)
- **index.dir**: Directory for index storage
- **query.top_k**: Number of top results to return
- **query.output_path**: Path for search results

## Usage

### Ingest (Build Index)

```bash
adb shell am broadcast \
  -a com.mira.clip.retrieval.ACTION_INGEST \
  --es manifest_path "/sdcard/Mira/manifests/h_clip.json"
```

### Retrieve (Search)

```bash
adb shell am broadcast \
  -a com.mira.clip.retrieval.ACTION_RETRIEVE \
  --es manifest_path "/sdcard/Mira/manifests/h_clip.json"
```

### Debug Actions

```bash
# Debug ingest
adb shell am broadcast \
  -a com.mira.clip.retrieval.debug.ACTION_INGEST \
  --es manifest_path "/sdcard/Mira/manifests/h_clip.json"

# Debug retrieve
adb shell am broadcast \
  -a com.mira.clip.retrieval.debug.ACTION_RETRIEVE \
  --es manifest_path "/sdcard/Mira/manifests/h_clip.json"
```

## File Layout

### Input/Output Structure

```
/sdcard/Mira/
├── manifests/
│   └── h_clip.json              # Configuration manifest
├── input/
│   ├── video_v1.mp4             # Input videos
│   └── video_v2.mp4
├── out/
│   ├── embeddings/
│   │   └── clip_vit_b32/
│   │       ├── video_v1.f32     # Vector embeddings
│   │       ├── video_v1.json    # Metadata
│   │       ├── video_v2.f32
│   │       └── video_v2.json
│   └── results/
│       └── q1.json              # Search results
├── index/
│   └── clip_vit_b32/
│       └── BIND.txt             # Index binding marker
└── query/
    └── q1.f32                   # Query vector
```

## Integration

### CLIP Module Bridge

The system expects a CLIP embedding module with the following interface:

```kotlin
object ClipEngines {
  @JvmStatic
  fun embedVideo(path: String, frameCount: Int, batchSize: Int, variant: String): FloatArray
}
```

If the CLIP module is not available, ingestion will fail (by design) to enforce reproducibility.

### Index Backend Extensibility

The system supports multiple index backends:

- **FLAT**: Pure Kotlin cosine similarity (current implementation)
- **FAISS_IVFPQ**: FAISS IVF-PQ via JNI (future)
- **HNSW**: FAISS HNSW via JNI (future)

## Testing

### Automated Test Script

```bash
./scripts/test/test_retrieval_system.sh
```

### Manual Testing

1. Push manifest to device:
   ```bash
   adb push manifests/h_clip.json /sdcard/Mira/manifests/
   ```

2. Run ingest:
   ```bash
   adb shell am broadcast -a com.mira.clip.retrieval.ACTION_INGEST --es manifest_path "/sdcard/Mira/manifests/h_clip.json"
   ```

3. Run retrieve:
   ```bash
   adb shell am broadcast -a com.mira.clip.retrieval.ACTION_RETRIEVE --es manifest_path "/sdcard/Mira/manifests/h_clip.json"
   ```

4. Check results:
   ```bash
   adb shell ls -la /sdcard/Mira/out/results/
   ```

## Scale-out Plan

### Index Backend Evolution

1. **Phase 1**: FLAT search (current) - always works, no JNI
2. **Phase 2**: FAISS IVF-PQ via JNI - for scale
3. **Phase 3**: FAISS HNSW via JNI - for low-latency RAM

### Performance Optimizations

- **Throughput**: Parallel workers for ingest
- **Memory**: Frame count based on device thermals
- **Storage**: Pre-quantize vectors to FP16 for I/O
- **Sharding**: Per-variant sharded subdirs
- **Caching**: Query result cache for hot prompts

### Observability

- **Ingest speed**: RTF-style metrics (infer_ms / audio_ms analog)
- **Retrieve latency**: P50/P90/P99 measurements
- **Quality**: Recall@k vs offline eval set

## Dependencies

- `androidx.work:work-runtime-ktx:2.9.1` - Background job processing
- `kotlinx-serialization-json:1.6.3` - JSON configuration parsing

## Notes

- The system maintains local isolation and namespacing
- Debug package provides `.debug` suffix without changing appId
- All control-knots are exposed in a single JSON design config
- Ready for FAISS integration via JNI when needed
- Compatible with existing CLIP module architecture
