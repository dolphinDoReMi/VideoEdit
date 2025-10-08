#!/bin/bash
# ops/verify_C_model_integrity.sh

echo "🔍 Verifying Model Integrity..."

# Check for SHA-256 validation
if ! grep -r "digestSha256" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: SHA-256 validation not found"
    exit 1
fi

# Check for model naming pattern
if ! grep -r "@.*sha" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Model naming pattern not found"
    exit 1
fi

# Check for SHA-256 parameter
if ! grep -r "expectSha256" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: SHA-256 parameter not found"
    exit 1
fi

echo "✅ PASS: Model integrity verification found"
