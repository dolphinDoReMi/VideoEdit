#!/bin/bash
# test_core_functionality.sh - Test CLIP4Clip core functionality directly

set -e

echo "=== Testing CLIP4Clip Core Functionality ==="

# Create a test video file (placeholder)
echo "Creating test video file..."
adb shell "echo 'test video content' > /sdcard/Movies/video_v1.mp4"

# Create test manifest
echo "Creating test manifest..."
cat > test_manifest.json << 'EOF'
{
  "variant": "clip_vit_b32_mean_v1",
  "frame_count": 2,
  "videos": [
    {
      "id": "test001",
      "path": "/sdcard/Movies/video_v1.mp4"
    }
  ],
  "output_dir": "/sdcard/MiraClip/out/embeddings"
}
EOF

# Push manifest
adb push test_manifest.json /sdcard/MiraClip/in/test_ingest.json

echo "Test setup complete. Core functionality test would require:"
echo "1. CLIP model assets (clip_image_encoder.ptl, clip_text_encoder.ptl, etc.)"
echo "2. Working broadcast receiver"
echo "3. Valid video file for frame extraction"

echo "=== Test Setup Complete ==="
