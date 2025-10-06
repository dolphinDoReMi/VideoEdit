# Xiaomi Pad Deployment Guide (Android 15)

A fast, reliable way to deploy and verify the app on Xiaomi Pad Ultra.

## Summary
- No special install permissions required
- No runtime storage/media permissions required for file access
- File/media access uses Android SAF (user-granted URI access)
- Applies to Whisper, UI, and Clip features

## Prerequisites
- Android SDK/Platform Tools (adb)
- Xiaomi Pad Ultra with Developer Options + USB debugging enabled
- USB or network ADB connection

Verify device:
```bash
adb devices
```

## Build & Install (Debug)
```bash
./gradlew :app:assembleDebug :app:installDebug -x lint --no-daemon
```
Expected: BUILD SUCCESSFUL and “Installed on 1 device.”

## Launch App
```bash
adb shell am start -n com.mira.com/com.mira.whisper.WhisperMainActivity
```
Expected: Whisper main screen (file selection) opens.

## Permissions Policy (What you should see)
- No prompts for storage/media permissions
- File selection works via system picker (SAF)
- Microphone permission only needed for real-time mic capture (not required for file-based processing)

## File Selection & Batch Processing
1. Tap “Pick Media (URI)” to open the system picker
2. Select one or more videos/images
3. Start processing; the app navigates to processing/results as jobs run

Notes:
- Multiple selection behavior depends on the device file picker. If only single-select is supported by the picker, batch still works when multiple URIs are provided.

## Logs & Verification
Monitor logs while testing:
```bash
adb logcat | grep -E "(WhisperConnectorService|AndroidWhisperBridge|TranscribeWorker|WhisperResults|WhisperMainActivity)"
```
Quick checks:
- App launch shows “Whisper app launched - initializing interface”
- File selection logs show selected file count
- Batch run logs show job/batch IDs

## Uninstall / Reset
If install fails due to a bad state, remove the existing package and reinstall:
```bash
adb uninstall com.mira.com || true
./gradlew :app:installDebug --no-daemon
```

## Troubleshooting
- File picker shows only single selection:
  - Use an alternative picker app (e.g., Files by Google), or select items sequentially
  - SAF multi-select support varies by OEM picker
- No files visible in picker:
  - Ensure “All files”/“Videos” filter is selected; try another picker app
- Exporting results to shared storage:
  - Use the built-in share flow (FileProvider) or pick a destination via SAF; no storage permission required
- App doesn’t appear after start command:
  - Confirm package/activity names; check `adb logcat` for errors

## Where is this documented?
- Index: `Doc/DEPLOYMENT.md`
- Reports: `Doc/deployment/XIAOMI_PAD_DEPLOYMENT_REPORT.md`, `Doc/deployment/xiaomi_pad_deployment_verification_report.md`, `Doc/deployment/xiaomi_pad_deployment_test_report.md`, `Doc/deployment/XIAOMI_PAD_UPDATED_DEPLOYMENT.md`
