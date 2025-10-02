#!/bin/bash

# Mira v0.1.0 - Firebase Setup Script
# This script helps complete the Firebase App Distribution setup

echo "🔥 Mira Firebase Setup Script"
echo "=============================="
echo ""

# Check if google-services.json exists
if [ -f "app/google-services.json" ]; then
    echo "✅ google-services.json found in app/ directory"
else
    echo "❌ google-services.json not found"
    echo ""
    echo "📋 Next steps to complete Firebase setup:"
    echo ""
    echo "1. Go to https://console.firebase.google.com/"
    echo "2. Create a new project named 'Mira Video Editor'"
    echo "3. Add Android app with package name: com.mira.videoeditor.internal"
    echo "4. Download google-services.json"
    echo "5. Place it in the app/ directory"
    echo "6. Run this script again"
    echo ""
    exit 1
fi

echo ""
echo "🔧 Checking Gradle configuration..."

# Check if Firebase plugin is enabled
if grep -q "id(\"com.google.gms.google-services\")" app/build.gradle.kts; then
    echo "✅ Firebase plugin enabled in app/build.gradle.kts"
else
    echo "❌ Firebase plugin not enabled"
    exit 1
fi

# Check if Firebase App Distribution is configured
if grep -q "firebaseAppDistribution" app/build.gradle.kts; then
    echo "✅ Firebase App Distribution configured"
else
    echo "❌ Firebase App Distribution not configured"
    exit 1
fi

echo ""
echo "🚀 Building internal APK..."

# Clean and build
./gradlew clean
./gradlew assembleInternal

if [ $? -eq 0 ]; then
    echo "✅ Internal APK built successfully"
else
    echo "❌ Build failed"
    exit 1
fi

echo ""
echo "📤 Uploading to Firebase App Distribution..."

# Upload to Firebase App Distribution
./gradlew appDistributionUploadInternal

if [ $? -eq 0 ]; then
    echo "✅ APK uploaded to Firebase App Distribution successfully!"
    echo ""
    echo "🎉 Firebase setup complete!"
    echo ""
    echo "📋 Next steps:"
    echo "1. Go to Firebase Console → App Distribution"
    echo "2. Click 'Add testers'"
    echo "3. Add email addresses of your testers"
    echo "4. Click 'Send invitations'"
    echo ""
    echo "📊 Monitor your app:"
    echo "- Firebase Console → App Distribution"
    echo "- Check crash reports and analytics"
    echo "- Review tester feedback"
else
    echo "❌ Upload failed"
    echo ""
    echo "🔧 Troubleshooting:"
    echo "1. Check if you're logged into Firebase CLI: firebase login"
    echo "2. Verify appId in firebaseAppDistribution block"
    echo "3. Ensure google-services.json is correct"
    exit 1
fi
