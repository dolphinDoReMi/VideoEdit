#!/bin/bash

# Ultra-simple test - just access WhisperBridge object
echo "=== Ultra-Simple WhisperBridge Test ==="

# Create a simple test that just accesses WhisperBridge
echo "1. Testing WhisperBridge object access..."
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity --es 'access_whisper_bridge' 'true'"

echo "2. Check for System.loadLibrary logs:"
adb logcat -d | grep -E "(loadLibrary|whisper_jni)" | tail -3

echo "3. Check for any JNI activity:"
adb logcat -d | grep -E "(WhisperJNI|JNI_OnLoad)" | tail -3

echo "4. If no logs, WhisperBridge is never being accessed"
