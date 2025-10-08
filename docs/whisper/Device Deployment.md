# Whisper Device Deployment

## Xiaomi Pad Optimization

### Device Specifications
- **Model**: Xiaomi Pad Ultra
- **RAM**: 11.8GB
- **Storage**: 256GB+ SSD
- **CPU**: Snapdragon 870 with ARM64 NEON support
- **Android**: API 30+ (Android 11+)

### Scoped Storage Configuration
**Important**: All file operations must use Android scoped storage to ensure proper file access permissions.

**Scoped Storage Paths:**
```bash
# Base scoped storage directory
SCOPED_BASE="/storage/emulated/0/Android/data/com.mira.com/files"

# Directory structure
MODELS_DIR="$SCOPED_BASE/MiraWhisper/models"
INPUT_DIR="$SCOPED_BASE/MiraWhisper/in"
OUTPUT_DIR="$SCOPED_BASE/MiraWhisper/out"
SIDECAR_DIR="$SCOPED_BASE/MiraWhisper/sidecars"
```

**Directory Creation:**
```bash
adb shell "mkdir -p $SCOPED_BASE/MiraWhisper/{models,in,out,sidecars}"
```

### Working Command Flow for Clip Processing

#### Successful Processing Sequence
The following command flow has been verified to work reliably for processing audio/video clips on Xiaomi Pad:

**1. Launch App (Bring to Foreground)**
```bash
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
```

**2. Start DirectWhisperService**
```bash
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/tennis_interview_clip_002.mp4" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"
```

**3. Monitor Processing**
```bash
adb logcat -s TranscribeWorker:V -d | tail -10
```

#### Key Success Factors
- **App Must Be in Foreground**: Service fails with "app is in background" error if app goes to background
- **Language Forced to "en"**: Prevents LID (Language Detection) hangs that occur with "auto" detection
- **DirectWhisperService**: Uses service-based processing with WorkManager instead of broadcast receivers
- **Storage Self-Test**: Must pass app-scoped storage directory creation for processing to continue

#### Troubleshooting Direct Service Issues

**Service Not Starting:**
```bash
# Error: Service not responding
# Solution: Check app state and restart service
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity && sleep 5
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"
```

**Storage Self-Test Failures:**
```bash
# Error: "Failed to create job directory"
# Check app-scoped storage permissions
adb shell "ls -la /storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/"
```

**Processing Hangs:**
```bash
# Check if WorkManager jobs are progressing
adb logcat -s TranscribeWorker:V -d | grep -E "(Storage self-test|Audio loaded|LID|Whisper|Processing|Completed)"

# Check WorkManager job status
adb shell dumpsys jobscheduler | grep com.mira.com
```

**Service Communication Issues:**
```bash
# Check if service is running
adb shell ps | grep com.mira.com

# Check service logs
adb logcat -s DirectWhisperService:V -d | tail -20

# Verify service can be started
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"
```

#### Alternative Processing Methods

**Direct Service Processing (Recommended):**
```bash
# Use direct service for reliable processing
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/tennis_interview_clip_002.mp4" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"
```

**Batch Processing:**
```bash
# Process multiple files
for file in /storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/*.mp4; do
  adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$file" \
    --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin" \
    --es "threads" "4" \
    --es "lang" "en"
done
```

### Performance Optimization

#### Model Optimization
- **GGUF Quantization**: Q5_1 quantization for optimal speed/accuracy balance
- **Model Variants**: tiny.en (39MB), base.en (142MB), small.en (244MB)
- **Memory Management**: Streaming processing for files >100MB
- **Chunking Strategy**: 30-second chunks with overlap handling

#### Advanced Optimization Control Knots

**Compute & Runtime Optimization:**
- **Backend Selection**: Vulkan GPU provides significant speedups if driver exposes FP16 and 16-bit storage
- **CPU Fallback**: Universal compatibility when Vulkan unavailable
- **Build Flag**: `GGML_VULKAN=1` enables Vulkan backend
- **Thread Configuration**: 4 threads optimal for Xiaomi Pad Snapdragon 870

