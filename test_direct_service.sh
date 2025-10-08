#!/bin/bash

# Direct Service Test - Works with Existing App
# Tests the new DirectWhisperService implementation

set -e

echo "🎾 Direct Service Test - Tennis Interview Clip 002"
echo "================================================="
echo "Testing DirectWhisperService with existing app"
echo "Timestamp: $(date)"
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_002.mp4"
DEVICE_PATH="/sdcard/MiraWhisper/in/$CLIP_FILE"
MODEL="/sdcard/MiraWhisper/models/whisper-base.q5_1.bin"
THREADS=4
LANGUAGE="auto"

# Results directory
RESULTS_DIR="./direct_service_test_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "📁 Results will be saved to: $RESULTS_DIR"
echo ""

# Quick setup check
echo "=== Quick Setup Check ==="
adb devices | grep "device$" || { echo "❌ No device connected"; exit 1; }
echo "✅ Device connected"

# Check if file exists
if ! adb shell "test -f $DEVICE_PATH" 2>/dev/null; then
    echo "⚠️  File not found, pushing..."
    adb push "/Users/dennis/Movies/VideoEdit/tennis_clips/$CLIP_FILE" "$DEVICE_PATH"
    echo "✅ File pushed"
else
    echo "✅ File already on device"
fi

# Check model
if ! adb shell "test -f $MODEL" 2>/dev/null; then
    echo "❌ Model not found"
    exit 1
else
    echo "✅ Model found"
fi

echo ""

# Test 1: Direct Service Call
echo "=== Test 1: Direct Service Call ==="
echo "Clearing logs..."
adb logcat -c

echo "🚀 Launching app..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
sleep 3

echo "🎤 Testing DirectWhisperService..."
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$DEVICE_PATH" \
    --es "model" "$MODEL" \
    --es "threads" "$THREADS" \
    --es "lang" "$LANGUAGE" \
    --ez "translate" "false"

echo "✅ Direct service call sent"

# Wait and check logs
echo "⏳ Waiting 10 seconds for service to process..."
sleep 10

echo "📊 Checking service logs..."
adb logcat -d | grep -E "(DirectWhisperService|WhisperApi|TranscribeWorker)" | tail -10 > "$RESULTS_DIR/service_logs.txt"

if [ -s "$RESULTS_DIR/service_logs.txt" ]; then
    echo "✅ Service logs found:"
    cat "$RESULTS_DIR/service_logs.txt"
else
    echo "⚠️  No service logs found"
fi

echo ""

# Test 2: Direct Intent Call
echo "=== Test 2: Direct Intent Call ==="
echo "🎯 Testing direct intent approach..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es "direct_action" "transcribe" \
    --es "file_uri" "file://$DEVICE_PATH" \
    --es "model_path" "$MODEL" \
    --es "thread_count" "$THREADS" \
    --es "language" "$LANGUAGE"

echo "✅ Direct intent sent"

# Wait and check logs
echo "⏳ Waiting 10 seconds for processing..."
sleep 10

echo "📊 Checking intent logs..."
adb logcat -d | grep -E "(WhisperMain|DirectWhisperService|WhisperApi)" | tail -10 > "$RESULTS_DIR/intent_logs.txt"

if [ -s "$RESULTS_DIR/intent_logs.txt" ]; then
    echo "✅ Intent logs found:"
    cat "$RESULTS_DIR/intent_logs.txt"
else
    echo "⚠️  No intent logs found"
fi

echo ""

# Test 3: Check for Processing Results
echo "=== Test 3: Check Processing Results ==="
echo "🔍 Checking for output files..."

# Wait a bit more for processing
sleep 20

if adb shell "test -f /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
    echo "✅ Transcript found!"
    adb shell "cat /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" > "$RESULTS_DIR/transcript.srt"
    
    echo "📝 Transcript preview:"
    head -10 "$RESULTS_DIR/transcript.srt"
    echo "..."
else
    echo "❌ No transcript found"
fi

# Check for any new files
echo "📁 Checking for any new output files..."
adb shell "ls -la /sdcard/MiraWhisper/out/ | tail -5" > "$RESULTS_DIR/output_files.txt"
cat "$RESULTS_DIR/output_files.txt"

echo ""

# Test 4: Performance Check
echo "=== Test 4: Performance Check ==="
echo "📊 Current performance metrics:"
echo "CPU Usage:"
adb shell "dumpsys cpuinfo | grep com.mira.com | head -1"

echo "Memory Usage:"
adb shell "dumpsys meminfo com.mira.com | grep TOTAL"

echo ""

# Generate Report
echo "=== Generating Report ==="
REPORT_FILE="$RESULTS_DIR/direct_service_test_report.md"

cat > "$REPORT_FILE" << EOF
# Direct Service Test Report

**Test Date:** $(date)  
**File:** tennis_interview_clip_002.mp4  
**Model:** whisper-base.q5_1.bin  
**Approach:** DirectWhisperService (no broadcast/receiver)  

## Test Results

### Test 1: Direct Service Call
EOF

if [ -s "$RESULTS_DIR/service_logs.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **Service logs found**
\`\`\`
$(cat "$RESULTS_DIR/service_logs.txt")
\`\`\`
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **No service logs found**
- DirectWhisperService may not be implemented yet
- Service call may have failed
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Test 2: Direct Intent Call
EOF

if [ -s "$RESULTS_DIR/intent_logs.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **Intent logs found**
\`\`\`
$(cat "$RESULTS_DIR/intent_logs.txt")
\`\`\`
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **No intent logs found**
- Intent handling may not be implemented yet
- Activity may not be processing direct intents
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Test 3: Processing Results
EOF

if [ -f "$RESULTS_DIR/transcript.srt" ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **Transcript generated successfully**
- File: \`$RESULTS_DIR/transcript.srt\`
- Direct processing approach working
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **No transcript generated**
- Processing may not have started
- Check service and intent logs for errors
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Analysis

### Current Status
- **DirectWhisperService:** $(if [ -s "$RESULTS_DIR/service_logs.txt" ]; then echo "✅ Working"; else echo "❌ Not working"; fi)
- **Direct Intent Handling:** $(if [ -s "$RESULTS_DIR/intent_logs.txt" ]; then echo "✅ Working"; else echo "❌ Not working"; fi)
- **Processing Results:** $(if [ -f "$RESULTS_DIR/transcript.srt" ]; then echo "✅ Generated"; else echo "❌ Not generated"; fi)

### Next Steps
1. **If service logs found:** DirectWhisperService is working, check for processing completion
2. **If intent logs found:** Intent handling is working, check for service calls
3. **If transcript generated:** Direct approach is fully functional
4. **If nothing works:** Need to implement DirectWhisperService and intent handling

## Files Generated
- **Service Logs:** \`$RESULTS_DIR/service_logs.txt\`
- **Intent Logs:** \`$RESULTS_DIR/intent_logs.txt\`
- **Transcript:** \`$RESULTS_DIR/transcript.srt\`
- **Output Files:** \`$RESULTS_DIR/output_files.txt\`

---

*Report generated automatically*
EOF

echo "✅ Report generated: $REPORT_FILE"

echo ""
echo "=== Test Summary ==="
echo "📊 Results saved to: $RESULTS_DIR"
echo "📋 Report generated: $REPORT_FILE"
echo ""

if [ -f "$RESULTS_DIR/transcript.srt" ]; then
    echo "🎉 SUCCESS: Direct processing approach working!"
    echo "✅ Transcript generated successfully"
else
    echo "⚠️  Direct approach needs implementation"
    echo "📋 Check report for details: $REPORT_FILE"
fi

echo ""
echo "🎯 Direct service test completed!"
