# Xiaomi Pad Ultra E2E Whisper Test

This comprehensive end-to-end test validates the complete Whisper transcription pipeline on the Xiaomi Pad Ultra device, including audio decoding, transcription, sidecar generation, and database persistence.

## Features

- **Local Media Processing**: Reads WAV/MP4 files from device storage
- **Audio Pipeline**: Decodes → mono 16 kHz PCM16 (downmix + resample)
- **Whisper Integration**: Calls whisper.cpp via JNI with configurable parameters
- **Sidecar Generation**: Creates JSON and SRT output files
- **Database Persistence**: Writes to Room asr.db (asr_files, asr_jobs, asr_segments)
- **Comprehensive Validation**: 
  - Sample rate verification (16,000 Hz mono)
  - Segment validation (non-empty, proper timestamps)
  - RTF performance checks
  - Optional WER evaluation against reference transcripts

## Test Structure

```
feature/whisper/src/androidTest/
├── AndroidManifest.xml                    # Test permissions
└── java/com/mira/whisper/
    └── E2EWhisperTranscribeTest.kt        # Main E2E test
```

## Usage

### Prerequisites

1. **Device Setup**: Xiaomi Pad Ultra connected via USB
2. **Test Media**: Place audio files in `/sdcard/MiraWhisper/in/`
3. **Permissions**: Ensure READ_MEDIA_AUDIO/VIDEO permissions are granted

### Running the Test

#### Option A: Gradle (Recommended)

```bash
# Ensure device is connected
adb devices

# Push test media (if not already on device)
adb push demo.mp4 /sdcard/MiraWhisper/in/demo.mp4

# Run the E2E test
./gradlew :feature:whisper:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.mira.whisper.E2EWhisperTranscribeTest \
  -Pandroid.testInstrumentationRunnerArguments.AUDIO_PATH=/sdcard/MiraWhisper/in/demo.mp4 \
  -Pandroid.testInstrumentationRunnerArguments.OUT_DIR=/sdcard/MiraWhisper/out \
  -Pandroid.testInstrumentationRunnerArguments.MODEL_ID=whisper-tiny.en-q5_1 \
  -Pandroid.testInstrumentationRunnerArguments.THREADS=6 \
  -Pandroid.testInstrumentationRunnerArguments.LANG=en \
  -Pandroid.testInstrumentationRunnerArguments.TRANSLATE=false \
  -Pandroid.testInstrumentationRunnerArguments.GREEDY=true \
  -Pandroid.testInstrumentationRunnerArguments.BEAM_SIZE=5 \
  -Pandroid.testInstrumentationRunnerArguments.TEMP=0.0 \
  -Pandroid.testInstrumentationRunnerArguments.RTF_MAX=1.5
```

#### Option B: Raw ADB Instrument

```bash
adb shell am instrument -w \
  -e class com.mira.whisper.E2EWhisperTranscribeTest \
  -e AUDIO_PATH "/sdcard/MiraWhisper/in/demo.mp4" \
  -e OUT_DIR "/sdcard/MiraWhisper/out" \
  -e MODEL_ID "whisper-tiny.en-q5_1" \
  -e THREADS "6" \
  -e LANG "en" \
  -e TRANSLATE "false" \
  -e GREEDY "true" \
  -e BEAM_SIZE "5" \
  -e TEMP "0.0" \
  -e RTF_MAX "1.5" \
  com.mira.com.test/androidx.test.runner.AndroidJUnitRunner
```

### Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `AUDIO_PATH` | **Required** | Path to input audio file (WAV/MP4) |
| `OUT_DIR` | `/sdcard/MiraWhisper/out` | Output directory for sidecars |
| `MODEL_ID` | `whisper-tiny.en-q5_1` | Whisper model identifier |
| `THREADS` | `4` | Number of CPU threads |
| `LANG` | `""` (auto) | Language code or empty for auto-detection |
| `TRANSLATE` | `false` | Enable translation to English |
| `GREEDY` | `true` | Use greedy decoding (vs beam search) |
| `BEAM_SIZE` | `5` | Beam size for beam search |
| `TEMP` | `0.0` | Sampling temperature |
| `RTF_MAX` | `1.5` | Maximum Real-Time Factor |
| `REF_TXT` | `null` | Optional reference transcript for WER |

## Output Artifacts

### JSON Sidecar
```json
{
  "audio_path": "/sdcard/MiraWhisper/in/demo.mp4",
  "audio_sha256": "abc123...",
  "model_variant": "whisper-tiny.en-q5_1",
  "model_sha": "",
  "decode_cfg": {
    "threads": 6,
    "language": "en",
    "translate": false,
    "greedy": true,
    "beam_size": 5,
    "temperature": 0.0
  },
  "device": "Xiaomi Pad Ultra",
  "rtf": 1.2,
  "created_at": "2024-01-15T10:30:00Z",
  "segments": [
    {"t0Ms": 0, "t1Ms": 1500, "text": "Hello world"},
    {"t0Ms": 1500, "t1Ms": 3000, "text": "This is a test"}
  ]
}
```

### SRT Subtitle File
```
1
00:00:00,000 --> 00:00:01,500
Hello world

2
00:00:01,500 --> 00:00:03,000
This is a test
```

### Database Records
- **asr_files**: File metadata and processing state
- **asr_jobs**: Transcription job parameters and results
- **asr_segments**: Individual transcribed segments with timestamps

## Performance Targets (Xiaomi Pad Ultra)

- **RTF ≤ 1.5**: Real-time factor should not exceed 1.5x audio duration
- **Threads = 6**: Optimized for big.LITTLE architecture
- **Temperature = 0.0**: Deterministic output for evaluation
- **Greedy = true**: Faster decoding, switch to beam search for quality

## Validation Checks

1. **Audio Format**: Ensures 16 kHz mono PCM16 output
2. **Segment Quality**: Non-empty segments with valid timestamps
3. **Temporal Order**: Segments are non-decreasing in time
4. **Performance**: RTF within acceptable limits
5. **WER (Optional)**: Word Error Rate ≤ 40% against reference

## Troubleshooting

### Permission Issues
```bash
adb shell pm grant com.mira.com android.permission.READ_MEDIA_AUDIO
adb shell pm grant com.mira.com android.permission.READ_MEDIA_VIDEO
```

### Path Issues
- Use absolute paths for local files
- For MediaStore files, use `content://` URIs
- Ensure test media exists before running

### Performance Issues
- Reduce `THREADS` if device overheats
- Increase `RTF_MAX` for slower devices
- Use smaller models (`whisper-tiny`) for faster processing

### Empty Segments
- Verify audio has speech content
- Check sample rate conversion
- Ensure proper amplitude levels

## Integration with Existing Codebase

The test leverages existing components:
- `AudioIO.loadPcm16()`: Audio decoding
- `AudioResampler`: Downmix and resampling
- `WhisperBridge.decodeJson()`: JNI transcription
- `AsrDb`: Room database operations
- `Sidecars`: JSON sidecar generation

This ensures consistency with the production pipeline while providing comprehensive E2E validation.
