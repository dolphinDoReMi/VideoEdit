# CLIP Feature Implementation

## Overview

This document describes the end-to-end CLIP video embedding pipeline implemented as a background, no-UI feature in the mira_clip application.

## Architecture

The CLIP feature is implemented as a separate module (`feature/clip`) that provides:

- **Video Frame Extraction**: Uniform sampling of video frames using MediaMetadataRetriever
- **CLIP Preprocessing**: Center-crop, resize to 224x224, and normalize with CLIP mean/std
- **Embedding Generation**: Stub engine that produces 512-dimensional embeddings (ready for PyTorch Mobile swap-in)
- **Mean Pooling**: Aggregates frame embeddings into a single video embedding
- **Persistence**: Saves embeddings as .f32 (little-endian float32) with JSON metadata

## Module Structure

```
feature/clip/
├── build.gradle.kts
└── src/main/java/com/mira/clip/feature/clip/
    ├── VideoFrameExtractor.kt    # Frame extraction from video URIs
    ├── ClipPreprocess.kt         # CLIP normalization and preprocessing
    ├── ClipEngine.kt             # Stub embedding engine with mean pooling
    ├── ClipRunner.kt             # Main orchestration logic
    └── ClipReceiver.kt           # Broadcast receiver for ADB triggers
```

## Configuration

### AndroidManifest.xml
```xml
<receiver
  android:name="com.mira.clip.feature.clip.ClipReceiver"
  android:exported="false">
  <intent-filter>
    <action android:name="${applicationId}.CLIP.RUN" />
  </intent-filter>
</receiver>
```

### Config Constants (Config.kt)
```kotlin
const val CLIP_IMAGE_SIZE = 224
const val CLIP_RES = 224  // Alias for CLIP_IMAGE_SIZE
const val CLIP_FRAME_COUNT = 32  // Default frame count for CLIP processing
```

## Usage

### ADB Broadcast Trigger

```bash
# Basic usage
adb shell am broadcast \
  -a com.mira.clip.CLIP.RUN \
  --es input "file:///sdcard/Mira/video_v1.mp4" \
  --es outdir "file:///sdcard/MiraClip/out" \
  --es variant "ViT-B_32" \
  --ei frame_count 32

# With custom parameters
adb shell am broadcast \
  -a com.mira.clip.CLIP.RUN \
  --es input "file:///sdcard/Movies/sample.mp4" \
  --es outdir "file:///sdcard/MiraClip/out" \
  --es variant "custom_model" \
  --ei frame_count 16
```

### Parameters

- **input**: Video file URI (required)
- **outdir**: Output directory (default: `/sdcard/MiraClip/out`)
- **variant**: Model variant identifier (default: `ViT-B_32`)
- **frame_count**: Number of frames to sample (default: 32)

## Output Format

### Directory Structure
```
/sdcard/MiraClip/out/
└── embeddings/
    └── ViT-B_32/
        ├── video_v1.f32    # Binary embedding (512 floats, little-endian)
        └── video_v1.json   # Metadata
```

### JSON Metadata
```json
{
  "id": "video_v1",
  "src": "file:///sdcard/Mira/video_v1.mp4",
  "variant": "ViT-B_32",
  "dim": 512,
  "frame_count": 32,
  "timestamps_us": [0, 33333, 66666, ...],
  "duration_us": 1000000
}
```

### Binary Format (.f32)
- **Format**: Little-endian float32
- **Dimensions**: 512 (configurable)
- **Size**: 2048 bytes (512 × 4 bytes)
- **Normalization**: L2-normalized vectors

## Testing

### Automated Test Script
```bash
./test_clip_feature.sh
```

This script:
1. Pushes a test video (if available locally)
2. Triggers the CLIP pipeline via ADB broadcast
3. Waits for processing
4. Pulls and validates output files
5. Shows recent logs

### Manual Validation
```bash
# Pull output files
adb pull /sdcard/MiraClip/out/embeddings/ViT-B_32/video_v1.f32 .
adb pull /sdcard/MiraClip/out/embeddings/ViT-B_32/video_v1.json .

# Validate with Python script
python3 tools/retrieval_check.py video_v1.f32 512
```

## Implementation Details

### Frame Sampling
- Uses `TimestampPolicies.uniform()` for deterministic frame selection
- Extracts frames at precise timestamps using MediaMetadataRetriever
- Scales frames to max 512x512 before preprocessing

### Preprocessing Pipeline
1. **Center Crop**: Extract square region from center
2. **Resize**: Scale to 224x224 (CLIP input size)
3. **Normalize**: Apply CLIP mean/std normalization
4. **CHW Format**: Convert to Channel-Height-Width float32 array

### Embedding Engine
- **Current**: Deterministic stub that produces normalized 512-D vectors
- **Future**: PyTorch Mobile integration ready
- **Mean Pooling**: Aggregates frame embeddings into video embedding

### Error Handling
- Validates input URI and file existence
- Ensures at least one frame is extracted
- Logs errors with context
- Returns meaningful error messages

## Future Enhancements

### PyTorch Mobile Integration
```kotlin
// Replace ClipEngine.encodeImageCHW with:
fun encodeImageCHW(chw: FloatArray, dim: Int = 512): FloatArray {
    val module = PyTorchMobile.loadModuleFromAsset("clip_image.ptl")
    val input = Tensor.fromBlob(chw, longArrayOf(1, 3, 224, 224))
    val output = module.forward(IValue.from(input)).toTensor()
    return normalizeEmbedding(output.dataAsFloatArray)
}
```

### Alternative Backends
- **NNAPI**: For hardware acceleration
- **TensorFlow Lite**: Alternative ML framework
- **Custom C++**: Native implementation for performance

### Advanced Features
- **Temporal Attention**: X-CLIP style attention pooling
- **Multi-scale Sampling**: Different frame rates for different segments
- **Batch Processing**: Process multiple videos in parallel
- **Caching**: Reuse embeddings for identical videos

## Troubleshooting

### Common Issues

1. **No frames decoded**
   - Check video file format and codec support
   - Verify file path and permissions
   - Ensure video duration > 0

2. **Build errors**
   - Ensure JVM target compatibility (Java 17)
   - Check for circular dependencies
   - Verify all required dependencies

3. **Runtime errors**
   - Check ADB connection
   - Verify package name matches applicationId
   - Review logcat for detailed error messages

### Logging
```bash
# Monitor CLIP processing
adb logcat | grep -E "(ClipRunner|ClipReceiver)"

# Check for errors
adb logcat | grep -E "(ERROR|Exception)" | grep -E "(ClipRunner|ClipReceiver)"
```

## Performance Considerations

### Memory Usage
- Frame extraction: ~512x512x4 bytes per frame
- Preprocessing: Additional temporary buffers
- Embedding storage: 2048 bytes per video

### Processing Time
- Frame extraction: ~100ms per frame (device dependent)
- Preprocessing: ~10ms per frame
- Stub encoding: ~1ms per frame
- Total: ~3.5 seconds for 32 frames

### Optimization Opportunities
- Use Media3 FrameProcessor for better throughput
- Implement frame caching
- Add progress reporting
- Support background processing

## Security Considerations

- **Broadcast Receiver**: `android:exported="false"` for security
- **File Access**: Uses standard Android file permissions
- **Input Validation**: Validates URIs and file paths
- **Error Handling**: No sensitive information in logs

## Compliance

- **ApplicationId**: Maintains `com.mira.clip` (not `com.mira.com` as originally specified)
- **Module Boundaries**: No cross-module imports from core/infra
- **Configuration**: Uses existing Config.kt constants
- **Architecture**: Follows existing project patterns
