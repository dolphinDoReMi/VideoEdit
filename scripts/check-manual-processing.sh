#!/bin/bash

# Post-Manual Processing Check Script
# Run this after completing manual UI processing

set -e

echo "🔍 Post-Manual Processing Check"
echo "=============================="
echo "Timestamp: $(date)"
echo ""

# Results directory
RESULTS_DIR="./manual_processing_results_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "📁 Results will be saved to: $RESULTS_DIR"
echo ""

# Check if transcript was generated
echo "=== Checking for Transcript ==="
if adb shell "test -f /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" 2>/dev/null; then
    echo "✅ Transcript found!"
    adb shell "cat /sdcard/MiraWhisper/out/tennis_interview_clip_002.srt" > "$RESULTS_DIR/transcript.srt"
    
    echo "📝 Transcript preview:"
    head -10 "$RESULTS_DIR/transcript.srt"
    echo "..."
    
    TRANSCRIPT_SIZE=$(wc -c < "$RESULTS_DIR/transcript.srt")
    echo "📊 Transcript size: $TRANSCRIPT_SIZE bytes"
    MANUAL_SUCCESS=true
else
    echo "❌ No transcript found"
    MANUAL_SUCCESS=false
fi

echo ""

# Check for any new files
echo "=== Checking Output Directory ==="
echo "📁 Checking for any new output files..."
adb shell "ls -la /sdcard/MiraWhisper/out/ | tail -10" > "$RESULTS_DIR/output_files.txt"
cat "$RESULTS_DIR/output_files.txt"

echo ""

# Check processing logs
echo "=== Checking Processing Logs ==="
echo "📊 Checking recent processing logs..."
adb logcat -d | grep -E "(WhisperApi|TranscribeWorker|WhisperJNI|Processing)" | tail -20 > "$RESULTS_DIR/processing_logs.txt"

if [ -s "$RESULTS_DIR/processing_logs.txt" ]; then
    echo "✅ Processing logs found:"
    cat "$RESULTS_DIR/processing_logs.txt"
else
    echo "⚠️  No processing logs found"
fi

echo ""

# Check performance
echo "=== Checking Performance ==="
echo "📊 Current performance metrics:"
echo "CPU Usage:"
adb shell "dumpsys cpuinfo | grep com.mira.com | head -1" > "$RESULTS_DIR/cpu_usage.txt"
cat "$RESULTS_DIR/cpu_usage.txt"

echo "Memory Usage:"
adb shell "dumpsys meminfo com.mira.com | grep TOTAL" > "$RESULTS_DIR/memory_usage.txt"
cat "$RESULTS_DIR/memory_usage.txt"

echo ""

# Generate simple report
echo "=== Generating Report ==="
REPORT_FILE="$RESULTS_DIR/manual_processing_report.md"

cat > "$REPORT_FILE" << EOF
# Manual Processing Test Report

**Test Date:** $(date)  
**File:** tennis_interview_clip_002.mp4  
**Model:** whisper-base.q5_1.bin  
**Method:** Manual UI Processing  

## Results

### Transcript Generation
EOF

if [ "$MANUAL_SUCCESS" = true ]; then
    cat >> "$REPORT_FILE" << EOF
✅ **SUCCESS: Transcript generated**
- File: \`$RESULTS_DIR/transcript.srt\`
- Size: $(wc -c < "$RESULTS_DIR/transcript.srt") bytes
- Manual UI processing working correctly
EOF
else
    cat >> "$REPORT_FILE" << EOF
❌ **FAILED: No transcript generated**
- Manual UI processing did not complete successfully
- Check app logs for errors
EOF
fi

cat >> "$REPORT_FILE" << EOF

### Performance Metrics
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

### Processing Logs
EOF

if [ -s "$RESULTS_DIR/processing_logs.txt" ]; then
    cat >> "$REPORT_FILE" << EOF
**Recent Processing Activity:**
\`\`\`
$(cat "$RESULTS_DIR/processing_logs.txt")
\`\`\`
EOF
else
    cat >> "$REPORT_FILE" << EOF
**No processing logs found**
EOF
fi

cat >> "$REPORT_FILE" << EOF

## Summary

**Status:** $(if [ "$MANUAL_SUCCESS" = true ]; then echo "✅ SUCCESS"; else echo "❌ FAILED"; fi)

**Next Steps:**
1. **If successful:** Proceed with CPU vs Vulkan ablation testing
2. **If failed:** Debug manual UI processing issues
3. **For ablation:** Use manual UI method as baseline

## Files Generated
- **Transcript:** \`$RESULTS_DIR/transcript.srt\`
- **Processing Logs:** \`$RESULTS_DIR/processing_logs.txt\`
- **Output Files:** \`$RESULTS_DIR/output_files.txt\`
- **CPU Usage:** \`$RESULTS_DIR/cpu_usage.txt\`
- **Memory Usage:** \`$RESULTS_DIR/memory_usage.txt\`

---

*Report generated automatically*
EOF

echo "✅ Report generated: $REPORT_FILE"

echo ""
echo "=== Summary ==="
echo "📊 Results saved to: $RESULTS_DIR"
echo "📋 Report generated: $REPORT_FILE"
echo ""

if [ "$MANUAL_SUCCESS" = true ]; then
    echo "🎉 SUCCESS: Manual processing working!"
    echo "✅ Transcript generated successfully"
    echo "📝 Size: $(wc -c < "$RESULTS_DIR/transcript.srt") bytes"
    echo ""
    echo "🎯 Ready for ablation testing!"
else
    echo "❌ Manual processing failed"
    echo "📋 Check report for details: $REPORT_FILE"
fi

echo ""
echo "🎯 Manual processing check completed!"
