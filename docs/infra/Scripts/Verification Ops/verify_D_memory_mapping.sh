#!/bin/bash
# ops/verify_D_memory_mapping.sh

echo "🔍 Verifying Memory Mapping Support..."

# Check for model path resolution
if ! grep -r "resolveModelPath" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Model path resolution not found"
    exit 1
fi

# Check for native interface
if ! grep -r "NativeWhisper" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Native Whisper interface not found"
    exit 1
fi

# Check for NativeClip interface
if ! grep -r "NativeClip" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Native CLIP interface not found"
    exit 1
fi

echo "✅ PASS: Memory mapping support verified"
