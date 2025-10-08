#!/bin/bash
# ops/verify_K_api_surface.sh

echo "🔍 Verifying API Surface..."

# Check for interface definition
if ! grep -r "interface DLStorage" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: DLStorage interface not found"
    exit 1
fi

# Check for data classes
if ! grep -r "data class ModelHandle" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: ModelHandle data class not found"
    exit 1
fi

# Check for embedding record
if ! grep -r "data class EmbeddingRecord" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: EmbeddingRecord data class not found"
    exit 1
fi

echo "✅ PASS: API surface verified"
