# Whisper Full Scale Implementation Details

## Production-Ready Android/Kotlin Implementation

### Problem Disaggregation

- **Inputs**: WAV/PCM16 files, MP4/AAC (no FFmpeg)
- **Outputs**: mono 16 kHz PCM16 (+ JSON sidecar) with stable PTS mapping
- **Runtime Surfaces**: Broadcast → WorkManager job → Decode pipeline → Storage writer
- **Isolation**:
  - Do not change your app id
  - Debug variant uses applicationIdSuffix ".debug" (installs side-by-side)
  - All actions/authorities use ${applicationId} placeholders, so debug/prod never collide

### Analysis with Trade-offs

- **MediaCodec vs FFmpeg**: native, HW-assisted, small footprint vs broader codec coverage. We choose MediaCodec (no FFmpeg)
- **Resampler**: windowed-sinc (best), polyphase (great), linear (good & simple). We ship linear first (fast, no deps), keep interface pluggable
- **PTS**: extract extractor PTS; for decoded PCM we track a sample index → PTS affine map (monotonic fallback)
- **Memory**: stream to disk (.pcm) vs RAM. We support both; default is stream to disk to avoid OOM on long media

### Design

**Pipeline Flow:**
```
Broadcast (ACTION_DECODE_URI) → WorkManager DecodeWorker → DecodePipeline.decodeUriToMono16k()
→ (WavReader | AacDecoder) → Normalizer (downmix+resample) → PcmWriter (.pcm + .json)
```

**Key Control Knots:**
- `TARGET_SR` (default 16_000)
- `TARGET_CH` (default 1/mono)
- `PCM_FORMAT` (PCM_16)
- `RESAMPLER` (LINEAR | SINC)
- `DOWNMIX` (AVERAGE | LEFT | RIGHT)
- `DECODE_BUFFER_MS` (e.g., 250)
- `TIMESTAMP_POLICY` (ExtractorPTS | Monotonic)
- `OUTPUT_MODE` (FILE | RAM)
- `SAVE_SIDE_CAR` (true) – codec, bit-rate, in/out SR/CH, length, SHA256 of PCM

### Implementation Architecture

#### 1. Audio Frontend Configuration
```kotlin
object AudioFrontEndConfig {
    const val TARGET_SAMPLE_RATE = 16000
    const val TARGET_CHANNELS = 1
    const val PCM_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val RESAMPLER = "LINEAR"
    const val DOWNMIX = "AVERAGE"
    const val DECODE_BUFFER_MS = 250
    const val TIMESTAMP_POLICY = "ExtractorPTS"
    const val OUTPUT_MODE = "FILE"
    const val SAVE_SIDE_CAR = true
}
```

#### 2. Audio Decoder
```kotlin
class AudioDecoder {
    fun decodeUriToMono16k(
        uri: Uri,
        context: Context
    ): DecodeResult {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        
        val codec = MediaCodec.createDecoderByType(getAudioFormat(extractor))
        val normalizer = Normalizer(
            targetSampleRate = TARGET_SAMPLE_RATE,
            targetChannels = TARGET_CHANNELS,
            resampler = RESAMPLER,
            downmix = DOWNMIX
        )
        
        return processAudio(extractor, codec, normalizer)
    }
}
```

#### 3. Whisper Worker
```kotlin
class WhisperWorker : Worker {
    override suspend fun doWork(): Result {
        return try {
            val audioUri = inputData.getString("audio_uri") ?: return Result.failure()
            val outputDir = inputData.getString("output_dir") ?: return Result.failure()
            
            // Decode audio to mono 16kHz
            val decodeResult = AudioDecoder().decodeUriToMono16k(
                Uri.parse(audioUri), 
                applicationContext
            )
            
            // Process with Whisper
            val whisperResult = WhisperEngine().transcribe(
                audioData = decodeResult.pcmData,
                language = "auto",
                translate = false
            )
            
            // Save results
            saveTranscript(outputDir, whisperResult)
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Whisper processing failed", e)
            Result.failure()
        }
    }
}
```

### Scale-out Plan

#### Single (Default Configuration)
```json
{
  "preset": "SINGLE",
  "decode": "mp4_aac_hw_first",
  "mic": { "buffer_ms": 160, "hop_ms": 10 },
  "resampler": "linear",
  "timestamp_policy": "hybrid_pts_sample",
  "io": { "pcm_chunk_ms": 500, "json_chunk": 128, "wm_profile": "balanced" },
  "obs": { "enable": true, "period_ms": 2000, "level": "info", "backend": "file" }
}
```

#### Ablations (Performance Variants)

**A. Low-latency Mic**
```json
{
  "preset": "LOW_LATENCY_MIC",
  "mic": { "buffer_ms": 120 },
  "io": { "json_chunk": 64, "wm_profile": "throughput" }
}
```

**B. Accuracy-leaning**
```json
{
  "preset": "ACCURACY_LEANING",
  "resampler": "polyphase_sinc"
}
```

**C. Web-robust Ingest**
```json
{
  "preset": "WEB_ROBUST_INGEST",
  "decode": "mp4_aac_plus_webm_opus_mp3_hw_first_sw_fallback"
}
```

**D. High-throughput Batch**
```json
{
  "preset": "HIGH_THROUGHPUT_BATCH",
  "io": { "pcm_chunk_ms": 300, "json_chunk": 64, "wm_profile": "throughput" },
  "obs": { "period_ms": 1000, "level": "debug" }
}
```

### Code Pointers

- **Audio Frontend**: `core/audio/AudioFrontEndConfig.kt`
- **Audio Decoder**: `core/audio/AudioDecoder.kt`
- **Normalizer**: `core/audio/Normalizer.kt`
- **Whisper Engine**: `feature/whisper/src/main/java/com/mira/com/feature/whisper/runner/WhisperEngine.kt`
- **Whisper Worker**: `feature/whisper/src/main/java/com/mira/com/feature/whisper/runner/TranscribeWorker.kt`
- **Android Bridge**: `app/src/main/java/com/mira/whisper/AndroidWhisperBridge.kt`
