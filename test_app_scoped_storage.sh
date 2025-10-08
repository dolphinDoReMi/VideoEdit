#!/bin/bash

# Test script to verify app-scoped storage implementation
# This script tests the new sidecar storage system to ensure EPERM issues are resolved

echo "=== Testing App-Scoped Storage Implementation ==="
echo "Date: $(date)"
echo ""

# Set up test environment
TEST_CLIP="/sdcard/MiraWhisper/in/tennis_interview_clip_002.mp4"
MODEL_PATH="/sdcard/MiraWhisper/models/whisper-base.q5_1.bin"
PACKAGE_NAME="com.mira.com"

echo "1. Checking test files..."
if [ ! -f "$TEST_CLIP" ]; then
    echo "ERROR: Test clip not found at $TEST_CLIP"
    exit 1
fi

if [ ! -f "$MODEL_PATH" ]; then
    echo "ERROR: Model not found at $MODEL_PATH"
    exit 1
fi

echo "✓ Test clip found: $TEST_CLIP"
echo "✓ Model found: $MODEL_PATH"
echo ""

echo "2. Checking app-scoped storage path..."
APP_SCOPED_PATH="/sdcard/Android/data/$PACKAGE_NAME/files/MiraWhisper/out"
echo "App-scoped path: $APP_SCOPED_PATH"

# Create the directory if it doesn't exist
adb shell "mkdir -p $APP_SCOPED_PATH"
if [ $? -eq 0 ]; then
    echo "✓ App-scoped directory created/verified"
else
    echo "✗ Failed to create app-scoped directory"
    exit 1
fi
echo ""

echo "3. Running transcription test..."
echo "Starting transcription with new storage system..."

# Run the transcription using the DirectWhisperService
adb shell "am startservice -n $PACKAGE_NAME/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es action com.mira.com.feature.whisper.PROCESS_DIRECT \
    --es uri file://$TEST_CLIP \
    --es model $MODEL_PATH \
    --es threads 4 \
    --es lang en \
    --ez translate false"

if [ $? -eq 0 ]; then
    echo "✓ Transcription service started successfully"
else
    echo "✗ Failed to start transcription service"
    exit 1
fi
echo ""

echo "4. Monitoring for sidecar creation..."
echo "Waiting for sidecar files to be created in app-scoped storage..."

# Wait for sidecar files to appear
TIMEOUT=60
ELAPSED=0
SIDECAR_FOUND=false

while [ $ELAPSED -lt $TIMEOUT ]; do
    # Check for sidecar files in app-scoped storage
    SIDECAR_COUNT=$(adb shell "find $APP_SCOPED_PATH -name '*.json' 2>/dev/null | wc -l" | tr -d ' ')
    
    if [ "$SIDECAR_COUNT" -gt 0 ]; then
        echo "✓ Found $SIDECAR_COUNT sidecar file(s) in app-scoped storage!"
        SIDECAR_FOUND=true
        break
    fi
    
    echo "Waiting... (${ELAPSED}s/${TIMEOUT}s)"
    sleep 5
    ELAPSED=$((ELAPSED + 5))
done

if [ "$SIDECAR_FOUND" = false ]; then
    echo "✗ No sidecar files found after $TIMEOUT seconds"
    echo "Checking logs for errors..."
    adb logcat -d | grep -E "(TranscribeWorker|StorageSelfTest|EPERM|Operation not permitted)" | tail -20
    exit 1
fi
echo ""

echo "5. Verifying sidecar content..."
# List the sidecar files
echo "Sidecar files found:"
adb shell "find $APP_SCOPED_PATH -name '*.json' -exec ls -la {} \;"

# Check one sidecar file for valid JSON
SIDECAR_FILE=$(adb shell "find $APP_SCOPED_PATH -name '*.json' | head -1" | tr -d '\r\n')
if [ -n "$SIDECAR_FILE" ]; then
    echo ""
    echo "Sample sidecar content from $SIDECAR_FILE:"
    adb shell "head -c 500 $SIDECAR_FILE"
    echo ""
    echo "..."
fi
echo ""

echo "6. Checking for EPERM errors in logs..."
EPERM_COUNT=$(adb logcat -d | grep -c "EPERM\|Operation not permitted" || echo "0")
if [ "$EPERM_COUNT" -eq 0 ]; then
    echo "✓ No EPERM errors found in logs!"
else
    echo "⚠ Found $EPERM_COUNT EPERM errors in logs:"
    adb logcat -d | grep -E "(EPERM|Operation not permitted)" | tail -5
fi
echo ""

echo "7. Storage self-test verification..."
SELFTEST_COUNT=$(adb logcat -d | grep -c "Storage self-test passed" || echo "0")
if [ "$SELFTEST_COUNT" -gt 0 ]; then
    echo "✓ Storage self-test passed ($SELFTEST_COUNT times)"
else
    echo "⚠ No storage self-test success messages found"
fi
echo ""

echo "=== Test Results Summary ==="
if [ "$SIDECAR_FOUND" = true ] && [ "$EPERM_COUNT" -eq 0 ]; then
    echo "✅ SUCCESS: App-scoped storage implementation working correctly!"
    echo "✅ No EPERM errors detected"
    echo "✅ Sidecar files created successfully"
    echo ""
    echo "The new storage system has resolved the EPERM issues."
    echo "Workers can now write sidecar files reliably to app-scoped storage."
else
    echo "❌ FAILURE: Issues detected with the implementation"
    echo "❌ EPERM errors: $EPERM_COUNT"
    echo "❌ Sidecar files found: $SIDECAR_FOUND"
    exit 1
fi

echo ""
echo "App-scoped storage path: $APP_SCOPED_PATH"
echo "Test completed at: $(date)"
