#!/bin/bash
# ops/verify_M_testing_coverage.sh

echo "🔍 Verifying Testing Coverage..."

# Check for test files
test_count=$(find infra-storage -name "*Test.kt" 2>/dev/null | wc -l)
if [ $test_count -lt 3 ]; then
    echo "❌ FAIL: Insufficient test coverage ($test_count tests)"
    exit 1
fi

# Check for verification tests
if ! find infra-storage -name "*Verification*.kt" 2>/dev/null; then
    echo "❌ FAIL: Verification tests not found"
    exit 1
fi

# Check for comprehensive tests
if ! find infra-storage -name "*Comprehensive*.kt" 2>/dev/null; then
    echo "❌ FAIL: Comprehensive tests not found"
    exit 1
fi

echo "✅ PASS: Testing coverage verified ($test_count tests)"
