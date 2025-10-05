# Whisper ASR Documentation

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
- **Inputs**: WAV/PCM16 files, MP4/AAC (no FFmpeg)
- **Outputs**: mono 16 kHz PCM16 (+ JSON sidecar) with stable PTS mapping
- **Runtime surfaces**: Broadcast → WorkManager job → Decode pipeline → Storage writer
- **Isolation**: Debug variant uses applicationIdSuffix ".debug" (installs side-by-side)

### Analysis with Trade-offs
- **MediaCodec vs FFmpeg**: Native, HW-assisted, small footprint vs broader codec coverage. We choose MediaCodec (no FFmpeg)
- **Resampler**: Windowed-sinc (best), polyphase (great), linear (good & simple). We ship linear first (fast, no deps)
- **PTS**: Extract extractor PTS; for decoded PCM we track sample index → PTS affine map (monotonic fallback)
- **Memory**: Stream to disk (.pcm) vs RAM. We support both; default is stream to disk to avoid OOM

### Design Pipeline
```
Broadcast (ACTION_DECODE_URI) → WorkManager DecodeWorker → DecodePipeline.decodeUriToMono16k()
→ (WavReader | AacDecoder) → Normalizer (downmix+resample) → PcmWriter (.pcm + .json)
```

### Key Control-knots (all exposed)
- `TARGET_SR` (default 16_000)
- `TARGET_CH` (default 1/mono)
- `PCM_FORMAT` (PCM_16)
- `RESAMPLER` (LINEAR | SINC)
- `DOWNMIX` (AVERAGE | LEFT | RIGHT)
- `DECODE_BUFFER_MS` (e.g., 250)
- `TIMESTAMP_POLICY` (ExtractorPTS | Monotonic)
- `OUTPUT_MODE` (FILE | RAM)
- `SAVE_SIDE_CAR` (true) – codec, bit-rate, in/out SR/CH, length, SHA256 of PCM

### Isolation & Namespacing
- **Broadcast actions**: `${applicationId}.action.DECODE_URI`
- **Work names**: `${BuildConfig.APPLICATION_ID}::decode::<hash(uri)>`
- **File authorities**: `${applicationId}.files`
- **Debug install**: `applicationIdSuffix ".debug"` → all names differ automatically

### Prioritization & Rationale
- **P0**: WAV/AAC decode, downmix, resample, sidecar, namespaced broadcasts & jobs
- **P1**: Opus/MP3 support (MediaCodec), streaming mic via AudioRecord, progress callbacks
- **P2**: Sinc resampler, on-device health metrics, codec-simulation augment toggles

### Workplan to Execute
1. Scaffold project + build variants (keep applicationId intact; add .debug)
2. Implement I/O (WAV reader, AAC decoder) + Normalizer (downmix/resample)
3. Pipeline + WorkManager + BroadcastReceiver with `${applicationId}` actions
4. Writers: .pcm (LE int16) + .json sidecar (audit)
5. E2E test via ADB broadcast on both release and debug APKs (install side-by-side)
6. Bench/verify: sample-accurate length, SR/CH post-conditions, monotonic PTS, SHA256

### Scale-out Plan: Control Knots and Impact

#### Single (one per knot)
| Knot | Choice | Rationale (technical • user goals) |
|------|--------|-----------------------------------|
| DECODE path | MP4/AAC — HW-first, SW fallback | Tech: Max native support, smallest surface, no FFmpeg. • User: Works everywhere; fewer surprises in demos |
| DECODE_BUFFER_MS | 160 ms buffer / 10 ms hop | Tech: Stable streaming, minimal underruns; ASR-friendly hop. • User: Smooth live capture with acceptable latency |
| RESAMPLER | Linear (baseline) | Tech: Fastest; protects RTF headroom. • User: Longer sessions on mobile; consistent performance |
| TIMESTAMP_POLICY | Hybrid (PTS + sample-count) with drift in sidecar | Tech: Sample-accurate CTM; drift visibility. • User: Trustworthy timestamps for search/quotes |
| Throughput | Stream .pcm (500 ms), JSON=128, WM=Balanced | Tech: Good I/O amortization; bounded queues. • User: No stalls; predictable background runs |
| Observability | On @2s, level=info → file | Tech: Low overhead counters (RTF, fps, drift). • User: Actionable logs without battery hit |

#### Ablations (combos)
| Combo | Knot changes (vs Single) | Rationale (technical • user goals) |
|-------|-------------------------|-----------------------------------|
| A. Low-latency Mic | Buffer 120/10; Throughput: JSON=64, WM=Throughput | Tech: Cut e2e latency; faster flush path. • User: Snappier live captions; trade a bit of stability |
| B. Accuracy-leaning | Resampler: Polyphase/Sinc (balanced); TS: Hybrid (same) | Tech: Better anti-aliasing → small WER gain; timestamps already robust. • User: Cleaner transcripts for export/QA |
| C. Web-robust Ingest | Decode: +WebM/Opus + MP3 (HW-first, SW fallback) | Tech: Wide format coverage; SW only when HW absent. • User: "It just opens" for web downloads/voice notes |
| D. High-throughput Batch | Throughput: .pcm 300 ms, JSON=64, WM=Throughput; Obs: debug@1s (time-boxed) | Tech: Higher ingest rate, faster drains; temporary deep metrics. • User: Faster backfills; short profiling bursts |

