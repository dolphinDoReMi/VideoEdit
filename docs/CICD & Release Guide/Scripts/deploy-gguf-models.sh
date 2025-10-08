#!/bin/bash

# Deploy GGUF Models to Android Device
# Deploys downloaded GGUF models to both scoped and internal storage

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MODELS_DIR="$PROJECT_ROOT/whisper_models/gguf"
APP_PACKAGE="com.mira.com"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

log_section() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE} $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

# Check prerequisites
check_prerequisites() {
    log_section "Checking Prerequisites"
    
    # Check if ADB is available
    if ! command -v adb &> /dev/null; then
        log_error "ADB not found. Please install Android SDK platform tools."
        exit 1
    fi
    
    # Check if device is connected
    if ! adb devices | grep -q "device$"; then
        log_error "No Android device connected. Please connect your device and enable USB debugging."
        exit 1
    fi
    
    # Check if app is installed
    if ! adb shell pm list packages | grep -q "^package:$APP_PACKAGE$"; then
        log_error "App not installed: $APP_PACKAGE"
        exit 1
    fi
    
    # Check if models directory exists
    if [[ ! -d "$MODELS_DIR" ]]; then
        log_error "Models directory not found: $MODELS_DIR"
        log_info "Run download_real_gguf_models.sh first to download models"
        exit 1
    fi
    
    # Check if GGUF files exist
    local gguf_count=$(find "$MODELS_DIR" -name "*.gguf" 2>/dev/null | wc -l)
    if [[ $gguf_count -eq 0 ]]; then
        log_error "No GGUF files found in $MODELS_DIR"
        log_info "Run download_real_gguf_models.sh first to download models"
        exit 1
    fi
    
    log_success "All prerequisites met"
    log_info "Found $gguf_count GGUF files to deploy"
}