**Model Choice & Weight Format:**
- **Model Size**: small.en-Q5_1 recommended for balanced performance
- **Quantization Strategy**: Q5_1 (sweet spot), Q8_0 (quality), Q4_* (memory-constrained)
- **Memory Constraints**: 11.8GB RAM allows medium models with Q8_0 quantization

**Audio Windowing & Context:**
- **Audio Context**: Default ~1500 frames (~30s) for batch processing
- **Realtime Mode**: Reduced to 1024 frames for lower latency
- **Chunking Strategy**: 30-second chunks optimal for memory/performance balance

**Decoding Strategy:**
- **Realtime**: Greedy decoding with temperature=0 for lowest latency
- **Batch Processing**: Beam search (beam size ~5) for higher accuracy
- **Noisy Audio**: Conservative VAD thresholds with log-prob filtering

#### Audio Processing
- **Sample Rate**: 16kHz mono (ASR-ready)
- **Format Support**: WAV, MP4/AAC, MP3
- **Resampling**: Linear resampler for speed
- **Downmixing**: Average stereo to mono

#### Vulkan Backend Optimization

**Driver Requirements:**
- **FP16 Support**: Driver must expose FP16 capabilities
- **16-bit Storage**: Driver must support 16-bit storage
- **Verification**: Use `vulkaninfo` to check capabilities

**Common Issues:**
- **Error Message**: "device does not support 16-bit storage"
- **Cause**: Driver capability limitation, not a Whisper setting
- **Solution**: Fall back to CPU if Vulkan requirements not met

**Build Configuration:**
```bash
# Enable Vulkan backend
export GGML_VULKAN=1

# Verify Vulkan support
vulkaninfo | grep -E "(FP16|16-bit storage)"
```

### Deployment Scripts

#### Model Deployment
```bash
# Deploy Whisper models to Xiaomi Pad
cd docs/whisper/scripts
./deploy_multilingual_models.sh

# Verify model integrity
./verify_whisper_models.sh

# Test audio processing
./test_audio_processing.sh
```

#### Performance Testing
```bash
# Benchmark Whisper processing
./benchmark_whisper_processing.sh

# Test memory usage
./test_memory_usage.sh

# Validate transcript quality
./validate_transcript_quality.sh
```

### Configuration

#### Optimized Configuration Combinations

**Realtime Captions (On-device UI):**
```kotlin
val realtimeConfig = WhisperConfig(
    backend = Backend.VULKAN_IF_AVAILABLE,
    model = Model.SMALL_EN_Q5_1,
    decoding = DecodingStrategy.GREEDY,
    temperature = 0.0f,
    audioContext = 1024,
    noSpeechThreshold = 0.6f,
    threads = 4,
    chunkSize = 30,
    memoryMode = "STREAMING"
)
```

**Batch Processing (Higher Accuracy):**
```kotlin
val batchConfig = WhisperConfig(
    backend = Backend.VULKAN_OR_CPU,
    model = Model.MEDIUM_Q5_1,
    decoding = DecodingStrategy.BEAM_SEARCH,
    beamSize = 5,
    temperature = 0.0f,
    audioContext = 1500,
    logProbThreshold = -1.0f,
    compressionRatioThreshold = 2.4f,
    threads = 4,
    chunkSize = 30,
    memoryMode = "STREAMING"
)
```

**Noisy Meetings / Music Background:**
```kotlin
val noisyConfig = WhisperConfig(
    language = "en", // Fixed language
    vadEnabled = true,
    vadThreshold = 0.6f,
    minSpeechDuration = 0.5f,
    temperature = 0.0f,
    logProbThreshold = -1.0f,
    compressionRatioThreshold = 2.4f,
    threads = 4,
    chunkSize = 30,
    memoryMode = "STREAMING"
)
```

**Low-memory Profile:**
```kotlin
val lowMemoryConfig = WhisperConfig(
    model = Model.TINY_Q4_0,
    decoding = DecodingStrategy.GREEDY,
    audioContext = 768,
    threads = 2,
    chunkSize = 30,
    memoryMode = "STREAMING"
)
```

