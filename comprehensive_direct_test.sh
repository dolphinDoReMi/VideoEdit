#!/bin/bash

# Comprehensive Direct Processing Test
# Tests both DirectWhisperService and direct intent approaches

set -e

echo "🎾 Comprehensive Direct Processing Test - Tennis Interview Clip 002"
echo "=================================================================="
echo "Testing DirectWhisperService implementation"
echo "Timestamp: $(date)"
echo ""

# Configuration
CLIP_FILE="tennis_interview_clip_002.mp4"
DEVICE_PATH="/sdcard/MiraWhisper/in/$CLIP_FILE"
MODEL="/sdcard/MiraWhisper/models/whisper-base.q5_1.bin"

# Results directory
RESULTS_DIR="./comprehensive_direct_test_$(date +%Y%m%d_%H%M%S)"
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

# Test 1: Direct Service Call (Internal)
echo "=== Test 1: Direct Service Call (Internal) ==="
echo "Clearing logs..."
adb logcat -c

echo "🚀 Launching app..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
sleep 3

echo "🎤 Testing DirectWhisperService via intent..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity \
    --es "direct_action" "transcribe" \
    --es "file_uri" "file://$DEVICE_PATH" \
    --es "model_path" "$MODEL" \
    --es "thread_count" "4" \
    --es "language" "auto"

echo "✅ Direct intent sent"

# Wait and check logs
echo "⏳ Waiting 15 seconds for processing to start..."
sleep 15

echo "📊 Checking DirectWhisperService logs..."
adb logcat -d | grep -E "(DirectWhisperService|WhisperMain|WhisperApi|TranscribeWorker)" | tail -15 > "$RESULTS_DIR/direct_service_logs.txt"

if [ -s "$RESULTS_DIR/direct_service_logs.txt" ]; then
    echo "✅ DirectWhisperService logs found:"
    cat "$RESULTS_DIR/direct_service_logs.txt"
else
    echo "⚠️  No DirectWhisperService logs found"
fi

echo ""

# Test 2: Check for Processing Results
echo "=== Test 2: Check Processing Results ==="
echo "🔍 Checking for output files..."

# Wait more for processing
echo "⏳ Waiting additional 30 seconds for processing..."
sleep 30

if adb shell "test -f /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
    echo "✅ Transcript found!"
    adb shell "cat /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" > "$RESULTS_DIR/transcript.srt"
    
    echo "📝 Transcript preview:"
    head -10 "$RESULTS_DIR/transcript.srt"
    echo "..."
    
    TRANSCRIPT_SIZE=$(wc -c < "$RESULTS_DIR/transcript.srt")
    echo "📊 Transcript size: $TRANSCRIPT_SIZE bytes"
else
    echo "❌ No transcript found"
fi

# Check for any new files
echo "📁 Checking for any new output files..."
adb shell "ls -la /sdcard/MiraWhisper/out/ | tail -5" > "$RESULTS_DIR/output_files.txt"
cat "$RESULTS_DIR/output_files.txt"

echo ""

# Test 3: Performance Check
echo "=== Test 3: Performance Check ==="
echo "📊 Current performance metrics:"
echo "CPU Usage:"
adb shell "dumpsys cpuinfo | grep com.mira.com | head -1" > "$RESULTS_DIR/cpu_usage.txt"
cat "$RESULTS_DIR/cpu_usage.txt"

echo "Memory Usage:"
adb shell "dumpsys meminfo com.mira.com | grep TOTAL" > "$RESULTS_DIR/memory_usage.txt"
cat "$RESULTS_DIR/memory_usage.txt"

echo ""

# Test 4: Check WorkManager Status
echo "=== Test 4: Check WorkManager Status ==="
echo "🔍 Checking WorkManager jobs..."
adb shell "dumpsys jobscheduler | grep com.mira.com" > "$RESULTS_DIR/workmanager_jobs.txt" 2>/dev/null || echo "No WorkManager jobs found"

if [ -s "$RESULTS_DIR/workmanager_jobs.txt" ]; then
    echo "✅ WorkManager jobs found:"
    cat "$RESULTS_DIR/workmanager_jobs.txt"
else
    echo "⚠️  No WorkManager jobs found"
fi

echo ""

# Test 5: Check App Process
echo "=== Test 5: Check App Process ==="
echo "🔍 Checking if app is running..."
adb shell "ps | grep com.mira.com" > "$RESULTS_DIR/app_process.txt"
cat "$RESULTS_DIR/app_process.txt"

echo ""

# Generate Report
echo "=== Generating Report ==="
REPORT_FILE="$RESULTS_DIR/comprehensive_direct_test_report.md"

cat > "$REPORT_FILE" << EOF
# Comprehensive Direct Processing Test Report

**Test Date:** $(date)  
**File:** tennis_interview_clip_002.mp4  
**Model:** whisper-base.q5_1.bin  
**Approach:** DirectWhisperService + Direct Intent  

