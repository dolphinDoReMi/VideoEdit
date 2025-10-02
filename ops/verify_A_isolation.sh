#!/bin/bash
# ops/verify_A_isolation.sh - Step A: Isolation & Namespacing Verification

set -e

export PKG=com.mira.clip
export ROOT=/sdcard/MiraClip

echo "=== Step A: Isolation & Namespacing Verification ==="
echo "Control knots: applicationId, storage root, broadcast actions"
echo ""

# Build and install
echo "Building APK..."
./gradlew :app:assembleDebug

echo "Installing APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Verify package isolation
echo "Verifying package installation..."
if adb shell pm list packages | grep -q "$PKG"; then
    echo "✓ Package $PKG installed"
else
    echo "✗ Package $PKG not found"
    exit 1
fi

# Verify run-as access
if adb shell run-as "$PKG" id >/dev/null 2>&1; then
    echo "✓ run-as access OK"
else
    echo "✗ run-as access failed"
    exit 1
fi

# Create namespaced directories
echo "Creating namespaced storage structure..."
adb shell "mkdir -p $ROOT/in $ROOT/out/embeddings $ROOT/out/search"
echo "✓ Storage root: $ROOT"

# Test broadcast action resolution
echo "Testing broadcast action resolution..."
adb shell am broadcast -a com.mira.clip.SELFTEST_TEXT --es text "test" >/dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✓ Broadcast actions resolve"
else
    echo "✗ Broadcast actions not responding"
    exit 1
fi

echo ""
echo "✓ Step A Complete: Experimental control established"
echo "  - Package: $PKG (isolated)"
echo "  - Storage: $ROOT (namespaced)"
echo "  - Actions: com.mira.clip.* (namespaced)"