#### Performance Tuning Guidelines
- **Thread Count**: 4 threads optimal for Snapdragon 870 big cores
- **Chunk Size**: 30 seconds for memory/performance balance
- **Memory Mode**: Streaming for files >100MB
- **Model Selection**: small.en-Q5_1 for balanced accuracy/speed
- **Vulkan Backend**: Enable when FP16/16-bit storage supported
- **Audio Context**: 1024 for realtime, 1500 for batch processing

### Monitoring

#### Performance Metrics
- **RTF**: 0.3-0.8 (real-time factor)
- **Memory Usage**: ~200MB for base model
- **Accuracy**: >95% on standard benchmarks
- **Language Detection**: >85% accuracy for Chinese
- **Vulkan Acceleration**: 2-3x speedup when supported
- **Thermal Management**: Automatic throttling at 45°C

#### Advanced Performance Monitoring
```kotlin
class XiaomiPadPerformanceMonitor {
    fun trackVulkanUsage(): Boolean
    fun trackThermalStatus(): Float
    fun trackMemoryPressure(): Long
    fun trackProcessingLatency(): Long
    fun trackAccuracyMetrics(): Float
}
```

#### Debug Tools
- **Audio Profiler**: Real-time audio processing profiling
- **Memory Monitor**: Real-time memory usage tracking
- **Transcript Validator**: Quality validation and metrics
- **Error Logger**: Comprehensive error logging
- **Vulkan Info**: Driver capability verification
- **Thermal Monitor**: Device temperature tracking

### Troubleshooting

#### Model Loading Reliability (GGML_ERROR_MODEL_LOAD_FAILED)

**Root Cause**: Whisper.cpp can't mmap and parse the model file. On Android this almost always comes from:
- Wrong file format or truncated/compressed asset
- Giving a content:// URI instead of a real file path
- Packaging/ABI/memory mismatches

**Error GGML_ERROR_MODEL_LOAD_FAILED (-3)** means "the bytes I opened are not a valid, readable model."

#### Surgical Troubleshooting Checklist (Ranked by Frequency)

**1. Model Format/Header Mismatch (Most Common)**

*Symptom*: Logs show "bad magic / not GGUF / cannot parse tensor X", then MODEL_LOAD_FAILED.

*Why*: Newer whisper.cpp expects GGUF models. If you pass an old GGML (.bin) model to a build that expects GGUF (or vice-versa), the header check fails.

*Fix*:
- Use a model that matches your whisper.cpp commit
- GGUF example: whisper-small-q5_1.gguf
- Don't rename extensions
- Quick header test: `hexdump -C -n 4 /path/to/model.gguf` → should print `47 47 55 46` (GGUF)

**2. Asset Got Compressed or Truncated in APK (Very Common on Android)**

*Symptom*: Works from `/sdcard/...` but fails when read from `assets/`. File size reported by loader is smaller than the original model.

*Why*: By default, Gradle compresses assets, breaking offset/length mapping; the native loader then reads garbage.

*Fix (AGP 8+)*:
```gradle
android {
  androidResources {
    noCompress += ['.gguf', '.ggml', '.bin']
  }
}
```

*Fix (older AGP)*:
```gradle
android {
  aaptOptions {
    noCompress 'gguf', 'ggml', 'bin'
  }
}
```

*Safer Pattern*: On first run, stream-copy from assets → app files dir (uncompressed real file) and load from that absolute path.

**3. Content:// URI (SAF) Passed to Native Code**

*Symptom*: Works if you copy the same file to `/data/data/<pkg>/files/models/...` and load by path; fails if using a content://… string.

*Why*: The C++ loader wants a filesystem path it can open()/mmap(). A content:// URI is not a path.

*Fix*: Always resolve/copy the picked file into your app's private dir and pass that absolute path to JNI.

**4. ABI / Build Flag / Vulkan Backend Mismatch**

*Symptoms*: Loading fails only on certain builds (e.g., armeabi-v7a) or only when Vulkan is enabled.

*Why*:
- 32-bit builds can run out of VA space on large models
- Wrong combination of GGML_USE_VULKAN vs runtime driver
- Stripped symbols not the issue; memory/map is

*Fix*:
- Ship arm64-v8a only for Whisper
- If Vulkan is enabled, confirm device has the required extensions; otherwise build CPU-only or add a runtime fallback

