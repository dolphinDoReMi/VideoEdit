## Local + Edge Device Builds (Android Xiaomi Pad, iOS iPad)

This guide standardizes per-thread app IDs/bundle IDs, minimal dependencies, package structure, and copy-pasteable commands to run local and edge-device tests.

### App ID isolation (per-thread-of-change)

- Android base: `com.mira.com` → derived: `com.mira.com.t.<THREAD>`
- iOS base: `com.mira.com` → derived: `com.mira.com.t.<THREAD>`

Derive suffix (sanitized branch + short SHA):

```bash
# Android charset [a-z0-9_.]
./scripts/thread_suffix.sh

# iOS charset [A-Za-z0-9.-]
./scripts/thread_suffix_ios.sh
```

### Android (Gradle)

- Supports `-PappIdSuffix=<THREAD>`; debug `applicationIdSuffix` becomes `.t.<THREAD>` and the launcher label includes the suffix: `Mira (debug.<THREAD>)`.

Build, install, and run androidTest on Xiaomi Pad:

```bash
THREAD=$(./scripts/thread_suffix.sh)
adb -s <DEVICE> push ~/video_v1.mp4 /sdcard/Movies/video_v1.mp4
./gradlew :app:installDebug -PappIdSuffix="$THREAD"
./gradlew connectedAndroidTest \
  -PappIdSuffix="$THREAD" \
  -Pandroid.testInstrumentationRunnerArguments.videoPath=/sdcard/Movies/video_v1.mp4 \
  -Pandroid.testInstrumentationRunnerArguments.modelId=clip_vit_b32_mean_v1 \
  -Pandroid.testInstrumentationRunnerArguments.frameCount=32 \
  --tests "com.mira.com.feature.clip.E2EVideoEmbeddingTest.generateEmbedding_fromLocalVideo_andAudit"
```

Assets for `androidTest` should be placed in `app/src/androidTest/assets` (wired in Gradle).

### iOS (Xcode)

- Use an `xcconfig` to compute `PRODUCT_BUNDLE_IDENTIFIER=$(BUNDLE_ID_BASE).t.$(THREAD_SUFFIX)`.
- Build and test on device with provisioning updates enabled.

```bash
THREAD=$(./scripts/thread_suffix_ios.sh)
xcodebuild \
  -scheme MiraApp -configuration Debug \
  -destination 'platform=iOS,id=<YOUR_DEVICE_UDID>' \
  -allowProvisioningUpdates \
  THREAD_SUFFIX="$THREAD" \
  build

# XCTest (mirror env vars in your test target)
xcodebuild test \
  -scheme MiraApp -configuration Debug \
  -destination 'platform=iOS,id=<YOUR_DEVICE_UDID>' \
  -allowProvisioningUpdates \
  THREAD_SUFFIX="$THREAD" \
  TEST_VIDEO_PATH="/var/mobile/Media/DCIM/100APPLE/video_v1.MOV" \
  FRAME_COUNT=32 MODEL_ID="clip_vit_b32_mean_v1"
```

Required iOS `xcconfig` keys (example `Config/Base.xcconfig`):

```ini
BUNDLE_ID_BASE = com.mira.com
THREAD_SUFFIX = local
PRODUCT_BUNDLE_IDENTIFIER = $(BUNDLE_ID_BASE).t.$(THREAD_SUFFIX)
PRODUCT_NAME = Mira (debug.$(THREAD_SUFFIX))
CODE_SIGN_STYLE = Automatic
```

### CI (GitHub Actions)

Workflow `.github/workflows/device-tests.yml` builds both platforms and derives the same suffix via the scripts. Android passes `-PappIdSuffix`, iOS sets `THREAD_SUFFIX`.

### Troubleshooting

- Android collision: verify suffix via `adb shell pm list packages | grep com.mira.com.t`.
- iOS signing loop: open once in Xcode, set your Team, then re-run with `-allowProvisioningUpdates`.
- Model paths: ensure your engine logs model id + path and fails fast.


