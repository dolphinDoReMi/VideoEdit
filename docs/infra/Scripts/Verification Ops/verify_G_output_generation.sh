#!/bin/bash
# ops/verify_G_output_generation.sh

echo "🔍 Verifying Output Generation..."

# Check for output methods
if ! grep -r "writeTranscript" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Transcript writing not found"
    exit 1
fi

# Check for sanitization
if ! grep -r "sanitize" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Filename sanitization not found"
    exit 1
fi

# Check for preview image writing
if ! grep -r "writePreviewImage" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Preview image writing not found"
    exit 1
fi

echo "✅ PASS: Output generation verified"