**5. File Permission / Location Issues (MIUI Specifics)**

*Symptoms*: Model under `/sdcard/Android/data/<other.pkg>/...` or random Downloads path fails on MIUI; same file loads from your app dir.

*Why*: Scoped storage + MIUI quirks. NDK open() can fail silently for some external paths.

*Fix*: Keep models in `context.getFilesDir()/models` or `getExternalFilesDir("models")` that belongs to your package.

**6. Corrupted/Incomplete Download**

*Symptoms*: Header is GGUF, size looks right-ish, still fails mid-parse.

*Why*: Partial downloads (e.g., Drive/Browser cutoffs) or resume errors.

*Fix*: Verify SHA-256 and exact size against the source. Re-download if mismatch.

#### Model Loading Preflight Check Implementation

**Kotlin Preflight Function**:
```kotlin
suspend fun ensureModelPath(ctx: Context, src: Uri, nameHint: String): File {
    val modelsDir = File(ctx.filesDir, "models").apply { mkdirs() }
    val out = File(modelsDir, nameHint)
    
    // Copy file from URI to app directory
    ctx.contentResolver.openInputStream(src).use { ins ->
        out.outputStream().use { outs -> ins!!.copyTo(outs) }
    }
    
    // Quick header sanity check: must start with "GGUF"
    ctx.contentResolver.openInputStream(src).use { ins ->
        val header = ByteArray(4)
        ins!!.read(header)
        require(String(header) == "GGUF") { 
            "Model is not GGUF (header=${String(header)})" 
        }
    }
    return out
}
```

**JNI-Side Logging**:
```cpp
#include <android/log.h>
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "WhisperJNI", __VA_ARGS__)

static void* load_model_checked(const char* path) {
    FILE* f = fopen(path, "rb");
    if (!f) { 
        LOGE("fopen failed: %s", path); 
        return nullptr; 
    }
    
    char hdr[4] = {0};
    fread(hdr, 1, 4, f); 
    fseek(f, 0, SEEK_SET);
    
    if (strncmp(hdr, "GGUF", 4) != 0) { 
        LOGE("Bad header: '%.4s' (expected GGUF)", hdr); 
        fclose(f); 
        return nullptr; 
    }
    fclose(f);
    
    // Call whisper.cpp's model loader with detailed status logs enabled
    return whisper_model_load_from_file(path, /*params*/);
}
```

#### Prioritized Actions (Fastest to Slowest ROI)

1. **Confirm header/format**: `hexdump` first 4 bytes → must be GGUF
2. **Disable compression & copy to app dir**: Load from an absolute path
3. **Ship arm64-v8a only**: Test CPU path first; add Vulkan later
4. **Verify SHA-256 & size**: Of the model file
5. **If still failing**: Post the first 20 log lines from the loader (with the JNI header check added)

#### One-Liner Solution

Because the native loader isn't seeing a valid, mmap-able GGUF file at a real path. Make sure the model is GGUF, not compressed in the APK, copied to your app's files dir, and that you're loading on arm64 with sane memory. After those four, GGML_ERROR_MODEL_LOAD_FAILED goes away.

#### Using the Model Loading Reliability Features

**Preflight Check Before Loading:**
```kotlin
import com.mira.com.feature.whisper.utils.ModelValidationUtils

// Validate model before loading
val preflightResult = ModelValidationUtils.preflightCheck(context, modelPath)
if (!preflightResult.isReady) {
    Log.e(TAG, "Model preflight failed: ${preflightResult.summary}")
    preflightResult.errors.forEach { Log.e(TAG, it) }
    return
}

// Safe to load model
val whisperContext = WhisperBridge.decodeJson(...)
```

**Enhanced Model Path Handling:**
```kotlin
// For content:// URIs, use the safe copy function
val modelFile = ModelValidationUtils.ensureModelPath(context, contentUri, "whisper-base.gguf")
val whisperContext = WhisperBridge.decodeJson(
    pcm16 = audioData,
    sampleRate = 16000,
    modelPath = modelFile.absolutePath, // Use absolute path
    threads = 4,
    lang = "en"
)
```

