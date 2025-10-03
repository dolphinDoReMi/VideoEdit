#!/bin/bash
# ops/01_push_manifests.sh - Push manifest files to device

set -e

PKG=${PKG:-"com.mira.clip"}
IN_ROOT="/sdcard/MiraClip/in"

echo "=== Pushing CLIP4Clip manifests ==="
echo "Package: $PKG"
echo "Input root: $IN_ROOT"

# Create manifests directory if it doesn't exist
mkdir -p manifests

# Create ingest manifest
cat > manifests/video_ingest_manifest.json << 'EOF'
{
  "variant": "clip_vit_b32_mean_v1",
  "frame_count": 32,
  "videos": [
    {
      "id": "v001",
      "path": "/sdcard/Movies/video_v1.mp4"
    }
  ],
  "output_dir": "/sdcard/MiraClip/out/embeddings"
}
EOF

# Create search manifest
cat > manifests/text_queries.json << 'EOF'
{
  "variant": "clip_vit_b32_mean_v1",
  "queries": [
    {
      "id": "q1",
      "text": "a person surfing on ocean waves"
    },
    {
      "id": "q2", 
      "text": "someone walking on a beach"
    },
    {
      "id": "q3",
      "text": "a dog running in a park"
    }
  ],
  "top_k": 5,
  "index_dir": "/sdcard/MiraClip/out/embeddings",
  "output_path": "/sdcard/MiraClip/out/search/results_q.json"
}
EOF

# Push manifests to device
adb push manifests/video_ingest_manifest.json $IN_ROOT/ingest.json
adb push manifests/text_queries.json $IN_ROOT/search.json

echo "Pushed manifests:"
adb shell "ls -la $IN_ROOT/"

echo "=== Manifest push complete ==="
