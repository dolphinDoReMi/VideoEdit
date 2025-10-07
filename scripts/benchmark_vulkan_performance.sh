#!/bin/bash

# Vulkan Performance Benchmark Script
# This script compares CPU vs Vulkan performance on Xiaomi Pad 7 Ultra

set -e

echo "=== Vulkan Performance Benchmark ==="
echo "Timestamp: $(date)"
echo

PACKAGE_NAME="com.mira.com.debug"
TEST_AUDIO_FILE="/sdcard/test_audio.wav"
RESULTS_DIR="vulkan_benchmark_results"

# Create results directory
mkdir -p "$RESULTS_DIR"

# Function to create test audio file
create_test_audio() {
    echo "1. Creating test audio file..."
    
    if ! adb shell test -f "$TEST_AUDIO_FILE"; then
        # Create a 10-second test audio file (silence with some noise)
        adb shell "dd if=/dev/urandom of=$TEST_AUDIO_FILE bs=1024 count=100 2>/dev/null"
        echo "✅ Test audio file created: $TEST_AUDIO_FILE"
    else
        echo "✅ Test audio file exists: $TEST_AUDIO_FILE"
    fi
}

# Function to run CPU-only test
run_cpu_test() {
    echo "2. Running CPU-only test..."
    
    # Disable Vulkan
    adb shell setenv GGML_VULKAN_DEVICE ""
    adb shell setenv GGML_VULKAN_DEBUG "0"
    
    # Clear logs
    adb logcat -c
    
    # Launch app
    adb shell am start -n "$PACKAGE_NAME/com.mira.whisper.WhisperMainActivity"
    sleep 3
    
    # Start transcription
    echo "   Starting CPU transcription..."
    START_TIME=$(date +%s)
    adb shell am broadcast -a "$PACKAGE_NAME.whisper.RUN" --es "file_path" "$TEST_AUDIO_FILE"
    
    # Monitor performance
    CPU_METRICS=()
    MEMORY_METRICS=()
    
    for i in {1..60}; do
        CPU_USAGE=$(adb shell dumpsys cpuinfo | grep "$PACKAGE_NAME" | awk '{print $1}' | head -1 || echo "0")
        MEMORY_USAGE=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}' || echo "0")
        
        CPU_METRICS+=("$CPU_USAGE")
        MEMORY_METRICS+=("$MEMORY_USAGE")
        
        if [ $((i % 10)) -eq 0 ]; then
            echo "   CPU Test - Time ${i}s: CPU=${CPU_USAGE}%, Memory=${MEMORY_USAGE}KB"
        fi
        
        sleep 1
    done
    
    END_TIME=$(date +%s)
    CPU_DURATION=$((END_TIME - START_TIME))
    
    # Calculate averages
    CPU_AVG=$(printf '%s\n' "${CPU_METRICS[@]}" | awk '{sum+=$1} END {print sum/NR}')
    MEMORY_AVG=$(printf '%s\n' "${MEMORY_METRICS[@]}" | awk '{sum+=$1} END {print sum/NR}')
    
    # Get CPU logs
    CPU_LOGS=$(adb logcat -d | grep -i "ggml.*cpu\|whisper.*cpu" | wc -l)
    
    echo "✅ CPU test completed"
    echo "   Duration: ${CPU_DURATION}s"
    echo "   Average CPU: ${CPU_AVG}%"
    echo "   Average Memory: ${MEMORY_AVG}KB"
    echo "   CPU logs: $CPU_LOGS"
    
    # Save CPU results
    {
        echo "CPU Test Results"
        echo "================"
        echo "Duration: ${CPU_DURATION}s"
        echo "Average CPU Usage: ${CPU_AVG}%"
        echo "Average Memory Usage: ${MEMORY_AVG}KB"
        echo "CPU Log Count: $CPU_LOGS"
        echo
        echo "CPU Usage Over Time:"
        printf '%s\n' "${CPU_METRICS[@]}"
        echo
        echo "Memory Usage Over Time:"
        printf '%s\n' "${MEMORY_METRICS[@]}"
    } > "$RESULTS_DIR/cpu_test_results.txt"
    
    # Store results for comparison
    CPU_AVG_RESULT="$CPU_AVG"
    MEMORY_AVG_RESULT="$MEMORY_AVG"
    CPU_DURATION_RESULT="$CPU_DURATION"
}

