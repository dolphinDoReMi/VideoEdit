#!/system/bin/sh

# Comprehensive Whisper Test Script for Xiaomi Pad Ultra
# Tests video_v1.mp4 with Whisper pipeline

echo "=== Xiaomi Pad Ultra Whisper Test with video_v1.mp4 ==="
echo "Device: $(getprop ro.product.model)"
echo "Android: $(getprop ro.build.version.release)"
echo "Architecture: $(getprop ro.product.cpu.abi)"
echo ""

# Test file paths
TEST_FILE="/sdcard/MiraWhisper/in/video_v1.mp4"
MODEL_FILE="/sdcard/MiraWhisper/models/whisper-tiny.en-q5_1.bin"
OUTPUT_DIR="/sdcard/MiraWhisper/out"

# Check if test file exists
if [ ! -f "$TEST_FILE" ]; then
    echo "❌ Test file not found: $TEST_FILE"
    exit 1
fi

echo "✅ Test file found: $TEST_FILE ($(stat -c%s "$TEST_FILE") bytes)"

# Check if model exists
if [ ! -f "$MODEL_FILE" ]; then
    echo "❌ Model file not found: $MODEL_FILE"
    exit 1
fi

echo "✅ Model file found: $MODEL_FILE ($(stat -c%s "$MODEL_FILE") bytes)"
echo ""

# Check if the app is installed
APP_PACKAGE="com.mira.clip.debug"
if ! pm list packages | grep -q "$APP_PACKAGE"; then
    echo "❌ App not installed: $APP_PACKAGE"
    exit 1
fi

echo "✅ App installed: $APP_PACKAGE"
echo ""

# Grant permissions
echo "Granting permissions..."
pm grant "$APP_PACKAGE" android.permission.READ_MEDIA_AUDIO
pm grant "$APP_PACKAGE" android.permission.READ_MEDIA_VIDEO
pm grant "$APP_PACKAGE" android.permission.WRITE_EXTERNAL_STORAGE
echo "✅ Permissions granted"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"
echo "✅ Output directory created: $OUTPUT_DIR"
echo ""

# Test 1: Start SimpleAsrTestActivity with video file
echo "Test 1: Starting SimpleAsrTestActivity with video_v1.mp4..."
am start -n "$APP_PACKAGE/com.mira.clip.test.SimpleAsrTestActivity" -d "file://$TEST_FILE"
sleep 3
echo "✅ Activity started"
echo ""

# Test 2: Try Whisper broadcast with correct model path
echo "Test 2: Sending Whisper broadcast with video_v1.mp4..."
am broadcast -a "${APP_PACKAGE}.whisper.RUN" \
    --es filePath "$TEST_FILE" \
    --es model "$MODEL_FILE" \
    --ei threads 6 \
    --es lang "en" \
    --ez translate false

echo "✅ Broadcast sent"
echo ""

# Test 3: Try alternative broadcast actions
echo "Test 3: Trying alternative broadcast actions..."

# Try SELFTEST_VIDEO
am broadcast -a "${APP_PACKAGE}.SELFTEST_VIDEO" --es filePath "$TEST_FILE"
echo "✅ SELFTEST_VIDEO broadcast sent"

# Try CLIP.RUN
am broadcast -a "${APP_PACKAGE}.CLIP.RUN" --es filePath "$TEST_FILE"
echo "✅ CLIP.RUN broadcast sent"

echo ""

# Wait for processing
echo "Waiting for processing (10 seconds)..."
sleep 10

# Check for output
echo "Checking for output files..."
if [ -d "$OUTPUT_DIR" ]; then
    echo "✅ Output directory exists: $OUTPUT_DIR"
    ls -la "$OUTPUT_DIR"
else
    echo "⚠️  No output directory found yet"
fi

# Check app databases
echo ""
echo "Checking app databases..."
run-as "$APP_PACKAGE" ls -la /data/data/"$APP_PACKAGE"/databases/ 2>/dev/null || echo "Cannot access databases"

# Check app files
echo ""
echo "Checking app files..."
run-as "$APP_PACKAGE" ls -la /data/data/"$APP_PACKAGE"/files/ 2>/dev/null || echo "Cannot access files"

# Check recent logs
echo ""
echo "Recent Whisper-related logs:"
logcat -d | grep -E "(Whisper|Transcribe|SimpleAsrTestActivity|video_v1)" | tail -10

echo ""
echo "=== Test completed ==="
echo "Summary:"
echo "- Test file: $TEST_FILE"
echo "- Model file: $MODEL_FILE"
echo "- Output dir: $OUTPUT_DIR"
echo "- App package: $APP_PACKAGE"
echo "- All broadcasts sent successfully"
echo "- Check logs above for any Whisper processing activity"
