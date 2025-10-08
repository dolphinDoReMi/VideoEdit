#!/bin/bash
# ops/verify_I_cleanup_operations.sh

echo "🔍 Verifying Cleanup Operations..."

# Check for TTL cleanup
if ! grep -r "vacuumAndTtl" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: TTL cleanup not found"
    exit 1
fi

# Check for database vacuum
if ! grep -r "VACUUM" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Database vacuum not found"
    exit 1
fi

# Check for file cleanup
if ! grep -r "lastModified" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: File cleanup not found"
    exit 1
fi

echo "✅ PASS: Cleanup operations verified"
