#!/bin/bash
# Quick wrapper for Xiaomi hands-free install
# Usage: ./quick_install.sh [build_type]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_TYPE="${1:-debug}"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 Quick Xiaomi Install${NC}"
echo "Build type: $BUILD_TYPE"

# Determine APK path based on build type
case "$BUILD_TYPE" in
    "debug")
        APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
        ;;
    "release")
        APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/release/app-release.apk"
        ;;
    "xiaomi")
        APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/xiaomiPad/app-xiaomiPad-debug.apk"
        ;;
    *)
        echo "Unknown build type: $BUILD_TYPE"
        echo "Available types: debug, release, xiaomi"
        exit 1
        ;;
esac

# Check if APK exists, if not try to build it
if [[ ! -f "$APK_PATH" ]]; then
    echo -e "${BLUE}📦 APK not found, building...${NC}"
    cd "$PROJECT_ROOT"
    
    case "$BUILD_TYPE" in
        "debug")
            ./gradlew assembleDebug
            ;;
        "release")
            ./gradlew assembleRelease
            ;;
        "xiaomi")
            ./gradlew assembleXiaomiPadDebug
            ;;
    esac
fi

# Run the hands-free install script
echo -e "${BLUE}📱 Installing to Xiaomi device...${NC}"
"$SCRIPT_DIR/xiaomi_hands_free_install.sh" "$APK_PATH"

# Validate VULKAN GPU acceleration
echo -e "${BLUE}🔧 Validating VULKAN GPU acceleration...${NC}"
if [ -f "$SCRIPT_DIR/validate_vulkan_gpu.sh" ]; then
    "$SCRIPT_DIR/validate_vulkan_gpu.sh"
else
    echo -e "${YELLOW}⚠️  VULKAN validation script not found${NC}"
fi
