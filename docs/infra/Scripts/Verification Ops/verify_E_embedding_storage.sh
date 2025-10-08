#!/bin/bash
# ops/verify_E_embedding_storage.sh

echo "🔍 Verifying Embedding Storage..."

# Check for Room database
if ! grep -r "EmbeddingDb" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Room database not found"
    exit 1
fi

# Check for binary conversion
if ! grep -r "floatArrayToBlob" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Binary conversion not found"
    exit 1
fi

# Check for embedding record
if ! grep -r "EmbeddingRecord" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: EmbeddingRecord not found"
    exit 1
fi

echo "✅ PASS: Embedding storage verified"
