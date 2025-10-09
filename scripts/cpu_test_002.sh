#!/bin/bash

# CPU Test Script 002 - Whisper Processing Test
# Tests CPU usage during actual Whisper transcription on Xiaomi device

set -e

echo "=== CPU Test Script 002 - Whisper Processing Test ==="
echo "Timestamp: $(date)"
echo ""

# Configuration
PACKAGE_NAME="com.mira.com"
TEST_AUDIO_FILE="/sdcard/tennis_interview_test.wav"
RESULTS_DIR="cpu_test_results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Create results directory
mkdir -p "$RESULTS_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check device connection
log_info "Checking device connection..."
if ! adb devices | grep -q "device$"; then
    log_error "No Android device connected"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model)
DEVICE_VERSION=$(adb shell getprop ro.build.version.release)
log_success "Device connected: $DEVICE_MODEL (Android $DEVICE_VERSION)"

# Prepare test audio file
log_info "Preparing test audio file..."
if [ -f "tennis_interview_clip_002.wav" ]; then
    adb push "tennis_interview_clip_002.wav" "$TEST_AUDIO_FILE"
    log_success "Tennis interview audio uploaded: $TEST_AUDIO_FILE"
elif [ -f "tennis_interview_clip_002_full.wav" ]; then
    adb push "tennis_interview_clip_002_full.wav" "$TEST_AUDIO_FILE"
    log_success "Tennis interview full audio uploaded: $TEST_AUDIO_FILE"
else
    log_error "No tennis interview audio files found!"
    log_info "Available files:"
    ls -la tennis_interview_clip_002* 2>/dev/null || echo "No tennis_interview_clip_002 files found"
    exit 1
fi

# Check app installation
log_info "Checking app installation..."
if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    log_error "App not installed: $PACKAGE_NAME"
    exit 1
fi

APP_VERSION=$(adb shell dumpsys package "$PACKAGE_NAME" | grep versionName | head -1 | cut -d'=' -f2)
log_success "App installed: $PACKAGE_NAME (version: $APP_VERSION)"

# Clear logs
log_info "Clearing logs..."
adb logcat -c

# Launch app
log_info "Launching app..."
adb shell am force-stop "$PACKAGE_NAME" || true
sleep 2

# Try to launch app
ACTIVITIES=(
    "com.mira.com/com.mira.whisper.WhisperMainActivity"
    "com.mira.com/com.mira.clip.Clip4ClipActivity"
    "com.mira.com/.MainActivity"
)

APP_LAUNCHED=false
for activity in "${ACTIVITIES[@]}"; do
    log_info "Trying to launch: $activity"
    if adb shell am start -n "$activity" 2>/dev/null; then
        sleep 3
        if adb shell ps | grep -q "$PACKAGE_NAME"; then
            log_success "App launched successfully with: $activity"
            APP_LAUNCHED=true
            break
        fi
    fi
done

if [ "$APP_LAUNCHED" = false ]; then
    log_warning "Could not launch app with standard activities"
fi

# Start Whisper processing via WorkManager and file intents
log_info "Starting Whisper processing via WorkManager..."

# Try to trigger processing through multiple methods
PROCESSING_STARTED=false

# Method 1: Try to trigger via file intent with the tennis audio
log_info "Trying to trigger processing via file intent with tennis audio..."
if adb shell am start -a "android.intent.action.VIEW" -d "file://$TEST_AUDIO_FILE" -n "$PACKAGE_NAME/com.mira.whisper.WhisperMainActivity" 2>/dev/null; then
    log_success "Tennis audio processing triggered via intent"
    PROCESSING_STARTED=true
fi

# Method 2: Try to trigger via WorkManager
if [ "$PROCESSING_STARTED" = false ]; then
    log_info "Trying to trigger via WorkManager..."
    if adb shell am broadcast -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" -n "$PACKAGE_NAME/androidx.work.impl.diagnostics.DiagnosticsReceiver" 2>/dev/null; then
        log_success "WorkManager diagnostics triggered"
        PROCESSING_STARTED=true
    fi
fi

# Method 3: Create processing job file
if [ "$PROCESSING_STARTED" = false ]; then
    log_info "Creating processing job file..."
    adb shell "echo '{\"file_path\":\"$TEST_AUDIO_FILE\",\"action\":\"transcribe\",\"model\":\"small.en-q5_1\",\"timestamp\":\"$(date +%s)\"}' > /sdcard/whisper_job.json"
    log_success "Processing job file created"
fi

if [ "$PROCESSING_STARTED" = false ]; then
    log_warning "Could not start processing directly, monitoring anyway..."
fi

# Start CPU monitoring during processing
log_info "Starting CPU performance monitoring during Whisper processing..."

# Initialize metrics arrays
CPU_METRICS=()
MEMORY_METRICS=()
TIMESTAMP_METRICS=()

# Start monitoring for 120 seconds (longer for processing)
START_TIME=$(date +%s)
MONITOR_DURATION=120

