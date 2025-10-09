#!/bin/bash
# ops/verify_B_capability_access.sh

echo "🔍 Verifying Capability-Based Access..."

# Check for URI-based methods
if ! grep -r "openReadFd(uri: Uri)" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: URI-based FD access not found"
    exit 1
fi

# Check for FileProvider usage
if ! grep -r "FileProvider.getUriForFile" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: FileProvider sharing not found"
    exit 1
fi

# Check for URI parameter usage
if ! grep -r "uri: Uri" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: URI parameter usage not found"
    exit 1
fi

echo "✅ PASS: Capability-based access verified"
