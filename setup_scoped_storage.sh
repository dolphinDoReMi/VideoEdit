#!/bin/bash

# Scoped Storage Setup Script
# Sets up proper Android scoped storage directories and permissions
# Ensures all Whisper processing uses app-scoped storage paths

set -e

echo "🔧 Scoped Storage Setup for Whisper Processing"
echo "=============================================="
echo "Setting up Android scoped storage directories"
echo "Timestamp: $(date)"
echo ""

# Configuration
APP_PACKAGE="com.mira.com"
SCOPED_BASE="/storage/emulated/0/Android/data/$APP_PACKAGE/files"
MODELS_DIR="$SCOPED_BASE/MiraWhisper/models"
INPUT_DIR="$SCOPED_BASE/MiraWhisper/in"
OUTPUT_DIR="$SCOPED_BASE/MiraWhisper/out"
SIDECAR_DIR="$SCOPED_BASE/MiraWhisper/sidecars"

echo "📱 App Package: $APP_PACKAGE"
echo "📁 Scoped Base: $SCOPED_BASE"
echo ""

# Check device connection
echo "=== Device Connection Check ==="
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    echo "Please connect your Android device and enable USB debugging"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model)
echo "✅ Device connected: $DEVICE_MODEL"
echo ""

# Check if app is installed
echo "=== App Installation Check ==="
if ! adb shell pm list packages | grep -q "$APP_PACKAGE"; then
    echo "❌ App not installed: $APP_PACKAGE"
    echo "Please install the app first"
    exit 1
fi

echo "✅ App installed: $APP_PACKAGE"
echo ""

# Create scoped storage directories
echo "=== Creating Scoped Storage Directories ==="
echo "Creating directory structure..."

adb shell "mkdir -p $MODELS_DIR"
adb shell "mkdir -p $INPUT_DIR"
adb shell "mkdir -p $OUTPUT_DIR"
adb shell "mkdir -p $SIDECAR_DIR"

echo "✅ Directories created:"
echo "   📁 Models: $MODELS_DIR"
echo "   📁 Input:  $INPUT_DIR"
echo "   📁 Output: $OUTPUT_DIR"
echo "   📁 Sidecar: $SIDECAR_DIR"
echo ""

# Verify directory creation
echo "=== Verifying Directory Creation ==="
for dir in "$MODELS_DIR" "$INPUT_DIR" "$OUTPUT_DIR" "$SIDECAR_DIR"; do
    if adb shell "test -d $dir"; then
        echo "✅ $dir exists"
    else
        echo "❌ $dir missing"
        exit 1
    fi
done
echo ""

# Check directory permissions
echo "=== Checking Directory Permissions ==="
for dir in "$MODELS_DIR" "$INPUT_DIR" "$OUTPUT_DIR" "$SIDECAR_DIR"; do
    PERMS=$(adb shell "ls -ld $dir" | awk '{print $1}')
    echo "📁 $dir: $PERMS"
done
echo ""

# Model deployment check
echo "=== Model Deployment Check ==="
MODEL_FILE="$MODELS_DIR/small.en-q5_1.bin"
if adb shell "test -f $MODEL_FILE"; then
    MODEL_SIZE=$(adb shell "stat -c%s $MODEL_FILE")
    MODEL_SIZE_MB=$((MODEL_SIZE / 1024 / 1024))
    echo "✅ Model found: $MODEL_FILE (${MODEL_SIZE_MB}MB)"
else
    echo "⚠️  Model not found: $MODEL_FILE"
    echo "Please deploy the model using:"
    echo "   adb push whisper_models/small.en-q5_1.bin $MODEL_FILE"
fi
echo ""

# Test file access
echo "=== Testing File Access ==="
TEST_FILE="$INPUT_DIR/test_access.txt"
echo "Creating test file..."
adb shell "echo 'test' > $TEST_FILE"

if adb shell "test -f $TEST_FILE"; then
    echo "✅ File creation successful"
    adb shell "rm $TEST_FILE"
    echo "✅ File deletion successful"
else
    echo "❌ File access test failed"
    exit 1
fi
echo ""

# Generate environment variables
echo "=== Environment Variables ==="
echo "Add these to your shell environment:"
echo ""
echo "export SCOPED_BASE=\"$SCOPED_BASE\""
echo "export MODELS_DIR=\"$MODELS_DIR\""
echo "export INPUT_DIR=\"$INPUT_DIR\""
echo "export OUTPUT_DIR=\"$OUTPUT_DIR\""
echo "export SIDECAR_DIR=\"$SIDECAR_DIR\""
echo ""

# Generate test commands
echo "=== Test Commands ==="
echo "Use these commands to test scoped storage:"
echo ""
echo "# Test DirectWhisperService with scoped storage"
echo "adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \\"
echo "  --es \"action\" \"com.mira.com.feature.whisper.PROCESS_DIRECT\" \\"
echo "  --es \"uri\" \"file://$INPUT_DIR/test.wav\" \\"
echo "  --es \"model\" \"$MODEL_FILE\" \\"
echo "  --es \"threads\" \"4\" \\"
echo "  --es \"lang\" \"en\""
echo ""

echo "✅ Scoped storage setup complete!"
echo ""
echo "📋 Next Steps:"
echo "1. Deploy your model to: $MODEL_FILE"
echo "2. Copy input files to: $INPUT_DIR"
echo "3. Run tests using scoped storage paths"
echo "4. Check output in: $OUTPUT_DIR"
echo ""
echo "🔍 Monitoring:"
echo "adb logcat -s WhisperJNI:V TranscribeWorker:V DirectWhisperService:V"
