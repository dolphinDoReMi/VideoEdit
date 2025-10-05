# Whisper Full Scale Implementation Details

## Production-Ready Android/Kotlin Implementation

### Problem Disaggregation

- **Inputs**: MP4/AAC files, WAV/PCM16 files (no FFmpeg dependency)
- **Outputs**: Mono 16 kHz PCM16 + JSON sidecar with stable PTS mapping
- **Runtime Surfaces**: Broadcast → WorkManager job → LID pipeline → Storage writer
- **Isolation**: 
  - Preserve existing applicationId
  - Debug variant uses applicationIdSuffix ".debug" (side-by-side install)
  - All actions/authorities use ${applicationId} placeholders

### Analysis with Trade-offs

- **Multilingual vs English-only**: Must use multilingual models (no ".en" suffix) for LID accuracy
- **Model Size**: base/small LID >> tiny. Memory/latency trade-off on device
- **Quantization**: q5_1 is optimal; consider q5_0/q6/FP16 for LID robustness
- **Two-pass LID**: Cheap first pass + top-k forced decoding is very robust; costs ~1.3-1.8× time
- **VAD Pre-processing**: 10-30s voiced window gives cleaner LID; minimal cost

### Design

**Pipeline Flow:**
```
Broadcast (ACTION_RUN) → WhisperReceiver → WhisperApi → TranscribeWorker 
→ LanguageDetectionService → WhisperBridge → whisper.cpp → Enhanced Sidecar
```

**Key Control Knots (all exposed):**
- `TARGET_SR` (default 16_000)
- `TARGET_CH` (default 1/mono)
- `MODEL_PATH` (whisper-base.q5_1.bin)
- `LANG` ("auto" | forced language)
- `TRANSLATE` (false for LID)
- `DETECT_LANGUAGE` (true)
- `NO_CONTEXT` (true)
- `CONFIDENCE_THRESHOLD` (0.80)
- `VAD_WINDOW_MS` (20000)
- `MIN_VOICED_MS` (4000)

**Isolation & Namespacing:**
- Broadcast actions: `${applicationId}.whisper.RUN`
- Work names: `${BuildConfig.APPLICATION_ID}::transcribe::<hash(uri)>`
- File authorities: `${applicationId}.files`
- Debug install: applicationIdSuffix ".debug" → all names differ automatically

### Implementation Architecture

#### 1. LanguageDetectionService
```kotlin
// VAD + Two-pass LID Pipeline
class LanguageDetectionService {
    fun detectLanguage(
        pcm16: ShortArray,
        sampleRate: Int,
        modelPath: String,
        threads: Int = 4
    ): LanguageDetectionResult {
        // Pass 0: VAD pre-processing
        val voicedWindow = extractVoicedWindow(pcm16, sampleRate)
        
        // Pass 1: Whisper auto-LID
        val lidResult = detectLanguageFromAudio(voicedWindow, sampleRate, modelPath, threads)
        
        // Pass 2: Two-pass re-scoring for uncertain cases
        if (lidResult.confidence < CONFIDENCE_THRESHOLD) {
            return rescoreUncertainLanguage(voicedWindow, sampleRate, modelPath, lidResult.topK, threads)
        }
        
        return lidResult
    }
}
```

#### 2. TranscribeWorker (Background Processing)
```kotlin
class TranscribeWorker : Worker {
    override suspend fun doWork(): Result {
        // 1. Load & condition audio
        val pcm = AudioIO.loadPcm16(ctx, Uri.parse(uri))
        val mono = AudioResampler.downmixToMono(pcm.pcm16, pcm.ch)
        val pcm16k = AudioResampler.resampleLinear(mono, pcm.sr, 16_000)

        // 2. Robust LID Pipeline
        val lidResult = if (lang == "auto") {
            val lidService = LanguageDetectionService()
            lidService.detectLanguage(pcm16k, 16_000, model, threads)
        } else {
            LanguageDetectionResult(/* forced language */)
        }

        // 3. Decode with detected/forced language
        val json = WhisperBridge.decodeJson(
            pcm16 = pcm16k,
            sampleRate = 16_000,
            modelPath = model,
            threads = threads,
            beam = beam,
            lang = lidResult.chosen,
            translate = translate,
            temperature = 0.0f,
            enableWordTimestamps = false,
            detectLanguage = false, // Already done above
            noContext = true
        )

        // 4. Enhanced sidecar with LID data
        val lidService = LanguageDetectionService()
        val lidSidecar = lidService.generateLidSidecar(lidResult)
        sidecar.put("lid", lidSidecar)

        return Result.success()
    }
}
```

