#!/bin/bash

echo "=== CHECKING E2E TEST RESULTS ==="
echo "Checking logs for streaming processing results..."
echo ""

# Check for streaming processing logs
echo "=== STREAMING PROCESSING LOGS ==="
adb logcat -d | grep -E '(STREAMING|TECHNICAL.*streaming|TECHNICAL.*chunk|TECHNICAL.*Large file)' | tail -20

echo ""
echo "=== ERROR LOGS ==="
adb logcat -d | grep -E '(ERROR|libwhisper_jni)' | tail -10

echo ""
echo "=== BATCH COMPLETION LOGS ==="
adb logcat -d | grep -E '(Batch.*completed|Processing.*complete)' | tail -10

echo ""
echo "=== RECENT TECHNICAL LOGS ==="
adb logcat -d | grep 'TECHNICAL:' | tail -15

echo ""
echo "=== TEST ANALYSIS COMPLETE ==="
