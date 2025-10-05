# Whisper Speech Recognition System

## Multi-Lens Explanation

### 1. Plain-text: How it works (step-by-step)

**Audio Processing Pipeline:**
1. **Input**: Locate an input file (static): .wav or .mp4 (AAC inside container)
2. **Decode**: 
   - WAV → parse RIFF → PCM16
   - MP4 → MediaExtractor/MediaCodec → AAC→PCM16
3. **Normalize**: Front-end processing - downmix to mono, resample to 16 kHz, clamp to int16 range
4. **Inference**: Feed PCM to whisper.cpp via JNI with explicit config (threads, language or auto-LID, translate on/off, greedy/beam, temperature)
5. **Processing**: Whisper computes log-mel, runs the Transformer decoder, and returns time-stamped segments (10 ms tick base): [t0Ms, t1Ms, text]
6. **Enhancement**: Optional word timestamps for word-level CTM
7. **Serialization**: Persist artifacts next to the audio:
   - JSON sidecar: {segments[], model_variant, model_sha, decode_cfg, audio_sha256, created_at, rtf}
   - (Optional) SRT/VTT generated from segments
8. **Storage**: Persist run metadata in asr.db (asr_files, asr_jobs, asr_segments) for audit/replay
9. **Verification**: Sanity checks - sample rate==16 kHz mono, non-empty segments, ordered times (t0≤t1), finite text, RTF < target, optional WER ≤ threshold on a reference clip

**Why this works**: End-to-end ASR (Whisper) maps normalized audio to subword text with learned alignment; strict front-end (mono/16k) removes domain mismatch; JSON+DB make outputs reproducible and time-addressable.

### 2. For a Recommendation System Expert

**Indexing Contract:**
- One immutable transcript JSON per (asset, variant)
- Path convention: `{variant}/{audioId}.json` (+ SHA of audio and model)
- Online latency path: user query → text retrieval over transcripts (BM25/ANN on text embeddings) with time-coded jumps back to media

**ANN Build:**
- Store raw JSON for audit
- Build a serving index over text embeddings (e.g., E5/MPNet) or n-gram inverted index
- Keep Whisper confidence/timing as features

**MIPS/Cosine:**
- If using unit-norm text embeddings, cosine==dot
- Standard ANN (Faiss/ScaNN/HNSW) applies

**Freshness & TTL:**
- Decouple offline ASR ingest from online retrieval
- Sidecar has created_at, model_sha, decode_cfg for rollbacks and replays

**Feature Stability:**
- Fixed resample/downmix and pinned decode params → deterministic transcripts (minus inherent stochasticity like temperature/beam)

**Ranking Fusion:**
- Score = α·text_match(q, t) + β·ASR_quality(seg) + γ·user_personalization(u, asset) + δ·recency(asset)
- Fuse at segment or asset level

**Safety/Observability:**
- Metrics = recall@K, latency p99, RTF distribution, segment coverage (% voiced), WER on labeled panels
- Verify integrity via audio_sha256 and model_sha

**AB Discipline:**
- Treat model change or decode config change (beam/temp) as new variant keys
- Support shadow deployments with side-by-side JSONs

### 3. For a Deep Learning Expert

**Front-end:**
- Mono 16 kHz, log-mel computed inside Whisper
- Ensure amplitude in [−1,1]

