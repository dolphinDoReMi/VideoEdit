#!/bin/bash

# DirectWhisperService instantiation test
echo "=== DirectWhisperService Instantiation ==="

# Create a simple test that directly calls DirectWhisperService
echo "1. Creating direct test..."
adb shell "echo 'test' > /sdcard/direct_test.txt"

# Try to trigger DirectWhisperService directly
echo "2. Starting DirectWhisperService..."
adb shell "am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es 'action' 'com.mira.com.feature.whisper.PROCESS_DIRECT' \
    --es 'uri' 'file:///sdcard/direct_test.txt' \
    --es 'model' '/sdcard/MiraWhisper/models/whisper-base.q5_1.bin' \
    --es 'threads' '4' \
    --es 'lang' 'auto' \
    --ez 'translate' 'false'"

echo "3. Check for DirectWhisperService activity:"
adb logcat -d | grep -E "(DirectWhisperService|TranscribeWorker|WhisperApi)" | tail -5

echo "4. Check for JNI loading:"
adb logcat -d | grep -E "(loadLibrary|whisper_jni)" | tail -3

echo "5. DirectWhisperService should work without broadcast receivers"