**Model Validation:**
```kotlin
// Validate existing model file
val validation = ModelValidationUtils.validateModelFile("/path/to/model.gguf")
if (!validation.isValid) {
    Log.e(TAG, "Model validation failed: ${validation.error}")
    return
}

Log.i(TAG, "Model validated: ${validation.fileSize} bytes, checksum: ${validation.checksum}")
```

#### Common Issues
1. **Model Loading Failures**: Check model file integrity and storage permissions
2. **Audio Processing Errors**: Validate input format (16kHz, mono, PCM16)
3. **Performance Issues**: Monitor RTF and adjust thread count
4. **Language Detection Problems**: Check LID confidence thresholds
5. **Vulkan Backend Issues**: Verify driver FP16/16-bit storage support
6. **Thermal Throttling**: Reduce threads, monitor device temperature
7. **Memory Pressure**: Use smaller models or reduce audio context
8. **Poor Accuracy**: Increase model size, use beam search, adjust silence thresholds

#### Advanced Troubleshooting

**Vulkan Backend Issues:**
```bash
# Check Vulkan support
vulkaninfo | grep -E "(FP16|16-bit storage)"

# Test Vulkan backend via direct service
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_audio.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"

# Fallback to CPU if Vulkan fails
export GGML_VULKAN=0
```

**Performance Optimization Issues:**
```bash
# Monitor thermal status
adb shell dumpsys thermal

# Check memory pressure
adb shell dumpsys meminfo com.mira.com

# Monitor CPU usage
adb shell top -n 1 | grep com.mira.com

# Test different thread counts via direct service
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_audio.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin" \
  --es "threads" "2" \
  --es "lang" "en"

adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_audio.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"

adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_audio.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin" \
  --es "threads" "6" \
  --es "lang" "en"
```

**Model Optimization Issues:**
```bash
# Test different model sizes via direct service
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_audio.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/whisper-tiny.en.q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"

adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_audio.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/whisper-small.en.q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"

adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_audio.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/whisper-base.en.q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"

# Test different quantization levels
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_audio.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/whisper-base.en.q4_0.bin" \
  --es "threads" "4" \
  --es "lang" "en"

adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_audio.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/whisper-base.en.q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"

adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_audio.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/whisper-base.en.q8_0.bin" \
  --es "threads" "4" \
  --es "lang" "en"
```

#### Debug Commands
```bash
# Check audio processing
adb shell dumpsys media.audio_flinger

# Monitor memory usage
adb shell dumpsys meminfo com.mira.com

# Check Whisper logs
adb logcat | grep Whisper

# Test audio format via direct service
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/in/test_audio.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"
```

### iPad Deployment

#### Device Specifications
- **Model**: iPad Pro (M1/M2)
- **RAM**: 8GB+ unified memory
- **Storage**: 128GB+ SSD
- **CPU**: Apple Silicon with Neural Engine
- **iOS**: iOS 16+

#### Core ML Integration
- **Model Format**: Core ML optimized models
- **Neural Engine**: Hardware-accelerated inference
- **Memory Management**: Unified memory architecture
- **Performance**: Optimized for Apple Silicon

#### Deployment Process
```bash
# Convert GGUF to Core ML
python convert_to_coreml.py

# Deploy to iPad
cd docs/whisper/scripts
./deploy_ipad_whisper.sh

# Test Core ML performance
./test_coreml_performance.sh
```

### Cross-Platform Considerations

#### Android (Primary)
- **GGUF Models**: Quantized models for efficiency
- **MediaCodec**: Hardware-accelerated audio decoding
- **Memory**: Traditional RAM architecture
- **Performance**: Optimized for ARM64

#### iOS (Secondary)
- **Core ML**: Apple's machine learning framework
- **Neural Engine**: Hardware acceleration support
- **Memory**: Unified memory architecture
- **Performance**: Optimized for Apple Silicon

#### Web (Tertiary)
- **WebAssembly**: Near-native performance
- **Web Audio API**: Audio processing support
- **Memory**: Browser memory management
- **Performance**: Optimized for web browsers

### Job Scheduling System

#### Device-Level Job Management
The Whisper system operates as part of a comprehensive job scheduling system on Android devices, particularly optimized for Xiaomi Pad deployment.

