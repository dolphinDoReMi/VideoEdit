#!/bin/bash

# Comprehensive Ablation Test Analysis Script
# Analyzes results from redesigned DirectWhisperService ablation test
# Generates detailed performance analysis and recommendations

set -e

echo "📊 Comprehensive Ablation Test Analysis"
echo "======================================="
echo "Analyzing DirectWhisperService ablation test results"
echo "Timestamp: $(date)"
echo ""

# Configuration
RESULTS_DIR="$1"
if [ -z "$RESULTS_DIR" ]; then
    echo "Usage: $0 <results_directory>"
    echo "Example: $0 ./redesigned_ablation_20250108_120000"
    exit 1
fi

if [ ! -d "$RESULTS_DIR" ]; then
    echo "❌ Results directory not found: $RESULTS_DIR"
    exit 1
fi

echo "📁 Analyzing results from: $RESULTS_DIR"
echo ""

# Create analysis directory
ANALYSIS_DIR="$RESULTS_DIR/analysis"
mkdir -p "$ANALYSIS_DIR"

# Function to analyze CPU monitoring data
analyze_cpu_data() {
    local test_dir="$1"
    local test_name="$2"
    local cpu_file="$test_dir/cpu_monitoring.txt"
    
    if [ ! -f "$cpu_file" ]; then
        echo "⚠️  No CPU monitoring data found for $test_name"
        return
    fi
    
    echo "=== CPU Analysis for $test_name ==="
    
    # Extract CPU usage percentages
    local cpu_values=$(grep -o '[0-9]*%' "$cpu_file" | sed 's/%//' | sort -n)
    local cpu_count=$(echo "$cpu_values" | wc -l)
    
    if [ "$cpu_count" -gt 0 ]; then
        local cpu_min=$(echo "$cpu_values" | head -1)
        local cpu_max=$(echo "$cpu_values" | tail -1)
        local cpu_avg=$(echo "$cpu_values" | awk '{sum+=$1} END {print sum/NR}')
        
        echo "CPU Usage Statistics:"
        echo "  Min: ${cpu_min}%"
        echo "  Max: ${cpu_max}%"
        echo "  Avg: $(printf "%.1f" "$cpu_avg")%"
        echo "  Samples: $cpu_count"
        
        # Save to analysis file
        cat > "$ANALYSIS_DIR/${test_name}_cpu_analysis.txt" << EOF
CPU Usage Analysis for $test_name
================================
Min: ${cpu_min}%
Max: ${cpu_max}%
Avg: $(printf "%.1f" "$cpu_avg")%
Samples: $cpu_count

Raw CPU Values:
$cpu_values
EOF
    fi
    
    echo ""
}

# Function to analyze memory monitoring data
analyze_memory_data() {
    local test_dir="$1"
    local test_name="$2"
    local memory_file="$test_dir/memory_monitoring.txt"
    
    if [ ! -f "$memory_file" ]; then
        echo "⚠️  No memory monitoring data found for $test_name"
        return
    fi
    
    echo "=== Memory Analysis for $test_name ==="
    
    # Extract PSS values (Proportional Set Size)
    local pss_values=$(grep "TOTAL PSS:" "$memory_file" | awk '{print $3}' | sort -n)
    local memory_count=$(echo "$pss_values" | wc -l)
    
    if [ "$memory_count" -gt 0 ]; then
        local memory_min=$(echo "$pss_values" | head -1)
        local memory_max=$(echo "$pss_values" | tail -1)
        local memory_avg=$(echo "$pss_values" | awk '{sum+=$1} END {print sum/NR}')
        
        # Convert to MB
        local memory_min_mb=$((memory_min / 1024))
        local memory_max_mb=$((memory_max / 1024))
        local memory_avg_mb=$((memory_avg / 1024))
        
        echo "Memory Usage Statistics (PSS):"
        echo "  Min: ${memory_min_mb}MB"
        echo "  Max: ${memory_max_mb}MB"
        echo "  Avg: ${memory_avg_mb}MB"
        echo "  Samples: $memory_count"
        
        # Save to analysis file
        cat > "$ANALYSIS_DIR/${test_name}_memory_analysis.txt" << EOF
Memory Usage Analysis for $test_name
====================================
Min: ${memory_min_mb}MB (${memory_min}KB)
Max: ${memory_max_mb}MB (${memory_max}KB)
Avg: ${memory_avg_mb}MB ($(printf "%.0f" "$memory_avg")KB)
Samples: $memory_count

Raw PSS Values (KB):
$pss_values
EOF
    fi
    
    echo ""
}