# Deploy to scoped storage
deploy_scoped_storage() {
    log_section "Deploying to Scoped Storage"
    
    local scoped_path="/storage/emulated/0/Android/data/$APP_PACKAGE/files/MiraWhisper/models"
    
    log_info "Creating scoped storage directory..."
    adb shell "mkdir -p $scoped_path"
    
    local deployed_count=0
    local total_count=0
    
    for model_file in "$MODELS_DIR"/*.gguf; do
        if [[ -f "$model_file" ]]; then
            local filename=$(basename "$model_file")
            local file_size=$(du -sh "$model_file" | cut -f1)
            ((total_count++))
            
            log_info "Deploying $filename ($file_size) to scoped storage..."
            
            if adb push "$model_file" "$scoped_path/" >/dev/null 2>&1; then
                log_success "$filename deployed to scoped storage"
                ((deployed_count++))
            else
                log_error "Failed to deploy $filename"
            fi
        fi
    done
    
    log_info "Scoped storage: $deployed_count/$total_count files deployed"
    
    # Verify deployment
    log_info "Verifying scoped storage deployment..."
    local remote_files=$(adb shell "ls $scoped_path/*.gguf 2>/dev/null | wc -l" || echo "0")
    log_info "Remote GGUF files: $remote_files"
}

# Deploy to internal storage
deploy_internal_storage() {
    log_section "Deploying to Internal Storage"
    
    log_info "Creating internal storage directory..."
    adb shell run-as "$APP_PACKAGE" "mkdir -p files/models" || true
    
    local deployed_count=0
    local total_count=0
    
    for model_file in "$MODELS_DIR"/*.gguf; do
        if [[ -f "$model_file" ]]; then
            local filename=$(basename "$model_file")
            local file_size=$(du -sh "$model_file" | cut -f1)
            ((total_count++))
            
            log_info "Deploying $filename ($file_size) to internal storage..."
            
            # Use a more reliable method for large files
            if adb shell run-as "$APP_PACKAGE" sh -c "cat > files/models/$filename" < "$model_file" >/dev/null 2>&1; then
                log_success "$filename deployed to internal storage"
                ((deployed_count++))
            else
                log_warning "Failed to deploy $filename to internal storage (may be too large)"
                log_info "Try using scoped storage instead"
            fi
        fi
    done
    
    log_info "Internal storage: $deployed_count/$total_count files deployed"
    
    # Verify deployment
    log_info "Verifying internal storage deployment..."
    local remote_files=$(adb shell run-as "$APP_PACKAGE" "ls files/models/*.gguf 2>/dev/null | wc -l" || echo "0")
    log_info "Remote GGUF files: $remote_files"
}

# Verify deployment
verify_deployment() {
    log_section "Verifying Deployment"
    
    local scoped_path="/storage/emulated/0/Android/data/$APP_PACKAGE/files/MiraWhisper/models"
    
    echo "📱 Android Deployment Status:"
    echo ""
    
    # Check scoped storage
    echo "🔍 Scoped Storage:"
    local scoped_files=$(adb shell "ls $scoped_path/*.gguf 2>/dev/null" || echo "")
    if [[ -n "$scoped_files" ]]; then
        echo "$scoped_files" | while read -r file; do
            local filename=$(basename "$file")
            local size=$(adb shell "stat -c%s $file" 2>/dev/null || echo "unknown")
            local size_mb=$((size / 1024 / 1024))
            echo "   ✅ $filename: ${size_mb} MB"
        done
    else
        echo "   ❌ No GGUF files found"
    fi
    
    echo ""
    echo "🔍 Internal Storage:"
    local internal_files=$(adb shell run-as "$APP_PACKAGE" "ls files/models/*.gguf 2>/dev/null" || echo "")
    if [[ -n "$internal_files" ]]; then
        echo "$internal_files" | while read -r file; do
            local filename=$(basename "$file")
            local size=$(adb shell run-as "$APP_PACKAGE" "stat -c%s $file" 2>/dev/null || echo "unknown")
            local size_mb=$((size / 1024 / 1024))
            echo "   ✅ $filename: ${size_mb} MB"
        done
    else
        echo "   ❌ No GGUF files found"
    fi
    
    echo ""
    echo "📋 Usage in Android:"
    echo "   • Scoped storage: File(context.getExternalFilesDir(null), \"MiraWhisper/models/model_q4_k.gguf\")"
    echo "   • Internal storage: File(context.filesDir, \"models/model_q4_k.gguf\")"
    echo "   • Pass absolute path to JNI: modelFile.absolutePath"
}

# Main execution
main() {
    log_section "GGUF Model Android Deployment"
    echo "This script deploys GGUF models to your Android device:"
    echo "• Scoped storage: /storage/emulated/0/Android/data/$APP_PACKAGE/files/MiraWhisper/models/"
    echo "• Internal storage: /data/data/$APP_PACKAGE/files/models/"
    echo ""
    
    check_prerequisites
    deploy_scoped_storage
    deploy_internal_storage
    verify_deployment
    
    log_section "Deployment Complete!"
    log_success "GGUF models have been deployed to Android device"
    echo ""
    echo "Next steps:"
    echo "1. Test model loading in your Android app"
    echo "2. Use File(context.filesDir, \"models/model_q4_k.gguf\") for internal storage"
    echo "3. Use File(context.getExternalFilesDir(null), \"MiraWhisper/models/model_q4_k.gguf\") for scoped storage"
}

# Handle command line arguments
case "${1:-}" in
    "scoped")
        check_prerequisites
        deploy_scoped_storage
        ;;
    "internal")
        check_prerequisites
        deploy_internal_storage
        ;;
    "verify")
        verify_deployment
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  (no args)  Deploy to both scoped and internal storage"
        echo "  scoped     Deploy only to scoped storage"
        echo "  internal   Deploy only to internal storage"
        echo "  verify     Verify deployment status"
        echo "  help       Show this help message"
        ;;
    "")
        main
        ;;
    *)
        log_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac
