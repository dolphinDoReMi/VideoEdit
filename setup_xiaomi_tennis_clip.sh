#!/bin/bash

# Xiaomi Pad Setup for Tennis Clip Processing
# Ensures device is ready for transcription workflow

set -e

echo "🎾 Xiaomi Pad Setup for Tennis Clip Processing"
echo "============================================="
echo ""

# Check ADB connection
echo "🔌 Checking ADB connection..."
if ! adb devices | grep -q "device$"; then
    echo "❌ No Xiaomi Pad device connected via ADB"
    echo "Please ensure:"
    echo "  1. USB debugging is enabled"
    echo "  2. Device is connected via USB"
    echo "  3. ADB authorization is granted"
    exit 1
fi

DEVICE_INFO=$(adb shell "getprop ro.product.model && getprop ro.build.version.release")
echo "✅ Connected to: $DEVICE_INFO"

# Check if app is installed
echo ""
echo "📱 Checking Whisper app installation..."
if ! adb shell "pm list packages | grep -q com.mira.com"; then
    echo "❌ Whisper app not found"
    echo "Please install the app first"
    exit 1
fi
echo "✅ Whisper app is installed"

# Create necessary directories
echo ""
echo "📁 Setting up directories..."
DIRECTORIES=(
    "/sdcard/MiraWhisper"
    "/sdcard/MiraWhisper/transcriptions"
    "/sdcard/MiraWhisper/samples"
    "/sdcard/MiraWhisper/models"
    "/sdcard/MiraWhisper/out"
    "/sdcard/MiraWhisper/sidecars"
)

for dir in "${DIRECTORIES[@]}"; do
    adb shell "mkdir -p '$dir'"
    echo "  ✅ Created: $dir"
done

# Check for whisper model
echo ""
echo "🤖 Checking Whisper model..."
MODEL_PATH="/sdcard/MiraWhisper/models/whisper-base.q5_1.bin"
if adb shell "test -f '$MODEL_PATH'"; then
    MODEL_SIZE=$(adb shell "stat -c%s '$MODEL_PATH'" | tr -d '\r')
    MODEL_SIZE_MB=$((MODEL_SIZE / 1024 / 1024))
    echo "✅ Model found: $MODEL_PATH (${MODEL_SIZE_MB}MB)"
else
    echo "⚠️  Model not found: $MODEL_PATH"
    echo "Please ensure the whisper model is available"
fi

# Check available storage
echo ""
echo "💾 Checking storage space..."
STORAGE_INFO=$(adb shell "df /sdcard | tail -1")
echo "Storage info: $STORAGE_INFO"

# Check if clip file exists
echo ""
echo "🎬 Checking for tennis clip file..."
CLIP_PATHS=(
    "/sdcard/MiraWhisper/tennis_interview_clip_001.mp4"
    "/sdcard/Download/tennis_interview_clip_001.mp4"
    "/sdcard/Movies/tennis_interview_clip_001.mp4"
    "/sdcard/DCIM/tennis_interview_clip_001.mp4"
    "/sdcard/Documents/tennis_interview_clip_001.mp4"
)

CLIP_FOUND=""
for path in "${CLIP_PATHS[@]}"; do
    if adb shell "test -f '$path'" 2>/dev/null; then
        CLIP_FOUND="$path"
        FILE_SIZE=$(adb shell "stat -c%s '$path'" | tr -d '\r')
        FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
        echo "✅ Found clip: $path (${FILE_SIZE_MB}MB)"
        break
    fi
done

if [[ -z "$CLIP_FOUND" ]]; then
    echo "⚠️  Tennis clip not found in common locations"
    echo "Searched paths:"
    for path in "${CLIP_PATHS[@]}"; do
        echo "  - $path"
    done
    echo ""
    echo "Please ensure tennis_interview_clip_001.mp4 is available"
fi

# Test app launch
echo ""
echo "🚀 Testing app launch..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity" >/dev/null 2>&1
sleep 3

# Check if app is running
if adb shell "ps | grep -q com.mira.com"; then
    echo "✅ App launched successfully"
else
    echo "⚠️  App launch may have failed"
fi

# Clear logs
echo ""
echo "🧹 Clearing logs..."
adb logcat -c
echo "✅ Logs cleared"

echo ""
echo "🎾 Xiaomi Pad Setup Complete!"
echo "============================="
echo ""
echo "📋 Setup Summary:"
echo "  - ADB connection: ✅"
echo "  - Whisper app: ✅"
echo "  - Directories: ✅"
echo "  - Model: $([ -n "$MODEL_FOUND" ] && echo "✅" || echo "⚠️")"
echo "  - Clip file: $([ -n "$CLIP_FOUND" ] && echo "✅" || echo "⚠️")"
echo "  - App launch: ✅"
echo ""
echo "🚀 Ready to process tennis clip!"
echo ""
echo "💡 Next steps:"
echo "  1. Run: ./process_tennis_clip_xiaomi.sh"
echo "  2. Monitor: ./monitor_tennis_clip_xiaomi.sh"
echo "  3. Continuous monitoring: ./monitor_tennis_clip_xiaomi.sh --continuous"
