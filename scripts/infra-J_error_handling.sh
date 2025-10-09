#!/bin/bash
# ops/verify_J_error_handling.sh

echo "🔍 Verifying Error Handling..."

# Check for exception handling
if ! grep -r "catch" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Exception handling not found"
    exit 1
fi

# Check for resource management
if ! grep -r "use.*{" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Resource management not found"
    exit 1
fi

# Check for error messages
if ! grep -r "SecurityException\|IllegalStateException" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Error messages not found"
    exit 1
fi

echo "✅ PASS: Error handling verified"
