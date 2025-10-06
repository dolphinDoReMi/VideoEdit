#!/bin/bash

echo "=== CHECKING E2E TEST RESULTS ==="
echo ""

echo "=== STREAMING PROCESSING LOGS ==="
adb logcat -d | grep -E '(STREAMING|TECHNICAL.*streaming|TECHNICAL.*chunk|TECHNICAL.*Large file)' | tail -10

echo ""
echo "=== ERROR LOGS ==="
adb logcat -d | grep -E '(ERROR|libwhisper_jni)' | tail -5

echo ""
echo "=== BATCH PROCESSING LOGS ==="
adb logcat -d | grep -E '(Batch|Processing)' | tail -5

echo ""
echo "=== RECENT TECHNICAL LOGS ==="
adb logcat -d | grep 'TECHNICAL:' | tail -10

echo ""
echo "=== TEST ANALYSIS COMPLETE ==="