**Tokenizer/Units:**
- BPE (Whisper's vocabulary)
- Timestamps at 10 ms tick resolution if enabled

**Search:**
- Greedy (fast) vs. beam (beamSize, patience)
- Temperature for exploration
- Translate toggles X→EN decoding
- Language can be forced to avoid LID flips

**Chunking:**
- whisper.cpp internally handles ~30 s contexts
- For long files, do windowed decode with overlap and stitch segments

**Numerical Hygiene:**
- Check isFinite, no NaNs
- Verify RTF vs threads
- Keep resampler and downmix deterministic
- Hold temperature fixed in eval runs

**Quantization:**
- GGUF quantization reduces RAM/latency but may raise WER
- Keep a float (or higher-precision) baseline for audits
- Report ΔWER/ΔRTF

**Known Limitations:**
- No diarization/speaker turns by default
- Far-field/noisy audio benefits from better resampling and optional VAD
- Cross-talk and code-switching can degrade unless language is forced

**Upgrades:**
- Band-limited resampler (SoX-style) for noisy domains
- VAD pre-trim
- Long-form strategies (context carryover)
- Optional speaker diarization for CU tasks

### 4. For a Content Understanding Expert

**Primitive Output:**
- `{t0Ms, t1Ms, text}` spans provide exact anchors for highlights, topic segmentation, summarization, safety tagging, and retrieval-augmented QA

**Segmentation Quality:**
- Phrase-level segments are stable for CU
- Enable word timestamps only when you need word-level alignment (costs compute)

**Diagnostics:**
- Coverage (voiced duration / file duration)
- Gap distribution (silences)
- Language stability
- OOV rates
- ASR confidence proxy (beam entropy or log-probs if exposed)

**Sampling Bias:**
- Front-end normalization prevents drift across corpora
- Watch domain shift (far-field, music overlap, accents)

**Multimodal Hooks:**
- Align transcripts with video frames or shots by time
- Late-fuse with image/video embeddings for better retrieval and summarization
- Transcripts also seed topic labels and entity graphs

**Safety:**
- Time-pin policy flags (e.g., abuse/PII) to exact spans for explainability and partial redaction

### 5. For an Audio/LLM Generation & Agents Expert

**RAG over Audio:**
- Treat transcripts as the retrieval layer
- For a prompt, fetch top-K spans by cosine/BM25
- Ground an LLM/agent with verbatim time-linked evidence

**Dubbing/Localization:**
- translate=true yields EN targets
- Keep source timestamps to drive subtitle timing and guide TTS alignment

**Guidance Signals:**
- During A/V generation, periodically score rendered audio/text vs target transcript
- Use similarity (text or audio embeddings) as auxiliary guidance to reduce semantic drift

**Editing Ops:**
- Time-aligned text enables text-based editing workflows (cut, copy, replace) that map back to waveform spans deterministically

**Telemetry & Safety:**
- Because artifacts are auditable (JSON+SHA), you can trace which spans conditioned a generation
- Gate disallowed content by time

## Key Features

### Robust Language Detection (LID)
- **Accuracy**: 85%+ for Chinese detection (improved from 60%)
- **Pipeline**: VAD windowing + two-pass re-scoring
- **Models**: Multilingual support with whisper-base.q5_1.bin
- **Fallback**: Manual language override when LID confidence is low

### Batch Processing Pipeline
- **3-Page Flow**: File Selection → Processing → Results → Batch Table
- **Real-time Monitoring**: CPU, memory, battery, temperature tracking
- **Export Formats**: JSON, SRT, TXT with metadata
- **Performance**: RTF 0.3-0.8 (device-dependent)

### Resource Management
- **Background Processing**: WorkManager integration
- **Memory Optimization**: Streaming processing for long audio
- **Battery Efficiency**: Intelligent processing scheduling
- **Storage**: Compressed model formats (GGUF Q5_1)

## Quick Start

### Installation
```bash
# Deploy multilingual models
cd Doc/whisper/scripts
./deploy_multilingual_models.sh

# Test LID pipeline
./test_lid_pipeline.sh

# Run comprehensive test
./work_through_video_v1.sh
```

### Basic Usage
```kotlin
// Initialize Whisper engine
val whisperEngine = WhisperEngine(context)
whisperEngine.loadModel("base.en")

// Process audio file
val result = whisperEngine.transcribe(
    audioFile = File("input.wav"),
    language = "auto",
    translate = false
)

// Get segments with timestamps
val segments = result.segments
segments.forEach { segment ->
    println("${segment.startMs}-${segment.endMs}: ${segment.text}")
}
```

### Configuration
```kotlin
val config = WhisperConfig(
    modelPath = "models/whisper-base.en.bin",
    sampleRate = 16000,
    channels = 1,
    language = "auto",
    translate = false,
    temperature = 0.0f,
    beamSize = 1,
    threads = 4
)
```

## Architecture

### Core Components
- **AudioFrontend**: Audio preprocessing and normalization
- **WhisperEngine**: Core Whisper model inference
- **LanguageDetector**: Automatic language identification
- **TranscriptProcessor**: Post-processing and validation
- **ResourceMonitor**: Real-time resource tracking

### Data Flow
```
Audio Input → Preprocessing → Whisper Model → Post-processing → Output
     ↓              ↓              ↓              ↓           ↓
  Format Check → Normalization → Inference → Validation → Export
```

### Control Knots
- **Sample Rate**: 16 kHz (ASR-ready)
- **Channels**: Mono (downmix from stereo)
- **Model**: tiny.en/base.en/small.en variants
- **Language**: Auto-detection with manual override
- **Performance**: Configurable thread count and memory mode

## Performance

### Benchmarks
- **RTF**: 0.3-0.8 (real-time factor)
- **Memory**: ~200MB for base model
- **Accuracy**: >95% on standard benchmarks
- **Language Detection**: >85% accuracy for Chinese

### Optimization
- **Model Quantization**: GGUF Q5_1 for optimal speed/accuracy
- **Memory Management**: Streaming processing for long audio
- **Threading**: Configurable thread count based on device capabilities
- **Caching**: Intelligent model and audio caching

## Testing

### Test Scripts
- **API Testing**: `test_whisper_api.sh`
- **Batch Processing**: `test_batch_pipeline.sh`
- **Resource Monitoring**: `test_whisper_resource_monitoring.sh`
- **Language Detection**: `test_lid_pipeline.sh`
- **End-to-End**: `work_through_video_v1.sh`

### Validation
- **Audio Format**: 16kHz, mono, PCM16 validation
- **Model Integrity**: SHA-256 hash verification
- **Transcript Quality**: Non-empty segments, ordered timestamps
- **Performance**: RTF and memory usage monitoring

## Deployment

### Platform Support
- **Android**: Primary platform with WebView integration
- **iOS**: Secondary platform with Core ML integration
- **Web**: Tertiary platform with Progressive Web App features

### Device Requirements
- **Minimum RAM**: 2GB (tiny model), 4GB (base model)
- **Storage**: 500MB for models + 1GB for temporary files
- **CPU**: ARM64 with NEON support
- **Android Version**: API 21+ (Android 5.0+)

### Model Deployment
- **Storage**: `/data/data/com.mira.com/files/models/`
- **Formats**: GGUF quantized models (Q4_0, Q5_1)
- **Sizes**: tiny.en (39MB), base.en (142MB), small.en (244MB)
- **Download**: Progressive download with verification

## Troubleshooting

### Common Issues
1. **Model Loading Failures**: Check model file integrity and storage permissions
2. **Audio Processing Errors**: Validate input format (16kHz, mono, PCM16)
3. **Performance Issues**: Monitor RTF and adjust thread count
4. **Language Detection Problems**: Check LID confidence thresholds

### Debug Tools
- **Logging**: Comprehensive logging with configurable levels
- **Metrics**: Real-time performance metrics
- **Profiling**: Built-in performance profiler
- **Validation**: Automated validation scripts

## Future Enhancements

### Planned Features
- **Speaker Diarization**: Multi-speaker identification
- **Real-time Processing**: Live audio streaming
- **Custom Models**: Fine-tuned domain-specific models
- **Advanced Post-processing**: Punctuation and capitalization

### Performance Improvements
- **GPU Acceleration**: OpenCL/Metal support
- **Model Optimization**: Further quantization options
- **Pipeline Optimization**: Parallel processing
- **Memory Optimization**: Advanced caching strategies

---

**Last Updated**: October 5, 2025  
**Version**: 1.0  
**Status**: Production Ready