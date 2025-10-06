# Whisper Full Scale Implementation Details

## Problem Disaggregation

**Inputs:** WAV/PCM16 files, MP4/AAC (no FFmpeg)
**Outputs:** mono 16 kHz PCM16 (+ JSON sidecar) with stable PTS mapping
**Runtime surfaces:** Broadcast → WorkManager job → Decode pipeline → Storage writer
**Isolation:**
- Do not change your app id
- Debug variant uses applicationIdSuffix ".debug" (installs side-by-side)
- All actions/authorities use ${applicationId} placeholders, so debug/prod never collide

## Analysis with Trade-offs (Concise)

- **MediaCodec vs FFmpeg:** native, HW-assisted, small footprint vs broader codec coverage. We choose MediaCodec (no FFmpeg)
- **Resampler:** windowed-sinc (best), polyphase (great), linear (good & simple). We ship linear first (fast, no deps), keep interface pluggable
- **PTS:** extract extractor PTS; for decoded PCM we track a sample index → PTS affine map (monotonic fallback)
- **Memory:** stream to disk (.pcm) vs RAM. We support both; default is stream to disk to avoid OOM on long media

## Design Pipeline

```
Broadcast (ACTION_DECODE_URI) → WorkManager DecodeWorker → DecodePipeline.decodeUriToMono16k()
→ (WavReader | AacDecoder) → Normalizer (downmix+resample) → PcmWriter (.pcm + .json)
```

## Key Control-knots (all exposed)

- `TARGET_SR` (default 16_000)
- `TARGET_CH` (default 1/mono)
- `PCM_FORMAT` (PCM_16)
- `RESAMPLER` (LINEAR | SINC)
- `DOWNMIX` (AVERAGE | LEFT | RIGHT)
- `DECODE_BUFFER_MS` (e.g., 250)
- `TIMESTAMP_POLICY` (ExtractorPTS | Monotonic)
- `OUTPUT_MODE` (FILE | RAM)
- `SAVE_SIDE_CAR` (true) – codec, bit-rate, in/out SR/CH, length, SHA256 of PCM

## Isolation & Namespacing

- **Broadcast actions:** `${applicationId}.action.DECODE_URI`
- **Work names:** `${BuildConfig.APPLICATION_ID}::decode::<hash(uri)>`
- **File authorities (if ContentProvider later):** `${applicationId}.files`
- **Debug install:** `applicationIdSuffix ".debug"` → all names differ automatically

## Prioritization & Rationale

- **P0:** WAV/AAC decode, downmix, resample, sidecar, namespaced broadcasts & jobs
- **P1:** Opus/MP3 support (MediaCodec), streaming mic via AudioRecord, progress callbacks
- **P2:** Sinc resampler, on-device health metrics, codec-simulation augment toggles

## Workplan to Execute

1. Scaffold project + build variants (keep applicationId intact; add .debug)
2. Implement I/O (WAV reader, AAC decoder) + Normalizer (downmix/resample)
3. Pipeline + WorkManager + BroadcastReceiver with `${applicationId}` actions
4. Writers: .pcm (LE int16) + .json sidecar (audit)
5. E2E test via ADB broadcast on both release and debug APKs (install side-by-side)
6. Bench/verify: sample-accurate length, SR/CH post-conditions, monotonic PTS, SHA256

## Implementation (runnable Kotlin & Gradle)

### Code Pointers

- **AudioFrontEndConfig.kt:** `app/src/main/java/com/mira/whisper/config/AudioFrontEndConfig.kt`
- **Normalizer.kt:** `app/src/main/java/com/mira/whisper/dsp/Normalizer.kt`
- **DecodeWorker.kt:** `app/src/main/java/com/mira/whisper/workers/DecodeWorker.kt`
- **WavReader.kt:** `app/src/main/java/com/mira/whisper/io/WavReader.kt`
- **AacDecoder.kt:** `app/src/main/java/com/mira/whisper/io/AacDecoder.kt`
- **PcmWriter.kt:** `app/src/main/java/com/mira/whisper/io/PcmWriter.kt`

### E2E Test (no UI)

**Install both variants side-by-side:**
```bash
# Release variant
./gradlew assembleRelease
adb install app/build/outputs/apk/release/app-release.apk

# Debug variant  
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Trigger decode by broadcast:**
```bash
# Release variant
adb shell am broadcast -a com.mira.videoeditor.action.DECODE_URI \
  --es uri "file:///sdcard/test_audio.wav"

