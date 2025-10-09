#!/bin/bash

# Comprehensive Whisper Model Download Script
# Downloads all Whisper models across main formats: PyTorch, GGUF, CTranslate2
# Excludes ONNX as requested

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODELS_BASE_DIR="$SCRIPT_DIR/whisper_models"
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

# Model configurations
PYTORCH_MODELS=(
    "openai/whisper-tiny"
    "openai/whisper-tiny.en"
    "openai/whisper-base"
    "openai/whisper-base.en"
    "openai/whisper-small"
    "openai/whisper-small.en"
    "openai/whisper-medium"
    "openai/whisper-medium.en"
    "openai/whisper-large"
    "openai/whisper-large-v2"
    "openai/whisper-large-v3"
    "openai/whisper-large-v3-turbo"
)

GGUF_SIZES=("tiny" "tiny.en" "base" "base.en" "small" "small.en" "medium" "medium.en" "large-v2" "large-v3")
GGUF_QUANTS=("q5_1")  # Add "q8_0" for max quality if needed

CT2_MODELS=(
    "Systran/faster-whisper-tiny"
    "Systran/faster-whisper-tiny.en"
    "Systran/faster-whisper-base"
    "Systran/faster-whisper-base.en"
    "Systran/faster-whisper-small"
    "Systran/faster-whisper-small.en"
    "Systran/faster-whisper-medium"
    "Systran/faster-whisper-medium.en"
    "Systran/faster-whisper-large-v2"
    "Systran/faster-whisper-large-v3"
)

# Expected file sizes (MB) for verification
get_expected_size() {
    case "$1" in
        "tiny"|"tiny.en") echo "35-45" ;;
        "base"|"base.en") echo "130-160" ;;
        "small"|"small.en") echo "240-260" ;;
        "medium"|"medium.en") echo "750-800" ;;
        "large-v2"|"large-v3") echo "1500-1600" ;;
        *) echo "0-9999" ;;
    esac
}

# Setup function
setup_environment() {
    log_section "Setting up environment"
    
    # Create base directories
    mkdir -p "$MODELS_BASE_DIR"/{transformers,gguf,ct2}
    
    # Check Python availability
    if ! command -v python3 &> /dev/null; then
        log_error "Python3 not found. Please install Python3 first."
        exit 1
    fi
    
    # Create virtual environment
    log_info "Creating Python virtual environment..."
    python3 -m venv .venv
    source .venv/bin/activate
    
    # Install required packages
    log_info "Installing huggingface_hub..."
    pip install --upgrade huggingface_hub
    
    log_success "Environment setup complete"
}

# Verification functions
verify_file_size() {
    local file_path="$1"
    local model_name="$2"
    local file_size_mb
    
    if [[ ! -f "$file_path" ]]; then
        return 1
    fi
    
    file_size_mb=$(($(stat -f%z "$file_path" 2>/dev/null || stat -c%s "$file_path" 2>/dev/null) / 1024 / 1024))
    
    if [[ -n "$model_name" ]]; then
        local expected_range=$(get_expected_size "$model_name")
        if [[ "$expected_range" =~ ^([0-9]+)-([0-9]+)$ ]]; then
            local min_size="${BASH_REMATCH[1]}"
            local max_size="${BASH_REMATCH[2]}"
            
            if [[ $file_size_mb -ge $min_size && $file_size_mb -le $max_size ]]; then
                return 0
            else
                log_warning "File size $file_size_mb MB is outside expected range $expected_range MB"
                return 1
            fi
        fi
    fi
    
    return 0
}

verify_gguf_header() {
    local file_path="$1"
    
    if [[ ! -f "$file_path" ]]; then
        return 1
    fi
    
    local header=$(hexdump -C -n 4 "$file_path" | head -1 | awk '{print $2 $3 $4 $5}')
    if [[ "$header" == "47554746" ]]; then  # "GGUF" in hex
        return 0
    elif [[ "$header" == "67676d6c" ]]; then  # "ggml" in hex (big-endian)
        return 0
    elif [[ "$header" == "6c6d6767" ]]; then  # "lmgg" in hex (little-endian "ggml")
        return 0
    else
        log_warning "Invalid model header: $header (expected GGUF or GGML)"
        return 1
    fi
}

verify_model_exists() {
    local file_path="$1"
    local min_size_bytes="${2:-1024}"
    
    if [[ -f "$file_path" && $(stat -f%z "$file_path" 2>/dev/null || stat -c%s "$file_path" 2>/dev/null) -gt $min_size_bytes ]]; then
        return 0
    else
        return 1
    fi
}

