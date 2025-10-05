# CLIP Architecture Design and Control Knot

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

## CLIP-Specific Control Knots

**Video Processing Control Knots:**
- Frame sampling rate: uniform 1.0 fps (configurable)
- CLIP model: ViT-B/32 (balanced accuracy/speed)
- Embedding dimension: 512 (standard CLIP)
- Normalization: L2 normalization enabled
- Batch processing: 32 frames per batch

**Quality Control Knots:**
- Model versioning: SHA-256 hash of CLIP model weights
- Frame validation: Resolution and format checks
- Embedding validation: Dimension and range verification
- Storage integrity: JSON + binary format with checksums

**Performance Control Knots:**
- GPU acceleration: OpenCL for ARM Mali/Adreno, Metal for Apple Silicon
- Memory management: Stream processing to avoid OOM
- Cache strategy: Intelligent frame caching
- Threading: Configurable worker threads

**Verification Methods:**
- Model hash verification: `verify_clip_model.sh`
- Frame sampling validation: `test_frame_sampling.sh`
- Embedding quality check: `validate_embeddings.sh`
- Performance benchmarks: `benchmark_clip_processing.sh`