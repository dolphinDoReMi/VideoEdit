#!/bin/bash

# Tennis Clip Processing Monitor - Xiaomi Pad
# Monitors background transcription and retrieves results

set -e

echo "🎾 Tennis Clip Processing Monitor - Xiaomi Pad"
echo "============================================="
echo ""

# Configuration
OUTPUT_DIR="/sdcard/MiraWhisper/transcriptions"
LOCAL_OUTPUT_DIR="/Users/dennis/Movies/VideoEdit"

echo "🔍 Monitoring background transcription processes..."
echo ""

# Function to check for active transcription jobs
check_active_jobs() {
    echo "📊 Active transcription jobs:"
    echo "============================"
    
    # Look for recent job files
    RECENT_JOBS=$(adb shell "find $OUTPUT_DIR -name '*.txt' -o -name '*.json' -o -name '*.srt' | head -10" 2>/dev/null || echo "")
    
    if [[ -n "$RECENT_JOBS" ]]; then
        echo "$RECENT_JOBS" | while read -r file; do
            if [[ -n "$file" ]]; then
                FILE_SIZE=$(adb shell "stat -c%s '$file'" 2>/dev/null | tr -d '\r' || echo "0")
                FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))
                echo "  📄 $file (${FILE_SIZE_MB}MB)"
            fi
        done
    else
        echo "  No recent job files found"
    fi
    echo ""
}

# Function to monitor log activity
monitor_logs() {
    echo "📋 Recent transcription activity:"
    echo "================================"
    
    # Get recent transcription logs
    RECENT_LOGS=$(adb logcat -d | grep -E "(TranscribeWorker|WhisperConnectorService|TECHNICAL.*Transcription)" | tail -10)
    
    if [[ -n "$RECENT_LOGS" ]]; then
        echo "$RECENT_LOGS" | while read -r line; do
            echo "  $line"
        done
    else
        echo "  No recent transcription activity"
    fi
    echo ""
}

# Function to retrieve completed results
retrieve_results() {
    echo "📥 Retrieving completed results..."
    echo "================================"
    
    # Look for completed transcript files
    COMPLETED_FILES=$(adb shell "find $OUTPUT_DIR -name '*.txt' -o -name '*.srt' -o -name '*.json' | grep -E '(full_|clip001_)'" 2>/dev/null || echo "")
    
    if [[ -n "$COMPLETED_FILES" ]]; then
        echo "$COMPLETED_FILES" | while read -r file; do
            if [[ -n "$file" ]]; then
                FILENAME=$(basename "$file")
                LOCAL_FILE="$LOCAL_OUTPUT_DIR/$FILENAME"
                
                echo "  📤 Pulling $file -> $LOCAL_FILE"
                adb pull "$file" "$LOCAL_FILE" 2>/dev/null || echo "    ❌ Failed to pull $file"
            fi
        done
        echo ""
        echo "✅ Results retrieved to: $LOCAL_OUTPUT_DIR"
    else
        echo "  No completed files found yet"
    fi
    echo ""
}

# Function to show progress
show_progress() {
    echo "📈 Current processing status:"
    echo "============================"
    
    # Check for progress in logs
    PROGRESS_LOGS=$(adb logcat -d | grep -E "(STREAMING.*progress|Overall progress|TECHNICAL.*progress)" | tail -5)
    
    if [[ -n "$PROGRESS_LOGS" ]]; then
        echo "$PROGRESS_LOGS" | while read -r line; do
            echo "  $line"
        done
    else
        echo "  No progress information available"
    fi
    echo ""
}

# Function to check for errors
check_errors() {
    echo "⚠️  Error check:"
    echo "==============="
    
    # Check for recent errors
    ERROR_LOGS=$(adb logcat -d | grep -E "(ERROR|Exception|Failed)" | tail -5)
    
    if [[ -n "$ERROR_LOGS" ]]; then
        echo "$ERROR_LOGS" | while read -r line; do
            echo "  ❌ $line"
        done
    else
        echo "  ✅ No recent errors found"
    fi
    echo ""
}

# Main monitoring loop
if [[ "$1" == "--continuous" ]]; then
    echo "🔄 Starting continuous monitoring (Ctrl+C to stop)..."
    echo ""
    
    while true; do
        clear
        echo "🎾 Tennis Clip Processing Monitor - Xiaomi Pad"
        echo "============================================="
        echo "🕐 $(date)"
        echo ""
        
        check_active_jobs
        monitor_logs
        show_progress
        check_errors
        
        echo "⏳ Refreshing in 10 seconds..."
        sleep 10
    done
else
    # Single check
    check_active_jobs
    monitor_logs
    show_progress
    check_errors
    retrieve_results
    
    echo "💡 Usage:"
    echo "  $0                    # Single check"
    echo "  $0 --continuous      # Continuous monitoring"
    echo ""
    echo "🔍 Manual log monitoring:"
    echo "  adb logcat -s TranscribeWorker:D WhisperConnectorService:D"
    echo ""
    echo "📥 Manual result retrieval:"
    echo "  adb pull $OUTPUT_DIR/*.txt $LOCAL_OUTPUT_DIR/"
    echo "  adb pull $OUTPUT_DIR/*.srt $LOCAL_OUTPUT_DIR/"
fi
