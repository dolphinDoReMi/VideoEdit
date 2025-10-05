# CLIP Documentation

## Architecture Design and Control Knot

**Status: READY FOR VERIFICATION**

**Control knots:**
- Seedless pipeline: deterministic sampling
- Fixed preprocess: no random crop
- Same model assets: fixed hypothesis f_θ

**Implementation:**
- Deterministic sampling: uniform frame timestamps
- Fixed preprocessing: center-crop, no augmentation
- Re-ingest twice: SHA-256 hash comparison

**Verification:** Hash comparison script in `ops/verify_all.sh`

## Full Scale Implementation Details

### Problem Disaggregation
- **Inputs**: Video files (MP4, MOV, AVI), Image sequences
- **Outputs**: CLIP embeddings with temporal alignment
- **Runtime surfaces**: Video decoder → Frame sampler → CLIP encoder → Embedding storage
- **Isolation**: Debug variant uses applicationIdSuffix ".debug" (installs side-by-side)

### Analysis with Trade-offs
- **CLIP Model**: ViT-B/32 (balanced), ViT-L/14 (accuracy), ViT-B/16 (speed). We choose ViT-B/32
- **Frame Sampling**: Uniform (fast), Scene-based (accurate), Adaptive (intelligent). We ship uniform first
- **Embedding Storage**: Float32 (precision), Float16 (memory), Quantized (speed). We support Float32 with Float16 fallback
- **Memory**: Stream processing vs batch processing. We support both; default is stream to avoid OOM

### Design Pipeline
```
Video Input → MediaExtractor → Frame Sampler → CLIP Encoder → Embedding Normalizer → Storage Writer
```

### Key Control-knots (all exposed)
- `CLIP_MODEL` (ViT-B/32 | ViT-L/14 | ViT-B/16)
- `FRAME_RATE` (default 1.0 fps)
- `EMBEDDING_DIM` (512 | 768 | 1024)
- `NORMALIZE_EMBEDDINGS` (true)
- `SAMPLING_STRATEGY` (UNIFORM | SCENE_BASED | ADAPTIVE)
- `BATCH_SIZE` (default 32)
- `OUTPUT_FORMAT` (JSON | BINARY | HDF5)
- `SAVE_METADATA` (true) – video info, frame timestamps, model version

### Isolation & Namespacing
- **Broadcast actions**: `${applicationId}.action.CLIP_PROCESS`
- **Work names**: `${BuildConfig.APPLICATION_ID}::clip::<hash(uri)>`
- **File authorities**: `${applicationId}.clip.files`
- **Debug install**: `applicationIdSuffix ".debug"` → all names differ automatically

### Prioritization & Rationale
- **P0**: MP4/MOV decode, uniform frame sampling, CLIP encoding, embedding storage
- **P1**: Scene-based sampling, multiple CLIP models, batch processing
- **P2**: Adaptive sampling, embedding compression, similarity search

### Workplan to Execute
1. Scaffold CLIP integration + build variants
2. Implement video decoder + frame sampler
3. CLIP encoder + embedding normalizer
4. Storage writers: JSON + binary formats
5. E2E test via ADB broadcast on both variants
6. Bench/verify: embedding quality, processing speed, memory usage

### Scale-out Plan: Control Knots and Impact

#### Single (one per knot)
| Knot | Choice | Rationale (technical • user goals) |
|------|--------|-----------------------------------|
| CLIP Model | ViT-B/32 | Tech: Balanced accuracy/speed; good for most use cases. • User: Reliable results with reasonable processing time |
| Frame Rate | 1.0 fps | Tech: Captures key moments without overwhelming storage. • User: Good temporal coverage for video understanding |
| Embedding Dim | 512 | Tech: Standard CLIP dimension; efficient storage. • User: Compatible with existing similarity search systems |
| Sampling | Uniform | Tech: Predictable, fast processing. • User: Consistent results across different video types |
| Batch Size | 32 | Tech: Good GPU utilization without memory overflow. • User: Efficient processing of video content |
| Output Format | JSON | Tech: Human-readable, easy to debug. • User: Transparent and auditable results |

#### Ablations (combos)
| Combo | Knot changes (vs Single) | Rationale (technical • user goals) |
|-------|-------------------------|-----------------------------------|
| A. High Accuracy | Model: ViT-L/14; Frame Rate: 2.0 fps | Tech: Better visual understanding; more temporal detail. • User: Higher quality embeddings for critical applications |
| B. Fast Processing | Model: ViT-B/16; Batch Size: 64; Frame Rate: 0.5 fps | Tech: Optimized for speed; reduced temporal resolution. • User: Quick processing for real-time applications |
| C. Memory Efficient | Embedding Dim: 256; Output Format: Binary; Batch Size: 16 | Tech: Reduced memory footprint; compressed storage. • User: Suitable for resource-constrained environments |
| D. Scene-Aware | Sampling: Scene-based; Frame Rate: Adaptive | Tech: Intelligent frame selection; variable temporal resolution. • User: Better semantic understanding of video content |

### Configuration Presets

```json
// SINGLE (default)
{
  "preset": "SINGLE",
  "clip_model": "ViT-B/32",
  "frame_rate": 1.0,
  "embedding_dim": 512,
  "normalize_embeddings": true,
  "sampling_strategy": "UNIFORM",
  "batch_size": 32,
  "output_format": "JSON",
  "save_metadata": true
}

// A: HIGH_ACCURACY
{ "preset": "HIGH_ACCURACY", "clip_model": "ViT-L/14", "frame_rate": 2.0 }

// B: FAST_PROCESSING
{ "preset": "FAST_PROCESSING", "clip_model": "ViT-B/16", "batch_size": 64, "frame_rate": 0.5 }

// C: MEMORY_EFFICIENT
{ "preset": "MEMORY_EFFICIENT", "embedding_dim": 256, "output_format": "BINARY", "batch_size": 16 }

// D: SCENE_AWARE
{ "preset": "SCENE_AWARE", "sampling_strategy": "SCENE_BASED", "frame_rate": "ADAPTIVE" }
```

## Device Deployment

### Xiaomi Pad Deployment
- **Target Device**: Xiaomi Pad 6 (Android 13+)
- **Architecture**: ARM64-v8a
- **GPU**: Adreno 650 (supports OpenCL)
- **Testing**: CLIP model loading, frame processing performance

### iPad Deployment
- **Target Device**: iPad Pro (iOS 16+)
- **Architecture**: ARM64
- **GPU**: Apple Silicon (Metal Performance Shaders)
- **Testing**: Core ML integration, Metal shader validation

## Release

### Android Release Pipeline
1. **Model Assets**: CLIP model files bundled in APK
2. **GPU Acceleration**: OpenCL shaders for ARM Mali/Adreno
3. **Testing**: Video processing benchmarks on target devices
4. **Deployment**: APK with embedded CLIP models

### iOS Release Pipeline
1. **Core ML Models**: Converted CLIP models for Apple Silicon
2. **Metal Shaders**: Optimized for A-series chips
3. **Testing**: Performance validation on iPad Pro
4. **Deployment**: App Store with Core ML models

### macOS Web Version
1. **WebAssembly**: CLIP model compiled to WASM
2. **WebGL**: GPU acceleration via WebGL shaders
3. **Testing**: Browser compatibility across platforms
4. **Deployment**: Static hosting with WASM modules

## Scripts

See `scripts/` folder for:
- `test_clip_models.sh` - CLIP model validation
- `test_video_processing.sh` - Video processing pipeline
- `benchmark_embeddings.sh` - Performance benchmarking
- `deploy_clip_models.sh` - Model deployment automation