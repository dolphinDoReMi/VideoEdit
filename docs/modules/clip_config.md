# CLIP Feature — Configuration SSoT

This document serves as the single source of truth for CLIP feature configuration, including CI/CD, package layout, build/app IDs, feature flags, and broadcast wiring (including the debug-only K/L/M verifiers).

## 1) CI/CD Pipeline (GitHub Actions)

### Workflows
- **android-ci.yml** — build, unit tests, lint on push/PR (branches: main, develop)
- **automated-testing.yml** — nightly device/emulator sanity (K/L/M) + artifact upload
- **release-deployment.yml** — tagged build → Release AAB + (optionally) Firebase App Distribution

### Key steps
- **Gradle**: assembleDebug, bundleRelease, lint, testDebugUnitTest
- **(Nightly) Device sanity**:
  - install debug APK
  - run ADB broadcasts:
    - CLIP ingest → embeddings
    - K/L/M verifiers (debug-only)
  - adb pull /sdcard/MiraClip/out/debug/sanity → upload as artifacts

### Secrets / env
- FIREBASE_TOKEN, FIREBASE_APP_ID_* (per variant)
- ANDROID_SIGNING_* (release only)

## 2) Package & Source Sets

```
app/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/mira/com/feature/clip/
│   │       ├── VideoFrameExtractor.kt
│   │       ├── ClipPreprocess.kt
│   │       ├── ClipEngine.kt
│   │       ├── ClipRunner.kt
│   │       └── ClipReceiver.kt     # ${applicationId}.CLIP.RUN (not exported)
│   └── debug/
│       ├── AndroidManifest.xml     # adds non-exported debug receivers only
│       └── java/com/mira/clip/debug/
│           ├── DebugConfig.kt
│           ├── SanityActions.kt    # K/L/M action names
│           ├── SanityReceiver.kt   # routes to WorkManager
│           ├── io/F32.kt
│           ├── util/{Vec,HashUtils,Json}.kt
│           └── work/
│               ├── VerifyRepresentationWorker.kt  # K
│               ├── VerifyRetrievalWorker.kt       # L
│               └── VerifyReproWorker.kt           # M
```

- **Namespace (main)**: com.mira.com.feature.clip
- **Debug utilities** live under com.mira.clip.debug (debug sourceSet only).

## 3) Build Config / App IDs

### App ID (frozen): com.mira.com

### Variants

| Variant | applicationId | Broadcast prefix ${applicationId} |
|---------|---------------|-----------------------------------|
| debug | com.mira.com.debug | com.mira.com.debug |
| internal | com.mira.com.internal | com.mira.com.internal |
| release | com.mira.com | com.mira.com |

### Android SDK
- minSdk: 26 targetSdk: 34

### Deps (minimum)
- WorkManager 2.9.0 (debug sanity workers)
- JSON (org.json) – already on Android
- (If used) PyTorch Mobile 1.13.1 for on-device inference

## 4) Feature Flags (BuildConfig + Runtime Config)

### BuildConfig (module)

```kotlin
// build.gradle.kts -> buildConfigField(...) equivalents
DEFAULT_FRAME_COUNT = 32
DEFAULT_SCHEDULE = "UNIFORM"
DEFAULT_DECODE_BACKEND = "MMR"
DEFAULT_MEMORY_BUDGET_MB = 512
```

### Runtime constants (core/infra/Config.kt)

```kotlin
CLIP_RES = 224
CLIP_FRAME_COUNT = 32
CLIP_SCHEDULE = "uniform"
CLIP_TEXT_MAX_TOKENS = 77
RETR_USE_L2_NORM = true
RETR_SIMILARITY = "cosine"
RETR_STORAGE_FMT = ".f32"
RETR_ENABLE_ANN = false
```

### Debug-only design knots (DebugConfig.kt)

```kotlin
allowedDims = {512, 768}    // K
normTolerance = 1e-2        // K
defaultTopK = 5             // L
indexRoot = "/sdcard/MiraClip/out/embeddings"
debugOut = "/sdcard/MiraClip/out/debug/sanity"
autoFix = false             // K: rewrite normalized vectors if true
```

## 5) Broadcast Settings

### A. CLIP Run (main feature)
- **Action**: ${applicationId}.CLIP.RUN (non-exported)
- **Receiver**: ClipReceiver
- **Extras**

| Extra | Type | Example / Default |
|-------|------|-------------------|
| input | str | file:///sdcard/Mira/video_v1.mp4 |
| outdir | str | /sdcard/MiraClip/out |
| variant | str | ViT-B_32 |
| frame_count | int | 32 |

- **ADB examples**

```bash
# debug
adb shell am broadcast -a com.mira.com.debug.CLIP.RUN \
  --es input "/sdcard/Mira/video_v1.mp4" \
  --es outdir "/sdcard/MiraClip/out" \
  --es variant "ViT-B_32" \
  --ei frame_count 32

# release
adb shell am broadcast -a com.mira.com.CLIP.RUN \
  --es input "/sdcard/Mira/video_v1.mp4" \
  --es outdir "/sdcard/MiraClip/out" \
  --es variant "ViT-B_32" \
  --ei frame_count 32
```

