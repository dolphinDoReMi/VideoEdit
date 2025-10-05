#!/system/bin/sh

# Simple Whisper test script for Xiaomi Pad Ultra
# This script tests the core Whisper functionality without complex build dependencies

echo "=== Xiaomi Pad Ultra Whisper Test ==="
echo "Device: $(getprop ro.product.model)"
echo "Android: $(getprop ro.build.version.release)"
echo "Architecture: $(getprop ro.product.cpu.abi)"
echo ""

# Check if test file exists
TEST_FILE="/sdcard/MiraWhisper/in/test_sample.mp4"
if [ ! -f "$TEST_FILE" ]; then
    echo "❌ Test file not found: $TEST_FILE"
    exit 1
fi

echo "✅ Test file found: $TEST_FILE ($(stat -c%s "$TEST_FILE") bytes)"
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

# Test Whisper via broadcast
echo "Testing Whisper via broadcast..."
am broadcast -a "${APP_PACKAGE}.whisper.RUN" \
    --es filePath "$TEST_FILE" \
    --es model "whisper-tiny.en-q5_1" \
    --ei threads 6 \
    --es lang "en" \
    --ez translate false

echo "✅ Broadcast sent"
echo ""

# Wait a bit for processing
echo "Waiting for processing..."
sleep 5

# Check for output
OUTPUT_DIR="/sdcard/Mira/out"
if [ -d "$OUTPUT_DIR" ]; then
    echo "✅ Output directory found: $OUTPUT_DIR"
    ls -la "$OUTPUT_DIR"
else
    echo "⚠️  No output directory found yet"
fi

# Check app databases
echo ""
echo "Checking app databases..."
run-as "$APP_PACKAGE" ls -la /data/data/"$APP_PACKAGE"/databases/ 2>/dev/null || echo "Cannot access databases"

echo ""
echo "=== Test completed ==="
