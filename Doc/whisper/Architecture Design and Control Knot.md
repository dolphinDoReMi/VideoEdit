# Whisper Architecture Design and Control Knot

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

## Whisper-Specific Control Knots

**Audio Processing Control Knots:**
- Sample rate: 16 kHz (ASR-ready)
- Channels: Mono (downmix from stereo)
- Format: PCM16 (16-bit signed integer)
- Resampler: Linear (fast, deterministic)
- Buffer size: 160 ms (stable streaming)

**Model Control Knots:**
- Whisper model: tiny.en (fast) / base.en (balanced) / small.en (accurate)
- Language detection: Automatic LID with manual override
- Translation: Optional X→EN decoding
- Search strategy: Greedy (fast) / Beam (accurate)
- Temperature: Fixed for deterministic results

**Quality Control Knots:**
- Audio validation: Sample rate and format checks
- Model versioning: SHA-256 hash of Whisper model weights
- Transcript validation: Non-empty segments, ordered timestamps
- Performance metrics: RTF (Real-Time Factor) monitoring

**Verification Methods:**
- Audio hash verification: `verify_audio_processing.sh`
- Model validation: `test_whisper_models.sh`
- Transcript quality: `validate_transcripts.sh`
- Performance benchmarks: `benchmark_whisper_processing.sh`