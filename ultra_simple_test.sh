#!/bin/bash

# Ultra-simple whisper test - just test JNI loading
echo "=== Ultra-Simple Whisper Test ==="

# Force the app to load WhisperBridge by triggering a simple test
echo "1. Forcing WhisperBridge instantiation..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'test_whisper_bridge' 'true'"

echo "2. Check for JNI loading logs:"
echo "adb logcat -s WhisperJNI:D | head -5"

echo "3. If no logs, the JNI library isn't loading"
