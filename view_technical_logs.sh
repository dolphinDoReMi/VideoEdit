#!/bin/bash

echo "=== XIAOMI PAD TECHNICAL LOGS - CHUNKING & PROCESSING ANALYSIS ==="
echo ""

echo "=== STREAMING PROCESSING LOGS ==="
adb logcat -d | grep -E '(STREAMING|TECHNICAL.*streaming|TECHNICAL.*chunk|TECHNICAL.*Large file)' | tail -20

echo ""
echo "=== CHUNK PROCESSING LOGS ==="
adb logcat -d | grep -E '(STREAMING CHUNK|TECHNICAL.*Time range|TECHNICAL.*Chunk loaded|TECHNICAL.*Audio converted)' | tail -20

echo ""
echo "=== WHISPER PROCESSING LOGS ==="
adb logcat -d | grep -E '(TECHNICAL.*Whisper processing|TECHNICAL.*Overall progress|TECHNICAL.*Text extracted)' | tail -15

echo ""
echo "=== COMPLETION LOGS ==="
adb logcat -d | grep -E '(STREAMING.*COMPLETE|Streaming processing completed|TECHNICAL.*Final text)' | tail -10

echo ""
echo "=== ERROR LOGS ==="
adb logcat -d | grep -E '(ERROR|libwhisper_jni|Exception)' | tail -10

echo ""
echo "=== BATCH PROCESSING LOGS ==="
adb logcat -d | grep -E '(Batch.*completed|Processing.*complete|TECHNICAL.*Batch)' | tail -10

echo ""
echo "=== RECENT TECHNICAL LOGS ==="
adb logcat -d | grep 'TECHNICAL:' | tail -20

echo ""
echo "=== LOG ANALYSIS COMPLETE ==="
echo "Look for the streaming chunking and processing steps above."
