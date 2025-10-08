#!/bin/bash
# ops/verify_F_fd_processing.sh

echo "🔍 Verifying File Descriptor Processing..."

# Check for FD access
if ! grep -r "openFileDescriptor" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: File descriptor access not found"
    exit 1
fi

# Check for native FD processing
if ! grep -r "transcribeFromFd" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Native FD processing not found"
    exit 1
fi

# Check for detachFd usage
if ! grep -r "detachFd" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: detachFd usage not found"
    exit 1
fi

echo "✅ PASS: File descriptor processing verified"
