# Xiaomi/HyperOS Hands-Free USB Install Scripts

This directory contains scripts to automate USB installs on Xiaomi/HyperOS devices with minimal prompts and auto-granted permissions.

## Quick Start

### Simple Usage
```bash
# Install debug build (builds if needed)
./scripts/quick_install.sh debug

# Install release build
./scripts/quick_install.sh release

# Install Xiaomi-specific build
./scripts/quick_install.sh xiaomi
```

### Advanced Usage
```bash
# Install specific APK with custom package name
./scripts/xiaomi_hands_free_install.sh /path/to/app.apk com.your.package

# Install with default settings
./scripts/xiaomi_hands_free_install.sh
```

## What These Scripts Do

### 1. Device Setup Verification
- ✅ Checks USB debugging is enabled
- ✅ Verifies "Install via USB" is enabled
- ✅ Confirms device authorization
- ✅ Provides guidance for common MIUI/HyperOS issues

### 2. Permission Automation
- ✅ Disables package verifiers temporarily
- ✅ Installs APK with `-g` flag (auto-grant permissions)
- ✅ Grants all dangerous runtime permissions via `pm grant`
- ✅ Re-enables package verifiers after install

### 3. Error Handling
- ✅ Handles `INSTALL_FAILED_USER_RESTRICTED` errors
- ✅ Provides specific guidance for MIUI gate issues
- ✅ Manages insufficient storage errors
- ✅ Handles update incompatibility issues

## One-Time Device Setup

Before using these scripts, ensure your Xiaomi device is properly configured:

### 1. Enable Developer Options
1. Go to **Settings** → **About phone**
2. Tap **MIUI version** 7 times
3. Go back to **Settings** → **Additional settings** → **Developer options**

### 2. Configure USB Settings
1. Enable **USB debugging**
2. Enable **Install via USB**
   - If you see "temporarily restricted":
     - Turn off Wi-Fi
     - Turn on mobile data
     - Sign in to Mi account if prompted
     - Try enabling again
3. (Optional) Enable **USB debugging (Security settings)**

### 3. Authorize Your Computer
1. Connect device via USB
2. Accept the "Allow USB debugging?" dialog
3. **Important**: Check "Always allow from this computer"

## Script Features

### `xiaomi_hands_free_install.sh`
The main installation script with comprehensive error handling:

**Features:**
- Device connection verification
- Xiaomi-specific setting checks
- Automatic permission granting
- Robust error handling
- Installation verification
- Optional app launching

**Usage:**
```bash
./scripts/xiaomi_hands_free_install.sh [APK_PATH] [PACKAGE_NAME] [DEVICE_NAME]
```

### `quick_install.sh`
A simple wrapper that builds and installs:

**Features:**
- Automatic APK building
- Multiple build type support
- Simplified interface

**Usage:**
```bash
./scripts/quick_install.sh [debug|release|xiaomi]
```

## Troubleshooting

### Common Issues

#### "INSTALL_FAILED_USER_RESTRICTED"
This means the MIUI/HyperOS security gate is active:

**Solutions:**
1. Re-enable "Install via USB" in Developer Options
2. Toggle Wi-Fi off, mobile data on, then enable "Install via USB" again
3. Ensure you accepted your computer's ADB key with "Always allow"
4. Try a different USB port

#### "Remember my choice" doesn't stick
This is Xiaomi's policy, not a setup issue. The scripts handle this automatically.

#### USB debugging gets disabled
Xiaomi may disable this periodically. The scripts will detect and warn you.

#### Package verifier errors
The scripts temporarily disable verifiers during install, then re-enable them.

### Manual Recovery Commands

If you need to manually fix device settings:

```bash
# Check device connection
adb devices

# Re-enable USB debugging
adb shell "settings put global adb_enabled 1"

# Re-enable Install via USB
adb shell "settings put global install_non_market_apps 1"

# Disable verifiers for troubleshooting
adb shell "settings put global verifier_verify_adb_installs 0"
adb shell "settings put global package_verifier_enable 0"
```

## Expected Behavior

After proper setup:
- ✅ `adb install -r -t -g` pushes builds with no on-device prompts
- ✅ Runtime permissions are granted automatically
- ✅ No permission dialogs appear in the app
- ✅ Installation is nearly hands-free

## Limitations

- Xiaomi's "USB debugging (Security settings)" and "Install via USB" can reset occasionally
- This is Xiaomi's policy - there's no official way to keep them always-on without root
- The scripts detect when this happens and provide guidance

## Integration with Build Process

You can integrate these scripts into your build process:

```bash
# In your build script
./gradlew assembleDebug
./scripts/quick_install.sh debug
```

Or use the main script for more control:

```bash
# Custom build and install
./gradlew assembleXiaomiPadDebug
./scripts/xiaomi_hands_free_install.sh app/build/outputs/apk/xiaomiPad/app-xiaomiPad-debug.apk com.mira.videoeditor.xiaomi
```

## Security Notes

- Package verifiers are temporarily disabled during install for smoother operation
- They are automatically re-enabled after installation
- This is safe for development builds
- Consider the security implications for production deployments
