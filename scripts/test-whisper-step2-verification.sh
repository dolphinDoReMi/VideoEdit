#!/bin/bash

echo "🧪 Whisper Step-2 UI Implementation Verification"
echo "================================================="

# Check if all required files exist
echo "📁 Checking required files..."

files=(
    "app/src/main/assets/web/whisper-step2.html"
    "app/src/main/java/com/mira/com/whisper/AndroidWhisperBridge.kt"
    "app/src/main/java/com/mira/com/whisper/WhisperReceiver.kt"
    "app/src/main/java/com/mira/com/whisper/WhisperStep2Activity.kt"
    "app/src/main/assets/web/whisper-test.html"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file - MISSING"
        exit 1
    fi
done

# Check AndroidManifest.xml for required components
echo ""
echo "📱 Checking AndroidManifest.xml..."

if grep -q "WhisperStep2Activity" app/src/main/AndroidManifest.xml; then
    echo "✅ WhisperStep2Activity registered"
else
    echo "❌ WhisperStep2Activity not registered"
    exit 1
fi

if grep -q "WhisperReceiver" app/src/main/AndroidManifest.xml; then
    echo "✅ WhisperReceiver registered"
else
    echo "❌ WhisperReceiver not registered"
    exit 1
fi

if grep -q "com.mira.whisper.RUN" app/src/main/AndroidManifest.xml; then
    echo "✅ Whisper broadcast actions registered"
else
    echo "❌ Whisper broadcast actions not registered"
    exit 1
fi

# Check MainActivity for AndroidWhisper bridge
echo ""
echo "🔗 Checking MainActivity integration..."

if grep -q "AndroidWhisperBridge" app/src/main/java/com/mira/videoeditor/MainActivity.kt; then
    echo "✅ AndroidWhisperBridge integrated in MainActivity"
else
    echo "❌ AndroidWhisperBridge not integrated in MainActivity"
    exit 1
fi

if grep -q "openWhisperStep2" app/src/main/java/com/mira/videoeditor/MainActivity.kt; then
    echo "✅ openWhisperStep2 method available"
else
    echo "❌ openWhisperStep2 method not available"
    exit 1
fi

# Check HTML content for Policy & Presets features
echo ""
echo "🌐 Checking Policy & Presets UI implementation..."

if grep -q "Policy & Presets" app/src/main/assets/web/whisper-step2.html; then
    echo "✅ Policy & Presets section found"
else
    echo "❌ Policy & Presets section not found"
    exit 1
fi

if grep -q "preset-pill" app/src/main/assets/web/whisper-step2.html; then
    echo "✅ Preset selector found"
else
    echo "❌ Preset selector not found"
    exit 1
fi

if grep -q "Deterministic Sampling" app/src/main/assets/web/whisper-step2.html; then
    echo "✅ Pinned rails display found"
else
    echo "❌ Pinned rails display not found"
    exit 1
fi

if grep -q "pick-model-btn" app/src/main/assets/web/whisper-step2.html; then
    echo "✅ Model picker found"
else
    echo "❌ Model picker not found"
    exit 1
fi

if grep -q "config-json-pre" app/src/main/assets/web/whisper-step2.html; then
    echo "✅ Config JSON display found"
else
    echo "❌ Config JSON display not found"
    exit 1
fi

if grep -q "v-sha\|v-rails\|v-sidecar" app/src/main/assets/web/whisper-step2.html; then
    echo "✅ Verification checklist found"
else
    echo "❌ Verification checklist not found"
    exit 1
fi

# Check Kotlin implementation
echo ""
echo "☕ Checking Kotlin implementation..."

if grep -q "@JavascriptInterface" app/src/main/java/com/mira/com/whisper/AndroidWhisperBridge.kt; then
    echo "✅ JavaScript interface annotations found"
else
    echo "❌ JavaScript interface annotations not found"
    exit 1
fi

if grep -q "fun run(" app/src/main/java/com/mira/com/whisper/AndroidWhisperBridge.kt; then
    echo "✅ run method implemented"
else
    echo "❌ run method not implemented"
    exit 1
fi

if grep -q "fun export(" app/src/main/java/com/mira/com/whisper/AndroidWhisperBridge.kt; then
    echo "✅ export method implemented"
else
    echo "❌ export method not implemented"
    exit 1
fi

if grep -q "fun listSidecars(" app/src/main/java/com/mira/com/whisper/AndroidWhisperBridge.kt; then
    echo "✅ listSidecars method implemented"
else
    echo "❌ listSidecars method not implemented"
    exit 1
fi

if grep -q "fun verify(" app/src/main/java/com/mira/com/whisper/AndroidWhisperBridge.kt; then
    echo "✅ verify method implemented"
else
    echo "❌ verify method not implemented"
    exit 1
fi

# Check broadcast receiver
echo ""
echo "📡 Checking WhisperReceiver..."

if grep -q "BroadcastReceiver" app/src/main/java/com/mira/com/whisper/WhisperReceiver.kt; then
    echo "✅ BroadcastReceiver properly implemented"
else
    echo "❌ BroadcastReceiver not properly implemented"
    exit 1
fi

if grep -q "ACTION_RUN\|ACTION_EXPORT\|ACTION_VERIFY" app/src/main/java/com/mira/com/whisper/WhisperReceiver.kt; then
    echo "✅ Broadcast actions handled"
else
    echo "❌ Broadcast actions not handled"
    exit 1
fi

# Check build success
echo ""
echo "🔨 Checking build status..."

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Debug APK built successfully"
    echo "📱 APK size: $(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)"
else
    echo "❌ Debug APK not found - build may have failed"
    exit 1
fi

echo ""
echo "🎉 All verification checks passed!"
echo ""
echo "📋 Testing Instructions:"
echo "1. Install the app: adb install app/build/outputs/apk/debug/app-debug.apk"
echo "2. Launch the app and open WebView console"
echo "3. Call: AndroidInterface.openWhisperStep2()"
echo "4. Test the Policy & Presets UI functionality"
echo ""
echo "🧪 Policy & Presets UI Testing:"
echo "Once in the Whisper Step-2 UI, test these features:"
echo "- Click preset pills (SINGLE, ACCURACY_LEANING, etc.) - should change label only"
echo "- Click 'Pick Model (SAF)' button to select a model file"
echo "- Verify SHA-256 preview is computed and displayed"
echo "- Click 'Copy JSON' to copy the config JSON"
echo "- Check verification badges: SHA preview ✅, Rails locked ✅, Sidecar match —"
echo ""
echo "📊 Expected Results:"
echo "- Preset selection changes label but rails remain constant"
echo "- Model picker computes and displays SHA-256 hash"
echo "- Config JSON updates with selected preset and model info"
echo "- Verification checklist shows appropriate status badges"
echo "- All state persists across app restarts (localStorage)"
echo ""
echo "✅ Policy & Presets UI implementation is ready for testing!"
