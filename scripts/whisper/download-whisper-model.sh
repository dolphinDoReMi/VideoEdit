#!/bin/bash

# Whisper Model Download Script
# Downloads the recommended small.en-q5_1.gguf model using the most reliable method

set -e

echo "🎯 Whisper Model Download Script"
echo "================================"

# Configuration
MODEL_NAME="small.en-q5_1.gguf"
HF_FILENAME="ggml-small.en-q5_1.gguf"
MODELS_DIR="models/whisper"
APP_PACKAGE="com.mira.com"

# Expected file sizes (MB)
EXPECTED_SIZES=(
    "tiny.en-q4_0.gguf:35-45"
    "small.en-q5_1.gguf:240-260"
    "base.en-q5_1.gguf:130-160"
)

echo "📋 Configuration:"
echo "  Model: $MODEL_NAME"
echo "  HF Filename: $HF_FILENAME"
echo "  Local Directory: $MODELS_DIR"
echo "  App Package: $APP_PACKAGE"
echo ""

# Create models directory
mkdir -p "$MODELS_DIR"

# Method 1: Python huggingface_hub (most reliable)
echo "🐍 Method 1: Using Python huggingface_hub (recommended)"
echo "--------------------------------------------------------"

# Check if Python is available
if ! command -v python3 &> /dev/null; then
    echo "❌ Python3 not found. Please install Python3 first."
    exit 1
fi

# Create virtual environment
echo "📦 Creating Python virtual environment..."
python3 -m venv .venv
source .venv/bin/activate

# Install huggingface_hub
echo "📥 Installing huggingface_hub..."
pip install --upgrade huggingface_hub

# Download the model
echo "⬇️  Downloading model from Hugging Face..."
DOWNLOAD_PATH=$(python3 - <<EOF
from huggingface_hub import hf_hub_download
import os

try:
    path = hf_hub_download(
        repo_id="ggerganov/whisper.cpp",
        filename="$HF_FILENAME"
    )
    print(path)
except Exception as e:
    print(f"ERROR: {e}")
    exit(1)
EOF
)

if [ $? -ne 0 ]; then
    echo "❌ Download failed. Trying alternative method..."
    
    # Method 2: curl with retries
    echo ""
    echo "🌐 Method 2: Using curl with retries"
    echo "------------------------------------"
    
    curl -L --retry 10 --retry-all-errors --retry-delay 2 -C - \
        -o "$MODELS_DIR/$MODEL_NAME" \
        "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$HF_FILENAME?download=true"
    
    DOWNLOAD_PATH="$MODELS_DIR/$MODEL_NAME"
else
    # Copy to our models directory
    echo "📁 Copying model to local directory..."
    cp "$DOWNLOAD_PATH" "$MODELS_DIR/$MODEL_NAME"
    DOWNLOAD_PATH="$MODELS_DIR/$MODEL_NAME"
fi

echo ""
echo "✅ Download completed: $DOWNLOAD_PATH"

# Verification
echo ""
echo "🔍 Verifying downloaded model..."
echo "================================"

# Check file size
FILE_SIZE=$(ls -lh "$DOWNLOAD_PATH" | awk '{print $5}')
FILE_SIZE_BYTES=$(stat -f%z "$DOWNLOAD_PATH" 2>/dev/null || stat -c%s "$DOWNLOAD_PATH" 2>/dev/null)
FILE_SIZE_MB=$((FILE_SIZE_BYTES / 1024 / 1024))

echo "📏 File size: $FILE_SIZE ($FILE_SIZE_MB MB)"

# Check if size is in expected range
EXPECTED_MIN=240
EXPECTED_MAX=260
if [ $FILE_SIZE_MB -ge $EXPECTED_MIN ] && [ $FILE_SIZE_MB -le $EXPECTED_MAX ]; then
    echo "✅ File size is within expected range ($EXPECTED_MIN-$EXPECTED_MAX MB)"
else
    echo "⚠️  File size is outside expected range ($EXPECTED_MIN-$EXPECTED_MAX MB)"
fi

# Check file header (must be GGUF)
echo "🔍 Checking file header..."
HEADER=$(hexdump -C -n 4 "$DOWNLOAD_PATH" | head -1 | awk '{print $2 $3 $4 $5}')
if [ "$HEADER" = "47554746" ]; then
    echo "✅ File header is correct (GGUF)"
else
    echo "❌ File header is incorrect. Expected GGUF, got: $HEADER"
    echo "   This might be an HTML error page or corrupted file."
    exit 1
fi

# Calculate checksum
echo "🔐 Calculating SHA256 checksum..."
CHECKSUM=$(shasum -a 256 "$DOWNLOAD_PATH" | awk '{print $1}')
echo "📋 SHA256: $CHECKSUM"

# Check if file contains HTML (common error)
if head -1 "$DOWNLOAD_PATH" | grep -q "<html"; then
    echo "❌ File appears to be HTML (download was blocked)"
    echo "   Try using a VPN or different network"
    exit 1
fi

echo ""
echo "✅ Model verification completed successfully!"

# Device deployment
echo ""
echo "📱 Deploying to Android device..."
echo "================================="

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "⚠️  ADB not found. Skipping device deployment."
    echo "   You can manually deploy using:"
    echo "   adb push $DOWNLOAD_PATH /storage/emulated/0/Android/data/$APP_PACKAGE/files/MiraWhisper/models/"
    exit 0
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "⚠️  No Android device connected. Skipping device deployment."
    echo "   Connect your device and run:"
    echo "   adb push $DOWNLOAD_PATH /storage/emulated/0/Android/data/$APP_PACKAGE/files/MiraWhisper/models/"
    exit 0
fi

echo "📱 Device detected. Deploying model..."

# Method A: Scoped storage (easier)
echo "📁 Deploying to scoped storage..."
SCOPED_PATH="/storage/emulated/0/Android/data/$APP_PACKAGE/files/MiraWhisper/models"
adb shell "mkdir -p $SCOPED_PATH"
adb push "$DOWNLOAD_PATH" "$SCOPED_PATH/"

echo "✅ Model deployed to scoped storage: $SCOPED_PATH"

# Method B: Internal app storage (more secure)
echo "🔒 Deploying to internal app storage..."
adb shell run-as "$APP_PACKAGE" "mkdir -p files/models"
adb shell run-as "$APP_PACKAGE" sh -c "cat > files/models/$MODEL_NAME" < "$DOWNLOAD_PATH"

echo "✅ Model deployed to internal storage: /data/data/$APP_PACKAGE/files/models/$MODEL_NAME"

# Summary
echo ""
echo "🎉 Download and Deployment Complete!"
echo "===================================="
echo ""
echo "📋 Summary:"
echo "  • Model: $MODEL_NAME"
echo "  • Size: $FILE_SIZE ($FILE_SIZE_MB MB)"
echo "  • Checksum: $CHECKSUM"
echo "  • Local path: $DOWNLOAD_PATH"
echo "  • Scoped storage: $SCOPED_PATH/$MODEL_NAME"
echo "  • Internal storage: /data/data/$APP_PACKAGE/files/models/$MODEL_NAME"
echo ""
echo "🔧 Usage in your app:"
echo "  • Use File(context.filesDir, \"models/$MODEL_NAME\") for internal storage"
echo "  • Pass absolute filesystem path to JNI (not URI)"
echo "  • Example: /data/data/$APP_PACKAGE/files/models/$MODEL_NAME"
echo ""
echo "✨ Ready to use!"

# Cleanup
deactivate
rm -rf .venv
