# Xiaomi Pad Live Demo - Manual Testing Guide

## Quick Start

### 1. Update Device IP
Edit `test_xiaomi_pad_quick.sh` and update the DEVICE variable:
```bash
DEVICE="YOUR_XIAOMI_PAD_IP:5555"
```

### 2. Run Quick Test
```bash
./test_xiaomi_pad_quick.sh
```

## What the Demo Tests

### ✅ Duplicate Check Functionality
- **Test**: Convert the same video file twice
- **Expected**: Second conversion should detect existing file and skip
- **Logs to watch**: `Found existing converted file`, `Skipping conversion`

### ✅ Global Cleanup Functionality  
- **Test**: Trigger comprehensive cleanup
- **Expected**: All temporary and converted files removed
- **Logs to watch**: `Global cleanup`, `Deleted.*file`, `Cleanup completed`

### ✅ Live UI Interaction
- **Test**: Use the whisper_unified.html interface
- **Expected**: Interactive buttons work, real-time logs display
- **Screenshot**: `xiaomi_pad_current_state.png`

## Manual Testing Steps

### Step 1: Build and Install
```bash
./gradlew :app:installDebug
```

### Step 2: Launch App
```bash
adb -s YOUR_DEVICE_IP:5555 shell am start -n com.mira.videoeditor/.MainActivity
```

### Step 3: Navigate to Whisper Interface
```bash
adb -s YOUR_DEVICE_IP:5555 shell am start -n com.mira.videoeditor/.WhisperUnifiedActivity
```

### Step 4: Test Duplicate Check
1. Create a test video file
2. Convert it once
3. Try to convert the same file again
4. Check logs for duplicate detection

### Step 5: Test Global Cleanup
1. Create some test files in various locations
2. Trigger global cleanup
3. Verify files are removed
4. Check cleanup logs

### Step 6: Monitor Performance
```bash
adb -s YOUR_DEVICE_IP:5555 logcat | grep -E "(CPU|Memory|Resource|TECHNICAL)"
```

## Expected Log Messages

### Duplicate Check Success:
```
TECHNICAL: Found existing converted file with exact name: test_video_converted.mp4
TECHNICAL: Found existing converted file, skipping conversion
TECHNICAL: Skipping conversion, returning existing file
```

### Global Cleanup Success:
```
TECHNICAL: Starting global cleanup of all converted files and temporary data
TECHNICAL: Deleted cache file: test_cache.txt
TECHNICAL: Deleted Documents file: test_converted.mp4
TECHNICAL: Global cleanup completed successfully
```

## Troubleshooting

### Device Not Found
- Check ADB connection: `adb devices`
- Enable USB debugging
- Check IP address and port

### App Not Installing
- Check build: `./gradlew :app:assembleDebug`
- Check device storage space
- Try uninstalling first: `adb uninstall com.mira.videoeditor`

### No Logs Appearing
- Check logcat filters
- Ensure app is running
- Try: `adb logcat -c` to clear logs first

## Files Created During Demo
- `xiaomi_pad_current_state.png` - Screenshot of current state
- `xiaomi_pad_live_demo_report_*.md` - Comprehensive test report
- Various test files in `/storage/emulated/0/Documents/ConvertedMedia/`

## Success Indicators
✅ Duplicate files detected and skipped
✅ Global cleanup removes all temporary files  
✅ UI responds to test buttons
✅ Logs show detailed operation status
✅ Performance monitoring works
✅ Error handling is graceful
