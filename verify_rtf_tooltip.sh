#!/bin/bash

echo "🔍 RTF Tooltip Verification Test"
echo "================================="
echo ""

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No device connected"
    exit 1
fi

echo "✅ Device connected: $(adb devices | grep 'device$' | head -1 | cut -f1)"
echo ""

# Launch the results page
echo "🚀 Launching Whisper Results page..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperResultsActivity

echo "⏳ Waiting for page to load..."
sleep 3

# Take screenshot
echo "📸 Taking screenshot..."
adb shell screencap -p /sdcard/rtf_tooltip_verification.png
adb pull /sdcard/rtf_tooltip_verification.png

echo "✅ Screenshot saved as rtf_tooltip_verification.png"
echo ""

# Check if the HTML file contains the tooltip
echo "🔍 Verifying RTF tooltip implementation..."
if grep -q "RTF = Real-Time Factor" app/src/main/assets/web/whisper_results.html; then
    echo "✅ RTF tooltip HTML found in whisper_results.html"
else
    echo "❌ RTF tooltip HTML not found"
fi

if grep -q "updateRTFTooltip" app/src/main/assets/web/whisper_results.html; then
    echo "✅ RTF tooltip JavaScript function found"
else
    echo "❌ RTF tooltip JavaScript function not found"
fi

if grep -q "cursor-help" app/src/main/assets/web/whisper_results.html; then
    echo "✅ RTF hover cursor styling found"
else
    echo "❌ RTF hover cursor styling not found"
fi

echo ""
echo "🎯 Manual Testing Instructions:"
echo "1. Look at the screenshot: rtf_tooltip_verification.png"
echo "2. On the device, navigate to the RTF value (should show 0.45)"
echo "3. Hover over or tap the RTF value"
echo "4. Verify the comprehensive tooltip appears with:"
echo "   - Definition: RTF = Real-Time Factor"
echo "   - Formula: RTF = processing time / audio duration"
echo "   - Speed interpretation with color coding"
echo "   - Performance targets"
echo "   - Current speedup calculation"
echo ""
echo "✅ RTF Tooltip verification complete!"
