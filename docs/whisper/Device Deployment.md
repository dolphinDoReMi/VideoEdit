# Whisper Device Deployment

## Xiaomi Pad Optimization

### Device Specifications
- **Model**: Xiaomi Pad Ultra
- **RAM**: 11.8GB
- **Storage**: 256GB+ SSD
- **CPU**: Snapdragon 870 with ARM64 NEON support
- **Android**: API 30+ (Android 11+)

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

# Test Vulkan backend
adb shell am broadcast -a com.mira.com.action.TEST_VULKAN_BACKEND

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

# Test different thread counts
adb shell am broadcast -a com.mira.com.action.TEST_THREAD_COUNT --ei threads 2
adb shell am broadcast -a com.mira.com.action.TEST_THREAD_COUNT --ei threads 4
adb shell am broadcast -a com.mira.com.action.TEST_THREAD_COUNT --ei threads 6
```

**Model Optimization Issues:**
```bash
# Test different model sizes
adb shell am broadcast -a com.mira.com.action.TEST_MODEL_SIZE --es model "tiny.en"
adb shell am broadcast -a com.mira.com.action.TEST_MODEL_SIZE --es model "small.en"
adb shell am broadcast -a com.mira.com.action.TEST_MODEL_SIZE --es model "base.en"

# Test different quantization levels
adb shell am broadcast -a com.mira.com.action.TEST_QUANTIZATION --es quantization "Q4_0"
adb shell am broadcast -a com.mira.com.action.TEST_QUANTIZATION --es quantization "Q5_1"
adb shell am broadcast -a com.mira.com.action.TEST_QUANTIZATION --es quantization "Q8_0"
```

#### Debug Commands
```bash
# Check audio processing
adb shell dumpsys media.audio_flinger

# Monitor memory usage
adb shell dumpsys meminfo com.mira.com

# Check Whisper logs
adb logcat | grep Whisper

# Test audio format
adb shell am broadcast -a com.mira.com.action.TEST_AUDIO_FORMAT
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
7. **Notification**: Send completion notifications via broadcast

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

# Trigger Whisper processing
adb shell am broadcast -a com.mira.com.action.WHISPER_RUN \
  --es input_file "/sdcard/Mira/inbox/audio/sample.wav" \
  --es output_dir "/sdcard/Mira/output/transcripts/"

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