# Function to analyze TranscribeWorker logs
analyze_transcribe_logs() {
    local test_dir="$1"
    local test_name="$2"
    
    echo "=== TranscribeWorker Log Analysis for $test_name ==="
    
    # Find all transcribe log files
    local log_files=$(find "$test_dir" -name "transcribe_logs_*.txt" | sort)
    local log_count=$(echo "$log_files" | wc -l)
    
    if [ "$log_count" -eq 0 ]; then
        echo "⚠️  No TranscribeWorker log files found for $test_name"
        return
    fi
    
    echo "Found $log_count TranscribeWorker log files"
    
    # Analyze processing stages
    local stages=("Storage self-test" "Audio loaded" "LID" "Whisper" "Processing" "Completed" "Failed")
    local stage_counts=""
    
    for stage in "${stages[@]}"; do
        local count=$(grep -c "$stage" $log_files 2>/dev/null || echo "0")
        stage_counts="$stage_counts$stage:$count "
    done
    
    echo "Processing Stage Occurrences:"
    echo "$stage_counts" | tr ' ' '\n' | while read stage_count; do
        if [ -n "$stage_count" ]; then
            echo "  $stage_count"
        fi
    done
    
    # Check for errors
    local error_count=$(grep -c "Failed\|Error\|Exception" $log_files 2>/dev/null || echo "0")
    echo "Error Count: $error_count"
    
    # Save to analysis file
    cat > "$ANALYSIS_DIR/${test_name}_transcribe_analysis.txt" << EOF
TranscribeWorker Log Analysis for $test_name
===========================================
Log Files: $log_count
Processing Stage Occurrences:
$stage_counts

Error Count: $error_count

Detailed Log Analysis:
EOF
    
    # Add detailed log analysis
    for log_file in $log_files; do
        echo "" >> "$ANALYSIS_DIR/${test_name}_transcribe_analysis.txt"
        echo "=== $(basename "$log_file") ===" >> "$ANALYSIS_DIR/${test_name}_transcribe_analysis.txt"
        cat "$log_file" >> "$ANALYSIS_DIR/${test_name}_transcribe_analysis.txt"
    done
    
    echo ""
}

# Function to generate performance comparison
generate_performance_comparison() {
    local cpu_dir="$RESULTS_DIR/cpu_only"
    local vulkan_dir="$RESULTS_DIR/vulkan_enabled"
    
    echo "=== Performance Comparison Analysis ==="
    
    # Check if both tests completed
    local cpu_duration=""
    local vulkan_duration=""
    local cpu_status=""
    local vulkan_status=""
    
    if [ -f "$cpu_dir/duration.txt" ]; then
        cpu_duration=$(cat "$cpu_dir/duration.txt")
        cpu_status=$(cat "$cpu_dir/status.txt" 2>/dev/null || echo "UNKNOWN")
    fi
    
    if [ -f "$vulkan_dir/duration.txt" ]; then
        vulkan_duration=$(cat "$vulkan_dir/duration.txt")
        vulkan_status=$(cat "$vulkan_dir/status.txt" 2>/dev/null || echo "UNKNOWN")
    fi
    
    if [ -n "$cpu_duration" ] && [ -n "$vulkan_duration" ]; then
        echo "Processing Time Comparison:"
        echo "  CPU Only: ${cpu_duration} seconds ($cpu_status)"
        echo "  Vulkan Enabled: ${vulkan_duration} seconds ($vulkan_status)"
        
        if [ "$vulkan_duration" -lt "$cpu_duration" ]; then
            local speedup=$(echo "scale=2; $cpu_duration / $vulkan_duration" | bc)
            echo "  Speedup: ${speedup}x faster with Vulkan"
        else
            local slowdown=$(echo "scale=2; $vulkan_duration / $cpu_duration" | bc)
            echo "  Slowdown: ${slowdown}x slower with Vulkan"
        fi
        
        # Calculate efficiency metrics
        local cpu_efficiency=$(echo "scale=2; $cpu_duration / 60" | bc)  # seconds per minute of audio
        local vulkan_efficiency=$(echo "scale=2; $vulkan_duration / 60" | bc)
        
        echo "  CPU Efficiency: ${cpu_efficiency}s per minute of audio"
        echo "  Vulkan Efficiency: ${vulkan_efficiency}s per minute of audio"
        
    else
        echo "⚠️  Cannot compare performance - missing duration data"
        echo "  CPU Duration: ${cpu_duration:-"N/A"}"
        echo "  Vulkan Duration: ${vulkan_duration:-"N/A"}"
    fi
    
    echo ""
}

