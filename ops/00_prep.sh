#!/bin/bash
# ops/00_prep.sh - Prepare device directories for CLIP4Clip

set -e

PKG=${PKG:-"com.mira.clip"}
OUT_ROOT="/sdcard/MiraClip/out"
IN_ROOT="/sdcard/MiraClip/in"

echo "=== Preparing CLIP4Clip directories ==="
echo "Package: $PKG"
echo "Output root: $OUT_ROOT"
echo "Input root: $IN_ROOT"

# Create directory structure
adb shell "mkdir -p $IN_ROOT"
adb shell "mkdir -p $OUT_ROOT/embeddings"
adb shell "mkdir -p $OUT_ROOT/search"

echo "Created directory structure:"
adb shell "ls -la /sdcard/MiraClip/"

echo "=== Preparation complete ==="