**Outputs**
- Binary vectors: /sdcard/MiraClip/out/embeddings/{variant}/{videoId}.f32 (LE float32)
- JSON sidecar: {videoId}.json (timestamps, dim, SHA-256, variant)

### B. Debug-only sanity (K/L/M)
- **Receiver**: SanityReceiver (debug sourceSet, android:exported="false")

**Actions**

| Purpose | Action string |
|---------|---------------|
| K — Representation sanity | com.mira.clip.debug.VERIFY_REPRESENTATION |
| L — Retrieval sanity | com.mira.clip.debug.VERIFY_RETRIEVAL |
| M — Reproducibility | com.mira.clip.debug.VERIFY_REPRODUCIBILITY |

**Extras**
- **K**: emb_dir (default ${indexRoot}/{variant}), out_dir
- **L**: index_dir, query_dir or query_file, top_k (default 5), out_dir
- **M**: path_a, path_b (optional), out_dir

**ADB (quick runbook)**

```bash
# K) Representation: dims {512,768}, unit-norm ±1e-2
adb shell am broadcast -a com.mira.clip.debug.VERIFY_REPRESENTATION \
  --es emb_dir "/sdcard/MiraClip/out/embeddings/vit_b32" \
  --es out_dir "/sdcard/MiraClip/out/debug/sanity"

# L) Retrieval: finite, sorted top-K cosine on unit vectors
adb shell am broadcast -a com.mira.clip.debug.VERIFY_RETRIEVAL \
  --es index_dir "/sdcard/MiraClip/out/embeddings/vit_b32" \
  --es query_dir "/sdcard/MiraClip/out/queries/vit_b32" \
  --ei top_k 5 \
  --es out_dir "/sdcard/MiraClip/out/debug/sanity"

# M) Repro: SHA-256 tree compare between two runs
adb shell am broadcast -a com.mira.clip.debug.VERIFY_REPRODUCIBILITY \
  --es path_a "/sdcard/MiraClip/out/embeddings/vit_b32_run1" \
  --es path_b "/sdcard/MiraClip/out/embeddings/vit_b32_run2" \
  --es out_dir "/sdcard/MiraClip/out/debug/sanity"

# Pull reports for CI artifacts
adb pull /sdcard/MiraClip/out/debug/sanity ./sanity_reports
```

**Reports (JSON)**
- rep_sanity_report.json (K) — per-file dim/norm + summary
- retrieval_results.json + retrieval_sanity_report.json (L)
- repro_report.json (M) — identical flag, per-file hash diffs

## 6) Storage & Formats
- **Root (embeddings)**: /sdcard/MiraClip/out/embeddings/{variant}/
- **Vector format**: .f32 (little-endian, contiguous Float32)
- **Metadata sidecar**: .json with:
  - source_path, dim, frames, timestamp
  - sha256 (optional), variant

## 7) Operational Invariants (quick checklist)
- App ID stays frozen: com.mira.com (variant suffixes via Gradle only)
- Receivers: all non-exported
- Retrieval math: L2-normalize → cosine (dot on unit sphere)
- Dims: {512, 768} enforced for CLIP variants
- Determinism for M: uniform frame sampling, fixed center-crop, same model assets
- CI nightly: run K/L/M and archive JSON under sanity_reports/

## 8) Optional hardening (next)
- Add R8/Proguard keeps for debug classes (to avoid stripping in debug)
- Add metric@K / nDCG calculators (L) and ANN flag-gated backends
- Tolerance-based compare (M) for cross-ABI runs when bit-identity is not required

## 9) Verification Pipeline

Use `ops/verify_all.sh` to run the complete K→L→M verification chain:

```bash
# Set environment variables
export PKG=com.mira.com.debug  # or com.mira.com for release
export ROOT=/sdcard/MiraClip
export VARIANT=ViT-B_32
export VIDEO=/sdcard/Mira/video_v1.mp4

# Run complete verification
bash ops/verify_all.sh
```

The script will:
1. Test receiver resolution and isolation
2. Validate text encoder (E) - dimension and L2 normalization
3. Validate video encoder (F) - frame count, dimension, normalization
4. Test ingestion pipeline (H/J)
5. Verify embedding invariants (K) - representation sanity
6. Test retrieval pipeline (L) - finite scores, sorted descending
7. Validate reproducibility (M) - bit-identical outputs

## 10) Current Implementation Status

### ✅ Implemented
- Main CLIP receiver with ${applicationId}.CLIP.RUN broadcast
- Build variants with correct applicationId suffixes
- Core configuration constants in Config.kt
- Basic verification pipeline in ops/verify_all.sh
- Storage format (.f32) and metadata (.json)

### 🔄 Needs Implementation
- Debug-only K/L/M receivers and workers
- SanityReceiver for debug broadcasts
- DebugConfig.kt with design knots
- Complete debug manifest configuration
- CI/CD workflows for automated testing

### 📋 Next Steps
1. Implement debug-only receivers and workers
2. Add debug manifest entries
3. Create GitHub Actions workflows
4. Add Proguard keeps for debug classes
5. Implement tolerance-based comparison for cross-ABI runs