# Download PyTorch models
download_pytorch_models() {
    log_section "Downloading PyTorch (Transformers) Models"
    
    local transformers_dir="$MODELS_BASE_DIR/transformers"
    local success_count=0
    local total_count=${#PYTORCH_MODELS[@]}
    
    for model_id in "${PYTORCH_MODELS[@]}"; do
        local model_name=$(basename "$model_id")
        local local_dir="$transformers_dir/$model_name"
        
        log_info "Downloading $model_id..."
        
        if verify_model_exists "$local_dir/config.json" 100; then
            log_success "$model_id already exists, skipping"
            ((success_count++))
            continue
        fi
        
        if python3 - <<EOF
from huggingface_hub import snapshot_download
import os

try:
    local_path = snapshot_download(
        repo_id="$model_id",
        local_dir="$local_dir"
    )
    print("SUCCESS: $local_path")
except Exception as e:
    print(f"ERROR: {e}")
    exit(1)
EOF
        then
            log_success "$model_id downloaded successfully"
            ((success_count++))
        else
            log_error "Failed to download $model_id"
        fi
    done
    
    log_info "PyTorch models: $success_count/$total_count downloaded successfully"
}

# Download GGUF models
download_gguf_models() {
    log_section "Downloading GGUF (whisper.cpp) Models"
    
    local gguf_dir="$MODELS_BASE_DIR/gguf"
    local success_count=0
    local total_count=0
    
    # Define models with their available quantizations
    local models=(
        "tiny:q5_1 q8_0"
        "tiny.en:q5_1 q8_0"
        "base:q5_1 q8_0"
        "base.en:q5_1 q8_0"
        "small:q5_1 q8_0"
        "small.en:q5_1 q8_0"
        "medium:q5_0 q8_0"
        "medium.en:q5_0 q8_0"
        "large-v2:q5_0 q8_0"
        "large-v3:q5_0"
        "large-v3-turbo:q5_0 q8_0"
    )
    
    # Calculate total count
    for model_config in "${models[@]}"; do
        local size="${model_config%%:*}"
        local quants="${model_config##*:}"
        local quant_array=($quants)
        total_count=$((total_count + ${#quant_array[@]}))
    done
    
    for model_config in "${models[@]}"; do
        local size="${model_config%%:*}"
        local quants="${model_config##*:}"
        local quant_array=($quants)
        
        for quant in "${quant_array[@]}"; do
            local filename="ggml-${size}-${quant}.bin"
            local file_path="$gguf_dir/$filename"
            
            log_info "Downloading $filename..."
            
            if verify_gguf_header "$file_path"; then
                log_success "$filename already exists and is valid, skipping"
                ((success_count++))
                continue
            fi
            
            if python3 - <<EOF
from huggingface_hub import hf_hub_download
import os

try:
    path = hf_hub_download(
        repo_id="ggerganov/whisper.cpp",
        filename="$filename",
        local_dir="$gguf_dir"
    )
    print("SUCCESS: $path")
except Exception as e:
    print(f"ERROR: {e}")
    exit(1)
EOF
            then
                if verify_gguf_header "$file_path"; then
                    log_success "$filename downloaded and verified"
                    ((success_count++))
                else
                    log_error "$filename downloaded but failed verification"
                fi
            else
                log_error "Failed to download $filename"
            fi
        done
    done
    
    log_info "GGUF models: $success_count/$total_count downloaded successfully"
}

# Download CTranslate2 models
download_ct2_models() {
    log_section "Downloading CTranslate2 (faster-whisper) Models"
    
    local ct2_dir="$MODELS_BASE_DIR/ct2"
    local success_count=0
    local total_count=${#CT2_MODELS[@]}
    
    for model_id in "${CT2_MODELS[@]}"; do
        local model_name=$(basename "$model_id")
        local local_dir="$ct2_dir/$model_name"
        
        log_info "Downloading $model_id..."
        
        if verify_model_exists "$local_dir/config.json" 100; then
            log_success "$model_id already exists, skipping"
            ((success_count++))
            continue
        fi
        
        if python3 - <<EOF
from huggingface_hub import snapshot_download
import os

try:
    local_path = snapshot_download(
        repo_id="$model_id",
        local_dir="$local_dir"
    )
    print("SUCCESS: $local_path")
except Exception as e:
    print(f"ERROR: {e}")
    exit(1)
EOF
        then
            log_success "$model_id downloaded successfully"
            ((success_count++))
        else
            log_error "Failed to download $model_id"
        fi
    done
    
    log_info "CTranslate2 models: $success_count/$total_count downloaded successfully"
}

# Android deployment
deploy_to_android() {
    log_section "Deploying Models to Android Device"
    
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
    
    log_info "Android device detected. Deploying models..."
    
    # Deploy GGUF models (most relevant for Android)
    local gguf_dir="$MODELS_BASE_DIR/gguf"
    local scoped_path="/storage/emulated/0/Android/data/$APP_PACKAGE/files/MiraWhisper/models"
    
    log_info "Creating scoped storage directory..."
    adb shell "mkdir -p $scoped_path"
    
    local deployed_count=0
    for model_file in "$gguf_dir"/*.gguf; do
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
    
    log_info "Deployed $deployed_count GGUF models to Android device"
    
    # Also deploy to internal storage for testing
    log_info "Deploying to internal app storage..."
    adb shell run-as "$APP_PACKAGE" "mkdir -p files/models" || true
    
    local internal_count=0
    for model_file in "$gguf_dir"/*.gguf; do
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
    log_section "Download Summary Report"
    
    local transformers_dir="$MODELS_BASE_DIR/transformers"
    local gguf_dir="$MODELS_BASE_DIR/gguf"
    local ct2_dir="$MODELS_BASE_DIR/ct2"
    
    echo "📊 Model Download Summary:"
    echo ""
    
    # PyTorch models
    local pytorch_count=$(find "$transformers_dir" -name "config.json" 2>/dev/null | wc -l)
    echo "🐍 PyTorch (Transformers): $pytorch_count models"
    find "$transformers_dir" -maxdepth 1 -type d -name "whisper-*" 2>/dev/null | while read -r dir; do
        if [[ -d "$dir" ]]; then
            local size=$(du -sh "$dir" 2>/dev/null | cut -f1)
            echo "   • $(basename "$dir"): $size"
        fi
    done
    
    # GGUF models
    local gguf_count=$(find "$gguf_dir" -name "*.gguf" 2>/dev/null | wc -l)
    echo ""
    echo "🔧 GGUF (whisper.cpp): $gguf_count models"
    find "$gguf_dir" -name "*.gguf" 2>/dev/null | while read -r file; do
        if [[ -f "$file" ]]; then
            local size=$(du -sh "$file" 2>/dev/null | cut -f1)
            echo "   • $(basename "$file"): $size"
        fi
    done
    
    # CTranslate2 models
    local ct2_count=$(find "$ct2_dir" -name "config.json" 2>/dev/null | wc -l)
    echo ""
    echo "⚡ CTranslate2 (faster-whisper): $ct2_count models"
    find "$ct2_dir" -maxdepth 1 -type d -name "faster-whisper-*" 2>/dev/null | while read -r dir; do
        if [[ -d "$dir" ]]; then
            local size=$(du -sh "$dir" 2>/dev/null | cut -f1)
            echo "   • $(basename "$dir"): $size"
        fi
    done
    
    # Total size
    local total_size=$(du -sh "$MODELS_BASE_DIR" 2>/dev/null | cut -f1)
    echo ""
    echo "💾 Total storage used: $total_size"
    
    echo ""
    echo "📱 Android deployment paths:"
    echo "   • Scoped storage: /storage/emulated/0/Android/data/$APP_PACKAGE/files/MiraWhisper/models/"
    echo "   • Internal storage: /data/data/$APP_PACKAGE/files/models/"
    
    echo ""
    echo "🔧 Usage examples:"
    echo "   • GGUF: Use with whisper.cpp JNI"
    echo "   • PyTorch: Use with transformers library"
    echo "   • CTranslate2: Use with faster-whisper library"
}

# Main execution
main() {
    log_section "Comprehensive Whisper Model Download"
    echo "This script will download all Whisper models across main formats:"
    echo "• PyTorch (Transformers) - Official OpenAI models"
    echo "• GGUF (whisper.cpp) - Preconverted quantized models"
    echo "• CTranslate2 (faster-whisper) - Optimized inference models"
    echo ""
    echo "Models will be downloaded to: $MODELS_BASE_DIR"
    echo "Android app package: $APP_PACKAGE"
    echo ""
    
    read -p "Continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "Download cancelled by user"
        exit 0
    fi
    
    # Execute download steps
    setup_environment
    download_pytorch_models
    download_gguf_models
    download_ct2_models
    deploy_to_android
    generate_summary
    
    # Cleanup
    deactivate
    rm -rf .venv
    
    log_section "Download Complete!"
    log_success "All Whisper models have been downloaded and deployed"
    echo ""
    echo "Next steps:"
    echo "1. Test model loading in your Android app"
    echo "2. Verify GGUF models work with whisper.cpp JNI"
    echo "3. Use PyTorch models for development/testing"
    echo "4. Use CTranslate2 models for optimized inference"
}

# Handle command line arguments
case "${1:-}" in
    "pytorch"|"transformers")
        setup_environment
        download_pytorch_models
        ;;
    "gguf"|"whisper.cpp")
        setup_environment
        download_gguf_models
        ;;
    "ct2"|"ctranslate2"|"faster-whisper")
        setup_environment
        download_ct2_models
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
        echo "  (no args)  Download all model formats"
        echo "  pytorch    Download only PyTorch models"
        echo "  gguf       Download only GGUF models"
        echo "  ct2        Download only CTranslate2 models"
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