# Function to generate recommendations
generate_recommendations() {
    local cpu_dir="$RESULTS_DIR/cpu_only"
    local vulkan_dir="$RESULTS_DIR/vulkan_enabled"
    
    echo "=== Recommendations ==="
    
    # Check test results
    local cpu_success=false
    local vulkan_success=false
    
    if [ -f "$cpu_dir/status.txt" ] && [ "$(cat "$cpu_dir/status.txt")" = "SUCCESS" ]; then
        cpu_success=true
    fi
    
    if [ -f "$vulkan_dir/status.txt" ] && [ "$(cat "$vulkan_dir/status.txt")" = "SUCCESS" ]; then
        vulkan_success=true
    fi
    
    echo "Test Results:"
    echo "  CPU Only: $(if $cpu_success; then echo "✅ SUCCESS"; else echo "❌ FAILED"; fi)"
    echo "  Vulkan Enabled: $(if $vulkan_success; then echo "✅ SUCCESS"; else echo "❌ FAILED"; fi)"
    echo ""
    
    echo "Production Recommendations:"
    
    if $cpu_success && $vulkan_success; then
        local cpu_duration=$(cat "$cpu_dir/duration.txt")
        local vulkan_duration=$(cat "$vulkan_dir/duration.txt")
        
        if [ "$vulkan_duration" -lt "$cpu_duration" ]; then
            echo "  🚀 Use Vulkan acceleration for better performance"
            echo "  📱 Ensure device supports FP16 and 16-bit storage"
            echo "  🔧 Monitor thermal management during processing"
        else
            echo "  ⚡ Use CPU-only processing for this workload"
            echo "  💡 Vulkan overhead outweighs benefits"
            echo "  🔄 Consider Vulkan for larger models or different workloads"
        fi
    elif $cpu_success; then
        echo "  ⚡ Use CPU-only processing (Vulkan failed)"
        echo "  🔧 Check Vulkan driver compatibility"
        echo "  📱 Verify FP16 and 16-bit storage support"
    elif $vulkan_success; then
        echo "  🚀 Use Vulkan processing (CPU failed)"
        echo "  🔍 Investigate CPU-only failure"
        echo "  📊 Check resource constraints"
    else
        echo "  ❌ Both tests failed - investigate system issues"
        echo "  🔍 Check DirectWhisperService configuration"
        echo "  📱 Verify device compatibility"
    fi
    
    echo ""
    echo "Monitoring Recommendations:"
    echo "  📊 Use TranscribeWorker logs for real-time progress monitoring"
    echo "  🌡️  Monitor device temperature during processing"
    echo "  💾 Track memory usage patterns"
    echo "  ⚡ Monitor CPU utilization for optimization"
    echo ""
    echo "Deployment Recommendations:"
    echo "  🎯 Keep app in foreground during processing"
    echo "  🔤 Use fixed language 'en' to prevent LID hangs"
    echo "  📁 Ensure proper storage permissions"
    echo "  🔄 Use DirectWhisperService for reliable processing"
    echo ""
}

# Main analysis
echo "🔍 Starting comprehensive analysis..."
echo ""

# Analyze CPU data
analyze_cpu_data "$RESULTS_DIR/cpu_only" "CPU_ONLY"
analyze_cpu_data "$RESULTS_DIR/vulkan_enabled" "VULKAN_ENABLED"

# Analyze memory data
analyze_memory_data "$RESULTS_DIR/cpu_only" "CPU_ONLY"
analyze_memory_data "$RESULTS_DIR/vulkan_enabled" "VULKAN_ENABLED"

# Analyze TranscribeWorker logs
analyze_transcribe_logs "$RESULTS_DIR/cpu_only" "CPU_ONLY"
analyze_transcribe_logs "$RESULTS_DIR/vulkan_enabled" "VULKAN_ENABLED"

# Generate performance comparison
generate_performance_comparison

# Generate recommendations
generate_recommendations

# Create comprehensive analysis report
echo "=== Generating Comprehensive Analysis Report ==="
ANALYSIS_REPORT="$ANALYSIS_DIR/comprehensive_analysis_report.md"

cat > "$ANALYSIS_REPORT" << EOF
# Comprehensive Ablation Test Analysis Report

