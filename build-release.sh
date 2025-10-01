#!/bin/bash

# Mira Release Build Script
# This script builds the app for Xiaomi Store release

set -e  # Exit on any error

echo "🚀 Mira Release Build Script"
echo "=================================="

# Check if we're in the right directory
if [ ! -f "settings.gradle.kts" ]; then
    echo "❌ Error: Please run this script from the project root directory"
    exit 1
fi

# Check if keystore exists
KEYSTORE_FILE="keystore/mira-release.keystore"
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "❌ Error: Keystore file not found at $KEYSTORE_FILE"
    echo "Please create a keystore first:"
    echo "keytool -genkey -v -keystore $KEYSTORE_FILE -alias mira -keyalg RSA -keysize 2048 -validity 10000"
    exit 1
fi

# Check gradle.properties configuration
if grep -q "your_secure_store_password_here" gradle.properties; then
    echo "❌ Error: Please update gradle.properties with your actual keystore credentials"
    echo "Edit gradle.properties and replace placeholder values with real credentials"
    exit 1
fi

echo "📱 Building Mira for Xiaomi Store..."

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build release APK
echo "🔨 Building release APK..."
./gradlew assembleRelease

# Build release AAB (Android App Bundle) - preferred for Play Store
echo "📦 Building release AAB..."
./gradlew bundleRelease

# Build internal testing version
echo "🧪 Building internal testing version..."
./gradlew assembleInternal

echo "✅ Build completed successfully!"
echo ""
echo "📁 Output files:"
echo "   Release APK: app/build/outputs/apk/release/app-release.apk"
echo "   Release AAB: app/build/outputs/bundle/release/app-release.aab"
echo "   Internal APK: app/build/outputs/apk/internal/app-internal-release.apk"
echo ""
echo "📋 Next steps:"
echo "   1. Test the internal APK on Xiaomi devices"
echo "   2. Upload the AAB to Xiaomi Developer Console for review"
echo "   3. Complete store listing information"
echo "   4. Submit for public release"
echo ""
echo "🎉 Ready for Xiaomi Store submission!"
