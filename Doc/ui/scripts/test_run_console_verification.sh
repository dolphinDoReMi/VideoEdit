#!/bin/bash

echo "🧪 Whisper Run Console Implementation Verification"
echo "================================================="

# Test 1: Check required files
echo ""
echo "📁 Checking required files..."
if [ -f "app/src/main/assets/web/whisper-run-console.html" ]; then
    echo "✅ whisper-run-console.html found"
else
    echo "❌ whisper-run-console.html missing"
    exit 1
fi

if [ -f "app/src/main/java/com/mira/com/whisper/WhisperRunConsoleActivity.kt" ]; then
    echo "✅ WhisperRunConsoleActivity.kt found"
else
    echo "❌ WhisperRunConsoleActivity.kt missing"
    exit 1
fi

if [ -f "app/src/main/java/com/mira/com/whisper/AndroidWhisperBridge.kt" ]; then
    echo "✅ AndroidWhisperBridge.kt found"
else
    echo "❌ AndroidWhisperBridge.kt missing"
    exit 1
fi

# Test 2: Check AndroidManifest.xml
echo ""
echo "📱 Checking AndroidManifest.xml..."
if grep -q "WhisperRunConsoleActivity" app/src/main/AndroidManifest.xml; then
    echo "✅ WhisperRunConsoleActivity registered"
else
    echo "❌ WhisperRunConsoleActivity not registered"
    exit 1
fi

# Test 3: Check HTML content structure
echo ""
echo "🌐 Checking HTML content structure..."
if grep -q "Run Console" app/src/main/assets/web/whisper-run-console.html; then
    echo "✅ Run Console title found"
else
    echo "❌ Run Console title missing"
    exit 1
fi

if grep -q "jobs-list" app/src/main/assets/web/whisper-run-console.html; then
    echo "✅ Jobs list container found"
else
    echo "❌ Jobs list container missing"
    exit 1
fi

if grep -q "job-details-modal" app/src/main/assets/web/whisper-run-console.html; then
    echo "✅ Job details modal found"
else
    echo "❌ Job details modal missing"
    exit 1
fi

if grep -q "refreshJobs" app/src/main/assets/web/whisper-run-console.html; then
    echo "✅ Refresh functionality found"
else
    echo "❌ Refresh functionality missing"
    exit 1
fi

# Test 4: Check JavaScript functionality
echo ""
echo "🔧 Checking JavaScript functionality..."
if grep -q "AndroidWhisper" app/src/main/assets/web/whisper-run-console.html; then
    echo "✅ AndroidWhisper bridge integration found"
else
    echo "❌ AndroidWhisper bridge integration missing"
    exit 1
fi

if grep -q "getMockJobs" app/src/main/assets/web/whisper-run-console.html; then
    echo "✅ Mock data functionality found"
else
    echo "❌ Mock data functionality missing"
    exit 1
fi

if grep -q "statusColors" app/src/main/assets/web/whisper-run-console.html; then
    echo "✅ Status color mapping found"
else
    echo "❌ Status color mapping missing"
    exit 1
fi

# Test 5: Check MainActivity integration
echo ""
echo "🔗 Checking MainActivity integration..."
if grep -q "openRunConsole" app/src/main/java/com/mira/videoeditor/MainActivity.kt; then
    echo "✅ openRunConsole method found"
else
    echo "❌ openRunConsole method missing"
    exit 1
fi

# Test 6: Check build status
echo ""
echo "🔨 Checking build status..."
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Debug APK built successfully"
    apk_size=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    echo "📱 APK size: $apk_size"
else
    echo "❌ Debug APK not found"
    exit 1
fi

# Test 7: Check APK content
echo ""
echo "📦 Checking APK content..."
if unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -q "whisper-run-console.html"; then
    echo "✅ whisper-run-console.html found in APK"
else
    echo "❌ whisper-run-console.html not found in APK"
    exit 1
fi

# Test 8: Check device installation
echo ""
echo "📲 Checking device installation..."
if adb devices | grep -q "device$"; then
    echo "✅ Device connected"
    
    if adb shell pm list packages | grep -q "com.mira.videoeditor"; then
        echo "✅ App installed on device"
    else
        echo "❌ App not installed on device"
        exit 1
    fi
else
    echo "⚠️ No device connected - skipping installation test"
fi

echo ""
echo "🎉 All Run Console verification checks passed!"
echo ""
echo "📋 Testing Instructions:"
echo "1. Launch the app on your device"
echo "2. Open WebView console and call: AndroidInterface.openRunConsole()"
echo "3. Test the Run Console functionality"
echo ""
echo "🧪 Run Console Testing:"
echo "Once in the Run Console UI, test these features:"
echo "- View mock job data (3 sample jobs with different statuses)"
echo "- Click on job rows to expand details"
echo "- Test refresh functionality"
echo "- Verify status color coding (RUNNING=blue, COMPLETED=green, ERROR=red, TIMEOUT=yellow)"
echo "- Check job details modal with all metadata"
echo ""
echo "📊 Expected Results:"
echo "- Table shows 3 mock jobs with different statuses"
echo "- Job details modal displays comprehensive metadata"
echo "- Status pills show correct colors"
echo "- Refresh button works (though no real data yet)"
echo "- Auto-refresh every 5 seconds"
echo ""
echo "🔧 Integration Notes:"
echo "- Uses AndroidWhisper bridge for real job data"
echo "- Falls back to mock data when bridge unavailable"
echo "- Ready for integration with actual Whisper service"
echo "- Observes /sdcard/MiraWhisper/out/ directory"
echo ""
echo "✅ Run Console implementation is ready for testing!"
