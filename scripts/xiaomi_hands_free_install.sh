#!/bin/bash
# Xiaomi/HyperOS Hands-Free USB Install Script
# Automates ADB installs with minimal prompts and auto-granted permissions

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
APK_PATH="${1:-$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk}"
PACKAGE_NAME="${2:-com.mira.videoeditor}"
DEVICE_NAME="${3:-}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✓${NC} $1"
}

error() {
    echo -e "${RED}✗${NC} $1"
}

warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

info() {
    echo -e "${CYAN}ℹ${NC} $1"
}

# Check if APK exists
check_apk_exists() {
    if [[ ! -f "$APK_PATH" ]]; then
        error "APK not found at: $APK_PATH"
        error "Please build the project first or provide a valid APK path"
        exit 1
    fi
    success "APK found: $APK_PATH"
}

# Check device connection and authorization
check_device_connection() {
    log "Checking device connection..."
    
    local devices=$(adb devices | grep -v "List of devices" | grep -v "^$" | wc -l)
    
    if [[ $devices -eq 0 ]]; then
        error "No devices connected"
        error "Please connect your Xiaomi device and enable USB debugging"
        exit 1
    fi
    
    if [[ $devices -gt 1 ]]; then
        warning "Multiple devices detected. Using first device."
    fi
    
    local device_id=$(adb devices | grep -v "List of devices" | grep -v "^$" | head -1 | awk '{print $1}')
    local device_status=$(adb devices | grep -v "List of devices" | grep -v "^$" | head -1 | awk '{print $2}')
    
    if [[ "$device_status" != "device" ]]; then
        error "Device not authorized. Status: $device_status"
        error "Please accept the USB debugging authorization on your device"
        exit 1
    fi
    
    success "Device connected: $device_id"
    DEVICE_ID="$device_id"
}

# Check and enable required Xiaomi settings
check_xiaomi_settings() {
    log "Checking Xiaomi/HyperOS settings..."
    
    # Check if USB debugging is enabled
    local usb_debugging=$(adb shell "settings get global adb_enabled" 2>/dev/null || echo "0")
    if [[ "$usb_debugging" != "1" ]]; then
        warning "USB debugging may not be fully enabled"
        info "Please ensure 'USB debugging' is enabled in Developer Options"
    else
        success "USB debugging is enabled"
    fi
    
    # Check if Install via USB is enabled
    local install_via_usb=$(adb shell "settings get global install_non_market_apps" 2>/dev/null || echo "0")
    if [[ "$install_via_usb" != "1" ]]; then
        warning "Install via USB may not be enabled"
        info "Please enable 'Install via USB' in Developer Options"
        info "If you see 'temporarily restricted', try:"
        info "  1. Turn off Wi-Fi"
        info "  2. Turn on mobile data"
        info "  3. Sign in to Mi account if prompted"
        info "  4. Try enabling 'Install via USB' again"
    else
        success "Install via USB is enabled"
    fi
    
    # Check USB debugging (Security settings) if available
    local security_usb_debug=$(adb shell "settings get global adb_wifi_enabled" 2>/dev/null || echo "0")
    if [[ "$security_usb_debug" == "1" ]]; then
        success "USB debugging (Security settings) is enabled"
    else
        info "USB debugging (Security settings) not detected or not enabled"
        info "This is optional but may help with permission prompts"
    fi
}

# Disable package verifiers for smoother installs
disable_package_verifiers() {
    log "Disabling package verifiers for ADB installs..."
    
    # Disable ADB install verification
    adb shell "settings put global verifier_verify_adb_installs 0" 2>/dev/null || true
    
    # Disable package verifier
    adb shell "settings put global package_verifier_enable 0" 2>/dev/null || true
    
    success "Package verifiers disabled"
}

# Grant runtime permissions automatically
grant_runtime_permissions() {
    log "Granting runtime permissions..."
    
    # Common dangerous permissions for video editing apps
    local permissions=(
        "android.permission.RECORD_AUDIO"
        "android.permission.CAMERA"
        "android.permission.READ_EXTERNAL_STORAGE"
        "android.permission.WRITE_EXTERNAL_STORAGE"
        "android.permission.READ_MEDIA_VIDEO"
        "android.permission.READ_MEDIA_AUDIO"
        "android.permission.READ_MEDIA_IMAGES"
        "android.permission.MANAGE_EXTERNAL_STORAGE"
        "android.permission.ACCESS_MEDIA_LOCATION"
        "android.permission.WAKE_LOCK"
        "android.permission.FOREGROUND_SERVICE"
        "android.permission.POST_NOTIFICATIONS"
    )
    
    for permission in "${permissions[@]}"; do
        if adb shell "pm grant $PACKAGE_NAME $permission" 2>/dev/null; then
            success "Granted: $permission"
        else
            # Permission might not be declared in manifest, which is fine
            info "Skipped: $permission (not declared)"
        fi
    done
}

