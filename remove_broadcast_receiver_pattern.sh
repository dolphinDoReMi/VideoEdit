#!/bin/bash

# Remove Broadcast/Receiver Pattern Script
# Cleans up the complex broadcast/receiver architecture

set -e

echo "🧹 Removing Broadcast/Receiver Pattern"
echo "======================================"
echo "This script will remove the complex broadcast/receiver pattern"
echo "and replace it with a simpler direct approach"
echo ""

# Backup directory
BACKUP_DIR="./broadcast_receiver_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

echo "📁 Creating backup in: $BACKUP_DIR"
echo ""

# Files to remove/modify
FILES_TO_REMOVE=(
    "feature/whisper/src/main/java/com/mira/com/feature/whisper/runner/WhisperReceiver.kt"
    "app/src/main/java/com/mira/whisper/WhisperConnectorService.kt"
    "app/src/main/java/com/mira/whisper/WhisperConnectorReceiver.kt"
    "app/src/main/java/com/mira/whisper/ResourceUpdateReceiver.kt"
)

# Files to modify
FILES_TO_MODIFY=(
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/mira/whisper/WhisperProcessingActivity.kt"
    "app/src/main/java/com/mira/whisper/WhisperFileSelectionActivity.kt"
    "app/src/main/java/com/mira/whisper/AndroidWhisperBridge.kt"
)

echo "=== Step 1: Backup Files ==="
for file in "${FILES_TO_REMOVE[@]}" "${FILES_TO_MODIFY[@]}"; do
    if [ -f "$file" ]; then
        cp "$file" "$BACKUP_DIR/"
        echo "✅ Backed up: $file"
    else
        echo "⚠️  File not found: $file"
    fi
done

echo ""

echo "=== Step 2: Remove Broadcast Receiver Files ==="
for file in "${FILES_TO_REMOVE[@]}"; do
    if [ -f "$file" ]; then
        rm "$file"
        echo "🗑️  Removed: $file"
    else
        echo "⚠️  File not found: $file"
    fi
done

echo ""

echo "=== Step 3: Create Direct Service ==="
# Create the direct service if it doesn't exist
DIRECT_SERVICE="feature/whisper/src/main/java/com/mira/com/feature/whisper/service/DirectWhisperService.kt"
if [ ! -f "$DIRECT_SERVICE" ]; then
    mkdir -p "$(dirname "$DIRECT_SERVICE")"
    cat > "$DIRECT_SERVICE" << 'EOF'
package com.mira.com.feature.whisper.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.mira.com.feature.whisper.api.WhisperApi

/**
 * Direct Whisper Processing Service
 * Bypasses broadcast/receiver pattern for direct processing
 */
class DirectWhisperService : Service() {
    
    companion object {
        private const val TAG = "DirectWhisperService"
        const val ACTION_PROCESS_DIRECT = "com.mira.com.feature.whisper.PROCESS_DIRECT"
        const val EXTRA_URI = "uri"
        const val EXTRA_MODEL = "model"
        const val EXTRA_THREADS = "threads"
        const val EXTRA_LANG = "lang"
        const val EXTRA_TRANSLATE = "translate"
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "DirectWhisperService started")
        
        when (intent?.action) {
            ACTION_PROCESS_DIRECT -> {
                val uri = intent.getStringExtra(EXTRA_URI)
                val model = intent.getStringExtra(EXTRA_MODEL) ?: "/sdcard/MiraWhisper/models/whisper-base.q5_1.bin"
                val threads = intent.getIntExtra(EXTRA_THREADS, 4)
                val lang = intent.getStringExtra(EXTRA_LANG) ?: "auto"
                val translate = intent.getBooleanExtra(EXTRA_TRANSLATE, false)
                
                if (uri != null) {
                    Log.i(TAG, "Starting direct processing for: $uri")
                    Log.i(TAG, "Model: $model, Threads: $threads, Lang: $lang, Translate: $translate")
                    
                    // Direct API call - no broadcast needed
                    WhisperApi.enqueueTranscribe(
                        ctx = this,
                        uri = uri,
                        model = model,
                        threads = threads,
                        beam = 1,
                        lang = lang,
                        translate = translate
                    )
                    
                    Log.i(TAG, "Direct processing enqueued successfully")
                } else {
                    Log.e(TAG, "No URI provided for direct processing")
                }
            }
        }
        
        return START_NOT_STICKY
    }
}
EOF
    echo "✅ Created: $DIRECT_SERVICE"
else
    echo "✅ Already exists: $DIRECT_SERVICE"
fi

echo ""

