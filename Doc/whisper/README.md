# Whisper README.md

## Multi-Lens Explanation

### 1/ Plain-text: How it Works (Step-by-Step)

- **Locate an input file (static):** .wav or .mp4 (AAC inside container)
- **Decode to waveform:**
  - WAV → parse RIFF → PCM16
  - MP4 → MediaExtractor/MediaCodec → AAC→PCM16
- **Normalize front-end:** downmix to mono, resample to 16 kHz, clamp to int16 range
- **Feed PCM to whisper.cpp via JNI** with explicit config (threads, language or auto-LID, translate on/off, greedy/beam, temperature)
- **Whisper computes log-mel,** runs the Transformer decoder, and returns time-stamped segments (10 ms tick base): [t0Ms, t1Ms, text]
- **Optional:** enable word timestamps for word-level CTM
- **Serialize artifacts next to the audio:**
  - JSON sidecar: {segments[], model_variant, model_sha, decode_cfg, audio_sha256, created_at, rtf}
  - (Optional) SRT/VTT generated from segments
- **Persist run metadata** in asr.db (asr_files, asr_jobs, asr_segments) for audit/replay
- **Verify with sanity checks:** sample rate==16 kHz mono, non-empty segments, ordered times (t0≤t1), finite text, RTF < target, optional WER ≤ threshold on a reference clip

**Why this works:** End-to-end ASR (Whisper) maps normalized audio to subword text with learned alignment; strict front-end (mono/16k) removes domain mismatch; JSON+DB make outputs reproducible and time-addressable.

### 2/ For a Recsys System Design Expert

- **Indexing contract:** one immutable transcript JSON per (asset, variant); path convention: {variant}/{audioId}.json (+ SHA of audio and model)
- **Online latency path:** user query → text retrieval over transcripts (BM25/ANN on text embeddings) with time-coded jumps back to media
- **ANN build:** store raw JSON for audit; build a serving index over text embeddings (e.g., E5/MPNet) or n-gram inverted index; keep Whisper confidence/timing as features
- **MIPS/cosine:** if using unit-norm text embeddings, cosine==dot; standard ANN (Faiss/ScaNN/HNSW) applies
- **Freshness & TTL:** decouple offline ASR ingest from online retrieval; sidecar has created_at, model_sha, decode_cfg for rollbacks and replays
- **Feature stability:** fixed resample/downmix and pinned decode params → deterministic transcripts (minus inherent stochasticity like temperature/beam)
- **Ranking fusion:** score = α·text_match(q, t) + β·ASR_quality(seg) + γ·user_personalization(u, asset) + δ·recency(asset); fuse at segment or asset level
- **Safety/observability:** metrics = recall@K, latency p99, RTF distribution, segment coverage (% voiced), WER on labeled panels; verify integrity via audio_sha256 and model_sha
- **AB discipline:** treat model change or decode config change (beam/temp) as new variant keys; support shadow deployments with side-by-side JSONs

### 3/ For a Deep Learning Expert

- **Front-end:** mono 16 kHz, log-mel computed inside Whisper; ensure amplitude in [−1,1]
- **Tokenizer/units:** BPE (Whisper's vocabulary); timestamps at 10 ms tick resolution if enabled
- **Search:** greedy (fast) vs. beam (beamSize, patience); temperature for exploration; translate toggles X→EN decoding; language can be forced to avoid LID flips
- **Chunking:** whisper.cpp internally handles ~30 s contexts; for long files, do windowed decode with overlap and stitch segments
- **Numerical hygiene:** check isFinite, no NaNs; verify RTF vs threads; keep resampler and downmix deterministic; hold temperature fixed in eval runs
- **Quantization:** GGUF quantization reduces RAM/latency but may raise WER; keep a float (or higher-precision) baseline for audits; report ΔWER/ΔRTF
- **Known limitations:** no diarization/speaker turns by default; far-field/noisy audio benefits from better resampling and optional VAD; cross-talk and code-switching can degrade unless language is forced
- **Upgrades:** band-limited resampler (SoX-style) for noisy domains; VAD pre-trim; long-form strategies (context carryover); optional speaker diarization for CU tasks

### 4/ For a Content Understanding Expert

- **Primitive you get:** {t0Ms, t1Ms, text} spans—exact anchors for highlights, topic segmentation, summarization, safety tagging, and retrieval-augmented QA
- **Segmentation quality:** phrase-level segments are stable for CU; enable word timestamps only when you need word-level alignment (costs compute)
- **Diagnostics:** coverage (voiced duration / file duration), gap distribution (silences), language stability, OOV rates, ASR confidence proxy (beam entropy or log-probs if exposed)
- **Sampling bias:** front-end normalization prevents drift across corpora; watch domain shift (far-field, music overlap, accents)
- **Multimodal hooks:** align transcripts with video frames or shots by time; late-fuse with image/video embeddings for better retrieval and summarization; transcripts also seed topic labels and entity graphs
- **Safety:** time-pin policy flags (e.g., abuse/PII) to exact spans for explainability and partial redaction

### 5/ For an Audio/LLM Generation & Agents Expert

- **RAG over audio:** treat transcripts as the retrieval layer; for a prompt, fetch top-K spans by cosine/BM25, then ground an LLM/agent with verbatim time-linked evidence
- **Dubbing/localization:** translate=true yields EN targets; keep source timestamps to drive subtitle timing and guide TTS alignment
- **Guidance signals:** during A/V generation, periodically score rendered audio/text vs target transcript; use similarity (text or audio embeddings) as auxiliary guidance to reduce semantic drift
- **Editing ops:** time-aligned text enables text-based editing workflows (cut, copy, replace) that map back to waveform spans deterministically
- **Telemetry & safety:** because artifacts are auditable (JSON+SHA), you can trace which spans conditioned a generation and gate disallowed content by time

## Key Design Decisions

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

## Quick Start

1. **Install Dependencies**: Ensure Android SDK and NDK are configured
2. **Build Project**: Run `./gradlew assembleDebug` for debug build
3. **Deploy to Device**: Use `adb install` to deploy to Xiaomi Pad or iPad
4. **Test Pipeline**: Run verification scripts in `scripts/` directory
5. **Monitor Resources**: Use background resource monitoring service

## Architecture Overview

```
Audio Input → MediaCodec Decoder → Normalizer → Whisper.cpp → Transcript Output
     ↓              ↓                ↓            ↓              ↓
  WAV/MP4      PCM16 Buffer    Mono 16kHz    Segments      JSON Sidecar
```

## Control Knots

- **TARGET_SR**: 16000 Hz (ASR-ready sample rate)
- **TARGET_CH**: 1 (mono channel)
- **RESAMPLER**: LINEAR (fast, deterministic)
- **TIMESTAMP_POLICY**: Hybrid (PTS + sample-count)
- **OUTPUT_MODE**: FILE (stream to disk)
- **SAVE_SIDE_CAR**: true (auditability)

## Verification

- **Hash Comparison**: SHA-256 verification for deterministic outputs
- **Performance Benchmarks**: RTF, memory usage, processing speed
- **Quality Validation**: WER testing on reference clips
- **Cross-Platform**: Testing on Xiaomi Pad and iPad