### Configuration Presets

```json
// SINGLE (default)
{
  "preset": "SINGLE",
  "decode": "mp4_aac_hw_first",
  "mic": { "buffer_ms": 160, "hop_ms": 10 },
  "resampler": "linear",
  "timestamp_policy": "hybrid_pts_sample",
  "io": { "pcm_chunk_ms": 500, "json_chunk": 128, "wm_profile": "balanced" },
  "obs": { "enable": true, "period_ms": 2000, "level": "info", "backend": "file" }
}

// A: LOW_LATENCY_MIC
{ "preset": "LOW_LATENCY_MIC", "mic": { "buffer_ms": 120 }, "io": { "json_chunk": 64, "wm_profile": "throughput" } }

// B: ACCURACY_LEANING
{ "preset": "ACCURACY_LEANING", "resampler": "polyphase_sinc" }

// C: WEB_ROBUST_INGEST
{ "preset": "WEB_ROBUST_INGEST", "decode": "mp4_aac_plus_webm_opus_mp3_hw_first_sw_fallback" }

// D: HIGH_THROUGHPUT_BATCH
{ "preset": "HIGH_THROUGHPUT_BATCH", "io": { "pcm_chunk_ms": 300, "json_chunk": 64, "wm_profile": "throughput" }, "obs": { "period_ms": 1000, "level": "debug" } }
```

## Device Deployment

### Xiaomi Pad Deployment
- **Target Device**: Xiaomi Pad 6 (Android 13+)
- **Architecture**: ARM64-v8a
- **Permissions**: RECORD_AUDIO, FOREGROUND_SERVICE_DATA_SYNC, POST_NOTIFICATIONS
- **Testing**: Real-time resource monitoring, background service validation

### iPad Deployment  
- **Target Device**: iPad Pro (iOS 16+)
- **Architecture**: ARM64
- **Capabilities**: Background processing, Core Audio integration
- **Testing**: Cross-platform compatibility validation

## CI/CD Guide & Release

### Android Release Pipeline
1. **Build Variants**: Release and Debug (side-by-side installation)
2. **Signing**: Debug keystore for development, release keystore for production
3. **Testing**: Automated E2E tests via ADB broadcast
4. **Deployment**: APK distribution via internal channels

### iOS Release Pipeline
1. **Build Configuration**: Debug and Release schemes
2. **Code Signing**: Development and Distribution certificates
3. **Testing**: XCTest automation for core functionality
4. **Deployment**: TestFlight for beta, App Store for production

### macOS Web Version
1. **Build Target**: WebAssembly compilation
2. **Testing**: Browser compatibility testing
3. **Deployment**: Static hosting with CDN distribution

## README.md

### For Content Understanding Experts
- **Primitive Output**: `{t0Ms, t1Ms, text}` spans provide exact anchors for highlights, topic segmentation, summarization, safety tagging, and retrieval-augmented QA
- **Segmentation Quality**: Phrase-level segments are stable for content understanding; word timestamps available when needed
- **Multimodal Integration**: Transcripts align with video frames/shots by time for better retrieval and summarization

### For Video Experts
- **Deterministic Pipeline**: Fixed preprocessing, same model assets, deterministic sampling for reproducible results
- **Format Support**: WAV/PCM16 files, MP4/AAC containers with hardware-accelerated decoding
- **Quality Control**: SHA-256 hash comparison for verification, sample-accurate timestamps

### For Recommendation System Experts
- **Indexing Contract**: One immutable transcript JSON per (asset, variant) with SHA verification
- **Online Latency**: Text retrieval over transcripts with time-coded jumps back to media
- **Feature Stability**: Fixed resample/downmix and pinned decode params ensure deterministic transcripts

### For Deep Learning Experts
- **Front-end**: Mono 16 kHz, log-mel computed inside Whisper; ensure amplitude in [−1,1]
- **Tokenizer/units**: BPE (Whisper's vocabulary); timestamps at 10 ms tick resolution
- **Search**: Greedy (fast) vs. beam (beamSize, patience); temperature for exploration
- **Numerical Hygiene**: Check isFinite, no NaNs; verify RTF vs threads

### For Audio/LLM Generation & Agents Experts
- **RAG over Audio**: Treat transcripts as retrieval layer; fetch top-K spans by cosine/BM25
- **Dubbing/Localization**: Translate=true yields EN targets; keep source timestamps
- **Guidance Signals**: Score rendered audio/text vs target transcript during A/V generation
- **Editing Ops**: Time-aligned text enables text-based editing workflows

## Scripts

See `scripts/` folder for:
- `test_whisper.sh` - Core functionality testing
- `test_whisper_api.sh` - API endpoint validation
- `test_whisper_bridge.sh` - Android bridge testing
- `test_whisper_resource_monitoring.sh` - Resource monitoring validation
- `deploy_multilingual_models.sh` - Model deployment
- `verify_lid_implementation.sh` - Language detection verification