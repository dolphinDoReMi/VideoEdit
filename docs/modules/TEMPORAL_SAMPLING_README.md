# Temporal Sampling System

A production-lean Android/Kotlin solution for deterministic video frame sampling with fixed-length representations, auditable metadata, and clean extensibility.

## Features

- **Deterministic Sampling**: Extract N frames from any video into a fixed-length representation
- **Control Knots**: Centralized configuration with runtime overrides (debug builds only)
- **Broadcast API**: Request/progress/result communication with verification hooks
- **Package Isolation**: Debug builds use `.debug` suffix; release applicationId unchanged
- **Extensible Design**: Support for multiple sampling policies (UNIFORM, TSN_JITTER, SLOWFAST_LITE)
- **Memory Safe**: Configurable memory budget and resource management

## Architecture

### Core Components

1. **SamplerService**: Foreground service for video processing with progress notifications
2. **FrameSampler**: MediaMetadataRetriever-based frame extraction with multiple policies
3. **ConfigProvider**: Centralized control knots with debug overrides
4. **Broadcast System**: Package-scoped communication for orchestration

### Control Knots

- `frameCount`: Number of frames to sample (default: 32)
- `schedule`: Sampling policy (UNIFORM, TSN_JITTER, SLOWFAST_LITE)
- `decodeBackend`: Decoder backend (MMR, MEDIACODEC)
- `seekPolicy`: Frame seeking strategy (PRECISE_OR_NEXT)
- `memoryBudgetMb`: Memory limit for processing (default: 512MB)

## Usage

### Basic Sampling

```kotlin
val usageExample = SamplerUsageExample(context)
val inputUri = Uri.parse("content://media/external/video/media/123")
usageExample.startSampling(inputUri, frameCount = 32)
```

### Broadcast Handling

```kotlin
val receiver = object : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            SamplerIntents.ACTION_SAMPLE_PROGRESS -> {
                val progress = intent.getIntExtra(SamplerIntents.EXTRA_PROGRESS, 0)
                // Update UI
            }
            SamplerIntents.ACTION_SAMPLE_RESULT -> {
                val jsonPath = intent.getStringExtra(SamplerIntents.EXTRA_RESULT_JSON)
                // Process result
            }
            SamplerIntents.ACTION_SAMPLE_ERROR -> {
                val error = intent.getStringExtra(SamplerIntents.EXTRA_ERROR_MESSAGE)
                // Handle error
            }
        }
    }
}
```

### Debug Overrides

Debug builds can override control knots at runtime:

```kotlin
val sp = getSharedPreferences("sampler_overrides", Context.MODE_PRIVATE)
sp.edit()
    .putInt("frameCount", 64)
    .putString("schedule", "TSN_JITTER")
    .apply()
```

## Output Format

### Directory Structure
```
/sdcard/Android/data/com.mira.clip/files/samples/{requestId}/
├── frame_0000.png
├── frame_0001.png
├── ...
├── frame_0031.png
└── result.json
```

### Result JSON
```json
{
  "requestId": "1704067200000",
  "inputUri": "content://media/external/video/media/123",
  "frameCountExpected": 32,
  "frameCountObserved": 32,
  "timestampsMs": [0, 1000, 2000, ...],
  "durationMs": 31000,
  "frames": ["/path/to/frame_0000.png", ...]
}
```

## Verification Hooks

- **Metadata Invariant**: `frameCountObserved == frameCountExpected`
- **Timestamp Validation**: Monotonic, within [0, durationMs]
- **File Verification**: All frame files exist and are readable

## Build Configuration

### Debug Isolation
- Release: `applicationId = "com.mira.clip"`
- Debug: `applicationId = "com.mira.clip.debug"`

### Dependencies
- Gson for JSON serialization
- Material Design for notifications
- LocalBroadcastManager for communication

## Extensibility

### Adding New Sampling Policies

1. Extend `Schedule` enum
2. Add policy implementation in `TimestampPolicies`
3. Update `FrameSampler.sample()` method

### Adding MediaCodec Backend

1. Extend `DecodeBackend` enum
2. Implement MediaCodec-based decoder
3. Update `FrameSampler` to use new backend

## Performance Considerations

- **Memory Management**: Configurable budget with automatic cleanup
- **Concurrency**: Single-threaded by default (MMR limitation)
- **Storage**: Uses app-specific external storage
- **Notifications**: Low-priority foreground service

## Security

- **Package Isolation**: Non-exported services with custom permissions
- **Signature Protection**: Control permission requires app signature
- **Local Broadcasts**: Package-scoped communication only

## Testing

### Unit Tests
- Timestamp policy validation
- Configuration provider tests
- Frame sampler edge cases

### Integration Tests
- End-to-end sampling workflow
- Broadcast communication
- File I/O operations

## Future Enhancements

1. **MediaCodec Backend**: Hardware-accelerated decoding for long videos
2. **Adaptive Sampling**: Query-aware frame selection for CLIP embeddings
3. **Multi-rate Support**: SlowFast-style temporal sampling
4. **Streaming Output**: Zero-copy frame handoff to embedding pipeline

## Troubleshooting

### Common Issues

1. **OOM Errors**: Reduce `memoryBudgetMb` or frame count
2. **Permission Denied**: Ensure proper storage permissions
3. **Frame Extraction Fails**: Check video format compatibility
4. **Broadcast Not Received**: Verify package name and intent filters

### Debug Tools

- Use `SamplerDebugActivity` for runtime configuration
- Check notification channel settings
- Monitor logcat for service lifecycle events
