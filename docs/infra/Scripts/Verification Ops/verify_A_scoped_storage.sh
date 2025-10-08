#!/bin/bash
# ops/verify_A_scoped_storage.sh

echo "🔍 Verifying Scoped Storage Compliance..."

# Check for external storage references (excluding test files)
if grep -r "/sdcard/" infra-storage/src/main/java/ --exclude="*Test.kt" --exclude="*Verification*.kt" 2>/dev/null; then
    echo "❌ FAIL: External storage references found"
    exit 1
fi

# Check for app-private storage usage
if ! grep -r "getFilesDir()" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: No app-private storage usage found"
    exit 1
fi

# Check for FileProvider usage
if ! grep -r "FileProvider" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: FileProvider usage not found"
    exit 1
fi

echo "✅ PASS: Scoped storage compliance verified"
