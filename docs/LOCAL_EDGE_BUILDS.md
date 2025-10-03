## Local + Edge Device Builds (Android Xiaomi Pad, iOS iPad)

This doc standardizes per-thread install isolation, minimal dependencies, package paths, and copy-pasteable commands to build, install, and run device tests.

### App/Bundle ID isolation (per thread of change)

- Android base: `com.mira.com`, derived: `com.mira.com.t.<THREAD>`
- iOS base: `com.mira.com`, derived: `com.mira.com.t.<THREAD>`

Get a sanitized suffix:

```bash
# Android-compatible: [a-z0-9_.], ≤30 chars
./scripts/thread_suffix.sh

# iOS-compatible: [a-z0-9.-], dots preferred, ≤40 chars
./scripts/thread_suffix_ios.sh
```

### Android (Gradle)

`app/build.gradle.kts` accepts `-PappIdSuffix=<suffix>` and shows it in the launcher name: `Mira (debug.t.<suffix>)`.

Commands:

```bash
THREAD=$(./scripts/thread_suffix.sh)

# Optional: push test video to device
adb -s <DEVICE_ID> push ~/video_v1.mp4 /sdcard/Movies/video_v1.mp4

# Install debug with isolated appId
./gradlew :app:installDebug -PappIdSuffix="$THREAD"

# Run androidTest on connected Xiaomi Pad with overrides
./gradlew :app:connectedDebugAndroidTest \
  -PappIdSuffix="$THREAD" \
  -Pandroid.testInstrumentationRunnerArguments.videoPath=/sdcard/Movies/video_v1.mp4 \
  -Pandroid.testInstrumentationRunnerArguments.modelId=clip_vit_b32_mean_v1 \
  -Pandroid.testInstrumentationRunnerArguments.frameCount=32 \
  --tests "com.mira.com.feature.clip.E2EVideoEmbeddingTest.generateEmbedding_fromLocalVideo_andAudit"
```

Dependencies: uses AndroidX + Media3 (no FFmpeg). Feature code lives under `feature/clip/**` and is referenced by the app.

### iOS (Xcode)

Use xcconfig to compose `PRODUCT_BUNDLE_IDENTIFIER = com.mira.com.t.$(THREAD_SUFFIX)`. Build with a suffix to enable parallel installs.

Commands:

```bash
THREAD=$(./scripts/thread_suffix_ios.sh)

# Build on connected iPad (replace <UDID>)
xcodebuild \
  -scheme MiraApp -configuration Debug \
  -destination 'platform=iOS,id=<UDID>' \
  -allowProvisioningUpdates \
  THREAD_SUFFIX="$THREAD" \
  build

# XCTest on device (env overrides read by tests)
xcodebuild test \
  -scheme MiraApp -configuration Debug \
  -destination 'platform=iOS,id=<UDID>' \
  -allowProvisioningUpdates \
  THREAD_SUFFIX="$THREAD" \
  TEST_VIDEO_PATH="/var/mobile/Media/DCIM/100APPLE/video_v1.MOV" \
  FRAME_COUNT=32 MODEL_ID="clip_vit_b32_mean_v1"
```

Dependencies: AVFoundation/Accelerate (no third-party required). Keep `FrameSampler.swift`, `ClipEngine.swift`, `EmbeddingStore.swift` under `Feature/Clip/`.

### CI (GitHub Actions)

Workflow: `.github/workflows/device-tests.yml` builds Android with suffix and includes a stubbed iOS job.

```yaml
jobs:
  android:
    steps:
      - uses: actions/checkout@v4
      - id: suffix
        run: echo "suffix=$(./scripts/thread_suffix.sh)" >> $GITHUB_OUTPUT
      - uses: gradle/gradle-build-action@v3
        with:
          arguments: >
            :app:assembleDebug -PappIdSuffix=${{ steps.suffix.outputs.suffix }}
```

### Troubleshooting

- Android package collision: ensure `-PappIdSuffix` is set; verify with `adb shell pm list packages | grep com.mira.com.t`.
- iOS signing loops: open once in Xcode, set Team, then re-run with `-allowProvisioningUpdates`.
- Model/video not found in tests: pass `videoPath`, `modelId`, `frameCount` via runner args (Android) or environment (iOS).


