#!/bin/bash
# ops/03_search.sh - Trigger text-to-video search

set -e

PKG=${PKG:-"com.mira.clip"}
MANIFEST_PATH="/sdcard/MiraClip/in/search.json"

echo "=== Triggering CLIP4Clip text-to-video search ==="
echo "Package: $PKG"
echo "Manifest: $MANIFEST_PATH"

# Check if manifest exists
adb shell "test -f $MANIFEST_PATH" || {
    echo "ERROR: Search manifest not found: $MANIFEST_PATH"
    echo "Run ops/01_push_manifests.sh first"
    exit 1
}

# Check if embeddings exist
adb shell "test -d /sdcard/MiraClip/out/embeddings" || {
    echo "ERROR: No embeddings found. Run ops/02_ingest.sh first"
    exit 1
}

# Trigger search broadcast
echo "Sending SEARCH_MANIFEST broadcast..."
adb shell am broadcast -a com.mira.clip.SEARCH_MANIFEST --es manifest_path "$MANIFEST_PATH"

echo "Waiting for search to complete..."
sleep 5

# Check results
echo "Checking search results:"
adb shell "ls -la /sdcard/MiraClip/out/search/" || echo "No search directory found"

# Show results file
adb shell "test -f /sdcard/MiraClip/out/search/results_q.json" && {
    echo "Search results:"
    adb shell "cat /sdcard/MiraClip/out/search/results_q.json"
} || echo "No search results found"

echo "=== Search complete ==="
