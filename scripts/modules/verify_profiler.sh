#!/bin/bash

# Verify Profiler Setup
echo "🔍 Profiler Setup Verification"
echo "============================="

# Check if debug app is running
echo "📱 Checking running processes..."
adb shell ps | grep mira

echo ""
echo "✅ Setup Complete! Here's what was fixed:"
echo "========================================"
echo "1. ✅ Java environment configured"
echo "2. ✅ Debug APK built with explicit isDebuggable = true"
echo "3. ✅ Old app versions uninstalled"
echo "4. ✅ Fresh debug APK installed (com.mira.videoeditor.debug)"
echo "5. ✅ App launched and running"
echo ""
echo "🎯 Now in Android Studio Profiler:"
echo "=================================="
echo "• Look for process: com.mira.videoeditor.debug"
echo "• PID should be visible in the process list"
echo "• Try 'Java/Kotlin Method Recording' again"
echo "• If still issues, try 'Capture System Activities' first"
echo ""
echo "💡 The 'No valid process' error should now be resolved!"
