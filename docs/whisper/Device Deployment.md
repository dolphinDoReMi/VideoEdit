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

#### Audio Processing
- **Sample Rate**: 16kHz mono (ASR-ready)
- **Format Support**: WAV, MP4/AAC, MP3
- **Resampling**: Linear resampler for speed
- **Downmixing**: Average stereo to mono

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

#### Xiaomi Pad Specific Settings
```kotlin
val xiaomiPadConfig = WhisperConfig(
    modelPath = "models/whisper-base.en.bin",
    sampleRate = 16000,
    channels = 1,
    language = "auto",
    translate = false,
    temperature = 0.0f,
    beamSize = 1,
    threads = 4,
    chunkSize = 30,
    memoryMode = "STREAMING"
)
```

#### Performance Tuning
- **Thread Count**: 4 threads for optimal CPU utilization
- **Chunk Size**: 30 seconds for memory/performance balance
- **Memory Mode**: Streaming for files >100MB
- **Model**: base.en for balanced accuracy/speed

### Monitoring

#### Performance Metrics
- **RTF**: 0.3-0.8 (real-time factor)
- **Memory Usage**: ~200MB for base model
- **Accuracy**: >95% on standard benchmarks
- **Language Detection**: >85% accuracy for Chinese

#### Debug Tools
- **Audio Profiler**: Real-time audio processing profiling
- **Memory Monitor**: Real-time memory usage tracking
- **Transcript Validator**: Quality validation and metrics
- **Error Logger**: Comprehensive error logging

### Troubleshooting

#### Common Issues
1. **Model Loading Failures**: Check model file integrity and storage permissions
2. **Audio Processing Errors**: Validate input format (16kHz, mono, PCM16)
3. **Performance Issues**: Monitor RTF and adjust thread count
4. **Language Detection Problems**: Check LID confidence thresholds

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
- **Model Compression**: Further quantization options
- **Pipeline Optimization**: Parallel processing
- **Memory Optimization**: Advanced caching strategies

#### Performance Targets
- **Speed**: RTF < 0.1 (10x faster than real-time)
- **Memory**: <100MB peak usage
- **Accuracy**: Maintain 95%+ benchmark performance
- **Battery**: <2% per hour of processing