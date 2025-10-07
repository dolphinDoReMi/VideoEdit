#!/bin/bash

echo "=== TESTING NAVIGATION FIX ==="
echo ""
echo "This script will help you test if the navigation issue is fixed."
echo ""

echo "1. Launch the app:"
echo "   adb shell am start -n com.mira.com/.whisper.WhisperMainActivity"
echo ""

echo "2. Test the processing flow:"
echo "   - Select a video file"
echo "   - Click 'Start Processing'"
echo "   - Verify the app STAYS on the processing screen"
echo "   - Check that it doesn't navigate back to file selection"
echo ""

echo "3. Monitor the logs for debugging:"
echo "   adb logcat -d | grep -E '(UNIFIED|monitorBatch|showStep)' | tail -20"
echo ""

echo "=== EXPECTED BEHAVIOR ==="
echo "✅ App should stay on processing screen"
echo "✅ No automatic navigation back to file selection"
echo "✅ Progress updates should work normally"
echo "✅ Processing should complete and show results"
echo ""

echo "=== IF ISSUE PERSISTS ==="
echo "Run this command to see detailed logs:"
echo "   adb logcat -d | grep -E '(UNIFIED|monitorBatch|showStep|getBatchInfo)' | tail -30"
echo ""

echo "=== LAUNCHING APP FOR TESTING ==="
adb shell am start -n com.mira.com/.whisper.WhisperMainActivity

echo ""
echo "App launched! Please test the processing flow and report results."