# Install APK with auto-grant permissions
install_apk() {
    log "Installing APK with auto-granted permissions..."
    
    # Try to uninstall first to avoid conflicts
    if adb shell "pm list packages | grep -q $PACKAGE_NAME" 2>/dev/null; then
        log "Uninstalling existing app..."
        adb uninstall "$PACKAGE_NAME" 2>/dev/null || true
    fi
    
    # Install with auto-grant permissions
    local install_cmd="adb install -r -t -g \"$APK_PATH\""
    log "Running: $install_cmd"
    
    if eval "$install_cmd"; then
        success "APK installed successfully with auto-granted permissions"
    else
        error "APK installation failed"
        
        # Check for specific error types
        local error_output=$(eval "$install_cmd" 2>&1 || true)
        
        if echo "$error_output" | grep -q "INSTALL_FAILED_USER_RESTRICTED"; then
            error "INSTALL_FAILED_USER_RESTRICTED - MIUI/HyperOS gate is active"
            info "Try these fixes:"
            info "  1. Re-enable 'Install via USB' in Developer Options"
            info "  2. Toggle Wi-Fi off, mobile data on, then enable 'Install via USB' again"
            info "  3. Make sure you accepted your computer's ADB key with 'Always allow'"
            info "  4. Try connecting via different USB port"
        elif echo "$error_output" | grep -q "INSTALL_FAILED_INSUFFICIENT_STORAGE"; then
            error "Insufficient storage space"
            info "Free up space on your device"
        elif echo "$error_output" | grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE"; then
            error "Update incompatible - trying to force install..."
            adb install -r -t -g -d "$APK_PATH" || {
                error "Force install also failed"
                exit 1
            }
            success "APK force-installed successfully"
        else
            error "Unknown installation error: $error_output"
            exit 1
        fi
    fi
}

# Verify installation and permissions
verify_installation() {
    log "Verifying installation..."
    
    # Check if app is installed
    if adb shell "pm list packages | grep -q $PACKAGE_NAME"; then
        success "App is installed"
    else
        error "App installation verification failed"
        exit 1
    fi
    
    # Check granted permissions
    log "Checking granted permissions..."
    local granted_perms=$(adb shell "pm dump $PACKAGE_NAME | grep 'permission:' | grep 'granted=true'" 2>/dev/null || true)
    
    if [[ -n "$granted_perms" ]]; then
        success "Runtime permissions granted:"
        echo "$granted_perms" | while read -r line; do
            local perm=$(echo "$line" | sed 's/.*permission:\([^[:space:]]*\).*/\1/')
            info "  ✓ $perm"
        done
    else
        warning "No runtime permissions found (may be normal if not declared)"
    fi
    
    # Get app info
    local app_info=$(adb shell "dumpsys package $PACKAGE_NAME | grep -E '(versionName|versionCode|targetSdk)'" 2>/dev/null || true)
    if [[ -n "$app_info" ]]; then
        log "App information:"
        echo "$app_info" | while read -r line; do
            info "  $line"
        done
    fi
}

# Launch app (optional)
launch_app() {
    log "Launching app..."
    
    local main_activity=$(adb shell "pm dump $PACKAGE_NAME | grep -A 1 'android.intent.action.MAIN' | grep 'android.intent.category.LAUNCHER' -B 1 | head -1 | sed 's/.*\([^[:space:]]*\)\/\([^[:space:]]*\).*/\1\/\2/'" 2>/dev/null || true)
    
    if [[ -n "$main_activity" ]]; then
        adb shell "am start -n $main_activity" 2>/dev/null || {
            warning "Could not launch app automatically"
            info "You can launch it manually from the device"
        }
        success "App launched"
    else
        warning "Could not determine main activity"
        info "Please launch the app manually from the device"
    fi
}

# Re-enable package verifiers (cleanup)
reenable_package_verifiers() {
    log "Re-enabling package verifiers..."
    
    # Re-enable ADB install verification
    adb shell "settings put global verifier_verify_adb_installs 1" 2>/dev/null || true
    
    # Re-enable package verifier
    adb shell "settings put global package_verifier_enable 1" 2>/dev/null || true
    
    success "Package verifiers re-enabled"
}

# Show usage information
show_usage() {
    echo "Usage: $0 [APK_PATH] [PACKAGE_NAME] [DEVICE_NAME]"
    echo ""
    echo "Arguments:"
    echo "  APK_PATH      Path to the APK file (default: app/build/outputs/apk/debug/app-debug.apk)"
    echo "  PACKAGE_NAME  Package name of the app (default: com.mira.videoeditor)"
    echo "  DEVICE_NAME   Specific device name (optional)"
    echo ""
    echo "Examples:"
    echo "  $0"
    echo "  $0 /path/to/app.apk"
    echo "  $0 /path/to/app.apk com.example.app"
    echo ""
    echo "This script automates Xiaomi/HyperOS USB installs with minimal prompts."
}

# Main execution
main() {
    # Parse command line arguments
    if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
        show_usage
        exit 0
    fi
    
    log "Starting Xiaomi/HyperOS hands-free install process..."
    log "APK: $APK_PATH"
    log "Package: $PACKAGE_NAME"
    
    # Execute installation steps
    check_apk_exists
    check_device_connection
    check_xiaomi_settings
    disable_package_verifiers
    install_apk
    grant_runtime_permissions
    verify_installation
    launch_app
    reenable_package_verifiers
    
    success "Installation completed successfully!"
    log "Your app should now be running with all permissions granted"
    log "No on-device permission prompts should appear for future installs"
}

# Error handling
trap 'error "Script interrupted"; reenable_package_verifiers; exit 1' INT TERM

# Run main function
main "$@"
