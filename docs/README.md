# VideoEdit Documentation

## Overview

This documentation provides comprehensive guides for the VideoEdit multi-modal AI video processing platform, organized by feature and functionality.

## Documentation Structure

### Feature Documentation

#### [Whisper Speech Recognition](./whisper/)
- **Architecture Design and Control Knot**: Core design principles and control parameters
- **Full Scale Implementation Details**: Production-ready implementation guide
- **Device Deployment**: Xiaomi Pad and iPad deployment guides
- **XiaoMi Pad Inference Optimization**: Performance optimization for Xiaomi Pad
- **iPad Inference Optimization**: Performance optimization for iPad
- **Scripts**: Testing and deployment scripts

#### [CLIP Visual Understanding](./clip/)
- **Architecture Design and Control Knot**: Core design principles and control parameters
- **Full Scale Implementation Details**: Production-ready implementation guide
- **Device Deployment**: Xiaomi Pad and iPad deployment guides
- **XiaoMi Pad Inference Optimization**: Performance optimization for Xiaomi Pad
- **iPad Inference Optimization**: Performance optimization for iPad
- **Scripts**: Testing and deployment scripts

#### [Infrastructure Services](./infra/)
- **Architecture Design and Control Knot**: Core design principles and control parameters
- **Full Scale Implementation Details**: Production-ready implementation guide
- **Device Deployment**: Xiaomi Pad and iPad deployment guides
- **XiaoMi Pad Inference Optimization**: Performance optimization for Xiaomi Pad
- **iPad Inference Optimization**: Performance optimization for iPad

### Process Documentation

#### [CICD & Release Guide](./CICD%20&%20Release%20Guide/)
- **GitHub Guide**: Repository management and pull request process
- **CI/CD Workflows**: Automated testing and deployment pipelines
- **Release Process**: Version management and release procedures
- **Quality Assurance**: Code quality and testing standards
- **Monitoring and Alerting**: Build and deployment monitoring
- **Troubleshooting**: Common issues and solutions

#### [Cursor Rules](./cursor_rule.md)
- **Development Guidelines**: Coding standards and best practices
- **Code Organization**: Project structure and module dependencies
- **Testing Standards**: Unit, integration, and E2E testing
- **Performance Standards**: Memory, CPU, and storage optimization
- **Security Standards**: Data protection and code security
- **Deployment Standards**: Build configuration and release process

## Quick Start

### Installation
```bash
# Clone repository
git clone https://github.com/dolphinDoReMi/VideoEdit.git
cd VideoEdit

# Deploy Whisper models
cd docs/whisper/scripts
./deploy_multilingual_models.sh

# Deploy CLIP models
cd ../clip/scripts
./deploy_clip_model.sh

# Setup infrastructure
cd ../infra/scripts
./setup_infrastructure.sh
```

### Testing
```bash
# Test Whisper pipeline
cd docs/whisper/scripts
./test_comprehensive_pipeline.sh

# Test CLIP pipeline
cd ../clip/scripts
./test_clip_pipeline.sh

# Test infrastructure
cd ../infra/scripts
./test_infrastructure_consistency.sh

# Run E2E tests
cd ../whisper/scripts
./work_through_video_v1.sh
```

## Architecture Overview

### Core Components
- **Whisper Engine**: Speech recognition with robust language detection
- **CLIP Engine**: Video understanding and intelligent clip selection
- **Infrastructure Services**: File management and duplicate detection
- **AutoClipper Service**: Background video processing service
- **Resource Monitor**: Real-time resource tracking and management

### Data Flow
```
Video/Audio Input → Format Detection → Parallel Processing → Integration → App-Scoped Storage
     ↓                    ↓                    ↓              ↓              ↓
  CLIP Analysis → Video Understanding → Clip Selection → Time Alignment → SidecarStore
     ↓
  Whisper Analysis → Speech Recognition → Transcript Generation → Metadata Storage
     ↓
  Infrastructure → File Management → Duplicate Detection → Storage Optimization
     ↓
  Resource Monitoring → Performance Tracking → Error Recovery → Foreground Service
```

### Control Knots
- **Sample Rate**: 16 kHz (ASR-ready)
- **Channels**: Mono (downmix from stereo)
- **Models**: Configurable Whisper and CLIP model sizes
- **Language**: Auto-detection with manual override
- **Performance**: Configurable thread count and memory mode
- **Storage**: App-scoped storage with atomic writes
- **Resource Management**: Battery, storage, and memory constraints

## Performance

### Benchmarks
- **Whisper RTF**: 0.3-0.8 (real-time factor)
- **CLIP Speed**: 0.1s per frame on GPU
- **Memory Usage**: ~200MB for base models
- **Accuracy**: >95% on standard benchmarks
- **Language Detection**: >85% accuracy for Chinese
- **Storage Optimization**: Up to 40% space reduction

### Optimization
- **Model Quantization**: GGUF quantization for Whisper, optimized CLIP models
- **Memory Management**: Streaming processing for large files
- **Compute Optimization**: Vulkan backend for Whisper, GPU acceleration for CLIP
- **Storage**: App-scoped storage with atomic writes
- **Infrastructure**: Hash-based duplicate detection and cleanup

## Testing

### Test Scripts
- **Whisper Testing**: `docs/whisper/scripts/test_comprehensive_pipeline.sh`
- **CLIP Testing**: `docs/clip/scripts/test_clip_pipeline.sh`
- **Infrastructure Testing**: `docs/infra/scripts/test_infrastructure_consistency.sh`
- **E2E Testing**: `docs/whisper/scripts/work_through_video_v1.sh`

### Validation
- **Audio Format**: 16kHz, mono, PCM16 validation
- **Video Format**: MP4, MOV with proper codec support
- **Model Integrity**: SHA-256 hash verification
- **Transcript Quality**: Non-empty segments, ordered timestamps
- **Performance**: RTF and memory usage monitoring
- **Storage**: Hash-based duplicate detection accuracy

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
3. **Video Processing Errors**: Check video codec support and format
4. **Performance Issues**: Monitor RTF and adjust thread count
5. **Language Detection Problems**: Check LID confidence thresholds
6. **EPERM Errors**: Use app-scoped storage instead of public directories
7. **Worker Cancellation**: Ensure foreground service is properly configured
8. **Duplicate Detection Issues**: Check file permissions and hash calculation

### Debug Tools
- **Logging**: Comprehensive logging with configurable levels
- **Metrics**: Real-time performance metrics
- **Profiling**: Built-in performance profiler
- **Validation**: Automated validation scripts
- **Storage Self-Test**: Writability verification and diagnostics

## Future Enhancements

### Planned Features
- **Speaker Diarization**: Multi-speaker identification
- **Real-time Processing**: Live audio/video streaming
- **Custom Models**: Fine-tuned domain-specific models
- **Advanced Post-processing**: Punctuation and capitalization
- **Adaptive Chunking**: Dynamic chunk size based on content complexity
- **Advanced Video Clipping**: AI-powered clip selection with user preferences
- **Multi-modal Integration**: Enhanced audio-video synchronization
- **Advanced Infrastructure**: Distributed processing and intelligent caching

### Performance Improvements
- **GPU Acceleration**: OpenCL/Metal support for both Whisper and CLIP
- **Model Optimization**: Further quantization options
- **Pipeline Optimization**: Parallel processing for both audio and video
- **Memory Optimization**: Advanced caching strategies
- **Service Optimization**: Enhanced background processing efficiency
- **Infrastructure Optimization**: Advanced hash algorithms and caching

---

**Last Updated**: October 8, 2025  
**Version**: 1.3  
**Status**: Production Ready with Multi-Modal AI Processing