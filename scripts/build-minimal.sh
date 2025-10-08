#!/bin/bash
# Build and test minimal CLIP variant
# This script builds the minimal variant with only essential CLIP dependencies

set -euo pipefail

echo "🔧 Building minimal CLIP variant..."

# Build minimal variant
echo "Building minimal APK..."
./gradlew :app:assembleMinimal

# Check if APK was created
MINIMAL_APK="app/build/outputs/apk/minimal/app-minimal.apk"
if [ -f "$MINIMAL_APK" ]; then
    echo "✅ Minimal APK created: $MINIMAL_APK"
    
    # Get APK size
    APK_SIZE=$(du -h "$MINIMAL_APK" | cut -f1)
    echo "📦 Minimal APK size: $APK_SIZE"
    
    # Compare with debug APK size if it exists
    DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$DEBUG_APK" ]; then
        DEBUG_SIZE=$(du -h "$DEBUG_APK" | cut -f1)
        echo "📦 Debug APK size: $DEBUG_SIZE"
        echo "💡 Minimal variant should be significantly smaller"
    fi
    
    # Install minimal APK if device is connected
    if adb devices | grep -q "device$"; then
        echo "📱 Installing minimal APK..."
        adb install -r "$MINIMAL_APK"
        echo "✅ Minimal APK installed"
        
        # Test broadcast receiver
        echo "🧪 Testing broadcast receiver..."
        adb shell "am broadcast -a com.mira.com.minimal.DEBUG_LOG"
        echo "✅ Broadcast test completed"
    else
        echo "⚠️  No device connected - skipping installation"
    fi
    
else
    echo "❌ Minimal APK not found"
    exit 1
fi

echo "🎉 Minimal variant build completed successfully!"
echo ""
echo "📋 Summary:"
echo "  - Minimal APK: $MINIMAL_APK"
echo "  - Size: $APK_SIZE"
echo "  - Dependencies: PyTorch + Core orchestration only"
echo "  - Excluded: UI, Database, Media3, ML Kit, Security"
echo ""
echo "🚀 To run instrumented tests:"
echo "  ./gradlew :app:connectedDebugAndroidTest --tests '*VideoEmbeddingE2EInstrumentedTest*'"
