# VideoEdit Documentation

## Multi-Lens Expert Communication

### 1/ Plain-text: How it works (step-by-step)

**Whisper ASR Pipeline:**
- Locate input file: .wav or .mp4 (AAC inside container)
- Decode to waveform: WAV → parse RIFF → PCM16, MP4 → MediaExtractor/MediaCodec → AAC→PCM16
- Normalize front-end: downmix to mono, resample to 16 kHz, clamp to int16 range
- Feed PCM to whisper.cpp via JNI with explicit config (threads, language or auto-LID, translate on/off, greedy/beam, temperature)
- Whisper computes log-mel, runs Transformer decoder, returns time-stamped segments (10 ms tick base): [t0Ms, t1Ms, text]
- Optional: enable word timestamps for word-level CTM
- Serialize artifacts: JSON sidecar with segments[], model_variant, model_sha, decode_cfg, audio_sha256, created_at, rtf
- Persist run metadata in asr.db (asr_files, asr_jobs, asr_segments) for audit/replay
- Verify with sanity checks: sample rate==16 kHz mono, non-empty segments, ordered times (t0≤t1), finite text, RTF < target

**CLIP Visual Understanding Pipeline:**
- Extract video frames: uniform sampling at 1.0 fps (configurable)
- Preprocess frames: resize to 224x224, center crop, ImageNet normalization
- Encode with CLIP ViT-B/32: extract 512-dim embeddings
- Store embeddings: JSON + binary format with frame timestamps
- Enable similarity search: cosine similarity between text queries and frame embeddings

**Why this works:** End-to-end ASR (Whisper) maps normalized audio to subword text with learned alignment; CLIP provides cross-modal embeddings for visual-text retrieval; strict front-end normalization removes domain mismatch; JSON+DB make outputs reproducible and time-addressable.

### 2/ For a recsys system design expert

**Indexing contract:** One immutable transcript JSON per (asset, variant); path convention: {variant}/{audioId}.json (+ SHA of audio and model).

**Online latency path:** User query → text retrieval over transcripts (BM25/ANN on text embeddings) with time-coded jumps back to media.

**ANN build:** Store raw JSON for audit; build serving index over text embeddings (E5/MPNet) or n-gram inverted index; keep Whisper confidence/timing as features.

**MIPS/cosine:** If using unit-norm text embeddings, cosine==dot; standard ANN (Faiss/ScaNN/HNSW) applies.

**Freshness & TTL:** Decouple offline ASR ingest from online retrieval; sidecar has created_at, model_sha, decode_cfg for rollbacks and replays.

**Feature stability:** Fixed resample/downmix and pinned decode params → deterministic transcripts (minus inherent stochasticity like temperature/beam).

**Ranking fusion:** score = α·text_match(q, t) + β·ASR_quality(seg) + γ·user_personalization(u, asset) + δ·recency(asset); fuse at segment or asset level.

**Safety/observability:** metrics = recall@K, latency p99, RTF distribution, segment coverage (% voiced), WER on labeled panels; verify integrity via audio_sha256 and model_sha.

**AB discipline:** Treat model change or decode config change (beam/temp) as new variant keys; support shadow deployments with side-by-side JSONs.

### 3/ For a deep learning expert

**Front-end:** Mono 16 kHz, log-mel computed inside Whisper; ensure amplitude in [−1,1].

