# CLIP4Clip Test Suite

This directory contains a comprehensive test suite for the CLIP4Clip video-text retrieval system. The test suite provides step-by-step verification of all components from database operations to ML model inference.

## Test Structure

### Unit Tests (JVM)
- **DbDaoTest.kt** - Room database schema and DAO operations
- **RetrievalMathTest.kt** - Cosine similarity and vector math operations

### Instrumented Tests (Android Device/Emulator)
- **SamplerInstrumentedTest.kt** - Frame sampling from video files
- **ImageEncoderInstrumentedTest.kt** - CLIP image encoding
- **TextEncoderInstrumentedTest.kt** - CLIP text tokenization and encoding
- **IngestWorkerInstrumentedTest.kt** - End-to-end video ingestion workflow

### Host Operations Scripts
- **01_assets.sh** - Verify model files and tokenizer assets
- **03_jobs.sh** - Check WorkManager jobs and database health
- **05_parity.py** - Compare mobile embeddings with Python open_clip
- **07_perf_logcat.sh** - Monitor performance metrics and latency
- **99_report.sh** - Generate comprehensive test report

## Running Tests

### Unit Tests
```bash
./gradlew :app:testDebugUnitTest
```

### Instrumented Tests
```bash
./gradlew :app:connectedDebugAndroidTest
```

### Host Operations
```bash
# Set environment variables
export PKG=com.mira.videoeditor
export VARIANT=clip_vit_b32_mean_v1

# Run individual scripts
bash ops/01_assets.sh
bash ops/03_jobs.sh
python3 ops/05_parity.py
bash ops/07_perf_logcat.sh

# Generate comprehensive report
bash ops/99_report.sh
```

## Test Requirements

### Device Requirements
- Android device or emulator connected via ADB
- USB debugging enabled
- App installed (`com.mira.videoeditor`)

### Host Requirements
- Python 3.x with `open_clip_torch` (for parity tests)
- ADB tools installed
- FFmpeg (for test video generation)

### Model Requirements
- CLIP model files in `app/src/main/assets/`
- BPE tokenizer files (`bpe_vocab.json`, `bpe_merges.txt`)

## Test Coverage

| Component | Test Type | What It Proves | Pass Criteria |
|-----------|-----------|----------------|---------------|
| Database | JVM Unit | Schema/constraints correct | DAO ops succeed |
| Frame Sampling | Instrumented | Frames sampled deterministically | N frames returned |
| Image Encoding | Instrumented | Encoder loads & normalizes | dim=512/768, norm≈1 |
| Text Encoding | Instrumented | Tokenization & text encode | len≤77, has EOT, norm≈1 |
| Worker E2E | Instrumented | Complete video processing | Embedding written to DB |
| Retrieval Math | JVM Unit | Cosine similarity correct | Monotonic ordering |
| Model Parity | Host | Mobile vs Python consistency | cosine ≥ 0.995 |
| Performance | Host | Latency under budget | ≤ 2.5s per frame |

## Test Data

- **Test Video**: `app/src/androidTest/resources/raw/sample.mp4` (5-second test pattern)
- **Test Images**: Generated programmatically (black, colored bitmaps)
- **Test Texts**: Standard CLIP evaluation phrases

## Performance Thresholds

- **Frame Encoding**: ≤ 2.5 seconds per frame
- **Memory Usage**: ≤ 500MB peak
- **Embedding Dimension**: 512 or 768 (CLIP ViT-B-32)
- **Cosine Similarity**: ≥ 0.995 for parity tests

## Troubleshooting

### Common Issues

1. **Models Not Found**: Ensure CLIP model files are in `app/src/main/assets/`
2. **Device Not Connected**: Check ADB connection and USB debugging
3. **Tests Timeout**: Increase timeout or check device performance
4. **Parity Test Fails**: Install `open_clip_torch` and verify Python environment

### Debug Commands

```bash
# Check device connection
adb devices

# Check app installation
adb shell pm list packages | grep com.mira.videoeditor

# Check database
adb shell "run-as com.mira.videoeditor sqlite3 databases/app_database 'SELECT COUNT(*) FROM embeddings;'"

# Monitor logcat
adb logcat -s CLIP4Clip
```

## Continuous Integration

The test suite is designed to run in CI environments:

```bash
# CI-friendly test run
export PKG=com.mira.videoeditor
export VARIANT=clip_vit_b32_mean_v1

# Run all tests
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
bash ops/99_report.sh
```

## Test Maintenance

- **Adding New Tests**: Follow existing patterns for test structure
- **Updating Thresholds**: Modify constants in test files and scripts
- **Model Updates**: Update asset verification in `01_assets.sh`
- **Performance Tuning**: Adjust thresholds in `07_perf_logcat.sh`

## Contributing

When adding new tests:
1. Follow the existing test structure
2. Add appropriate pass criteria
3. Update this README
4. Ensure tests are deterministic
5. Add to CI pipeline if needed
