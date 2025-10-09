#!/bin/bash
# ops/verify_L_dependency_management.sh

echo "🔍 Verifying Dependency Management..."

# Check for Room dependency
if ! grep -r "androidx.room" infra-storage/build.gradle.kts 2>/dev/null; then
    echo "❌ FAIL: Room dependency not found"
    exit 1
fi

# Check for coroutines dependency
if ! grep -r "kotlinx-coroutines" infra-storage/build.gradle.kts 2>/dev/null; then
    echo "❌ FAIL: Coroutines dependency not found"
    exit 1
fi

# Check for core AndroidX dependency
if ! grep -r "androidx.core" infra-storage/build.gradle.kts 2>/dev/null; then
    echo "❌ FAIL: Core AndroidX dependency not found"
    exit 1
fi

echo "✅ PASS: Dependency management verified"
