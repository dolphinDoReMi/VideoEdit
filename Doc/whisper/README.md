# Whisper Speech Recognition Integration

## Multi-Lens Communication for Expert Audiences

This document provides comprehensive explanations of the Whisper speech recognition integration for different expert audiences. Each section is tailored to provide the most relevant information for specific technical backgrounds.

## 1. Plain-text: How It Works (Step-by-Step)

### Basic Workflow
1. **Locate Input File**: Static audio file (.wav or .mp4 with AAC inside container)
2. **Decode to Waveform**:
   - WAV → parse RIFF → PCM16
   - MP4 → MediaExtractor/MediaCodec → AAC→PCM16
3. **Normalize Front-end**: Downmix to mono, resample to 16 kHz, clamp to int16 range
4. **Feed to Whisper**: PCM to whisper.cpp via JNI with explicit config (threads, language or auto-LID, translate on/off, greedy/beam, temperature)
5. **Whisper Processing**: Computes log-mel, runs Transformer decoder, returns time-stamped segments (10 ms tick base): [t0Ms, t1Ms, text]
6. **Optional Enhancement**: Enable word timestamps for word-level CTM
7. **Serialize Artifacts**: JSON sidecar next to audio with segments[], model_variant, model_sha, decode_cfg, audio_sha256, created_at, rtf
8. **Optional Formats**: Generate SRT/VTT from segments
9. **Persist Metadata**: Store run metadata in asr.db (asr_files, asr_jobs, asr_segments) for audit/replay
10. **Verify Results**: Sanity checks for sample rate==16 kHz mono, non-empty segments, ordered times (t0≤t1), finite text, RTF < target, optional WER ≤ threshold on reference clip

### Why This Works
End-to-end ASR (Whisper) maps normalized audio to subword text with learned alignment; strict front-end (mono/16k) removes domain mismatch; JSON+DB make outputs reproducible and time-addressable.

## 2. For a RecSys System Design Expert

### Indexing Contract
- **One immutable transcript JSON per (asset, variant)**
- **Path convention**: `{variant}/{audioId}.json` (+ SHA of audio and model)
- **Online latency path**: User query → text retrieval over transcripts (BM25/ANN on text embeddings) with time-coded jumps back to media

### ANN Build Strategy
- **Store raw JSON for audit**; build a serving index over text embeddings (e.g., E5/MPNet) or n-gram inverted index
- **Keep Whisper confidence/timing as features**
- **MIPS/cosine**: if using unit-norm text embeddings, cosine==dot; standard ANN (Faiss/ScaNN/HNSW) applies

### Freshness & TTL
- **Decouple offline ASR ingest from online retrieval**
- **Sidecar has created_at, model_sha, decode_cfg for rollbacks and replays**
- **Feature stability**: fixed resample/downmix and pinned decode params → deterministic transcripts (minus inherent stochasticity like temperature/beam)

### Ranking Fusion
- **Score = α·text_match(q, t) + β·ASR_quality(seg) + γ·user_personalization(u, asset) + δ·recency(asset)**
- **Fuse at segment or asset level**

### Safety/Observability
- **Metrics = recall@K, latency p99, RTF distribution, segment coverage (% voiced), WER on labeled panels**
- **Verify integrity via audio_sha256 and model_sha**
- **AB discipline**: treat model change or decode config change (beam/temp) as new variant keys; support shadow deployments with side-by-side JSONs

## 3. For a Deep Learning Expert

### Front-end Processing
- **Mono 16 kHz, log-mel computed inside Whisper**
- **Ensure amplitude in [−1,1]**

