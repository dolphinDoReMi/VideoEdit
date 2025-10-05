# Whisper Scripts

This directory contains scripts for testing and validating Whisper ASR functionality.

## Core Testing Scripts

### `test_whisper.sh`
- **Purpose**: Core Whisper functionality testing
- **Usage**: `./test_whisper.sh`
- **Tests**: Basic ASR pipeline, model loading, audio processing

### `test_whisper_api.sh`
- **Purpose**: API endpoint validation
- **Usage**: `./test_whisper_api.sh`
- **Tests**: REST API endpoints, WebSocket connections, error handling

### `test_whisper_bridge.sh`
- **Purpose**: Android bridge testing
- **Usage**: `./test_whisper_bridge.sh`
- **Tests**: JavaScript bridge, native method calls, data serialization

### `test_whisper_resource_monitoring.sh`
- **Purpose**: Resource monitoring validation
- **Usage**: `./test_whisper_resource_monitoring.sh`
- **Tests**: Background service, real-time metrics, broadcast system

## Batch Processing Scripts

### `test_batch_*.sh`
- **Purpose**: Batch processing validation
- **Usage**: `./test_batch_*.sh`
- **Tests**: Multiple file processing, progress tracking, error handling

### `test_pipeline_*.sh`
- **Purpose**: End-to-end pipeline testing
- **Usage**: `./test_pipeline_*.sh`
- **Tests**: Complete workflow, data flow, performance benchmarks

## Deployment Scripts

### `deploy_multilingual_models.sh`
- **Purpose**: Model deployment automation
- **Usage**: `./deploy_multilingual_models.sh`
- **Features**: Model downloading, validation, installation

### `verify_lid_implementation.sh`
- **Purpose**: Language detection verification
- **Usage**: `./verify_lid_implementation.sh`
- **Tests**: LID accuracy, language switching, fallback behavior

## Usage Guidelines

1. **Prerequisites**: Ensure Android device is connected and app is installed
2. **Permissions**: Grant necessary permissions for audio and file access
3. **Testing Order**: Run core tests before batch processing tests
4. **Validation**: Check logs and output files for test results
5. **Cleanup**: Remove test files after validation

## Troubleshooting

- **Device Connection**: Use `adb devices` to verify connection
- **Permission Issues**: Check Android settings for app permissions
- **Model Loading**: Verify model files are present in assets
- **Resource Monitoring**: Check background service status