**Analysis Date:** $(date)  
**Results Directory:** $RESULTS_DIR  
**Analysis Method:** DirectWhisperService Performance Analysis  

## Executive Summary

This report provides a comprehensive analysis of the redesigned DirectWhisperService ablation test results, focusing on CPU vs Vulkan performance comparison.

## Test Configuration

- **Method:** DirectWhisperService (proven command flow)
- **Language:** Fixed to "en" (prevents LID hangs)
- **Monitoring:** TranscribeWorker logs + system metrics
- **Duration:** 120 seconds per test with real-time monitoring

## Performance Analysis

### Processing Time Comparison
EOF

# Add performance comparison to report
if [ -f "$RESULTS_DIR/cpu_only/duration.txt" ] && [ -f "$RESULTS_DIR/vulkan_enabled/duration.txt" ]; then
    CPU_DURATION=$(cat "$RESULTS_DIR/cpu_only/duration.txt")
    VULKAN_DURATION=$(cat "$RESULTS_DIR/vulkan_enabled/duration.txt")
    
    cat >> "$ANALYSIS_REPORT" << EOF
- **CPU Only:** ${CPU_DURATION} seconds
- **Vulkan Enabled:** ${VULKAN_DURATION} seconds
EOF
    
    if [ "$VULKAN_DURATION" -lt "$CPU_DURATION" ]; then
        SPEEDUP=$(echo "scale=2; $CPU_DURATION / $VULKAN_DURATION" | bc)
        cat >> "$ANALYSIS_REPORT" << EOF
- **Speedup:** ${SPEEDUP}x faster with Vulkan
EOF
    else
        SLOWDOWN=$(echo "scale=2; $VULKAN_DURATION / $CPU_DURATION" | bc)
        cat >> "$ANALYSIS_REPORT" << EOF
- **Slowdown:** ${SLOWDOWN}x slower with Vulkan
EOF
    fi
else
    cat >> "$ANALYSIS_REPORT" << EOF
- **Comparison:** Unable to compare due to missing duration data
EOF
fi

cat >> "$ANALYSIS_REPORT" << EOF

### Resource Usage Analysis

#### CPU Usage
EOF

# Add CPU analysis to report
if [ -f "$ANALYSIS_DIR/CPU_ONLY_cpu_analysis.txt" ]; then
    cat >> "$ANALYSIS_REPORT" << EOF
