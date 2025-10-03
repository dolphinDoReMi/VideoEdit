# CLIP4Clip Control Knots Reference

## Research-Oriented Experimental Control

This document defines the **control knots** - the tunable decisions that change behavior in the CLIP4Clip pipeline. Each knot represents a research variable that can be adjusted to test hypotheses about video-text retrieval.

## Control Knots Map

| Layer | Knot | Default | Effect if Changed | Verification |
|-------|------|---------|-------------------|--------------|
| **Representation** | Variant (dim) | `clip_vit_b32_mean_v1` (512-D) | Changes accuracy/latency; scripts accept 512/768 | Dimension check |
| **Temporal** | frame_count | 32 | ↑ frames → ↑ quality & latency; recorded in meta | Frame count metadata |
| **Temporal** | sampling | uniform | Can switch to shot-based for motion-aware summaries | Sampling strategy |
| **Preprocess** | crop/resize | center-crop 224 | Must match CLIP training; drift hurts retrieval | Preprocessing validation |
| **Text** | max_tokens | 77 | Truncation vs expressivity; keep consistent | Token count |
| **Retrieval** | top_k | 5/10 | Affects result list length only | Result count |
| **Math** | normalize | L2 on | Must be on for cosine to be meaningful | Norm ≈ 1.0 |
| **Storage** | index_dir | `/sdcard/MiraClip/out/embeddings` | Keeps indexes segregated per experiment | File existence |
| **Orchestration** | actions | `com.mira.clip.*` | Namespacing; don't collide with other apps | Broadcast resolution |
| **Performance** | batch_size | 8 | Throughput vs memory; adjust to device | Timing logs |

## Experimental Protocol

### A) Isolation & Namespacing = Experimental Control
**Literature lens:** In CBVR/CLIP pipelines, reproducibility depends on controlling the environment. New app id + storage root = new "experiment sandbox," avoiding hidden state carryover.

**Control knots:**
- `applicationId` (com.mira.clip)
- Storage root: `/sdcard/MiraClip`
- Broadcast actions: `com.mira.clip.*`

**Verify:** `ops/verify_A_isolation.sh`

### B) Minimal Dependency Surface = Fewer Confounds
**Literature lens:** Cut anything not essential to the hypothesis (CLIP encoders + deterministic sampling). Smaller graphs reduce non-determinism.

**Control knots:**
- TorchScript lite (+ torchvision for preprocessing tensor shapes)
- No Room/UI deps (file index is enough)

**Verify:** `./gradlew :app:compileDebugKotlin`

### C) Fixed Assets (Checkpoints & BPE) = Fixed Hypothesis f_θ
**Literature lens:** CLIP (image & text) checkpoints + BPE define your representation function. Changing them shifts the semantic space.

**Control knots:**
- Model family: ViT-B/32 (512-D) vs ViT-L/14 (768-D) → variant string
- Tokenizer files: exact merges/vocab

**Verify:** Embedding dimension + norm (Step K)

### D) Metric Space & Serialization = Valid Cosine & Auditable Bytes
**Literature lens:** Cosine similarity assumes unit-norm vectors. Float32 little-endian binary is the most portable, audit-friendly format.

**Control knots:**
- L2 normalization: on (required)
- Storage format: .f32 (LE) + JSON sidecar
- Similarity: cosine

**Verify:** Covered by Step K vector checks

### E) Representation Learning (Preprocess + Encoders)
**Literature lens:** CLIP provides a shared latent space for images and text; cosine approximates semantic alignment. Preprocess choices (crop/resize/mean/std) must match training.

**Control knots:**
- Preprocess: center-crop to 224, CLIP mean/std, CHW
- Text max length: 77 tokens with EOT pad
- Temporal aggregation: mean pooling of frame embeddings (late fusion baseline)
- Batch size on device (e.g., 8)

**Verify:** `ops/verify_E_text_encoder.sh`

### F) Temporal Sampling = Video → Fixed-Length Representation
**Literature lens:** Videos are variable length; retrieval wants fixed-length vectors. Uniform sampling is a simple, unbiased temporal strategy; mean-pooling approximates late fusion.