# Function to run Vulkan test
run_vulkan_test() {
    echo "3. Running Vulkan test..."
    
    # Enable Vulkan
    adb shell setenv GGML_VULKAN_DEVICE "0"
    adb shell setenv GGML_VULKAN_DEBUG "1"
    
    # Clear logs
    adb logcat -c
    
    # Launch app
    adb shell am start -n "$PACKAGE_NAME/com.mira.whisper.WhisperMainActivity"
    sleep 3
    
    # Start transcription
    echo "   Starting Vulkan transcription..."
    START_TIME=$(date +%s)
    adb shell am broadcast -a "$PACKAGE_NAME.whisper.RUN" --es "file_path" "$TEST_AUDIO_FILE"
    
    # Monitor performance
    VULKAN_CPU_METRICS=()
    VULKAN_MEMORY_METRICS=()
    
    for i in {1..60}; do
        CPU_USAGE=$(adb shell dumpsys cpuinfo | grep "$PACKAGE_NAME" | awk '{print $1}' | head -1 || echo "0")
        MEMORY_USAGE=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}' || echo "0")
        
        VULKAN_CPU_METRICS+=("$CPU_USAGE")
        VULKAN_MEMORY_METRICS+=("$MEMORY_USAGE")
        
        if [ $((i % 10)) -eq 0 ]; then
            echo "   Vulkan Test - Time ${i}s: CPU=${CPU_USAGE}%, Memory=${MEMORY_USAGE}KB"
        fi
        
        sleep 1
    done
    
    END_TIME=$(date +%s)
    VULKAN_DURATION=$((END_TIME - START_TIME))
    
    # Calculate averages
    VULKAN_CPU_AVG=$(printf '%s\n' "${VULKAN_CPU_METRICS[@]}" | awk '{sum+=$1} END {print sum/NR}')
    VULKAN_MEMORY_AVG=$(printf '%s\n' "${VULKAN_MEMORY_METRICS[@]}" | awk '{sum+=$1} END {print sum/NR}')
    
    # Get Vulkan logs
    VULKAN_LOGS=$(adb logcat -d | grep -i "ggml.*vulkan\|vulkan.*pipeline" | wc -l)
    
    echo "✅ Vulkan test completed"
    echo "   Duration: ${VULKAN_DURATION}s"
    echo "   Average CPU: ${VULKAN_CPU_AVG}%"
    echo "   Average Memory: ${VULKAN_MEMORY_AVG}KB"
    echo "   Vulkan logs: $VULKAN_LOGS"
    
    # Save Vulkan results
    {
        echo "Vulkan Test Results"
        echo "=================="
        echo "Duration: ${VULKAN_DURATION}s"
        echo "Average CPU Usage: ${VULKAN_CPU_AVG}%"
        echo "Average Memory Usage: ${VULKAN_MEMORY_AVG}KB"
        echo "Vulkan Log Count: $VULKAN_LOGS"
        echo
        echo "CPU Usage Over Time:"
        printf '%s\n' "${VULKAN_CPU_METRICS[@]}"
        echo
        echo "Memory Usage Over Time:"
        printf '%s\n' "${VULKAN_MEMORY_METRICS[@]}"
    } > "$RESULTS_DIR/vulkan_test_results.txt"
    
    # Store results for comparison
    VULKAN_CPU_AVG_RESULT="$VULKAN_CPU_AVG"
    VULKAN_MEMORY_AVG_RESULT="$VULKAN_MEMORY_AVG"
    VULKAN_DURATION_RESULT="$VULKAN_DURATION"
}