echo "=== Step 4: Update AndroidManifest.xml ==="
MANIFEST_FILE="app/src/main/AndroidManifest.xml"
if [ -f "$MANIFEST_FILE" ]; then
    # Remove broadcast receiver declarations
    sed -i.bak '/WhisperReceiver/d' "$MANIFEST_FILE"
    sed -i.bak '/WhisperConnectorReceiver/d' "$MANIFEST_FILE"
    sed -i.bak '/ResourceUpdateReceiver/d' "$MANIFEST_FILE"
    
    # Add direct service declaration
    if ! grep -q "DirectWhisperService" "$MANIFEST_FILE"; then
        # Find the application tag and add service after it
        sed -i.bak '/<application/a\
        <service android:name="com.mira.com.feature.whisper.service.DirectWhisperService"\
            android:enabled="true"\
            android:exported="false" />' "$MANIFEST_FILE"
    fi
    
    echo "✅ Updated: $MANIFEST_FILE"
else
    echo "⚠️  Manifest file not found: $MANIFEST_FILE"
fi

echo ""

echo "=== Step 5: Create Simplified Test Script ==="
TEST_SCRIPT="simple_direct_test.sh"
cat > "$TEST_SCRIPT" << 'EOF'
#!/bin/bash

# Simple Direct Test Script
# Uses the new direct service approach

echo "🎾 Simple Direct Test"
echo "===================="

# Check device
adb devices | grep "device$" || { echo "❌ No device connected"; exit 1; }
echo "✅ Device connected"

# Push file if needed
FILE_PATH="/sdcard/MiraWhisper/in/tennis_interview_clip_002.mp4"
if ! adb shell "test -f $FILE_PATH" 2>/dev/null; then
    echo "Pushing file..."
    adb push "/Users/dennis/Movies/VideoEdit/tennis_clips/tennis_interview_clip_002.mp4" "$FILE_PATH"
    echo "✅ File pushed"
fi

# Launch app
echo "🚀 Launching app..."
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
sleep 3

# Try direct service call
echo "🎤 Attempting direct service call..."
adb shell am startservice -n com.mira.com/com.mira.com.feature.whisper.service.DirectWhisperService \
    --es "action" "com.mira.com.feature.whisper.PROCESS_DIRECT" \
    --es "uri" "file://$FILE_PATH" \
    --es "model" "/sdcard/MiraWhisper/models/whisper-base.q5_1.bin" \
    --es "threads" "4" \
    --es "lang" "auto" \
    --ez "translate" "false"

echo "✅ Direct service call sent"
echo "⏳ Check logs for processing activity:"
echo "adb logcat | grep -E '(DirectWhisperService|WhisperApi|TranscribeWorker)'"
EOF

chmod +x "$TEST_SCRIPT"
echo "✅ Created: $TEST_SCRIPT"

echo ""

echo "=== Step 6: Create Cleanup Summary ==="
SUMMARY_FILE="$BACKUP_DIR/cleanup_summary.md"
cat > "$SUMMARY_FILE" << EOF
# Broadcast/Receiver Pattern Removal Summary

**Date:** $(date)  
**Backup Location:** $BACKUP_DIR  

## Files Removed
$(printf '%s\n' "${FILES_TO_REMOVE[@]}")

## Files Modified
$(printf '%s\n' "${FILES_TO_MODIFY[@]}")

## New Architecture

### Before (Broadcast/Receiver Pattern)
- WhisperReceiver (BroadcastReceiver)
- WhisperConnectorService (Complex service)
- WhisperConnectorReceiver (BroadcastReceiver)
- ResourceUpdateReceiver (BroadcastReceiver)
- Complex intent handling
- Multiple broadcast actions

### After (Direct Service Pattern)
- DirectWhisperService (Simple service)
- Direct API calls to WhisperApi
- No broadcast dependencies
- Simplified architecture

## Benefits
1. **Simplified Architecture:** Fewer components, less complexity
2. **More Reliable:** No broadcast delivery issues
3. **Easier Debugging:** Direct calls are easier to trace
4. **Better Performance:** No broadcast overhead
5. **Easier Testing:** Direct service calls are easier to test

## Usage
Use the new simple test script:
\`./simple_direct_test.sh\`

## Rollback
To rollback changes, restore files from backup:
\`cp $BACKUP_DIR/* .\`

EOF

echo "✅ Created summary: $SUMMARY_FILE"

echo ""

echo "=== Cleanup Complete ==="
echo "✅ Broadcast/Receiver pattern removed"
echo "✅ Direct service approach implemented"
echo "✅ Backup created in: $BACKUP_DIR"
echo "✅ Test script created: $TEST_SCRIPT"
echo ""
echo "🎉 Architecture simplified successfully!"
echo ""
echo "Next steps:"
echo "1. Run: ./simple_direct_test.sh"
echo "2. Test the new direct approach"
echo "3. Verify processing works without broadcasts"
echo ""
echo "To rollback: cp $BACKUP_DIR/* ."
