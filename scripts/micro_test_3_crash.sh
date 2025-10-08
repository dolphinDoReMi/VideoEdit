#!/bin/bash

# Micro Test 3: App Crash Investigation
# Following XiaoMi Pad Inference Optimization guide

echo "🔍 Micro Test 3: App Crash Investigation"
echo "========================================="
echo "Timestamp: $(date)"
echo ""

# Check app installation
echo "📱 Checking app installation..."
adb shell "pm list packages | grep com.mira.com"
echo ""

# Check app permissions
echo "🔐 Checking app permissions..."
adb shell "dumpsys package com.mira.com | grep -A 20 'requested permissions:'"
echo ""

# Try launching with more verbose logging
echo "🚀 Launching app with verbose logging..."
adb logcat -c
adb shell "am start -n com.mira.com/com.mira.whisper.WhisperMainActivity"

# Monitor logs for crashes
echo "⏳ Monitoring logs for 10 seconds..."
sleep 10

echo "📊 Crash/Error logs:"
adb logcat -d | grep -E "(FATAL|AndroidRuntime|com.mira.com)" | tail -20
echo ""

# Check if there are any ANR (Application Not Responding) logs
echo "📊 ANR logs:"
adb shell "ls -la /data/anr/ 2>/dev/null | tail -5" || echo "No ANR logs found"
echo ""

echo "✅ Micro Test 3 completed!"