# Function to compare results
compare_results() {
    echo "4. Comparing results..."
    
    # Calculate performance improvements
    CPU_IMPROVEMENT=$(echo "scale=2; ($CPU_AVG_RESULT - $VULKAN_CPU_AVG_RESULT) / $CPU_AVG_RESULT * 100" | bc -l 2>/dev/null || echo "0")
    MEMORY_IMPROVEMENT=$(echo "scale=2; ($MEMORY_AVG_RESULT - $VULKAN_MEMORY_AVG_RESULT) / $MEMORY_AVG_RESULT * 100" | bc -l 2>/dev/null || echo "0")
    SPEED_IMPROVEMENT=$(echo "scale=2; ($CPU_DURATION_RESULT - $VULKAN_DURATION_RESULT) / $CPU_DURATION_RESULT * 100" | bc -l 2>/dev/null || echo "0")
    
    echo "=== Performance Comparison ==="
    echo "CPU Usage:"
    echo "  CPU-only:  ${CPU_AVG_RESULT}%"
    echo "  Vulkan:    ${VULKAN_CPU_AVG_RESULT}%"
    echo "  Improvement: ${CPU_IMPROVEMENT}%"
    echo
    echo "Memory Usage:"
    echo "  CPU-only:  ${MEMORY_AVG_RESULT}KB"
    echo "  Vulkan:    ${VULKAN_MEMORY_AVG_RESULT}KB"
    echo "  Improvement: ${MEMORY_IMPROVEMENT}%"
    echo
    echo "Processing Time:"
    echo "  CPU-only:  ${CPU_DURATION_RESULT}s"
    echo "  Vulkan:    ${VULKAN_DURATION_RESULT}s"
    echo "  Improvement: ${SPEED_IMPROVEMENT}%"
    
    # Save comparison results
    {
        echo "Vulkan Performance Benchmark Results"
        echo "===================================="
        echo "Timestamp: $(date)"
        echo "Device: $(adb shell getprop ro.product.model)"
        echo
        echo "CPU Usage Comparison:"
        echo "  CPU-only:  ${CPU_AVG_RESULT}%"
        echo "  Vulkan:    ${VULKAN_CPU_AVG_RESULT}%"
        echo "  Improvement: ${CPU_IMPROVEMENT}%"
        echo
        echo "Memory Usage Comparison:"
        echo "  CPU-only:  ${MEMORY_AVG_RESULT}KB"
        echo "  Vulkan:    ${VULKAN_MEMORY_AVG_RESULT}KB"
        echo "  Improvement: ${MEMORY_IMPROVEMENT}%"
        echo
        echo "Processing Time Comparison:"
        echo "  CPU-only:  ${CPU_DURATION_RESULT}s"
        echo "  Vulkan:    ${VULKAN_DURATION_RESULT}s"
        echo "  Improvement: ${SPEED_IMPROVEMENT}%"
        echo
        echo "Summary:"
        if (( $(echo "$CPU_IMPROVEMENT > 0" | bc -l) )); then
            echo "✅ Vulkan reduces CPU usage by ${CPU_IMPROVEMENT}%"
        else
            echo "❌ Vulkan increases CPU usage by ${CPU_IMPROVEMENT}%"
        fi
        
        if (( $(echo "$MEMORY_IMPROVEMENT > 0" | bc -l) )); then
            echo "✅ Vulkan reduces memory usage by ${MEMORY_IMPROVEMENT}%"
        else
            echo "❌ Vulkan increases memory usage by ${MEMORY_IMPROVEMENT}%"
        fi
        
        if (( $(echo "$SPEED_IMPROVEMENT > 0" | bc -l) )); then
            echo "✅ Vulkan improves processing speed by ${SPEED_IMPROVEMENT}%"
        else
            echo "❌ Vulkan reduces processing speed by ${SPEED_IMPROVEMENT}%"
        fi
    } > "$RESULTS_DIR/benchmark_comparison.txt"
    
    echo
    echo "✅ Comparison results saved to: $RESULTS_DIR/benchmark_comparison.txt"
}