# Debug variant
adb shell am broadcast -a com.mira.videoeditor.debug.action.DECODE_URI \
  --es uri "file:///sdcard/test_audio.wav"
```

**Verify outputs:**
```bash
# Check PCM file
ls -la /sdcard/MiraWhisper/out/*.pcm

# Check JSON sidecar
cat /sdcard/MiraWhisper/sidecars/*.json

# Verify SHA256
sha256sum /sdcard/MiraWhisper/out/*.pcm
```

## Key Knots Surfaced in Configs (code pointers)

| Knot | BuildConfig | Purpose | Typical values |
|------|-------------|---------|----------------|
| Target sample rate | TARGET_SAMPLE_RATE | ASR-ready SR | 16000 |
| Target channels | TARGET_CHANNELS | Mono vs multi | 1 |
| Resampler | RESAMPLER | Quality/speed | LINEAR (default) / SINC |
| Downmix policy | DOWNMIX | Stereo→mono strategy | AVERAGE/LEFT/RIGHT |
| Decode buffer | DECODE_BUFFER_MS | Latency vs throughput | 100–500 ms |
| Timestamp policy | TIMESTAMP_POLICY | PTS handling | ExtractorPTS/Monotonic |
| Output mode | OUTPUT_MODE | File vs RAM | FILE |
| Sidecar | SAVE_SIDE_CAR | Auditability | true |

## Scale-out Plan: Control Knots and Impact

### Single (one per knot)

| Knot | Choice | Rationale (technical • user goals) |
|------|--------|-----------------------------------|
| DECODE path | MP4/AAC — HW-first, SW fallback | Tech: Max native support, smallest surface, no FFmpeg. • User: Works everywhere; fewer surprises in demos |
| DECODE_BUFFER_MS | 160 ms buffer / 10 ms hop | Tech: Stable streaming, minimal underruns; ASR-friendly hop. • User: Smooth live capture with acceptable latency |
| RESAMPLER | Linear (baseline) | Tech: Fastest; protects RTF headroom. • User: Longer sessions on mobile; consistent performance |
| TIMESTAMP_POLICY | Hybrid (PTS + sample-count) with drift in sidecar | Tech: Sample-accurate CTM; drift visibility. • User: Trustworthy timestamps for search/quotes |
| Throughput | Stream .pcm (500 ms), JSON=128, WM=Balanced | Tech: Good I/O amortization; bounded queues. • User: No stalls; predictable background runs |
| Observability | On @2s, level=info → file | Tech: Low overhead counters (RTF, fps, drift). • User: Actionable logs without battery hit |

### Ablations (combos)

| Combo | Knot changes (vs Single) | Rationale (technical • user goals) |
|-------|-------------------------|-----------------------------------|
| A. Low-latency Mic | Buffer 120/10; Throughput: JSON=64, WM=Throughput | Tech: Cut e2e latency; faster flush path. • User: Snappier live captions; trade a bit of stability |
| B. Accuracy-leaning | Resampler: Polyphase/Sinc (balanced); TS: Hybrid (same) | Tech: Better anti-aliasing → small WER gain; timestamps already robust. • User: Cleaner transcripts for export/QA |
| C. Web-robust Ingest | Decode: +WebM/Opus + MP3 (HW-first, SW fallback) | Tech: Wide format coverage; SW only when HW absent. • User: "It just opens" for web downloads/voice notes |
| D. High-throughput Batch | Throughput: .pcm 300 ms, JSON=64, WM=Throughput; Obs: debug@1s (time-boxed) | Tech: Higher ingest rate, faster drains; temporary deep metrics. • User: Faster backfills; short profiling bursts |

### Quick Read on Effects (rule-of-thumb)

- **A Low-latency Mic:** latency ↓, underrun risk ↑ (needs clean device)
- **B Accuracy-leaning:** WER ↓ slightly, CPU ↑ slightly (watch RTF budgets)
- **C Web-robust Ingest:** compatibility ↑↑, RTF may dip on SW decode
- **D High-throughput Batch:** tail latency ↓, power/IO ↑ (use when plugged-in)

## Configuration Presets

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