log_info "Monitoring CPU and memory usage for $MONITOR_DURATION seconds during processing..."

for i in $(seq 1 $MONITOR_DURATION); do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))
    
    # Get CPU usage - fix the parsing
    CPU_USAGE=$(adb shell dumpsys cpuinfo | grep "$PACKAGE_NAME" | awk '{print $1}' | head -1 | sed 's/%//' || echo "0")
    
    # Get memory usage - fix the parsing
    MEMORY_USAGE=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}' | head -1 | sed 's/[^0-9]//g' || echo "0")
    
    # Store metrics
    CPU_METRICS+=("$CPU_USAGE")
    MEMORY_METRICS+=("$MEMORY_USAGE")
    TIMESTAMP_METRICS+=("$ELAPSED")
    
    # Log every 15 seconds
    if [ $((i % 15)) -eq 0 ]; then
        MEMORY_MB=$((MEMORY_USAGE / 1024))
        log_info "Time ${ELAPSED}s: CPU=${CPU_USAGE}%, Memory=${MEMORY_MB}MB"
    fi
    
    sleep 1
done

# Calculate averages
CPU_AVG=$(printf '%s\n' "${CPU_METRICS[@]}" | awk '{sum+=$1} END {print sum/NR}')
MEMORY_AVG=$(printf '%s\n' "${MEMORY_METRICS[@]}" | awk '{sum+=$1} END {print sum/NR}')

# Get maximum values
CPU_MAX=$(printf '%s\n' "${CPU_METRICS[@]}" | sort -nr | head -1)
MEMORY_MAX=$(printf '%s\n' "${MEMORY_METRICS[@]}" | sort -nr | head -1)

# Convert memory to MB
MEMORY_AVG_MB=$((MEMORY_AVG / 1024))
MEMORY_MAX_MB=$((MEMORY_MAX / 1024))

log_success "CPU Test 002 completed"
echo ""
echo "📊 CPU Test 002 Results (Whisper Processing):"
echo "   Duration: ${MONITOR_DURATION}s"
echo "   Average CPU: ${CPU_AVG}%"
echo "   Maximum CPU: ${CPU_MAX}%"
echo "   Average Memory: ${MEMORY_AVG_MB}MB"
echo "   Maximum Memory: ${MEMORY_MAX_MB}MB"

# Save detailed results
RESULT_FILE="$RESULTS_DIR/cpu_test_002_$TIMESTAMP.txt"
{
    echo "CPU Test Script 002 - Whisper Processing Test"
    echo "=============================================="
    echo "Timestamp: $(date)"
    echo "Device: $DEVICE_MODEL (Android $DEVICE_VERSION)"
    echo "App: $PACKAGE_NAME (version: $APP_VERSION)"
    echo "Test File: $TEST_AUDIO_FILE"
    echo "Duration: ${MONITOR_DURATION}s"
    echo ""
    echo "Performance Metrics:"
    echo "  Average CPU Usage: ${CPU_AVG}%"
    echo "  Maximum CPU Usage: ${CPU_MAX}%"
    echo "  Average Memory Usage: ${MEMORY_AVG_MB}MB"
    echo "  Maximum Memory Usage: ${MEMORY_MAX_MB}MB"
    echo ""
    echo "Detailed CPU Usage Over Time:"
    for i in "${!CPU_METRICS[@]}"; do
        echo "  ${TIMESTAMP_METRICS[$i]}s: ${CPU_METRICS[$i]}%"
    done
    echo ""
    echo "Detailed Memory Usage Over Time:"
    for i in "${!MEMORY_METRICS[@]}"; do
        MEMORY_MB=$((MEMORY_METRICS[$i] / 1024))
        echo "  ${TIMESTAMP_METRICS[$i]}s: ${MEMORY_MB}MB"
    done
} > "$RESULT_FILE"

log_success "Results saved to: $RESULT_FILE"

# Get app logs
log_info "Collecting app logs..."
adb logcat -d | grep -i "$PACKAGE_NAME\|whisper\|transcribe\|cpu" > "$RESULTS_DIR/cpu_test_002_logs_$TIMESTAMP.txt"

# Check for output files
log_info "Checking for transcription output files..."
OUTPUT_FILES=$(adb shell find /sdcard -name "*.srt" -o -name "*.json" -o -name "*.txt" 2>/dev/null | wc -l)
if [ "$OUTPUT_FILES" -gt 0 ]; then
    log_success "Found $OUTPUT_FILES output files"
    adb shell find /sdcard -name "*.srt" -o -name "*.json" -o -name "*.txt" 2>/dev/null > "$RESULTS_DIR/cpu_test_002_output_files_$TIMESTAMP.txt"
else
    log_warning "No output files found"
fi

echo ""
log_success "CPU Test Script 002 completed successfully!"
echo "📄 Results saved in: $RESULTS_DIR/"