# Function to generate HTML report
generate_html_report() {
    echo "5. Generating HTML report..."
    
    HTML_FILE="$RESULTS_DIR/vulkan_benchmark_report.html"
    
    cat > "$HTML_FILE" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Vulkan Performance Benchmark Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .metric { display: inline-block; margin: 10px; padding: 10px; background-color: #e8f4f8; border-radius: 3px; }
        .improvement { color: green; font-weight: bold; }
        .degradation { color: red; font-weight: bold; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Vulkan Performance Benchmark Report</h1>
        <p>Generated: $(date)</p>
        <p>Device: $(adb shell getprop ro.product.model)</p>
    </div>
    
    <div class="section">
        <h2>Performance Summary</h2>
        <div class="metric">
            <strong>CPU Usage Improvement:</strong>
            <span class="$(if (( $(echo "$CPU_IMPROVEMENT > 0" | bc -l) )); then echo "improvement"; else echo "degradation"; fi)">${CPU_IMPROVEMENT}%</span>
        </div>
        <div class="metric">
            <strong>Memory Usage Improvement:</strong>
            <span class="$(if (( $(echo "$MEMORY_IMPROVEMENT > 0" | bc -l) )); then echo "improvement"; else echo "degradation"; fi)">${MEMORY_IMPROVEMENT}%</span>
        </div>
        <div class="metric">
            <strong>Speed Improvement:</strong>
            <span class="$(if (( $(echo "$SPEED_IMPROVEMENT > 0" | bc -l) )); then echo "improvement"; else echo "degradation"; fi)">${SPEED_IMPROVEMENT}%</span>
        </div>
    </div>
    
    <div class="section">
        <h2>Detailed Comparison</h2>
        <table>
            <tr>
                <th>Metric</th>
                <th>CPU-only</th>
                <th>Vulkan</th>
                <th>Improvement</th>
            </tr>
            <tr>
                <td>Average CPU Usage</td>
                <td>${CPU_AVG_RESULT}%</td>
                <td>${VULKAN_CPU_AVG_RESULT}%</td>
                <td class="$(if (( $(echo "$CPU_IMPROVEMENT > 0" | bc -l) )); then echo "improvement"; else echo "degradation"; fi)">${CPU_IMPROVEMENT}%</td>
            </tr>
            <tr>
                <td>Average Memory Usage</td>
                <td>${MEMORY_AVG_RESULT}KB</td>
                <td>${VULKAN_MEMORY_AVG_RESULT}KB</td>
                <td class="$(if (( $(echo "$MEMORY_IMPROVEMENT > 0" | bc -l) )); then echo "improvement"; else echo "degradation"; fi)">${MEMORY_IMPROVEMENT}%</td>
            </tr>
            <tr>
                <td>Processing Time</td>
                <td>${CPU_DURATION_RESULT}s</td>
                <td>${VULKAN_DURATION_RESULT}s</td>
                <td class="$(if (( $(echo "$SPEED_IMPROVEMENT > 0" | bc -l) )); then echo "improvement"; else echo "degradation"; fi)">${SPEED_IMPROVEMENT}%</td>
            </tr>
        </table>
    </div>
    
    <div class="section">
        <h2>Recommendations</h2>
        <ul>
EOF

    if (( $(echo "$CPU_IMPROVEMENT > 10" | bc -l) )); then
        echo "            <li>✅ Significant CPU improvement detected. Vulkan is working effectively.</li>" >> "$HTML_FILE"
    elif (( $(echo "$CPU_IMPROVEMENT > 0" | bc -l) )); then
        echo "            <li>⚠️ Minor CPU improvement. Consider optimizing Vulkan configuration.</li>" >> "$HTML_FILE"
    else
        echo "            <li>❌ No CPU improvement. Check Vulkan implementation and device compatibility.</li>" >> "$HTML_FILE"
    fi
    
    if (( $(echo "$SPEED_IMPROVEMENT > 5" | bc -l) )); then
        echo "            <li>✅ Processing speed improved. Vulkan acceleration is beneficial.</li>" >> "$HTML_FILE"
    elif (( $(echo "$SPEED_IMPROVEMENT > 0" | bc -l) )); then
        echo "            <li>⚠️ Minor speed improvement. Consider model optimization.</li>" >> "$HTML_FILE"
    else
        echo "            <li>❌ No speed improvement. Check for Vulkan fallback to CPU.</li>" >> "$HTML_FILE"
    fi
    
    cat >> "$HTML_FILE" << EOF
        </ul>
    </div>
</body>
</html>
EOF
    
    echo "✅ HTML report generated: $HTML_FILE"
}

# Main execution
main() {
    echo "Starting Vulkan performance benchmark..."
    echo
    
    create_test_audio
    echo
    
    run_cpu_test
    echo
    
    run_vulkan_test
    echo
    
    compare_results
    echo
    
    generate_html_report
    echo
    
    echo "=== Benchmark Complete ==="
    echo "Results saved in: $RESULTS_DIR/"
    echo "Open report: $RESULTS_DIR/vulkan_benchmark_report.html"
    echo "Timestamp: $(date)"
}

# Run main function
main "$@"
