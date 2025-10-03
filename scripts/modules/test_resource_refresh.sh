#!/bin/bash

# Test script to verify resource monitoring refresh functionality
# This script will help debug why resource updates aren't refreshing

echo "🔍 Testing Resource Monitoring Refresh"
echo "======================================"

# Check if the app is running
DEVICE="050C188041A00540"
PACKAGE="com.mira.videoeditor.debug"

echo "📱 Checking if app is running..."
APP_RUNNING=$(adb -s $DEVICE shell "pidof $PACKAGE" | tr -d '\r')
if [ -z "$APP_RUNNING" ]; then
    echo "❌ App is not running. Please start the app first."
    exit 1
else
    echo "✅ App is running (PID: $APP_RUNNING)"
fi

echo ""
echo "📊 Testing resource data collection..."

# Test memory monitoring
echo "Memory Usage:"
MEMORY=$(adb -s $DEVICE shell "dumpsys meminfo $PACKAGE | grep 'TOTAL PSS:'" | awk '{print $3}')
if [ ! -z "$MEMORY" ]; then
    MEMORY_GB=$(echo "$MEMORY" | awk '{printf "%.2f", $1/1048576}')
    echo "✅ Memory: ${MEMORY_GB}GB"
else
    echo "❌ Memory: Failed to get data"
fi

# Test CPU monitoring
echo "CPU Usage:"
CPU=$(adb -s $DEVICE shell "dumpsys cpuinfo | grep $PACKAGE | head -n1" | awk '{print $1}' | tr -d '%')
if [ ! -z "$CPU" ]; then
    echo "✅ CPU: ${CPU}%"
else
    echo "❌ CPU: Failed to get data"
fi

# Test battery monitoring
echo "Battery Level:"
BATTERY=$(adb -s $DEVICE shell "dumpsys battery | grep level" | awk '{print $2}')
if [ ! -z "$BATTERY" ]; then
    echo "✅ Battery: ${BATTERY}%"
else
    echo "❌ Battery: Failed to get data"
fi

# Test temperature monitoring
echo "Temperature:"
TEMP=$(adb -s $DEVICE shell "cat /sys/class/thermal/thermal_zone0/temp" 2>/dev/null)
if [ ! -z "$TEMP" ]; then
    TEMP_C=$(echo "$TEMP" | awk '{printf "%.1f", $1/1000}')
    echo "✅ Temperature: ${TEMP_C}°C"
else
    echo "❌ Temperature: Failed to get data"
fi

echo ""
echo "🔍 Checking WebView JavaScript interface..."

# Check if the updateRealResourceUsage function exists
echo "Testing JavaScript interface..."
adb -s $DEVICE shell "am start -n $PACKAGE/.MainActivity"
sleep 3

# Test the JavaScript function call
echo "Attempting to call updateRealResourceUsage function..."
adb -s $DEVICE shell "am broadcast -a android.intent.action.MEDIA_BUTTON --es 'test' 'resource_update'"

echo ""
echo "📝 Debugging Steps:"
echo "1. Check Android logs for ResourceMonitor messages:"
echo "   adb -s $DEVICE logcat | grep ResourceMonitor"
echo ""
echo "2. Check WebView console logs:"
echo "   adb -s $DEVICE shell 'am start -n $PACKAGE/.MainActivity'"
echo ""
echo "3. Monitor resource updates in real-time:"
echo "   watch -n 2 'adb -s $DEVICE shell \"dumpsys meminfo $PACKAGE | grep TOTAL\"'"
echo ""
echo "✅ Test completed. Check the logs above for any errors."
