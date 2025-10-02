#!/bin/bash
# Jobs status monitoring script
# Checks the health of background jobs and storage

set -e

echo "=== CLIP4Clip Jobs & Storage Health Check ==="

# Configuration
PKG=${PKG:-"com.mira.videoeditor"}
VARIANT=${VARIANT:-"clip_vit_b32_mean_v1"}
DEVICE_ID=${DEVICE_ID:-""}

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    echo "Please connect a device and enable USB debugging"
    exit 1
fi

echo "✅ Android device connected"

# Get device info
DEVICE_MODEL=$(adb shell getprop ro.product.model 2>/dev/null || echo "Unknown")
DEVICE_API=$(adb shell getprop ro.build.version.sdk 2>/dev/null || echo "Unknown")
echo "📱 Device: $DEVICE_MODEL (API $DEVICE_API)"

# Check if app is installed
if ! adb shell pm list packages | grep -q "$PKG"; then
    echo "❌ App $PKG not installed"
    echo "Please install the app first"
    exit 1
fi

echo "✅ App $PKG is installed"

# Check WorkManager jobs
echo ""
echo "🔍 Checking WorkManager jobs..."
WORK_INFO=$(adb shell "dumpsys jobscheduler | grep -A 10 -B 5 '$PKG'" 2>/dev/null || echo "")
if [ -n "$WORK_INFO" ]; then
    echo "📋 Active WorkManager jobs:"
    echo "$WORK_INFO"
else
    echo "ℹ️  No active WorkManager jobs found"
fi

# Check database status
echo ""
echo "🔍 Checking database status..."
DB_INFO=$(adb shell "run-as $PKG ls -la databases/" 2>/dev/null || echo "")
if [ -n "$DB_INFO" ]; then
    echo "📊 Database files:"
    echo "$DB_INFO"
    
    # Get database size
    DB_SIZE=$(adb shell "run-as $PKG du -sh databases/" 2>/dev/null || echo "Unknown")
    echo "💾 Database size: $DB_SIZE"
else
    echo "ℹ️  No database files found"
fi

# Check embedding counts
echo ""
echo "🔍 Checking embedding counts..."
EMBEDDING_COUNT=$(adb shell "run-as $PKG sqlite3 databases/app_database 'SELECT COUNT(*) FROM embeddings WHERE variant=\"$VARIANT\";'" 2>/dev/null || echo "0")
echo "📈 Embeddings count: $EMBEDDING_COUNT"

VIDEO_COUNT=$(adb shell "run-as $PKG sqlite3 databases/app_database 'SELECT COUNT(*) FROM videos;'" 2>/dev/null || echo "0")
echo "🎬 Videos count: $VIDEO_COUNT"

SHOT_COUNT=$(adb shell "run-as $PKG sqlite3 databases/app_database 'SELECT COUNT(*) FROM shots;'" 2>/dev/null || echo "0")
echo "🎞️  Shots count: $SHOT_COUNT"

# Check storage space
echo ""
echo "🔍 Checking storage space..."
STORAGE_INFO=$(adb shell df /data 2>/dev/null | tail -1 || echo "")
if [ -n "$STORAGE_INFO" ]; then
    echo "💾 Storage info: $STORAGE_INFO"
else
    echo "ℹ️  Storage info not available"
fi

# Check app memory usage
echo ""
echo "🔍 Checking app memory usage..."
MEMORY_INFO=$(adb shell dumpsys meminfo "$PKG" 2>/dev/null | head -20 || echo "")
if [ -n "$MEMORY_INFO" ]; then
    echo "🧠 Memory usage:"
    echo "$MEMORY_INFO"
else
    echo "ℹ️  Memory info not available"
fi

# Check for errors in logcat
echo ""
echo "🔍 Checking for recent errors..."
ERROR_COUNT=$(adb logcat -d -s "AndroidRuntime:E" | grep "$PKG" | wc -l 2>/dev/null || echo "0")
if [ "$ERROR_COUNT" -gt 0 ]; then
    echo "⚠️  Found $ERROR_COUNT recent errors in logcat"
    echo "Recent errors:"
    adb logcat -d -s "AndroidRuntime:E" | grep "$PKG" | tail -5
else
    echo "✅ No recent errors found"
fi

# Performance metrics
echo ""
echo "🔍 Checking performance metrics..."
PERF_INFO=$(adb shell "dumpsys activity $PKG | grep -A 5 -B 5 'Total time'" 2>/dev/null || echo "")
if [ -n "$PERF_INFO" ]; then
    echo "⚡ Performance info:"
    echo "$PERF_INFO"
else
    echo "ℹ️  Performance info not available"
fi

# Exit code policy
echo ""
echo "=== Health Check Summary ==="

# Determine exit code based on health status
EXIT_CODE=0

if [ "$ERROR_COUNT" -gt 10 ]; then
    echo "❌ Too many errors ($ERROR_COUNT)"
    EXIT_CODE=1
fi

if [ "$EMBEDDING_COUNT" -eq 0 ]; then
    echo "⚠️  No embeddings found"
    EXIT_CODE=2
fi

if [ "$VIDEO_COUNT" -eq 0 ]; then
    echo "⚠️  No videos found"
    EXIT_CODE=2
fi

if [ "$EXIT_CODE" -eq 0 ]; then
    echo "✅ All health checks passed"
    echo "📊 Status: Healthy"
elif [ "$EXIT_CODE" -eq 1 ]; then
    echo "❌ Critical issues detected"
    echo "📊 Status: Unhealthy"
else
    echo "⚠️  Minor issues detected"
    echo "📊 Status: Warning"
fi

echo ""
echo "🎉 Health check completed with exit code: $EXIT_CODE"
exit $EXIT_CODE
