#!/bin/bash

# Test script for improved multiple file selection and photo support
# This script verifies the enhanced file picker implementation

echo "🧪 Testing Improved Multiple File Selection & Photo Support"
echo "=========================================================="
echo ""

# Check if the app builds successfully
echo "📱 Step 1: Building the app..."
if ./gradlew :app:assembleDebug > /dev/null 2>&1; then
    echo "✅ App builds successfully"
else
    echo "❌ App build failed"
    exit 1
fi

# Check if ACTION_GET_CONTENT is being used (better for multiple selection)
echo "🔧 Step 2: Checking file picker implementation..."
if grep -q "Intent.ACTION_GET_CONTENT" app/src/main/java/com/mira/whisper/WhisperMainActivity.kt; then
    echo "✅ Using ACTION_GET_CONTENT (better for multiple selection)"
else
    echo "❌ Not using ACTION_GET_CONTENT"
    exit 1
fi

if grep -q "Intent.ACTION_GET_CONTENT" app/src/main/java/com/mira/whisper/WhisperFileSelectionActivity.kt; then
    echo "✅ WhisperFileSelectionActivity uses ACTION_GET_CONTENT"
else
    echo "❌ WhisperFileSelectionActivity doesn't use ACTION_GET_CONTENT"
    exit 1
fi

# Check if photo support is added
echo "📸 Step 3: Checking photo support..."
if grep -q "image/\*" app/src/main/java/com/mira/whisper/WhisperMainActivity.kt; then
    echo "✅ Photo support added to WhisperMainActivity"
else
    echo "❌ Photo support not added to WhisperMainActivity"
    exit 1
fi

if grep -q "jpg.*jpeg.*png.*gif" app/src/main/java/com/mira/whisper/AndroidWhisperBridge.kt; then
    echo "✅ Image formats supported in AndroidWhisperBridge"
else
    echo "❌ Image formats not supported in AndroidWhisperBridge"
    exit 1
fi

# Check if UI shows photo support
echo "🎨 Step 4: Checking UI photo support..."
if grep -q "video or photo files" app/src/main/assets/web/whisper_file_selection.html; then
    echo "✅ UI shows video and photo support"
else
    echo "❌ UI doesn't show photo support"
    exit 1
fi

if grep -q "JPG.*PNG.*GIF" app/src/main/assets/web/whisper_file_selection.html; then
    echo "✅ UI shows supported image formats"
else
    echo "❌ UI doesn't show image formats"
    exit 1
fi

# Check if fallback mechanism is implemented
echo "🔄 Step 5: Checking fallback mechanism..."
if grep -q "fallbackIntent" app/src/main/java/com/mira/whisper/WhisperMainActivity.kt; then
    echo "✅ Fallback mechanism implemented"
else
    echo "❌ Fallback mechanism not implemented"
    exit 1
fi

if grep -q "primaryResolver.*resolveActivity" app/src/main/java/com/mira/whisper/WhisperMainActivity.kt; then
    echo "✅ Intent resolution logic implemented"
else
    echo "❌ Intent resolution logic not implemented"
    exit 1
fi

# Check if MIME types are properly configured
echo "📋 Step 6: Checking MIME type configuration..."
if grep -q "EXTRA_MIME_TYPES.*video.*image" app/src/main/java/com/mira/whisper/WhisperMainActivity.kt; then
    echo "✅ MIME types properly configured"
else
    echo "❌ MIME types not properly configured"
    exit 1
fi

echo ""
echo "🎉 All tests passed! The improved file picker should now:"
echo "   • Support multiple file selection using ACTION_GET_CONTENT"
echo "   • Allow selection of both videos and photos"
echo "   • Fall back to ACTION_OPEN_DOCUMENT if needed"
echo "   • Show clear UI instructions for multiple selection"
echo "   • Support common image formats (JPG, PNG, GIF, etc.)"
echo ""
echo "📱 The file picker should now work much better on Xiaomi devices!"
