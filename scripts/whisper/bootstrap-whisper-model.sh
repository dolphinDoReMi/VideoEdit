#!/bin/bash

# ADB Bootstrap Script for Whisper Models
# Pushes models to internal app storage using run-as (no root required)

set -e

APP_PACKAGE="com.mira.com"  # ENFORCED: Fixed package name
MODEL_NAME="small.en.bin"
LOCAL_MODEL_PATH="whisper_real/models/for-tests-ggml-small.en.bin"

echo "=== Whisper Model ADB Bootstrap ==="
echo "Package: $APP_PACKAGE"
echo "Model: $MODEL_NAME"
echo "Local path: $LOCAL_MODEL_PATH"

# Check if local model exists
if [[ ! -f "$LOCAL_MODEL_PATH" ]]; then
    echo "❌ Local model not found: $LOCAL_MODEL_PATH"
    echo "Please ensure the model file exists at the specified path"
    exit 1
fi

echo "✅ Local model found: $LOCAL_MODEL_PATH"

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    exit 1
fi

echo "✅ Android device connected"

# Check if app is installed
if ! adb shell pm list packages | grep -q "^package:$APP_PACKAGE$"; then
    echo "❌ App not installed: $APP_PACKAGE"
    exit 1
fi

echo "✅ App installed: $APP_PACKAGE"

# Create internal models directory
echo "📁 Creating internal models directory..."
adb shell run-as $APP_PACKAGE "mkdir -p files/models" || echo "Directory may already exist"
adb shell run-as $APP_PACKAGE "ls -la files/" || echo "Checking files directory"

# Push model to internal storage
echo "📤 Pushing model to internal storage..."
adb shell run-as $APP_PACKAGE sh -c "cat > files/models/$MODEL_NAME" < "$LOCAL_MODEL_PATH"

# Verify the copy
echo "🔍 Verifying model copy..."
REMOTE_SIZE=$(adb shell run-as $APP_PACKAGE "stat -c%s files/models/$MODEL_NAME" 2>/dev/null || echo "0")
LOCAL_SIZE=$(stat -c%s "$LOCAL_MODEL_PATH" 2>/dev/null || stat -f%z "$LOCAL_MODEL_PATH" 2>/dev/null || echo "0")

if [[ "$REMOTE_SIZE" == "$LOCAL_SIZE" && "$REMOTE_SIZE" != "0" ]]; then
    echo "✅ Model copied successfully"
    echo "   Local size: $LOCAL_SIZE bytes"
    echo "   Remote size: $REMOTE_SIZE bytes"
else
    echo "❌ Model copy verification failed"
    echo "   Local size: $LOCAL_SIZE bytes"
    echo "   Remote size: $REMOTE_SIZE bytes"
    exit 1
fi

# Verify GGML/GGUF header
echo "🔍 Verifying model header..."
HEADER=$(adb shell run-as $APP_PACKAGE "head -c4 files/models/$MODEL_NAME" | xxd -p | tr -d '\n')
if [[ "$HEADER" == "47475546" ]]; then  # "GGUF" in hex
    echo "✅ GGUF header verified"
elif [[ "$HEADER" == "67676d6c" ]]; then  # "ggml" in hex
    echo "✅ GGML header verified"
else
    echo "❌ Invalid model header: $HEADER (expected GGUF or GGML)"
    exit 1
fi

echo ""
echo "🎉 Bootstrap completed successfully!"
echo "Model is now available at: /data/data/$APP_PACKAGE/files/models/$MODEL_NAME"
echo ""
echo "You can now test the service:"
echo "adb shell am startservice -n $APP_PACKAGE/com.mira.com.feature.whisper.service.DirectWhisperService \\"
echo "  --es \"action\" \"com.mira.com.feature.whisper.PROCESS_DIRECT\" \\"
echo "  --es \"uri\" \"file:///sdcard/MiraWhisper/in/test.wav\" \\"
echo "  --es \"threads\" \"4\" \\"
echo "  --es \"lang\" \"en\""
