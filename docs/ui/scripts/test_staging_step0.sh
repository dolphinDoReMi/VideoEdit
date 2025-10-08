#!/bin/bash

# Staging Step 0 Test Script
# Tests the staging functionality implementation

echo "🧪 Testing Staging Step 0 Implementation"
echo "=========================================="

# Check if the HTML file exists
if [ ! -f "assets/web/whisper-step1.html" ]; then
    echo "❌ Error: whisper-step1.html not found"
    exit 1
fi

echo "✅ Found whisper-step1.html"

# Check for staging section
if ! grep -q "Staging — Select & Inspect" assets/web/whisper-step1.html; then
    echo "❌ Error: Staging section not found"
    exit 1
fi

echo "✅ Found Staging section"

# Check for staging JavaScript module
if ! grep -q "STAGING (Step 0)" assets/web/whisper-step1.html; then
    echo "❌ Error: Staging JavaScript module not found"
    exit 1
fi

echo "✅ Found Staging JavaScript module"

# Check for bridge contract
if ! grep -q "window.StagingBridge" assets/web/whisper-step1.html; then
    echo "❌ Error: StagingBridge contract not found"
    exit 1
fi

echo "✅ Found StagingBridge contract"

# Check for mock bridge
if ! grep -q "mockBridge" assets/web/whisper-step1.html; then
    echo "❌ Error: Mock bridge not found"
    exit 1
fi

echo "✅ Found mock bridge implementation"

# Check for Android bridge class
if [ ! -f "app/src/main/java/com/mira/com/ui/web/StagingJsBridge.kt" ]; then
    echo "❌ Error: StagingJsBridge.kt not found"
    exit 1
fi

echo "✅ Found StagingJsBridge.kt"

# Check for key bridge methods
if ! grep -q "@JavascriptInterface fun addTree" app/src/main/java/com/mira/com/ui/web/StagingJsBridge.kt; then
    echo "❌ Error: addTree method not found"
    exit 1
fi

echo "✅ Found addTree method"

if ! grep -q "@JavascriptInterface fun enumerateAll" app/src/main/java/com/mira/com/ui/web/StagingJsBridge.kt; then
    echo "❌ Error: enumerateAll method not found"
    exit 1
fi

echo "✅ Found enumerateAll method"

if ! grep -q "@JavascriptInterface fun probe" app/src/main/java/com/mira/com/ui/web/StagingJsBridge.kt; then
    echo "❌ Error: probe method not found"
    exit 1
fi

echo "✅ Found probe method"

echo ""
echo "🎉 All staging implementation checks passed!"
echo ""
echo "📋 Test Instructions:"
echo "1. Open assets/web/whisper-step1.html in a browser"
echo "2. Scroll down to the 'Staging — Select & Inspect' section"
echo "3. Click 'Add Folder' to test mock folder addition"
echo "4. Verify mock videos appear in the table"
echo "5. Test filtering by extension and duration"
echo "6. Test individual file probing"
echo "7. Test selection and summary updates"
echo ""
echo "🔌 Android Integration:"
echo "1. Add StagingJsBridge to your Activity:"
echo "   webView.addJavascriptInterface(StagingJsBridge(this), \"StagingBridge\")"
echo "2. Test with real SAF folder selection"
echo "3. Verify persisted permissions work"
echo ""
echo "✅ Staging Step 0 implementation is ready for testing!"