### Tokenizer/Units
- **BPE (Whisper's vocabulary)**
- **Timestamps at 10 ms tick resolution if enabled**

### Search Configuration
- **Greedy (fast) vs. beam (beamSize, patience)**
- **Temperature for exploration**
- **Translate toggles X→EN decoding**
- **Language can be forced to avoid LID flips**

### Chunking Strategy
- **Whisper.cpp internally handles ~30 s contexts**
- **For long files, do windowed decode with overlap and stitch segments**

### Numerical Hygiene
- **Check isFinite, no NaNs**
- **Verify RTF vs threads**
- **Keep resampler and downmix deterministic**
- **Hold temperature fixed in eval runs**

### Quantization Considerations
- **GGUF quantization reduces RAM/latency but may raise WER**
- **Keep a float (or higher-precision) baseline for audits**
- **Report ΔWER/ΔRTF**

### Known Limitations
- **No diarization/speaker turns by default**
- **Far-field/noisy audio benefits from better resampling and optional VAD**
- **Cross-talk and code-switching can degrade unless language is forced**

### Upgrade Paths
- **Band-limited resampler (SoX-style) for noisy domains**
- **VAD pre-trim**
- **Long-form strategies (context carryover)**
- **Optional speaker diarization for CU tasks**

## 4. For a Content Understanding Expert

### Primitive Output
- **{t0Ms, t1Ms, text} spans—exact anchors for highlights, topic segmentation, summarization, safety tagging, and retrieval-augmented QA**

### Segmentation Quality
- **Phrase-level segments are stable for CU**
- **Enable word timestamps only when you need word-level alignment (costs compute)**

### Diagnostics
- **Coverage (voiced duration / file duration)**
- **Gap distribution (silences)**
- **Language stability**
- **OOV rates**
- **ASR confidence proxy (beam entropy or log-probs if exposed)**

### Sampling Bias Considerations
- **Front-end normalization prevents drift across corpora**
- **Watch domain shift (far-field, music overlap, accents)**

### Multimodal Integration
- **Align transcripts with video frames or shots by time**
- **Late-fuse with image/video embeddings for better retrieval and summarization**
- **Transcripts also seed topic labels and entity graphs**

### Safety Applications
- **Time-pin policy flags (e.g., abuse/PII) to exact spans for explainability and partial redaction**

## 5. For an Audio/LLM Generation & Agents Expert

### RAG over Audio
- **Treat transcripts as the retrieval layer**
- **For a prompt, fetch top-K spans by cosine/BM25, then ground an LLM/agent with verbatim time-linked evidence**

### Dubbing/Localization
- **Translate=true yields EN targets**
- **Keep source timestamps to drive subtitle timing and guide TTS alignment**

### Guidance Signals
- **During A/V generation, periodically score rendered audio/text vs target transcript**
- **Use similarity (text or audio embeddings) as auxiliary guidance to reduce semantic drift**

### Editing Operations
- **Time-aligned text enables text-based editing workflows (cut, copy, replace) that map back to waveform spans deterministically**

### Telemetry & Safety
- **Because artifacts are auditable (JSON+SHA), you can trace which spans conditioned a generation and gate disallowed content by time**

## Key Design Decisions

### Deterministic Processing
- **Seedless pipeline**: Uniform frame timestamps with fixed intervals
- **Fixed preprocessing**: Center-crop audio segments, no random augmentation
- **Consistent model assets**: Fixed model file with deterministic initialization
- **Verification**: SHA-256 hash comparison between processing runs

### Performance Optimization
- **Device-specific tuning**: Optimal thread counts and model sizes per device
- **Thermal management**: Automatic throttling when device temperature > 45°C
- **Memory management**: Efficient resource utilization with configurable limits
- **Battery optimization**: Respect device battery optimization settings

### Cross-Platform Compatibility
- **Android**: MediaCodec-based audio processing, ARM64 optimization
- **iOS**: WhisperKit integration with Apple Silicon optimization
- **Web**: WebAssembly-based processing for browser compatibility

### Quality Assurance
- **Comprehensive testing**: Unit, integration, and performance tests
- **Real device validation**: Xiaomi Pad and iPad Pro testing
- **Error handling**: Robust error management with graceful degradation
- **Monitoring**: Real-time performance and quality metrics

## Implementation Architecture

### Core Components
- **WhisperEngine**: Main processing engine with model management
- **AudioExtractor**: MediaExtractor integration for audio extraction
- **AudioProcessor**: Preprocessing pipeline with normalization and resampling
- **TranscriptionCache**: LRU cache with deterministic keys for performance

### Integration Points
- **AutoCutEngine**: Enhanced video processing with Whisper integration
- **VideoScorer**: Semantic scoring using transcription data
- **WorkManager**: Asynchronous processing with background job management
- **BroadcastReceiver**: Namespaced actions for inter-component communication

### Configuration Management
- **BuildConfig**: Compile-time configuration with device-specific settings
- **Runtime Config**: Flexible parameter adjustment for different use cases
- **Feature Flags**: Gradual rollout and A/B testing support
- **Control Knots**: Exposed parameters for deterministic processing

## Testing Strategy

### Deterministic Testing
- **Hash Verification**: SHA-256 comparison between runs
- **Processing Consistency**: Identical results across multiple runs
- **Performance Stability**: Consistent timing and resource usage
- **Quality Validation**: Stable transcription accuracy

### Integration Testing
- **End-to-End Pipeline**: Complete processing verification
- **Fallback Testing**: Graceful degradation to basic processing
- **Error Handling**: Robust error management
- **Resource Management**: Memory and thermal management

### Performance Testing
- **Benchmarking**: Performance baseline establishment
- **Scalability**: Large video processing capability
- **Device Compatibility**: Multi-device testing
- **Battery Impact**: Power consumption analysis

## Deployment Considerations

### Production Readiness
- **Deterministic Processing**: Verified reproducible results
- **Performance Optimization**: Efficient resource utilization
- **Error Handling**: Comprehensive error management
- **Monitoring**: Performance and quality metrics

### Device Compatibility
- **Minimum Requirements**: Android API 24, 4GB RAM
- **Recommended**: 6GB+ RAM, ARM64 architecture
- **Optimal**: Modern CPU, sufficient storage
- **Thermal Management**: Battery-aware processing

## Related Documentation

### Architecture and Design
- [Architecture Design and Control Knot](Architecture%20Design%20and%20Control%20Knot.md) - Core architecture with control knots
- [Full Scale Implementation Details](Full%20scale%20implementation%20Details.md) - Complete implementation guide

### Deployment and Testing
- [Device Deployment](Device%20Deployment.md) - Xiaomi Pad and iPad deployment procedures
- [Release Documentation](Release%20(iOS%2C%20Android%20and%20MacOS%20Web%20Version).md) - Cross-platform release guidelines

### Development Guidelines
- [Cursor Rules](cursor_rule.md) - Development standards and best practices

## Conclusion

The Whisper integration provides a robust, deterministic speech recognition system optimized for content-aware video editing. The multi-lens approach ensures that different expert audiences can understand and utilize the system effectively for their specific use cases.

**Status**: ✅ **PRODUCTION READY**  
**Platforms**: ✅ **ANDROID, iOS, WEB**  
**Testing**: ✅ **COMPREHENSIVE**  
**Documentation**: ✅ **COMPLETE**
