# Mira v0.1.0 - Internal Testing Submission

## 📱 App Information
- **App Name**: Mira
- **Package**: com.mira.videoeditor.internal
- **Version**: 0.1.0-internal (Build 1)
- **Target SDK**: 34 (Android 14)
- **Minimum SDK**: 24 (Android 7.0)

## 📦 Submission Package Contents
- `app-internal.apk` - Internal testing APK (44M)
- `RELEASE_NOTES_v0.1.0.md` - Release notes
- `INTERNAL_TESTING.md` - Testing guidelines
- `INTERNAL_TESTING_SUBMISSION.md` - This submission guide

## 🔥 Firebase App Distribution
**Status**: ✅ LIVE  
**Distribution Method**: Firebase App Distribution  
**Firebase Console**: https://console.firebase.google.com/project/mira-282f2/appdistribution/app/android:com.mira.videoeditor.internal/releases/6r0b9efjd42u8  
**Tester Link**: https://appdistribution.firebase.google.com/testerapps/1:384262830567:android:1960eb5e2470beb09ce542/releases/6r0b9efjd42u8

## 🧪 Internal Testing Instructions

### Installation
**Method 1: Firebase App Distribution (Recommended)**
1. **Add your email** in Firebase Console → App Distribution → Add testers
2. **Check your email** for Firebase invitation
3. **Click download link** to install APK
4. **Enable "Unknown Sources"** on Android device if needed
5. **Install and test** Mira v0.1.0-internal

**Method 2: Direct APK Installation**
1. Enable "Unknown Sources" or "Install from Unknown Sources" on your Android device
2. Transfer `app-internal.apk` to your device
3. Install the APK using a file manager or ADB

### Testing Focus Areas
1. **Core Functionality**
   - App launches successfully
   - Video selection works
   - Auto-cut simulation runs
   - Progress indicators display correctly

2. **UI/UX Testing**
   - Interface is responsive
   - Text is readable
   - Buttons work correctly
   - Progress feedback is clear

3. **Device Compatibility**
   - Test on different Android versions (7.0+)
   - Test on different screen sizes
   - Test on Xiaomi devices specifically

4. **Performance**
   - App doesn't crash
   - Memory usage is reasonable
   - Battery impact is minimal

### Known Issues (v0.1.0)
- Video processing is currently simulated (no actual Media3 Transformer integration)
- ML Kit face detection is not yet implemented
- Export functionality creates placeholder files

### Feedback Collection
Please report:
- Crashes or freezes
- UI/UX issues
- Performance problems
- Feature requests
- Device-specific issues

## 📋 Testing Checklist
- [ ] App installs successfully
- [ ] App launches without crashes
- [ ] Video selection dialog opens
- [ ] Auto-cut button responds
- [ ] Progress indicator shows
- [ ] App handles back button correctly
- [ ] App handles rotation changes
- [ ] Memory usage is stable
- [ ] No battery drain issues

## 🔧 Technical Details
- **Build Type**: Internal (debugging enabled)
- **Logging**: Enabled for troubleshooting
- **Obfuscation**: Disabled for easier debugging
- **Signing**: Release keystore (mira-release.keystore)

## 📞 Support
For technical issues or questions, contact the development team.

---
*Mira v0.1.0 Internal Testing - October 2025*
