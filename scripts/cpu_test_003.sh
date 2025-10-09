#!/bin/bash

# CPU Test Script 003 - Stress Test
# Tests CPU usage under stress conditions on Xiaomi device

set -e

echo "=== CPU Test Script 003 - Stress Test ==="
echo "Timestamp: $(date)"
echo ""

# Configuration
PACKAGE_NAME="com.mira.com"
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

# Get device specifications
log_info "Getting device specifications..."
CPU_CORES=$(adb shell "cat /proc/cpuinfo | grep 'processor' | wc -l")
RAM_TOTAL=$(adb shell "cat /proc/meminfo | grep MemTotal | awk '{print \$2}'")
RAM_GB=$((RAM_TOTAL / 1024 / 1024))

log_info "Device specs: $CPU_CORES cores, ${RAM_GB}GB RAM"

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

# Start multiple processing jobs via WorkManager and file intents
log_info "Starting multiple processing jobs via WorkManager..."

# Initialize metrics arrays
CPU_METRICS=()
MEMORY_METRICS=()
TIMESTAMP_METRICS=()
THERMAL_METRICS=()
BATTERY_METRICS=()

# Start monitoring for 180 seconds (3 minutes stress test)
START_TIME=$(date +%s)
MONITOR_DURATION=180

log_info "Running stress test for $MONITOR_DURATION seconds..."

# Prepare real tennis audio files for stress testing
log_info "Preparing real tennis audio files for stress testing..."

# Use real tennis clips for stress testing
TENNIS_FILES=(
    "tennis_interview_clip_001.wav"
    "tennis_interview_clip_002.wav" 
    "tennis_interview_clip_002_full.wav"
)

STRESS_FILES=()
for i in {1..3}; do
    if [ $i -le ${#TENNIS_FILES[@]} ] && [ -f "${TENNIS_FILES[$((i-1))]}" ]; then
        TEST_FILE="/sdcard/stress_test_$i.wav"
        adb push "${TENNIS_FILES[$((i-1))]}" "$TEST_FILE"
        STRESS_FILES+=("$TEST_FILE")
        log_success "Real tennis audio uploaded: $TEST_FILE (${TENNIS_FILES[$((i-1))]})"
    else
        log_warning "Tennis file ${TENNIS_FILES[$((i-1))]:-not specified} not found, creating random file..."
        TEST_FILE="/sdcard/stress_test_$i.wav"
        adb shell "dd if=/dev/urandom of=$TEST_FILE bs=1024 count=500 2>/dev/null"
        STRESS_FILES+=("$TEST_FILE")
    fi
done
log_success "Prepared ${#STRESS_FILES[@]} test audio files"

# Start multiple processing jobs using file intents and WorkManager
log_info "Starting multiple processing jobs using file intents..."

for i in {1..3}; do
    TEST_FILE="${STRESS_FILES[$((i-1))]}"
    
    # Method 1: Try file intent
    log_info "Starting job $i with file intent: $TEST_FILE"
    adb shell am start -a "android.intent.action.VIEW" -d "file://$TEST_FILE" -n "$PACKAGE_NAME/com.mira.whisper.WhisperMainActivity" 2>/dev/null || true
    sleep 2
    
    # Method 2: Create job file
    log_info "Creating job file for stress test $i"
    adb shell "echo '{\"file_path\":\"$TEST_FILE\",\"action\":\"transcribe\",\"job_id\":\"stress_$i\",\"timestamp\":\"$(date +%s)\"}' > /sdcard/stress_job_$i.json"
    sleep 1
done

log_info "Monitoring system performance during stress test..."

for i in $(seq 1 $MONITOR_DURATION); do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))
    
    # Get CPU usage - fix the parsing
    CPU_USAGE=$(adb shell dumpsys cpuinfo | grep "$PACKAGE_NAME" | awk '{print $1}' | head -1 | sed 's/%//' || echo "0")
    
    # Get memory usage - fix the parsing
    MEMORY_USAGE=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | awk '{print $2}' | head -1 | sed 's/[^0-9]//g' || echo "0")
    
    # Get thermal information
    THERMAL_TEMP=$(adb shell "cat /sys/class/thermal/thermal_zone*/temp 2>/dev/null" | head -1 | sed 's/[^0-9]//g' || echo "0")
    THERMAL_CELSIUS=$((THERMAL_TEMP / 1000))
    
    # Get battery information
    BATTERY_LEVEL=$(adb shell dumpsys battery | grep level | awk '{print $2}' | sed 's/[^0-9]//g' || echo "0")
    
    # Store metrics
    CPU_METRICS+=("$CPU_USAGE")
    MEMORY_METRICS+=("$MEMORY_USAGE")
    TIMESTAMP_METRICS+=("$ELAPSED")
    THERMAL_METRICS+=("$THERMAL_CELSIUS")
    BATTERY_METRICS+=("$BATTERY_LEVEL")
    
    # Log every 20 seconds
    if [ $((i % 20)) -eq 0 ]; then
        MEMORY_MB=$((MEMORY_USAGE / 1024))
        log_info "Time ${ELAPSED}s: CPU=${CPU_USAGE}%, Memory=${MEMORY_MB}MB, Temp=${THERMAL_CELSIUS}°C, Battery=${BATTERY_LEVEL}%"
    fi
    
    sleep 1