**Tokenizer/units:** BPE (Whisper's vocabulary); timestamps at 10 ms tick resolution if enabled.

**Search:** Greedy (fast) vs. beam (beamSize, patience); temperature for exploration; translate toggles X→EN decoding; language can be forced to avoid LID flips.

**Chunking:** whisper.cpp internally handles ~30 s contexts; for long files, do windowed decode with overlap and stitch segments.

**Numerical hygiene:** Check isFinite, no NaNs; verify RTF vs threads; keep resampler and downmix deterministic; hold temperature fixed in eval runs.

**Quantization:** GGUF quantization reduces RAM/latency but may raise WER; keep a float (or higher-precision) baseline for audits; report ΔWER/ΔRTF.

**Known limitations:** No diarization/speaker turns by default; far-field/noisy audio benefits from better resampling and optional VAD; cross-talk and code-switching can degrade unless language is forced.

**Upgrades:** Band-limited resampler (SoX-style) for noisy domains; VAD pre-trim; long-form strategies (context carryover); optional speaker diarization for CU tasks.

### 4/ For a content understanding expert

**Primitive you get:** {t0Ms, t1Ms, text} spans—exact anchors for highlights, topic segmentation, summarization, safety tagging, and retrieval-augmented QA.

**Segmentation quality:** Phrase-level segments are stable for CU; enable word timestamps only when you need word-level alignment (costs compute).

**Diagnostics:** Coverage (voiced duration / file duration), gap distribution (silences), language stability, OOV rates, ASR confidence proxy (beam entropy or log-probs if exposed).

**Sampling bias:** Front-end normalization prevents drift across corpora; watch domain shift (far-field, music overlap, accents).

**Multimodal hooks:** Align transcripts with video frames or shots by time; late-fuse with image/video embeddings for better retrieval and summarization; transcripts also seed topic labels and entity graphs.

**Safety:** Time-pin policy flags (e.g., abuse/PII) to exact spans for explainability and partial redaction.

### 5/ For an audio/LLM generation & agents expert

**RAG over audio:** Treat transcripts as the retrieval layer; for a prompt, fetch top-K spans by cosine/BM25, then ground an LLM/agent with verbatim time-linked evidence.

**Dubbing/localization:** translate=true yields EN targets; keep source timestamps to drive subtitle timing and guide TTS alignment.

**Guidance signals:** During A/V generation, periodically score rendered audio/text vs target transcript; use similarity (text or audio embeddings) as auxiliary guidance to reduce semantic drift.

**Editing ops:** Time-aligned text enables text-based editing workflows (cut, copy, replace) that map back to waveform spans deterministically.

**Telemetry & safety:** Because artifacts are auditable (JSON+SHA), you can trace which spans conditioned a generation and gate disallowed content by time.

## Feature Documentation Structure

### [CLIP Documentation](./clip/)
- **Architecture Design and Control Knot** - Core architecture and control parameters
- **Full scale implementation Details** - Complete implementation guide  
- **Device Deployment** - Platform-specific deployment
- **README.md** - Multi-lens explanation for experts
- **Release (iOS, Android and MacOS Web Version)** - Automated testing and release
- **scripts/** - CLIP-specific scripts and tools

### [Whisper Documentation](./whisper/)
- **Architecture Design and Control Knot** - Core architecture and control parameters
- **Full scale implementation Details** - Complete implementation guide
- **Device Deployment** - Platform-specific deployment  
- **README.md** - Multi-lens explanation for experts
- **Release (iOS, Android and MacOS Web Version)** - Automated testing and release
- **scripts/** - Whisper-specific scripts and tools

### [UI Documentation](./ui/)
- **Architecture Design and Control Knot** - UI architecture and design system
- **Full scale implementation Details** - Complete implementation guide
- **Device Deployment** - Cross-platform deployment
- **README.md** - Multi-lens explanation for experts
- **Release (iOS, Android and MacOS Web Version)** - Automated testing and release
- **scripts/** - UI-specific scripts and tools

## Development Guidelines

### [Cursor Rules](./cursor_rule.md)
- Project overview and code organization
- Coding standards and best practices
- Development workflow and testing requirements
- Architecture decisions and trade-offs

## Quick Start

### Whisper (Speech Recognition)
```bash
cd Doc/whisper/scripts
./deploy_multilingual_models.sh
./test_lid_pipeline.sh
./work_through_video_v1.sh
```

### CLIP (Visual Understanding)
```bash
cd Doc/clip/scripts
./deploy_clip_model.sh
./test_clip_pipeline.sh
./work_through_clip_xiaomi.sh
```

### UI (User Interface)
```bash
cd Doc/ui/scripts
./test_ui_components.sh
./test_responsive_design.sh
./test_accessibility.sh
```

## Key Features

### Whisper
- **Multilingual ASR**: Support for 99+ languages with automatic detection
- **Real-time processing**: Background processing with WorkManager
- **Batch processing**: Multiple file processing with progress tracking
- **Resource monitoring**: Real-time CPU, memory, and battery monitoring
- **Quality control**: Deterministic processing with hash verification

### CLIP
- **Cross-modal embeddings**: Text-image similarity search
- **Video understanding**: Frame-level visual analysis
- **Scalable indexing**: Efficient similarity search with ANN
- **Multi-platform**: Android, iOS, macOS web support
- **Performance optimized**: GPU acceleration and memory management

### UI
- **Cross-platform**: Android WebView, iOS WKWebView, macOS PWA
- **Accessibility**: Full WCAG compliance and screen reader support
- **Responsive design**: Multi-device layout optimization
- **Performance**: 60fps target with efficient resource management
- **Security**: Content Security Policy and secure JavaScript bridge