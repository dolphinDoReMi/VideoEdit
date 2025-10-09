#!/bin/bash
# ops/verify_H_secure_sharing.sh

echo "🔍 Verifying Secure Sharing..."

# Check for FileProvider configuration
if [ ! -f "infra-storage/src/main/res/xml/filepaths.xml" ]; then
    echo "❌ FAIL: FileProvider configuration not found"
    exit 1
fi

# Check for sharing method
if ! grep -r "shareOutput" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Output sharing method not found"
    exit 1
fi

# Check for content URI generation
if ! grep -r "getUriForFile" infra-storage/src/main/java/ 2>/dev/null; then
    echo "❌ FAIL: Content URI generation not found"
    exit 1
fi

echo "✅ PASS: Secure sharing verified"