done

# Calculate averages
CPU_AVG=$(printf '%s\n' "${CPU_METRICS[@]}" | awk '{sum+=$1} END {print sum/NR}')
MEMORY_AVG=$(printf '%s\n' "${MEMORY_METRICS[@]}" | awk '{sum+=$1} END {print sum/NR}')
THERMAL_AVG=$(printf '%s\n' "${THERMAL_METRICS[@]}" | awk '{sum+=$1} END {print sum/NR}')
BATTERY_AVG=$(printf '%s\n' "${BATTERY_METRICS[@]}" | awk '{sum+=$1} END {print sum/NR}')

# Get maximum values
CPU_MAX=$(printf '%s\n' "${CPU_METRICS[@]}" | sort -nr | head -1)
MEMORY_MAX=$(printf '%s\n' "${MEMORY_METRICS[@]}" | sort -nr | head -1)
THERMAL_MAX=$(printf '%s\n' "${THERMAL_METRICS[@]}" | sort -nr | head -1)

# Convert memory to MB
MEMORY_AVG_MB=$((MEMORY_AVG / 1024))
MEMORY_MAX_MB=$((MEMORY_MAX / 1024))

log_success "CPU Test 003 completed"
echo ""
echo "📊 CPU Test 003 Results (Stress Test):"
echo "   Duration: ${MONITOR_DURATION}s"
echo "   Average CPU: ${CPU_AVG}%"
echo "   Maximum CPU: ${CPU_MAX}%"
echo "   Average Memory: ${MEMORY_AVG_MB}MB"
echo "   Maximum Memory: ${MEMORY_MAX_MB}MB"
echo "   Average Temperature: ${THERMAL_AVG}°C"
echo "   Maximum Temperature: ${THERMAL_MAX}°C"
echo "   Average Battery: ${BATTERY_AVG}%"

# Save detailed results
RESULT_FILE="$RESULTS_DIR/cpu_test_003_$TIMESTAMP.txt"
{
    echo "CPU Test Script 003 - Stress Test"
    echo "================================="
    echo "Timestamp: $(date)"
    echo "Device: $DEVICE_MODEL (Android $DEVICE_VERSION)"
    echo "App: $PACKAGE_NAME (version: $APP_VERSION)"
    echo "Device Specs: $CPU_CORES cores, ${RAM_GB}GB RAM"
    echo "Duration: ${MONITOR_DURATION}s"
    echo ""
    echo "Performance Metrics:"
    echo "  Average CPU Usage: ${CPU_AVG}%"
    echo "  Maximum CPU Usage: ${CPU_MAX}%"
    echo "  Average Memory Usage: ${MEMORY_AVG_MB}MB"
    echo "  Maximum Memory Usage: ${MEMORY_MAX_MB}MB"
    echo "  Average Temperature: ${THERMAL_AVG}°C"
    echo "  Maximum Temperature: ${THERMAL_MAX}°C"
    echo "  Average Battery Level: ${BATTERY_AVG}%"
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
    echo ""
    echo "Detailed Temperature Over Time:"
    for i in "${!THERMAL_METRICS[@]}"; do
        echo "  ${TIMESTAMP_METRICS[$i]}s: ${THERMAL_METRICS[$i]}°C"
    done
    echo ""
    echo "Detailed Battery Level Over Time:"
    for i in "${!BATTERY_METRICS[@]}"; do
        echo "  ${TIMESTAMP_METRICS[$i]}s: ${BATTERY_METRICS[$i]}%"
    done
} > "$RESULT_FILE"

log_success "Results saved to: $RESULT_FILE"

# Get app logs
log_info "Collecting app logs..."
adb logcat -d | grep -i "$PACKAGE_NAME\|whisper\|transcribe\|cpu\|thermal\|battery" > "$RESULTS_DIR/cpu_test_003_logs_$TIMESTAMP.txt"

# Check system performance
log_info "Checking system performance..."
adb shell "top -n 1" > "$RESULTS_DIR/cpu_test_003_system_top_$TIMESTAMP.txt"
adb shell "cat /proc/meminfo" > "$RESULTS_DIR/cpu_test_003_meminfo_$TIMESTAMP.txt"

# Clean up test files
log_info "Cleaning up test files..."
adb shell "rm -f /sdcard/stress_test_*.wav"

echo ""
log_success "CPU Test Script 003 completed successfully!"
echo "📄 Results saved in: $RESULTS_DIR/"
