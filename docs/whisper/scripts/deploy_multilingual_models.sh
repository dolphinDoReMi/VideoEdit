#!/bin/bash

# Deploy Multilingual Whisper Models for Robust LID
# This script replaces English-only models with multilingual models for better language detection

set -e

echo "🚀 Deploying Multilingual Whisper Models for Robust LID"
echo "========================================================"

# Configuration
DEVICE_MODEL_DIR="/sdcard/MiraWhisper/models"
LOCAL_MODEL_DIR="./mobile_models"

# Ensure model directory exists
echo "📁 Creating model directory..."
adb shell "mkdir -p $DEVICE_MODEL_DIR"

# Check if multilingual models exist locally
if [ ! -f "$LOCAL_MODEL_DIR/ggml-base-q5_1.bin" ]; then
    echo "❌ Multilingual base model not found locally"
    echo "Please download ggml-base-q5_1.bin from:"
    echo "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin"
    echo ""
    echo "Or run:"
    echo "mkdir -p $LOCAL_MODEL_DIR"
    echo "wget -O $LOCAL_MODEL_DIR/ggml-base-q5_1.bin https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin"
    exit 1
fi

# Deploy multilingual base model (primary)
echo "📤 Deploying multilingual base model..."
adb push "$LOCAL_MODEL_DIR/ggml-base-q5_1.bin" "$DEVICE_MODEL_DIR/whisper-base.q5_1.bin"

# Verify deployment
echo "✅ Verifying model deployment..."
MODEL_SIZE=$(adb shell "stat -c%s $DEVICE_MODEL_DIR/whisper-base.q5_1.bin" 2>/dev/null || echo "0")
if [ "$MODEL_SIZE" -gt 1000000 ]; then
    echo "✅ Model deployed successfully (${MODEL_SIZE} bytes)"
else
    echo "❌ Model deployment failed"
    exit 1
fi

# Optional: Deploy small model for high-accuracy scenarios
if [ -f "$LOCAL_MODEL_DIR/ggml-small-q5_1.bin" ]; then
    echo "📤 Deploying multilingual small model (optional)..."
    adb push "$LOCAL_MODEL_DIR/ggml-small-q5_1.bin" "$DEVICE_MODEL_DIR/whisper-small.q5_1.bin"
    echo "✅ Small model deployed"
fi

# Remove old English-only models if they exist
echo "🧹 Cleaning up old English-only models..."
adb shell "rm -f $DEVICE_MODEL_DIR/whisper-tiny.en-q5_1.bin" 2>/dev/null || true
adb shell "rm -f $DEVICE_MODEL_DIR/ggml-small.en.bin" 2>/dev/null || true
adb shell "rm -f $DEVICE_MODEL_DIR/ggml-base.en.bin" 2>/dev/null || true

# List deployed models
echo ""
echo "📋 Deployed Models:"
adb shell "ls -la $DEVICE_MODEL_DIR/"

echo ""
echo "🎯 Configuration Summary:"
echo "  - Primary Model: whisper-base.q5_1.bin (multilingual)"
echo "  - Language Detection: Auto with two-pass re-scoring"
echo "  - Translation: Disabled (transcribe only)"
echo "  - Confidence Threshold: 0.80"
echo "  - VAD Window: 20 seconds for LID"

echo ""
echo "✅ Multilingual LID deployment complete!"
echo ""
echo "Next steps:"
echo "1. Test with Chinese/English mixed content"
echo "2. Verify LID confidence scores in sidecar files"
echo "3. Check Run Console for language detection results"
