#!/bin/bash

# Micro Test 4: Broadcast Intent Test
# Following XiaoMi Pad Inference Optimization guide - try broadcast approach

echo "📡 Micro Test 4: Broadcast Intent Test"
echo "======================================"
echo "Timestamp: $(date)"
echo ""

# Clear app state
echo "🧹 Cleaning app state..."
adb shell "am force-stop com.mira.com"
adb logcat -c
echo ""

# Try using broadcast intents instead of direct activity launch
echo "📡 Testing broadcast intents..."

# Test 1: Simple broadcast
echo "📤 Test 1: Simple broadcast..."
adb shell "am broadcast -a com.mira.com.action.TEST"

sleep 2

# Test 2: Whisper-specific broadcast
echo "📤 Test 2: Whisper broadcast..."
adb shell "am broadcast -a com.mira.com.whisper.TEST"

sleep 2

# Test 3: Check if any services are running
echo "🔍 Test 3: Check running services..."
adb shell "dumpsys activity services | grep com.mira.com"
echo ""

# Test 4: Try to start a service directly
echo "📤 Test 4: Start service directly..."
adb shell "am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService"

sleep 3

# Check if service is running
echo "🔍 Check if service started..."
adb shell "dumpsys activity services | grep DirectWhisperService"
echo ""

# Check logs
echo "📊 Broadcast test logs:"
adb logcat -d | grep -E "(com.mira.com|Whisper|DirectWhisper)" | tail -10
echo ""

echo "✅ Micro Test 4 completed!"
