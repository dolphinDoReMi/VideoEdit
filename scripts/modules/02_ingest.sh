#!/bin/bash
# ops/02_ingest.sh - Trigger video ingestion

set -e

PKG=${PKG:-"com.mira.clip"}
MANIFEST_PATH="/sdcard/MiraClip/in/ingest.json"

echo "=== Triggering CLIP4Clip video ingestion ==="
echo "Package: $PKG"
echo "Manifest: $MANIFEST_PATH"

# Check if manifest exists
adb shell "test -f $MANIFEST_PATH" || {
    echo "ERROR: Manifest file not found: $MANIFEST_PATH"
    echo "Run ops/01_push_manifests.sh first"
    exit 1
}

# Check if default video exists
adb shell "test -f /sdcard/Movies/video_v1.mp4" || {
    echo "WARNING: Default video not found: /sdcard/Movies/video_v1.mp4"
    echo "Please ensure test video is available"
}

# Trigger ingestion broadcast
echo "Sending INGEST_MANIFEST broadcast..."
adb shell am broadcast -a com.mira.clip.INGEST_MANIFEST --es manifest_path "$MANIFEST_PATH"

echo "Waiting for ingestion to complete..."
sleep 5

# Check results
echo "Checking ingestion results:"
adb shell "ls -la /sdcard/MiraClip/out/embeddings/" || echo "No embeddings directory found"

# List generated files
adb shell "find /sdcard/MiraClip/out/embeddings -name '*.f32' -o -name '*.json'" || echo "No embedding files found"

echo "=== Ingestion complete ==="