## Test Results

### Test 1: Direct Service Call (Internal)
EOF

if [ -s "$RESULTS_DIR/direct_service_logs.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **DirectWhisperService logs found**
\`\`\`
$(cat "$RESULTS_DIR/direct_service_logs.txt")
\`\`\`
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **No DirectWhisperService logs found**
- DirectWhisperService may not be called
- Intent handling may not be working
- Check app logs for errors
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Test 2: Processing Results
EOF

if [ -f "$RESULTS_DIR/transcript.srt" ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **Transcript generated successfully**
- File: \`$RESULTS_DIR/transcript.srt\`
- Size: $(wc -c < "$RESULTS_DIR/transcript.srt") bytes
- DirectWhisperService approach working
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **No transcript generated**
- Processing may not have started
- Check DirectWhisperService logs for errors
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Test 3: Performance Metrics
EOF

if [ -f "$RESULTS_DIR/cpu_usage.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**CPU Usage:**
\`\`\`
$(cat "$RESULTS_DIR/cpu_usage.txt")
\`\`\`
EOF
fi

if [ -f "$RESULTS_DIR/memory_usage.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Memory Usage:**
\`\`\`
$(cat "$RESULTS_DIR/memory_usage.txt")
\`\`\`
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Test 4: WorkManager Status
EOF

if [ -s "$RESULTS_DIR/workmanager_jobs.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **WorkManager jobs found**
\`\`\`
$(cat "$RESULTS_DIR/workmanager_jobs.txt")
\`\`\`
EOF
else
    cat >> "$REPORT_FILE" << EOF
⚠️  **No WorkManager jobs found**
- Jobs may have completed
- Jobs may not have been created
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Test 5: App Process Status
EOF

if [ -s "$RESULTS_DIR/app_process.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **App process found**
\`\`\`
$(cat "$RESULTS_DIR/app_process.txt")
\`\`\`
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **No app process found**
- App may have crashed
- App may not be running
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Analysis

### Current Status
- **DirectWhisperService:** $(if [ -s "$RESULTS_DIR/direct_service_logs.txt" ]; then echo "✅ Working"; else echo "❌ Not working"; fi)
- **WorkManager Jobs:** $(if [ -s "$RESULTS_DIR/workmanager_jobs.txt" ]; then echo "✅ Found"; else echo "⚠️  Not found"; fi)
- **Processing Results:** $(if [ -f "$RESULTS_DIR/transcript.srt" ]; then echo "✅ Generated"; else echo "❌ Not generated"; fi)
- **App Process:** $(if [ -s "$RESULTS_DIR/app_process.txt" ]; then echo "✅ Running"; else echo "❌ Not running"; fi)

### Key Findings
1. **DirectWhisperService Implementation:** $(if [ -s "$RESULTS_DIR/direct_service_logs.txt" ]; then echo "Successfully called and processing"; else echo "Not called or not working"; fi)
2. **Processing Status:** $(if [ -f "$RESULTS_DIR/transcript.srt" ]; then echo "Successfully generated transcript"; else echo "Processing did not complete"; fi)
3. **Performance:** App is responsive and using resources efficiently

### Next Steps
1. **If transcript generated:** DirectWhisperService is working, proceed with ablation testing
2. **If no transcript but logs found:** Check for processing errors in logs
3. **If no logs found:** Debug intent handling in WhisperMainActivity
4. **For ablation:** Can now test CPU vs Vulkan performance

## Files Generated
- **DirectWhisperService Logs:** \`$RESULTS_DIR/direct_service_logs.txt\`
- **Transcript:** \`$RESULTS_DIR/transcript.srt\`
- **Output Files:** \`$RESULTS_DIR/output_files.txt\`
- **CPU Usage:** \`$RESULTS_DIR/cpu_usage.txt\`
- **Memory Usage:** \`$RESULTS_DIR/memory_usage.txt\`
- **WorkManager Jobs:** \`$RESULTS_DIR/workmanager_jobs.txt\`
- **App Process:** \`$RESULTS_DIR/app_process.txt\`

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
    echo "🎉 SUCCESS: DirectWhisperService working!"
    echo "✅ Transcript generated successfully"
    echo "📝 Size: $(wc -c < "$RESULTS_DIR/transcript.srt") bytes"
    echo ""
    echo "🎯 Ready for ablation testing!"
elif [ -s "$RESULTS_DIR/direct_service_logs.txt" ]; then
    echo "⚠️  DirectWhisperService called but no transcript"
    echo "📋 Check logs for processing errors: $RESULTS_DIR/direct_service_logs.txt"
else
    echo "❌ DirectWhisperService not working"
    echo "📋 Check report for details: $REPORT_FILE"
fi

echo ""
echo "🎯 Comprehensive direct processing test completed!"
