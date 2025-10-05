# CLIP Scripts

This directory contains scripts for testing and validating CLIP video understanding functionality.

## Core Testing Scripts

### `test_clip_models.sh`
- **Purpose**: CLIP model validation
- **Usage**: `./test_clip_models.sh`
- **Tests**: Model loading, embedding generation, similarity search

### `test_video_processing.sh`
- **Purpose**: Video processing pipeline testing
- **Usage**: `./test_video_processing.sh`
- **Tests**: Video decoding, frame sampling, CLIP encoding

### `benchmark_embeddings.sh`
- **Purpose**: Performance benchmarking
- **Usage**: `./benchmark_embeddings.sh`
- **Tests**: Processing speed, memory usage, accuracy metrics

## Deployment Scripts

### `deploy_clip_models.sh`
- **Purpose**: Model deployment automation
- **Usage**: `./deploy_clip_models.sh`
- **Features**: Model downloading, validation, installation

## Usage Guidelines

1. **Prerequisites**: Ensure video files are available for testing
2. **GPU Support**: Verify OpenCL/Metal support for acceleration
3. **Model Assets**: Ensure CLIP models are bundled in APK
4. **Testing Order**: Run model tests before video processing tests
5. **Validation**: Check embedding quality and processing performance

## Troubleshooting

- **Model Loading**: Verify CLIP model files are present
- **GPU Acceleration**: Check OpenCL/Metal driver support
- **Memory Issues**: Monitor memory usage during video processing
- **Performance**: Profile CPU and GPU utilization