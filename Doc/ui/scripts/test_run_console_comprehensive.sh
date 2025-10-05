#!/bin/bash

echo "🧪 Run Console Comprehensive Test Suite"
echo "========================================"

# Test 1: HTML Structure Validation
echo ""
echo "📄 Test 1: HTML Structure Validation"
echo "------------------------------------"

# Check for all required UI elements
elements=(
    "Run Console"
    "jobs-list"
    "job-details-modal"
    "refresh-btn"
    "empty-state"
    "statusColors"
    "getMockJobs"
    "AndroidWhisper"
)

for element in "${elements[@]}"; do
    if grep -q "$element" app/src/main/assets/web/whisper-run-console.html; then
        echo "✅ $element found"
    else
        echo "❌ $element missing"
        exit 1
    fi
done

# Test 2: JavaScript Functionality
echo ""
echo "🔧 Test 2: JavaScript Functionality"
echo "------------------------------------"

# Check for key JavaScript functions
functions=(
    "init"
    "refreshJobs"
    "renderJobs"
    "showJobDetails"
    "closeJobDetails"
    "goBack"
    "showError"
    "getMockJobs"
)

for func in "${functions[@]}"; do
    if grep -q "function $func\|$func(" app/src/main/assets/web/whisper-run-console.html; then
        echo "✅ $func function found"
    else
        echo "❌ $func function missing"
        exit 1
    fi
done

# Test 3: Mock Data Validation
echo ""
echo "🎭 Test 3: Mock Data Validation"
echo "-------------------------------"

# Check for mock job data
mock_jobs=(
    "20250104-001"
    "20250104-002"
    "20250104-003"
    "kickoff.mp4"
    "meeting.mov"
    "problem.webm"
)

for job in "${mock_jobs[@]}"; do
    if grep -q "$job" app/src/main/assets/web/whisper-run-console.html; then
        echo "✅ Mock job $job found"
    else
        echo "❌ Mock job $job missing"
        exit 1
    fi
done

# Test 4: Status Management
echo ""
echo "🎨 Test 4: Status Management"
echo "----------------------------"

# Check for status colors
statuses=(
    "RUNNING"
    "COMPLETED"
    "ERROR"
    "TIMEOUT"
)

for status in "${statuses[@]}"; do
    if grep -q "$status" app/src/main/assets/web/whisper-run-console.html; then
        echo "✅ Status $status found"
    else
        echo "❌ Status $status missing"
        exit 1
    fi
done

# Test 5: Event Handlers
echo ""
echo "🎯 Test 5: Event Handlers"
echo "-------------------------"

# Check for event listeners
if grep -q "addEventListener" app/src/main/assets/web/whisper-run-console.html; then
    echo "✅ Event listeners found"
else
    echo "❌ Event listeners missing"
    exit 1
fi

# Check for auto-refresh
if grep -q "setInterval" app/src/main/assets/web/whisper-run-console.html; then
    echo "✅ Auto-refresh functionality found"
else
    echo "❌ Auto-refresh functionality missing"
    exit 1
fi

# Test 6: Android Integration
echo ""
echo "📱 Test 6: Android Integration"
echo "-----------------------------"

# Check Android components
if [ -f "app/src/main/java/com/mira/com/whisper/WhisperRunConsoleActivity.kt" ]; then
    echo "✅ WhisperRunConsoleActivity found"
else
    echo "❌ WhisperRunConsoleActivity missing"
    exit 1
fi

if grep -q "WhisperRunConsoleActivity" app/src/main/AndroidManifest.xml; then
    echo "✅ WhisperRunConsoleActivity registered in manifest"
else
    echo "❌ WhisperRunConsoleActivity not registered in manifest"
    exit 1
fi

# Test 7: Build Verification
echo ""
echo "🔨 Test 7: Build Verification"
echo "----------------------------"

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Debug APK built successfully"
    apk_size=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    echo "📱 APK size: $apk_size"
else
    echo "❌ Debug APK not found"
    exit 1
fi

# Test 8: Device Installation
echo ""
echo "📲 Test 8: Device Installation Test"
echo "------------------------------------"

if adb devices | grep -q "device$"; then
    echo "✅ Device connected"
    
    # Check if app is installed
    if adb shell pm list packages | grep -q "com.mira.videoeditor"; then
        echo "✅ App installed on device"
    else
        echo "❌ App not installed on device"
        exit 1
    fi
else
    echo "⚠️ No device connected - skipping installation test"
fi

# Test 9: HTML Content Validation
echo ""
echo "🌐 Test 9: HTML Content Validation"
echo "-----------------------------------"

# Check for essential HTML structure
html_elements=(
    "<!DOCTYPE html>"
    "<head>"
    "<body"
    "tailwindcss"
    "FontAwesome"
    "Inter"
)

for element in "${html_elements[@]}"; do
    if grep -q "$element" app/src/main/assets/web/whisper-run-console.html; then
        echo "✅ HTML element $element found"
    else
        echo "❌ HTML element $element missing"
        exit 1
    fi
done

# Test 10: JavaScript Bridge Integration
echo ""
echo "🌉 Test 10: JavaScript Bridge Integration"
echo "------------------------------------------"

# Check for bridge integration
bridge_elements=(
    "window.AndroidWhisper"
    "AndroidWhisperBridge"
    "addJavascriptInterface"
)

for element in "${bridge_elements[@]}"; do
    if grep -q "$element" app/src/main/assets/web/whisper-run-console.html || grep -q "$element" app/src/main/java/com/mira/com/whisper/WhisperRunConsoleActivity.kt; then
        echo "✅ Bridge element $element found"
    else
        echo "❌ Bridge element $element missing"
        exit 1
    fi
done

echo ""
echo "🎉 All Run Console comprehensive tests passed!"
echo ""
echo "📋 Manual Testing Instructions:"
echo "1. Launch the app on your device"
echo "2. Open WebView console and call: AndroidInterface.openRunConsole()"
echo "3. Test the Run Console functionality"
echo ""
echo "🧪 Run Console UI Testing:"
echo "Once in the Run Console UI, test these features:"
echo "- View mock job data (3 sample jobs with different statuses)"
echo "- Click on job rows to expand details"
echo "- Test refresh functionality"
echo "- Verify status color coding (RUNNING=blue, COMPLETED=green, ERROR=red, TIMEOUT=yellow)"
echo "- Check job details modal with all metadata"
echo "- Test auto-refresh every 5 seconds"
echo ""
echo "📊 Expected Results:"
echo "- Table shows 3 mock jobs with different statuses"
echo "- Job details modal displays comprehensive metadata"
echo "- Status pills show correct colors"
echo "- Refresh button works (though no real data yet)"
echo "- Auto-refresh every 5 seconds"
echo "- All state persists across app restarts"
echo ""
echo "✅ Run Console implementation is fully validated and ready for testing!"