#### 3. WhisperApi (Multilingual Model Selection)
```kotlin
object WhisperApi {
    fun enqueueTranscribe(
        ctx: Context,
        uri: String,
        model: String,
        threads: Int,
        beam: Int,
        lang: String?,
        translate: Boolean,
    ) {
        // Use multilingual model by default for robust LID
        val multilingualModel = if (model.contains(".en")) {
            model.replace(".en", "").replace("tiny", "base")
        } else {
            model
        }
        
        val data = workDataOf(
            "uri" to uri,
            "model" to multilingualModel,
            "threads" to threads,
            "beam" to beam,
            "lang" to (lang ?: "auto"),
            "translate" to translate,
        )
        // ... enqueue work
    }
}
```

### Enhanced Sidecar Logging

```json
{
  "job_id": "whisper_test_003",
  "uri": "file:///sdcard/video_v1_long.mp4",
  "preset": "Single",
  "model_sha": "sha_model_12345678",
  "audio_sha": "sha_audio_87654321",
  "transcript_sha": "sha_txt_abcdef12",
  "segments_sha": "sha_seg_12fedcba",
  "rtf": 0.45,
  "created_at": 1728061878000,
  "lid": {
    "topk": [
      {"lang": "zh", "p": 0.62},
      {"lang": "en", "p": 0.35}
    ],
    "chosen": "zh",
    "method": "auto+forced",
    "threshold": 0.80,
    "confidence": 0.62
  }
}
```

### Scale-out Plan (Control Knot Modifications)

#### Single (Default Configuration)
```json
{
  "preset": "SINGLE",
  "model": "whisper-base.q5_1.bin",
  "lid": { "confidence_threshold": 0.80, "vad_window_ms": 20000 },
  "decode": { "lang": "auto", "translate": false, "detect_language": true },
  "processing": { "threads": 6, "beam": 1, "temperature": 0.0 },
  "io": { "sidecar": true, "rtf_target": 0.8 }
}
```

#### Ablations (Performance Variants)

**A. Low-latency Processing**
```json
{
  "preset": "LOW_LATENCY",
  "lid": { "vad_window_ms": 10000, "min_voiced_ms": 2000 },
  "processing": { "threads": 8, "beam": 0 },
  "io": { "rtf_target": 0.5 }
}
```

**B. Accuracy-leaning**
```json
{
  "preset": "ACCURACY_LEANING",
  "model": "whisper-small.q5_1.bin",
  "lid": { "confidence_threshold": 0.70 },
  "processing": { "beam": 2, "temperature": 0.1 }
}
```

**C. High-throughput Batch**
```json
{
  "preset": "HIGH_THROUGHPUT_BATCH",
  "lid": { "vad_window_ms": 30000 },
  "processing": { "threads": 4, "beam": 0 },
  "io": { "rtf_target": 1.2 }
}
```

**D. Multilingual Robust**
```json
{
  "preset": "MULTILINGUAL_ROBUST",
  "lid": { "confidence_threshold": 0.85, "force_rescore": true },
  "processing": { "beam": 1, "temperature": 0.0 }
}
```

### Code Pointers

- **LanguageDetectionService**: `feature/whisper/src/main/java/com/mira/com/feature/whisper/engine/LanguageDetectionService.kt`
- **TranscribeWorker**: `feature/whisper/src/main/java/com/mira/com/feature/whisper/runner/TranscribeWorker.kt`
- **WhisperApi**: `feature/whisper/src/main/java/com/mira/com/feature/whisper/api/WhisperApi.kt`
- **WhisperParams**: `feature/whisper/src/main/java/com/mira/com/feature/whisper/engine/WhisperParams.kt`
- **WhisperBridge**: `feature/whisper/src/main/java/com/mira/com/feature/whisper/engine/WhisperBridge.kt`
- **WhisperReceiver**: `feature/whisper/src/main/java/com/mira/com/feature/whisper/runner/WhisperReceiver.kt`

### Deployment Scripts

- **Model Deployment**: `deploy_multilingual_models.sh`
- **LID Testing**: `test_lid_pipeline.sh`
- **Multilingual Testing**: `test_multilingual_lid.sh`
- **End-to-end Testing**: `work_through_video_v1.sh`
- **Device Testing**: `work_through_xiaomi_pad.sh`
- **Validation**: `validate_cicd_pipeline.sh`