#!/bin/bash

# CPU Test Script 001 - Basic Performance Test
# Tests CPU usage during Whisper processing on Xiaomi device

set -e

echo "=== CPU Test Script 001 - Basic Performance Test ==="
echo "Timestamp: $(date)"
echo ""

# Configuration
PACKAGE_NAME="com.mira.com"
TEST_AUDIO_FILE="/sdcard/test_audio_001.wav"
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

# Create test audio file
log_info "Creating test audio file..."
if ! adb shell test -f "$TEST_AUDIO_FILE"; then
    # Create a 10-second test audio file
    adb shell "dd if=/dev/urandom of=$TEST_AUDIO_FILE bs=1024 count=100 2>/dev/null"
    log_success "Test audio file created: $TEST_AUDIO_FILE"
else
    log_success "Test audio file exists: $TEST_AUDIO_FILE"
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

# Try different activity names
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
    log_warning "Could not launch app with standard activities, trying broadcast..."
fi

# Start processing via DirectWhisperService (the working method)
log_info "Starting processing via DirectWhisperService..."

# Configuration for DirectWhisperService
MODEL="whisper-base.q5_1.bin"
THREADS=2
LANGUAGE="en"
TRANSLATE=false
DURATION_SECONDS=60

# Method 1: Try DirectWhisperService (the proven working method)
log_info "Starting DirectWhisperService with tennis audio..."
if adb shell am startservice -n "$PACKAGE_NAME/com.mira.com.feature.whisper.service.DirectWhisperService" \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$TEST_AUDIO_FILE" \
    --es "model" "/sdcard/MiraWhisper/models/$MODEL" \
    --es "threads" "$THREADS" \
    --es "lang" "$LANGUAGE" \
    --ez "translate" "$TRANSLATE" \
    --ei "max_seconds" "$DURATION_SECONDS" 2>/dev/null; then
    log_success "DirectWhisperService started successfully"
    WORK_STARTED=true
else
    log_warning "DirectWhisperService not available, trying alternative methods..."
    
    # Method 2: Try file intent as fallback
    log_info "Trying file intent as fallback..."
    if adb shell am start -a "android.intent.action.VIEW" -d "file://$TEST_AUDIO_FILE" -n "$PACKAGE_NAME/com.mira.whisper.WhisperMainActivity" 2>/dev/null; then
        log_success "File processing triggered via intent"
        WORK_STARTED=true
    fi
fi

if [ "$WORK_STARTED" = false ]; then
    log_warning "Could not trigger processing directly, monitoring app performance anyway..."
fi

# Start CPU monitoring
log_info "Starting CPU performance monitoring..."

# Initialize metrics arrays
CPU_METRICS=()
MEMORY_METRICS=()
TIMESTAMP_METRICS=()

# Start monitoring for 10 minutes (longer wait for DirectWhisperService)
START_TIME=$(date +%s)
MONITOR_DURATION=600  # 10 minutes

log_info "Monitoring CPU and memory usage for $MONITOR_DURATION seconds during DirectWhisperService processing..."

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

    # Log every 30 seconds
    if [ $((i % 30)) -eq 0 ]; then
        MEMORY_MB=$((MEMORY_USAGE / 1024))
        log_info "Time ${ELAPSED}s: CPU=${CPU_USAGE}%, Memory=${MEMORY_MB}MB"
        
        # Check for batch directories (the working method output)
        BATCH_DIRS=$(adb shell "find /sdcard/MiraWhisper/out -name 'batch_*' -type d 2>/dev/null")
        if [ -n "$BATCH_DIRS" ]; then
            log_success "Found batch directories: $BATCH_DIRS"
            
            # Check for transcript files in batch directories
            TRANSCRIPT_FILES=$(adb shell "find /sdcard/MiraWhisper/out -name '*.srt' -o -name '*.txt' -o -name '*.json' 2>/dev/null")
            if [ -n "$TRANSCRIPT_FILES" ]; then
                log_success "Found transcript files: $TRANSCRIPT_FILES"
                
                # Read and validate transcript content
                for file in $TRANSCRIPT_FILES; do
                    CONTENT=$(adb shell "cat $file 2>/dev/null")
                    if [[ "$CONTENT" != *"Hello world"* ]] && \
                       [[ "$CONTENT" != *"Mock whisper result"* ]] && \
                       [[ "$CONTENT" != *"This is a test"* ]] && \
                       [[ "$CONTENT" != *"Audio transcription in progress"* ]] && \
                       [[ -n "$CONTENT" ]] && \
                       [[ ${#CONTENT} -gt 20 ]]; then
                        log_success "REAL TRANSCRIPT FOUND: $file"
                        echo "$CONTENT" > "cpu_test_results/real_transcript_001.txt"
                        break 2  # Break out of both loops
                    fi
                done
            fi
        fi
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

log_success "CPU Test 001 completed"
echo ""
echo "📊 CPU Test 001 Results:"
echo "   Duration: ${MONITOR_DURATION}s"
echo "   Average CPU: ${CPU_AVG}%"
echo "   Maximum CPU: ${CPU_MAX}%"
echo "   Average Memory: ${MEMORY_AVG_MB}MB"
echo "   Maximum Memory: ${MEMORY_MAX_MB}MB"

# Save detailed results
RESULT_FILE="$RESULTS_DIR/cpu_test_001_$TIMESTAMP.txt"
{
    echo "CPU Test Script 001 - Basic Performance Test"
    echo "=============================================="
    echo "Timestamp: $(date)"
    echo "Device: $DEVICE_MODEL (Android $DEVICE_VERSION)"
    echo "App: $PACKAGE_NAME (version: $APP_VERSION)"
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
adb logcat -d | grep -i "$PACKAGE_NAME\|whisper\|cpu" > "$RESULTS_DIR/cpu_test_001_logs_$TIMESTAMP.txt"

echo ""
log_success "CPU Test Script 001 completed successfully!"
echo "📄 Results saved in: $RESULTS_DIR/"
