#!/bin/bash
# Test script for the retrieval system

set -e

MANIFEST_PATH="/sdcard/Mira/manifests/h_clip.json"

echo "=== Testing Retrieval System ==="

# Ensure manifest exists on device
echo "Pushing manifest to device..."
adb push manifests/h_clip.json "$MANIFEST_PATH"

# Test ingest
echo "Testing ingest..."
adb shell am broadcast \
  -a com.mira.clip.retrieval.ACTION_INGEST \
  --es manifest_path "$MANIFEST_PATH"

echo "Waiting for ingest to complete..."
sleep 5

# Test retrieve
echo "Testing retrieve..."
adb shell am broadcast \
  -a com.mira.clip.retrieval.ACTION_RETRIEVE \
  --es manifest_path "$MANIFEST_PATH"

echo "Waiting for retrieve to complete..."
sleep 3

# Check results
echo "Checking results..."
adb shell ls -la /sdcard/Mira/out/results/

echo "=== Test Complete ==="
