# CLIP Feature Development Runbook

## Quick Test Commands

### 1. Push video to device
```bash
adb push video_v1.mp4 /sdcard/Mira/video_v1.mp4
```

### 2. Run CLIP processing via broadcast
```bash
adb shell am broadcast -a com.mira.com.CLIP.RUN \
  --es input "file:///sdcard/Mira/video_v1.mp4" \
  --es outdir "file:///sdcard/MiraClip/out" \
  --es variant "ViT-B_32" \
  --ei frame_count 32
```

### 3. Check output artifacts
```bash
adb shell ls /sdcard/MiraClip/out/embeddings/ViT-B_32
```

### 4. Pull artifacts for inspection
```bash
adb pull /sdcard/MiraClip/out/embeddings/ViT-B_32/video_v1.f32
adb pull /sdcard/MiraClip/out/embeddings/ViT-B_32/video_v1.json
```

## Build Variants

- **Debug**: `com.mira.com.debug` - Full debugging enabled
- **Internal**: `com.mira.com.internal` - Internal testing with some debugging
- **Release**: `com.mira.com` - Production build with optimizations

## Firebase Setup

Create Android apps in Firebase Console for each variant:
- `com.mira.com` (release)
- `com.mira.com.internal` (internal)  
- `com.mira.com.debug` (debug)

Place the downloaded `google-services.json` files in:
- `app/src/release/google-services.json`
- `app/src/internal/google-services.json`
- `app/src/debug/google-services.json`

## CI/CD Verification

The CI pipeline will:
1. Verify appId freeze (`com.mira.com` unchanged)
2. Run static checks (detekt, ktlint, lint)
3. Execute unit tests
4. Run e2e tests on emulator (API 31)
5. Validate Firebase package mapping per build variant

## Output Format

CLIP processing produces:
- `{videoId}.f32` - Binary float32 embedding vector
- `{videoId}.json` - Metadata with timestamps, dimensions, etc.

Example JSON:
```json
{
  "id": "video_v1",
  "src": "file:///sdcard/Mira/video_v1.mp4",
  "variant": "ViT-B_32",
  "dim": 512,
  "frame_count": 32,
  "timestamps_us": [0, 1000000, ...],
  "duration_us": 30000000
}
```
