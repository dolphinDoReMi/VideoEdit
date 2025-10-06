#!/bin/bash

# Whisper Bridge Test Script
# Tests the bridge functionality using the console snippet

echo "🧪 Testing Whisper Step-1 Bridge Implementation"
echo "================================================"

# Check if the HTML file exists
if [ ! -f "assets/web/whisper-step1.html" ]; then
    echo "❌ Error: whisper-step1.html not found"
    exit 1
fi

echo "✅ Found whisper-step1.html"

# Check if the documentation exists
if [ ! -f "docs/whisper/BRIDGE.md" ]; then
    echo "❌ Error: BRIDGE.md documentation not found"
    exit 1
fi

echo "✅ Found BRIDGE.md documentation"

# Check if Android assets exist
if [ ! -f "app/src/main/assets/whisper/step1.html" ]; then
    echo "❌ Error: Android assets not found"
    exit 1
fi

echo "✅ Found Android assets"

# Check for bridge implementation in the HTML file
if ! grep -q "BroadcastChannel" assets/web/whisper-step1.html; then
    echo "❌ Error: BroadcastChannel implementation not found"
    exit 1
fi

echo "✅ Found BroadcastChannel implementation"

if ! grep -q "whisper.run" assets/web/whisper-step1.html; then
    echo "❌ Error: whisper.run event not found"
    exit 1
fi

echo "✅ Found whisper.run event handling"

if ! grep -q "whisper.ack" assets/web/whisper-step1.html; then
    echo "❌ Error: whisper.ack event not found"
    exit 1
fi

echo "✅ Found whisper.ack event handling"

if ! grep -q "whisper.progress" assets/web/whisper-step1.html; then
    echo "❌ Error: whisper.progress event not found"
    exit 1
fi

echo "✅ Found whisper.progress event handling"

if ! grep -q "whisper.done" assets/web/whisper-step1.html; then
    echo "❌ Error: whisper.done event not found"
    exit 1
fi

echo "✅ Found whisper.done event handling"

if ! grep -q "}, 800);" assets/web/whisper-step1.html; then
    echo "❌ Error: 800ms timeout not found"
    exit 1
fi

echo "✅ Found 800ms timeout fallback"

if ! grep -q "isDevBuild" assets/web/whisper-step1.html; then
    echo "❌ Error: Dev build detection not found"
    exit 1
fi

echo "✅ Found dev build detection"

echo ""
echo "🎉 All bridge implementation checks passed!"
echo ""
echo "📋 Test Instructions:"
echo "1. Open assets/web/whisper-step1.html?bridge=1 in a browser"
echo "2. Open DevTools Console"
echo "3. Run the test snippet from BRIDGE.md:"
echo ""
echo "   const ch = new BroadcastChannel('whisper-ui');"
echo "   ch.onmessage = (e) => {"
echo "     if (e.data?.type === 'whisper.run') {"
echo "       console.log('Host got whisper.run', e.data);"
echo "       ch.postMessage({ type: 'whisper.ack' });"
echo "       // ... rest of test snippet"
echo "     }"
echo "   };"
echo ""
echo "4. Select an audio file and click 'Run Transcription'"
echo "5. Verify: Host acknowledged run, progress updates, artifacts + Verify enabled"
echo ""
echo "✅ Bridge implementation is ready for testing!"
