#!/bin/bash

# Download Real GGUF Whisper Models
# Downloads verified GGUF format models from community repositories
# 
# This script downloads actual GGUF container files (not GGML conversions)
# that work with modern whisper.cpp implementations.

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

# Setup function
setup_environment() {
    log_section "Setting up environment"
    
    # Create models directory
    mkdir -p "$MODELS_DIR"
    
    # Check Python availability
    if ! command -v python3 &> /dev/null; then
        log_error "Python3 not found. Please install Python3 first."
        exit 1
    fi
    
    # Install huggingface_hub if needed
    if ! python3 -c "import huggingface_hub" 2>/dev/null; then
        log_info "Installing huggingface_hub..."
        pip3 install huggingface_hub
    fi
    
    log_success "Environment setup complete"
}

# Verification functions
verify_gguf_header() {
    local file_path="$1"
    
    if [[ ! -f "$file_path" ]]; then
        return 1
    fi
    
    local header=$(hexdump -C -n 4 "$file_path" | head -1 | awk '{print $2 $3 $4 $5}')
    if [[ "$header" == "47554746" ]]; then  # "GGUF" in hex
        return 0
    else
        log_warning "Invalid GGUF header: $header (expected GGUF)"
        return 1
    fi
}

# Download verified GGUF models
download_gguf_models() {
    log_section "Downloading Verified GGUF Models"
    
    local success_count=0
    local total_count=0
    
    # Define verified repositories and files
    local downloads=(
        "xkeyC/whisper-large-v3-turbo-gguf:model_q4_k.gguf:453.6 MB:Q4_K quantized"
        "xkeyC/whisper-large-v3-turbo-gguf:model_q4_1.gguf:501.5 MB:Q4_1 quantized"
        "vonjack/whisper-large-v3-gguf:whisper-large-v3-q8_0.gguf:1.5 GB:Q8_0 quantized"
        "OllmOne/whisper-medium-GGUF:model-q4k.gguf:800 MB:Q4_K quantized"
    )
    
    total_count=${#downloads[@]}
    
    for download in "${downloads[@]}"; do
        IFS=':' read -r repo filename expected_size description <<< "$download"
        local file_path="$MODELS_DIR/$filename"
        
        log_info "Downloading $filename ($description)..."
        log_info "Expected size: $expected_size"
        
        # Check if file already exists and is valid
        if verify_gguf_header "$file_path"; then
            log_success "$filename already exists and is valid, skipping"
            ((success_count++))
            continue
        fi
        
        # Download the file
        if python3 - <<EOF
from huggingface_hub import hf_hub_download
import os

try:
    path = hf_hub_download(
        repo_id="$repo",
        filename="$filename",
        local_dir="$MODELS_DIR"
    )
    print("SUCCESS: $path")
except Exception as e:
    print(f"ERROR: {e}")
    exit(1)
EOF
        then
            # Verify the downloaded file
            if verify_gguf_header "$file_path"; then
                local actual_size=$(du -sh "$file_path" | cut -f1)
                log_success "$filename downloaded and verified as GGUF"
                log_info "Actual size: $actual_size"
                ((success_count++))
            else
                log_error "$filename downloaded but failed GGUF verification"
            fi
        else
            log_error "Failed to download $filename"
        fi
    done
    
    log_info "GGUF models: $success_count/$total_count downloaded successfully"
}

# Android deployment
deploy_to_android() {
    log_section "Deploying GGUF Models to Android"
    
    # Check if ADB is available
    if ! command -v adb &> /dev/null; then
        log_warning "ADB not found. Skipping device deployment."
        return 0
    fi
    
    # Check if device is connected
    if ! adb devices | grep -q "device$"; then
        log_warning "No Android device connected. Skipping device deployment."
        return 0
    fi
    
    log_info "Android device detected. Deploying GGUF models..."
    
    local scoped_path="/storage/emulated/0/Android/data/$APP_PACKAGE/files/MiraWhisper/models"
    
    # Create scoped storage directory
    log_info "Creating scoped storage directory..."
    adb shell "mkdir -p $scoped_path"
    
    # Deploy GGUF files to scoped storage
    local deployed_count=0
    for model_file in "$MODELS_DIR"/*.gguf; do
        if [[ -f "$model_file" ]]; then
            local filename=$(basename "$model_file")
            log_info "Deploying $filename to scoped storage..."
            
            if adb push "$model_file" "$scoped_path/" >/dev/null 2>&1; then
                log_success "$filename deployed to scoped storage"
                ((deployed_count++))
            else
                log_error "Failed to deploy $filename"
            fi
        fi
    done
    
    log_info "Deployed $deployed_count GGUF models to scoped storage"
    
    # Deploy to internal storage
    log_info "Deploying to internal app storage..."
    adb shell run-as "$APP_PACKAGE" "mkdir -p files/models" || true
    
    local internal_count=0
    for model_file in "$MODELS_DIR"/*.gguf; do
        if [[ -f "$model_file" ]]; then
            local filename=$(basename "$model_file")
            log_info "Deploying $filename to internal storage..."
            
            if adb shell run-as "$APP_PACKAGE" sh -c "cat > files/models/$filename" < "$model_file" >/dev/null 2>&1; then
                log_success "$filename deployed to internal storage"
                ((internal_count++))
            else
                log_error "Failed to deploy $filename to internal storage"
            fi
        fi
    done
    
    log_info "Deployed $internal_count GGUF models to internal storage"
}

# Generate summary report
generate_summary() {
    log_section "Download Summary"
    
    echo "📊 GGUF Model Download Summary:"
    echo ""
    
    # List downloaded GGUF files
    local gguf_count=$(find "$MODELS_DIR" -name "*.gguf" 2>/dev/null | wc -l)
    echo "🔧 GGUF Models: $gguf_count files"
    
    if [[ $gguf_count -gt 0 ]]; then
        find "$MODELS_DIR" -name "*.gguf" 2>/dev/null | while read -r file; do
            if [[ -f "$file" ]]; then
                local size=$(du -sh "$file" 2>/dev/null | cut -f1)
                local filename=$(basename "$file")
                echo "   • $filename: $size"
            fi
        done
    fi
    
    # Total size
    local total_size=$(du -sh "$MODELS_DIR" 2>/dev/null | cut -f1)
    echo ""
    echo "💾 Total storage used: $total_size"
    
    echo ""
    echo "📱 Android deployment paths:"
    echo "   • Scoped storage: /storage/emulated/0/Android/data/$APP_PACKAGE/files/MiraWhisper/models/"
    echo "   • Internal storage: /data/data/$APP_PACKAGE/files/models/"
    
    echo ""
    echo "🔧 Usage in Android:"
    echo "   • Use File(context.filesDir, \"models/model_q4_k.gguf\") for internal storage"
    echo "   • Pass absolute filesystem path to JNI (not URI)"
    echo "   • These are real GGUF files, not GGML conversions"
}

# Main execution
main() {
    log_section "Real GGUF Whisper Model Download"
    echo "This script downloads verified GGUF format Whisper models:"
    echo "• Large v3 Turbo models (Q4_K, Q4_1 quantized)"
    echo "• Large v3 models (Q8_0 quantized)"
    echo "• Medium models (Q4_K quantized)"
    echo ""
    echo "Models will be downloaded to: $MODELS_DIR"
    echo "Android app package: $APP_PACKAGE"
    echo ""
    
    # Execute download steps
    setup_environment
    download_gguf_models
    deploy_to_android
    generate_summary
    
    log_section "Download Complete!"
    log_success "Real GGUF Whisper models have been downloaded and deployed"
    echo ""
    echo "Next steps:"
    echo "1. Test model loading in your Android app"
    echo "2. Verify GGUF models work with whisper.cpp JNI"
    echo "3. Choose optimal model for your use case"
}

# Handle command line arguments
case "${1:-}" in
    "download")
        setup_environment
        download_gguf_models
        ;;
    "android"|"deploy")
        deploy_to_android
        ;;
    "summary"|"report")
        generate_summary
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  (no args)  Download all verified GGUF models"
        echo "  download   Download only GGUF models"
        echo "  android    Deploy models to Android device"
        echo "  summary    Generate download summary report"
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
