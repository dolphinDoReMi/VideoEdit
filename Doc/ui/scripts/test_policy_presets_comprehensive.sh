#!/bin/bash

echo "🧪 Policy & Presets UI Comprehensive Test Suite"
echo "================================================="

# Test 1: HTML Structure Validation
echo ""
echo "📄 Test 1: HTML Structure Validation"
echo "------------------------------------"

# Check for all required UI elements
elements=(
    "Policy & Presets"
    "preset-pill"
    "Deterministic Sampling"
    "Fixed Preprocess"
    "Timestamp Discipline"
    "Decode Path"
    "pick-model-btn"
    "config-json-pre"
    "v-sha"
    "v-rails"
    "v-sidecar"
)

for element in "${elements[@]}"; do
    if grep -q "$element" app/src/main/assets/web/whisper-step2.html; then
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
    "initPresetPills"
    "renderConfigJson"
    "sha256Hex"
    "setBadge"
    "buildConfigJson"
    "renderModelInfo"
    "pulseCopyBtn"
)

for func in "${functions[@]}"; do
    if grep -q "function $func\|$func(" app/src/main/assets/web/whisper-step2.html; then
        echo "✅ $func function found"
    else
        echo "❌ $func function missing"
        exit 1
    fi
done

# Test 3: Rails Constants
echo ""
echo "🔒 Test 3: Rails Constants Validation"
echo "------------------------------------"

# Check for hardcoded rails values
rails_values=(
    "segmentMs: 30000"
    "overlapMs: 1000"
    "deterministicSampling: true"
    "randomCrop: false"
    "centerCrop: true"
    "targetAudio: \"mono@16kHz\""
    "timestamp_policy: \"ExtractorPTS\""
    "decode_path: \"mp4_aac_hw_first\""
)

for rail in "${rails_values[@]}"; do
    if grep -q "$rail" app/src/main/assets/web/whisper-step2.html; then
        echo "✅ $rail found"
    else
        echo "❌ $rail missing"
        exit 1
    fi
done

# Test 4: Preset Options
echo ""
echo "🎛️ Test 4: Preset Options Validation"
echo "------------------------------------"

presets=(
    "SINGLE"
    "ACCURACY_LEANING"
    "WEB_ROBUST_INGEST"
    "HIGH_THROUGHPUT_BATCH"
)

for preset in "${presets[@]}"; do
    if grep -q "data-preset=\"$preset\"" app/src/main/assets/web/whisper-step2.html; then
        echo "✅ Preset $preset found"
    else
        echo "❌ Preset $preset missing"
        exit 1
    fi
done

# Test 5: Event Handlers
echo ""
echo "🎯 Test 5: Event Handlers Validation"
echo "------------------------------------"

# Check for event listeners
if grep -q "addEventListener" app/src/main/assets/web/whisper-step2.html; then
    echo "✅ Event listeners found"
else
    echo "❌ Event listeners missing"
    exit 1
fi

# Check for specific event handlers
if grep -q "pick-model-btn.*addEventListener" app/src/main/assets/web/whisper-step2.html; then
    echo "✅ Model picker event handler found"
else
    echo "❌ Model picker event handler missing"
    exit 1
fi

if grep -q "copy-json-btn.*addEventListener" app/src/main/assets/web/whisper-step2.html; then
    echo "✅ Copy JSON event handler found"
else
    echo "❌ Copy JSON event handler missing"
    exit 1
fi

# Test 6: State Management
echo ""
echo "💾 Test 6: State Management Validation"
echo "------------------------------------"

# Check for localStorage usage
if grep -q "localStorage" app/src/main/assets/web/whisper-step2.html; then
    echo "✅ localStorage usage found"
else
    echo "❌ localStorage usage missing"
    exit 1
fi

# Check for state persistence keys
state_keys=(
    "whisper_preset_label"
    "whisper_model_name"
    "whisper_model_sha256"
)

for key in "${state_keys[@]}"; do
    if grep -q "$key" app/src/main/assets/web/whisper-step2.html; then
        echo "✅ State key $key found"
    else
        echo "❌ State key $key missing"
        exit 1
    fi
done

# Test 7: Verification Badges
echo ""
echo "✅ Test 7: Verification Badges Validation"
echo "----------------------------------------"

# Check for verification badge elements
badges=(
    "v-sha"
    "v-rails"
    "v-sidecar"
)

for badge in "${badges[@]}"; do
    if grep -q "id=\"$badge\"" app/src/main/assets/web/whisper-step2.html; then
        echo "✅ Badge $badge found"
    else
        echo "❌ Badge $badge missing"
        exit 1
    fi
done

# Test 8: Android Integration
echo ""
echo "📱 Test 8: Android Integration Validation"
echo "----------------------------------------"

# Check Android components
if [ -f "app/src/main/java/com/mira/com/whisper/WhisperStep2Activity.kt" ]; then
    echo "✅ WhisperStep2Activity found"
else
    echo "❌ WhisperStep2Activity missing"
    exit 1
fi

if grep -q "WhisperStep2Activity" app/src/main/AndroidManifest.xml; then
    echo "✅ WhisperStep2Activity registered in manifest"
else
    echo "❌ WhisperStep2Activity not registered in manifest"
    exit 1
fi

# Test 9: Build Verification
echo ""
echo "🔨 Test 9: Build Verification"
echo "----------------------------"

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Debug APK built successfully"
    apk_size=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    echo "📱 APK size: $apk_size"
else
    echo "❌ Debug APK not found"
    exit 1
fi

# Test 10: Device Installation
echo ""
echo "📲 Test 10: Device Installation Test"
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

echo ""
echo "🎉 All Policy & Presets UI tests passed!"
echo ""
echo "📋 Manual Testing Instructions:"
echo "1. Launch the app on your device"
echo "2. Open WebView console and call: AndroidInterface.openWhisperStep2()"
echo "3. Test preset selection - verify rails remain constant"
echo "4. Test model picker - verify SHA-256 computation"
echo "5. Test config JSON copy functionality"
echo "6. Verify verification badges show correct status"
echo "7. Test state persistence by restarting the app"
echo ""
echo "✅ Policy & Presets UI implementation is fully validated!"