#### Input/Output Directory Structure
```
/sdcard/Mira/
├── inbox/                    # Input directory
│   ├── audio/               # Audio files (.wav, .mp4)
│   ├── video/               # Video files (.mp4, .mov)
│   └── batch/               # Batch processing files
├── processing/              # Active processing directory
│   ├── temp/               # Temporary files during processing
│   ├── chunks/             # Audio/video chunks
│   └── intermediate/       # Intermediate processing files
├── output/                  # Output directory
│   ├── transcripts/        # Whisper transcription results
│   ├── clips/              # AutoClipper video clips
│   ├── metadata/           # Processing metadata
│   └── exports/            # Exported files (JSON, SRT, TXT)
├── models/                  # ML models storage
│   ├── whisper/            # Whisper model files
│   └── clip/               # CLIP model files
└── logs/                    # System logs
    ├── whisper/            # Whisper processing logs
    ├── autoclip/           # AutoClipper service logs
    └── system/             # System resource logs
```

#### Job Scheduling Workflow
1. **File Detection**: Monitor `/sdcard/Mira/inbox/audio/` for new audio files
2. **Job Creation**: Create WorkManager WhisperWorker jobs for detected files
3. **Resource Check**: Verify device resources (CPU, memory, battery) before processing
4. **Processing**: Execute Whisper transcription with chunking for large files
5. **Output Generation**: Save transcripts to `/sdcard/Mira/output/transcripts/`
6. **Cleanup**: Remove temporary files from `/sdcard/Mira/processing/`
7. **Notification**: Log completion status and update job state

#### WorkManager Job Configuration
```kotlin
val whisperJob = OneTimeWorkRequestBuilder<WhisperWorker>()
    .setInputData(workDataOf(
        "input_file" to "/sdcard/Mira/inbox/audio/sample.wav",
        "output_dir" to "/sdcard/Mira/output/transcripts/",
        "model_path" to "/sdcard/Mira/models/whisper/base.en.bin"
    ))
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .setRequiresBatteryNotLow(true)
        .setRequiresStorageNotLow(true)
        .build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .build()
```

#### Job Management Commands
```bash
# Setup directory structure
adb shell mkdir -p /sdcard/Mira/inbox/audio
adb shell mkdir -p /sdcard/Mira/output/transcripts

# Trigger Whisper processing via direct service
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
  --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
  --es "uri" "file:///sdcard/Mira/inbox/audio/sample.wav" \
  --es "model" "/storage/emulated/0/Android/data/com.mira.com/files/MiraWhisper/models/small.en-q5_1.bin" \
  --es "threads" "4" \
  --es "lang" "en"

# Monitor job status
adb shell dumpsys jobscheduler | grep com.mira.com

# Check processing results
adb shell ls -la /sdcard/Mira/output/transcripts/
```

### Future Enhancements

#### Planned Optimizations
- **GPU Acceleration**: OpenCL/Metal support for model inference
- **Model Compression**: Further quantization options (Q3_* for extreme memory constraints)
- **Pipeline Optimization**: Parallel processing with WorkManager coordination
- **Memory Optimization**: Advanced caching strategies and predictive memory allocation
- **Vulkan Optimization**: Enhanced Vulkan backend with better driver compatibility
- **Thermal Management**: Intelligent thermal throttling and workload scheduling

#### Performance Targets
- **Speed**: RTF < 0.1 (10x faster than real-time) with Vulkan acceleration
- **Memory**: <100MB peak usage with streaming processing
- **Accuracy**: Maintain 95%+ benchmark performance across all model sizes
- **Battery**: <2% per hour of processing with optimized thread scheduling
- **Thermal**: Maintain <45°C during continuous processing
- **Vulkan**: 2-3x speedup when driver supports FP16/16-bit storage

#### Advanced Features
- **Adaptive Optimization**: Dynamic model/backend selection based on device capabilities
- **Predictive Processing**: ML-based workload prediction and resource allocation
- **Multi-modal Integration**: Audio-video synchronization with CLIP processing
- **Cloud Fallback**: Seamless cloud processing for complex workloads
- **Real-time Analytics**: Live performance monitoring and optimization suggestions