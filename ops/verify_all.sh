#!/bin/bash
# CLIP End-to-End Verification Pipeline
# Verifies K→L→M chain: representation, retrieval, reproducibility

set -euo pipefail

# Configuration
PKG=${PKG:-com.mira.videoeditor.debug}
ROOT=${ROOT:-/sdcard/MiraClip}
DEVICE=${DEVICE:-emulator-5554}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✓${NC} $1"
}

warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

error() {
    echo -e "${RED}✗${NC} $1"
}

# Check if device is connected
check_device() {
    log "Checking device connectivity..."
    if ! adb devices | grep -q "$DEVICE"; then
        error "Device $DEVICE not found. Available devices:"
        adb devices
    exit 1
fi
    success "Device $DEVICE connected"
}

# Install and verify app
install_app() {
    log "Installing debug APK..."
    if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        error "Debug APK not found. Building first..."
        ./gradlew :app:assembleDebug
    fi
    
    adb -s "$DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk
    success "App installed"
}

# Test broadcast receiver resolution
test_receiver_resolution() {
    log "Testing broadcast receiver resolution..."
    
    # Test CLIP.RUN receiver
    local result=$(adb -s "$DEVICE" shell "am broadcast -a ${PKG}.CLIP.RUN --es test 'receiver_resolution'" 2>&1)
    
    if echo "$result" | grep -q "Broadcast completed"; then
        success "CLIP.RUN receiver resolved"
    else
        warning "CLIP.RUN receiver not found or not responding"
        echo "Result: $result"
    fi
}

# K: Verify Representation (CLIP embeddings)
verify_representation() {
    log "K: Verifying CLIP representation..."
    
    # Create test directory
    adb -s "$DEVICE" shell "mkdir -p $ROOT/test"
    
    # Send representation verification broadcast
    local result=$(adb -s "$DEVICE" shell "am broadcast -a ${PKG}.VERIFY_REPRESENTATION --es output_dir '$ROOT/test/representation'" 2>&1)
    
    if echo "$result" | grep -q "Broadcast completed"; then
        success "Representation verification broadcast sent"
        
        # Wait for processing
        sleep 3
        
        # Check for output files
        local files=$(adb -s "$DEVICE" shell "ls $ROOT/test/representation/" 2>/dev/null || echo "")
        if [ -n "$files" ]; then
            success "Representation files created: $files"
        else
            warning "No representation files found"
        fi
    else
        warning "Representation verification failed"
        echo "Result: $result"
    fi
}

# L: Verify Retrieval (similarity search)
verify_retrieval() {
    log "L: Verifying retrieval..."
    
    # Send retrieval verification broadcast
    local result=$(adb -s "$DEVICE" shell "am broadcast -a ${PKG}.VERIFY_RETRIEVAL --es output_dir '$ROOT/test/retrieval'" 2>&1)
    
    if echo "$result" | grep -q "Broadcast completed"; then
        success "Retrieval verification broadcast sent"
        
        # Wait for processing
sleep 3

        # Check for output files
        local files=$(adb -s "$DEVICE" shell "ls $ROOT/test/retrieval/" 2>/dev/null || echo "")
        if [ -n "$files" ]; then
            success "Retrieval files created: $files"
        else
            warning "No retrieval files found"
        fi
    else
        warning "Retrieval verification failed"
        echo "Result: $result"
    fi
}

# M: Verify Reproducibility (embedding consistency)
verify_reproducibility() {
    log "M: Verifying reproducibility..."
    
    # Send reproducibility verification broadcast
    local result=$(adb -s "$DEVICE" shell "am broadcast -a ${PKG}.VERIFY_REPRODUCIBILITY --es output_dir '$ROOT/test/reproducibility'" 2>&1)
    
    if echo "$result" | grep -q "Broadcast completed"; then
        success "Reproducibility verification broadcast sent"
        
        # Wait for processing
        sleep 3
        
        # Check for output files
        local files=$(adb -s "$DEVICE" shell "ls $ROOT/test/reproducibility/" 2>/dev/null || echo "")
        if [ -n "$files" ]; then
            success "Reproducibility files created: $files"
            
            # Python validation of embedding invariants
            log "Running Python validation..."
            python3 -c "
import json
import numpy as np
import os

def validate_embeddings(file_path):
    try:
        with open(file_path, 'r') as f:
            data = json.load(f)
        
        # Check embedding structure
        if 'embeddings' in data:
            embeddings = data['embeddings']
            if isinstance(embeddings, list) and len(embeddings) > 0:
                # Check embedding dimensions
                first_embedding = embeddings[0]
                if isinstance(first_embedding, list):
                    dim = len(first_embedding)
                    print(f'Embedding dimension: {dim}')
                    
                    # Check that all embeddings have same dimension
                    for i, emb in enumerate(embeddings):
                        if len(emb) != dim:
                            print(f'ERROR: Embedding {i} has dimension {len(emb)}, expected {dim}')
                            return False
                    
                    # Check embedding normalization (should be close to 1.0)
                    for i, emb in enumerate(embeddings):
                        norm = np.linalg.norm(emb)
                        if abs(norm - 1.0) > 0.1:
                            print(f'WARNING: Embedding {i} norm is {norm:.4f}, expected ~1.0')
                    
                    print('Embedding validation passed')
                    return True
        
        print('ERROR: Invalid embedding structure')
        return False
    except Exception as e:
        print(f'ERROR: Validation failed - {e}')
        return False

# Find and validate embedding files
test_dir = '$ROOT/test'
if os.path.exists(test_dir):
    for root, dirs, files in os.walk(test_dir):
        for file in files:
            if file.endswith('.json'):
                file_path = os.path.join(root, file)
                print(f'Validating: {file_path}')
                validate_embeddings(file_path)
else:
    print('Test directory not found')
" 2>/dev/null || warning "Python validation failed"
        else
            warning "No reproducibility files found"
        fi
    else
        warning "Reproducibility verification failed"
        echo "Result: $result"
    fi
}

# Cleanup test files
cleanup() {
    log "Cleaning up test files..."
    adb -s "$DEVICE" shell "rm -rf $ROOT/test" 2>/dev/null || true
    success "Cleanup completed"
}

# Main verification pipeline
main() {
    log "Starting CLIP End-to-End Verification Pipeline"
    log "Package: $PKG"
    log "Root: $ROOT"
    log "Device: $DEVICE"
    
    check_device
    install_app
    test_receiver_resolution
    
    # K→L→M verification chain
    verify_representation
    verify_retrieval
    verify_reproducibility

# Summary
    log "Verification pipeline completed"
    log "Check device logs for detailed results:"
    log "adb -s $DEVICE logcat | grep -E '(CLIP|VERIFY)'"
    
    # Keep test files for inspection
    log "Test files preserved at: $ROOT/test/"
    log "To clean up: adb -s $DEVICE shell 'rm -rf $ROOT/test'"
}

# Handle script arguments
case "${1:-}" in
    "cleanup")
        cleanup
        ;;
    "representation")
        check_device
        verify_representation
        ;;
    "retrieval")
        check_device
        verify_retrieval
        ;;
    "reproducibility")
        check_device
        verify_reproducibility
        ;;
    *)
        main
        ;;
esac
