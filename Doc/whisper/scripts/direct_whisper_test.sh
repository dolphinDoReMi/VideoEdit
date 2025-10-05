#!/system/bin/sh

# Direct Whisper Test Script - Bypass Installation Restrictions
# This script attempts to test Whisper functionality directly

echo "=== Direct Whisper Test - Bypassing Installation Restrictions ==="
echo "Device: $(getprop ro.product.model)"
echo "Android: $(getprop ro.build.version.release)"
echo "Architecture: $(getprop ro.product.cpu.abi)"
echo ""

# Test files
AUDIO_FILE="/sdcard/MiraWhisper/in/test_audio.wav"
MODEL_FILE="/sdcard/MiraWhisper/models/whisper-tiny.en-q5_1.bin"
OUTPUT_DIR="/sdcard/MiraWhisper/out"

# Check files
if [ ! -f "$AUDIO_FILE" ]; then
    echo "❌ Audio file not found: $AUDIO_FILE"
    exit 1
fi

if [ ! -f "$MODEL_FILE" ]; then
    echo "❌ Model file not found: $MODEL_FILE"
    exit 1
fi

echo "✅ Audio file: $AUDIO_FILE ($(stat -c%s "$AUDIO_FILE") bytes)"
echo "✅ Model file: $MODEL_FILE ($(stat -c%s "$MODEL_FILE") bytes)"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"
echo "✅ Output directory: $OUTPUT_DIR"
echo ""

# Try to access the app's native libraries
APP_PACKAGE="com.mira.clip.debug"
APP_LIB_DIR="/data/app/*/com.mira.clip.debug*/lib/arm64"

echo "Checking for Whisper native libraries..."
find /data/app -name "*com.mira.clip.debug*" -type d 2>/dev/null | head -1 | while read app_dir; do
    if [ -d "$app_dir/lib/arm64" ]; then
        echo "✅ Found app lib directory: $app_dir/lib/arm64"
        ls -la "$app_dir/lib/arm64" | grep -E "(whisper|jni)" || echo "⚠️  No Whisper JNI libraries found"
    fi
done

echo ""

# Try to trigger processing via multiple methods
echo "Method 1: Direct broadcast to WhisperReceiver..."
am broadcast -a "${APP_PACKAGE}.whisper.RUN" \
    --es filePath "$AUDIO_FILE" \
    --es model "$MODEL_FILE" \
    --ei threads 6 \
    --es lang "en" \
    --ez translate false

echo "✅ Whisper broadcast sent"
echo ""

echo "Method 2: Try alternative broadcast actions..."
am broadcast -a "${APP_PACKAGE}.SELFTEST_AUDIO" --es filePath "$AUDIO_FILE"
am broadcast -a "${APP_PACKAGE}.AUDIO.RUN" --es filePath "$AUDIO_FILE"
am broadcast -a "${APP_PACKAGE}.TRANSCRIBE" --es filePath "$AUDIO_FILE"

echo "✅ Alternative broadcasts sent"
echo ""

echo "Method 3: Try to start activity with audio file..."
am start -n "$APP_PACKAGE/com.mira.clip.test.SimpleAsrTestActivity" -d "file://$AUDIO_FILE"

echo "✅ Activity started with audio file"
echo ""

# Wait for processing
echo "Waiting for processing (15 seconds)..."
sleep 15

# Check for output
echo "Checking for output files..."
if [ -d "$OUTPUT_DIR" ]; then
    echo "✅ Output directory exists: $OUTPUT_DIR"
    ls -la "$OUTPUT_DIR"
    
    # Check for specific output files
    if [ -f "$OUTPUT_DIR/test_audio.json" ]; then
        echo "✅ Found JSON output: test_audio.json"
        cat "$OUTPUT_DIR/test_audio.json"
    fi
    
    if [ -f "$OUTPUT_DIR/test_audio.srt" ]; then
        echo "✅ Found SRT output: test_audio.srt"
        cat "$OUTPUT_DIR/test_audio.srt"
    fi
else
    echo "⚠️  No output directory found"
fi

echo ""

# Check app databases
echo "Checking app databases..."
run-as "$APP_PACKAGE" ls -la /data/data/"$APP_PACKAGE"/databases/ 2>/dev/null || echo "Cannot access databases"

# Check recent logs
echo ""
echo "Recent Whisper-related logs:"
logcat -d | grep -E "(Whisper|Transcribe|SimpleAsrTestActivity|test_audio|WhisperReceiver)" | tail -15

echo ""
echo "=== Direct Whisper Test Completed ==="
echo "Summary:"
echo "- Audio file: $AUDIO_FILE"
echo "- Model file: $MODEL_FILE"
echo "- Output dir: $OUTPUT_DIR"
echo "- All test methods executed"
echo "- Check logs above for any Whisper processing activity"