**CPU Only Test:**
\`\`\`
$(head -10 "$ANALYSIS_DIR/CPU_ONLY_cpu_analysis.txt")
\`\`\`
EOF
fi

if [ -f "$ANALYSIS_DIR/VULKAN_ENABLED_cpu_analysis.txt" ]; then
    cat >> "$ANALYSIS_REPORT" << EOF
**Vulkan Enabled Test:**
\`\`\`
$(head -10 "$ANALYSIS_DIR/VULKAN_ENABLED_cpu_analysis.txt")
\`\`\`
EOF
fi

cat >> "$ANALYSIS_REPORT" << EOF

#### Memory Usage
EOF

# Add memory analysis to report
if [ -f "$ANALYSIS_DIR/CPU_ONLY_memory_analysis.txt" ]; then
    cat >> "$ANALYSIS_REPORT" << EOF
**CPU Only Test:**
\`\`\`
$(head -10 "$ANALYSIS_DIR/CPU_ONLY_memory_analysis.txt")
\`\`\`
EOF
fi

if [ -f "$ANALYSIS_DIR/VULKAN_ENABLED_memory_analysis.txt" ]; then
    cat >> "$ANALYSIS_REPORT" << EOF
**Vulkan Enabled Test:**
\`\`\`
$(head -10 "$ANALYSIS_DIR/VULKAN_ENABLED_memory_analysis.txt")
\`\`\`
EOF
fi

cat >> "$ANALYSIS_REPORT" << EOF

### TranscribeWorker Log Analysis
EOF

# Add TranscribeWorker analysis to report
if [ -f "$ANALYSIS_DIR/CPU_ONLY_transcribe_analysis.txt" ]; then
    cat >> "$ANALYSIS_REPORT" << EOF
**CPU Only Test Processing Stages:**
\`\`\`
$(grep "Processing Stage Occurrences:" -A 10 "$ANALYSIS_DIR/CPU_ONLY_transcribe_analysis.txt")
\`\`\`
EOF
fi

if [ -f "$ANALYSIS_DIR/VULKAN_ENABLED_transcribe_analysis.txt" ]; then
    cat >> "$ANALYSIS_REPORT" << EOF
**Vulkan Enabled Test Processing Stages:**
\`\`\`
$(grep "Processing Stage Occurrences:" -A 10 "$ANALYSIS_DIR/VULKAN_ENABLED_transcribe_analysis.txt")
\`\`\`
EOF
fi

cat >> "$ANALYSIS_REPORT" << EOF

## Recommendations

### Production Deployment
EOF

# Add recommendations to report
if [ -f "$RESULTS_DIR/cpu_only/status.txt" ] && [ -f "$RESULTS_DIR/vulkan_enabled/status.txt" ]; then
    CPU_STATUS=$(cat "$RESULTS_DIR/cpu_only/status.txt")
    VULKAN_STATUS=$(cat "$RESULTS_DIR/vulkan_enabled/status.txt")
    
    if [ "$CPU_STATUS" = "SUCCESS" ] && [ "$VULKAN_STATUS" = "SUCCESS" ]; then
        if [ "$VULKAN_DURATION" -lt "$CPU_DURATION" ]; then
            cat >> "$ANALYSIS_REPORT" << EOF
- **Use Vulkan acceleration** for better performance
- **Verify device compatibility** (FP16, 16-bit storage support)
- **Monitor thermal management** during processing
EOF
        else
            cat >> "$ANALYSIS_REPORT" << EOF
- **Use CPU-only processing** for this workload
- **Vulkan overhead** outweighs benefits for this case
- **Consider Vulkan** for larger models or different workloads
EOF
        fi
    elif [ "$CPU_STATUS" = "SUCCESS" ]; then
        cat >> "$ANALYSIS_REPORT" << EOF
- **Use CPU-only processing** (Vulkan failed)
- **Check Vulkan driver compatibility**
- **Verify FP16 and 16-bit storage support**
EOF
    elif [ "$VULKAN_STATUS" = "SUCCESS" ]; then
        cat >> "$ANALYSIS_REPORT" << EOF
- **Use Vulkan processing** (CPU failed)
- **Investigate CPU-only failure**
- **Check resource constraints**
EOF
    else
        cat >> "$ANALYSIS_REPORT" << EOF
- **Both tests failed** - investigate system issues
- **Check DirectWhisperService configuration**
- **Verify device compatibility**
EOF
    fi
else
    cat >> "$ANALYSIS_REPORT" << EOF
- **Unable to determine optimal configuration**
- **Check test results and system logs**
- **Verify DirectWhisperService setup**
EOF
fi

cat >> "$ANALYSIS_REPORT" << EOF

### Monitoring Strategy
- **TranscribeWorker logs** for real-time progress monitoring
- **Device temperature** monitoring during processing
- **Memory usage patterns** tracking
- **CPU utilization** monitoring for optimization

### Deployment Best Practices
- **Keep app in foreground** during processing
- **Use fixed language 'en'** to prevent LID hangs
- **Ensure proper storage permissions**
- **Use DirectWhisperService** for reliable processing

## Files Generated

### Analysis Files
- **CPU Analysis:** \`$ANALYSIS_DIR/CPU_ONLY_cpu_analysis.txt\`
- **Vulkan CPU Analysis:** \`$ANALYSIS_DIR/VULKAN_ENABLED_cpu_analysis.txt\`
- **Memory Analysis:** \`$ANALYSIS_DIR/CPU_ONLY_memory_analysis.txt\`
- **Vulkan Memory Analysis:** \`$ANALYSIS_DIR/VULKAN_ENABLED_memory_analysis.txt\`
- **TranscribeWorker Analysis:** \`$ANALYSIS_DIR/*_transcribe_analysis.txt\`

### Original Test Results
- **CPU Test Results:** \`$RESULTS_DIR/cpu_only/\`
- **Vulkan Test Results:** \`$RESULTS_DIR/vulkan_enabled/\`
- **Original Report:** \`$RESULTS_DIR/redesigned_ablation_report.md\`

---

*Comprehensive analysis generated automatically*
EOF

echo "✅ Comprehensive analysis report generated: $ANALYSIS_REPORT"
echo ""
echo "📊 Analysis Summary:"
echo "  📁 Analysis files saved to: $ANALYSIS_DIR"
echo "  📋 Comprehensive report: $ANALYSIS_REPORT"
echo "  🔍 Detailed metrics and recommendations included"
echo ""
echo "🎯 Comprehensive ablation test analysis completed!"
