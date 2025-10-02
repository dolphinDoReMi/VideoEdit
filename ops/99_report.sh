#!/bin/bash
# Comprehensive test report generator
# Runs all tests and generates a detailed report

set -e

echo "=== CLIP4Clip Comprehensive Test Report ==="

# Configuration
PKG=${PKG:-"com.mira.clip"}
VARIANT=${VARIANT:-"clip_vit_b32_mean_v1"}
REPORT_FILE="test_report_$(date +%Y%m%d_%H%M%S).md"

# Initialize report
cat > "$REPORT_FILE" << EOF
# CLIP4Clip Test Report

**Generated:** $(date)
**Package:** $PKG
**Variant:** $VARIANT
**Device:** $(adb shell getprop ro.product.model 2>/dev/null || echo "Unknown")

## Test Summary

| Test Category | Status | Details |
|---------------|--------|---------|
EOF

# Test results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to add test result to report
add_test_result() {
    local category="$1"
    local status="$2"
    local details="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    if [ "$status" = "PASSED" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    
    echo "| $category | $status | $details |" >> "$REPORT_FILE"
}

# Function to run test and capture result
run_test() {
    local test_name="$1"
    local test_command="$2"
    local test_category="$3"
    
    echo "🔍 Running $test_name..."
    
    if eval "$test_command" >/dev/null 2>&1; then
        add_test_result "$test_category" "PASSED" "$test_name completed successfully"
        echo "✅ $test_name: PASSED"
    else
        add_test_result "$test_category" "FAILED" "$test_name failed"
        echo "❌ $test_name: FAILED"
    fi
}

# Check device connection
echo "📱 Checking device connection..."
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    add_test_result "Device" "FAILED" "No Android device connected"
    exit 1
fi

echo "✅ Android device connected"
add_test_result "Device" "PASSED" "Android device connected"

# Check app installation
echo "📱 Checking app installation..."
if ! adb shell pm list packages | grep -q "$PKG"; then
    echo "❌ App $PKG not installed"
    add_test_result "App" "FAILED" "App $PKG not installed"
    exit 1
fi

echo "✅ App $PKG is installed"
add_test_result "App" "PASSED" "App $PKG is installed"

# Run unit tests
echo ""
echo "🧪 Running unit tests..."
run_test "Room DAO Tests" "./gradlew :app:testDebugUnitTest --tests '*DbDaoTest*'" "Unit Tests"
run_test "Retrieval Math Tests" "./gradlew :app:testDebugUnitTest --tests '*RetrievalMathTest*'" "Unit Tests"

# Run instrumented tests
echo ""
echo "🔬 Running instrumented tests..."
run_test "Frame Sampler Tests" "./gradlew :app:connectedDebugAndroidTest --tests '*SamplerInstrumentedTest*'" "Instrumented Tests"
run_test "Image Encoder Tests" "./gradlew :app:connectedDebugAndroidTest --tests '*ImageEncoderInstrumentedTest*'" "Instrumented Tests"
run_test "Text Encoder Tests" "./gradlew :app:connectedDebugAndroidTest --tests '*TextEncoderInstrumentedTest*'" "Instrumented Tests"
run_test "Ingest Worker Tests" "./gradlew :app:connectedDebugAndroidTest --tests '*IngestWorkerInstrumentedTest*'" "Instrumented Tests"

# Run host operations
echo ""
echo "🖥️  Running host operations..."
run_test "Assets Verification" "bash ops/01_assets.sh" "Host Operations"
run_test "Jobs Status Check" "bash ops/03_jobs.sh" "Host Operations"
run_test "Performance Monitoring" "bash ops/07_perf_logcat.sh" "Host Operations"

# Run parity test (if Python environment is available)
echo ""
echo "🔄 Running parity tests..."
if command -v python3 >/dev/null 2>&1; then
    run_test "Parity Test" "python3 ops/05_parity.py" "Parity Tests"
else
    add_test_result "Parity Tests" "SKIPPED" "Python3 not available"
    echo "⚠️  Parity test skipped - Python3 not available"
fi

# Generate detailed report
echo ""
echo "📊 Generating detailed report..."

# Add test statistics
cat >> "$REPORT_FILE" << EOF

## Test Statistics

- **Total Tests:** $TOTAL_TESTS
- **Passed:** $PASSED_TESTS
- **Failed:** $FAILED_TESTS
- **Success Rate:** $((PASSED_TESTS * 100 / TOTAL_TESTS))%

## Detailed Results

EOF

# Add device information
cat >> "$REPORT_FILE" << EOF

### Device Information

- **Model:** $(adb shell getprop ro.product.model 2>/dev/null || echo "Unknown")
- **API Level:** $(adb shell getprop ro.build.version.sdk 2>/dev/null || echo "Unknown")
- **Android Version:** $(adb shell getprop ro.build.version.release 2>/dev/null || echo "Unknown")
- **Architecture:** $(adb shell getprop ro.product.cpu.abi 2>/dev/null || echo "Unknown")

EOF

# Add database information
cat >> "$REPORT_FILE" << EOF

### Database Information

- **Videos Count:** $(adb shell "run-as $PKG sqlite3 databases/app_database 'SELECT COUNT(*) FROM videos;'" 2>/dev/null || echo "0")
- **Embeddings Count:** $(adb shell "run-as $PKG sqlite3 databases/app_database 'SELECT COUNT(*) FROM embeddings WHERE variant=\"$VARIANT\";'" 2>/dev/null || echo "0")
- **Shots Count:** $(adb shell "run-as $PKG sqlite3 databases/app_database 'SELECT COUNT(*) FROM shots;'" 2>/dev/null || echo "0")

EOF

# Add performance information
cat >> "$REPORT_FILE" << EOF

### Performance Information

- **Memory Usage:** $(adb shell "dumpsys meminfo $PKG | grep 'TOTAL' | awk '{print \$2}'" 2>/dev/null || echo "Unknown") KB
- **CPU Usage:** $(adb shell "top -n 1 | grep $PKG | awk '{print \$9}'" 2>/dev/null || echo "Unknown") %

EOF

# Add recent log entries
cat >> "$REPORT_FILE" << EOF

### Recent Log Entries

\`\`\`
$(adb logcat -d -s "CLIP4Clip" | tail -20)
\`\`\`

EOF

# Final summary
echo ""
echo "=== Test Report Summary ==="
echo "📊 Total Tests: $TOTAL_TESTS"
echo "✅ Passed: $PASSED_TESTS"
echo "❌ Failed: $FAILED_TESTS"
echo "📈 Success Rate: $((PASSED_TESTS * 100 / TOTAL_TESTS))%"

if [ "$FAILED_TESTS" -eq 0 ]; then
    echo "🎉 All tests passed!"
    echo "📊 Status: SUCCESS"
    EXIT_CODE=0
else
    echo "⚠️  Some tests failed!"
    echo "📊 Status: FAILURE"
    EXIT_CODE=1
fi

echo ""
echo "📄 Detailed report saved to: $REPORT_FILE"
echo "🎉 Test report generation completed with exit code: $EXIT_CODE"

exit $EXIT_CODE