**Control knots:**
- frame_count (e.g., 32)
- Sampling schedule: uniform (baseline), (future: shot-based, stride, scene-aware)

**Verify:** `ops/verify_F_video_encoder.sh`

### G) Indexing as Artifacts = IR Separation (Build vs Query)
**Literature lens:** Separate index construction (offline) from retrieval (online). Persisted vectors enable audits, hashing, sharing.

**Control knots:**
- Index root: `/sdcard/MiraClip/out/embeddings/{variant}/`
- File format: .f32 + .json meta (source path, dim, frames)

**Verify:** Existence, dim, norm, and meta checks (Step K)

### H) Offline Index Build & Online Retrieval = Classic Pipeline
**Literature lens:** CBVR: build embeddings offline; at query time, embed text and score via cosine; return top-K.

**Control knots:**
- variant, frame_count, batch_size (build)
- top_k (query), index dir, output path

**Verify:** Runs via broadcasts; validated by Steps K–L

### I) Orchestration = Non-Interactive Reproducibility
**Literature lens:** Pipelines should be invokable without UI: think Airflow/cron. Deterministic runs use configuration-as-data (manifests) + a command channel.

**Control knots:**
- Broadcast actions & payloads
- Manifest paths

**Verify:** Check broadcasts resolve

### J) Manifests = Protocol of the Experiment
**Literature lens:** A manifest is your experimental protocol: the ground truth of what ran and how. It's diffable.

**Control knots:**
- Ingest: {variant, frame_count, videos[], output_dir}
- Search: {variant, queries[], top_k, index_dir, output_path}
- Default path → `/sdcard/Movies/video_v1.mp4` if omitted

**Verify:** Syntax check with jq; logical check via downstream scripts

### K) Representation Sanity = Embedding Invariants
**Literature lens:** Before metrics like mAP/nDCG, verify invariants: correct dimension and unit norm. If these fail, ranking is meaningless.

**Control knots:**
- Allowed dims: {512, 768}
- Norm tolerance: ±1e-2

**Verify:** Vector dimension and L2 norm validation

### L) Retrieval Sanity = Scores Well-Formed
**Literature lens:** Basic IR sanity: scores must be finite and sorted descending. This catches NaN/Inf and sort bugs before real eval.

**Control knots:**
- top_k
- Cosine (no temperature scaling baseline)

**Verify:** Finite scores, sorted descending

### M) Reproducibility = Bit-Identical Index
**Literature lens:** Same inputs + same code → the same bytes. This closes the loop on hidden randomness (sampling, threading, numeric drift).

**Control knots:**
- Seedless pipeline (deterministic sampling)
- Fixed preprocess (no random crop)
- Same model assets

**Verify:** SHA-256 hash comparison

### N) Performance Envelope (optional gate)
**Literature lens:** Models are useful only if they meet latency/throughput. Lightweight gates (stage timings) help keep budgets in check.

**Control knots:**
- batch_size, frame_count
- Device class (Pad SoC)
- Optional: log timings per stage

**Verify:** Optional logcat parser

## Quick Verification Commands

```bash
# Set defaults
export PKG=com.mira.clip
export ROOT=/sdcard/MiraClip
export VARIANT=clip_vit_b32_mean_v1
export VIDEO=/sdcard/Movies/video_v1.mp4

# Run individual verifications
bash ops/verify_A_isolation.sh
bash ops/verify_E_text_encoder.sh
bash ops/verify_F_video_encoder.sh

# Run complete verification pipeline
bash ops/verify_all.sh
```

## Research Framing Summary

- **Isolation** → experimental control
- **Fixed assets** → fixed hypothesis f_θ
- **Preprocess+Tokenizer** → correct input space
- **Temporal sampling+Pooling** → video → fixed representation (late fusion baseline)
- **File index** → IR separation (build vs query)
- **Cosine+L2** → valid metric space
- **Sanity gates** (dim, norm, finite/sorted) → pre-metric validity
- **Bit-identical** → reproducibility
- **Broadcasts+Manifests** → configuration-as-data, non-interactive labs
