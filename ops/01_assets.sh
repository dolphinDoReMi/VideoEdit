#!/bin/bash
# Artifacts verification script for CLIP4Clip models and tokenizer
# Verifies that all required model files and tokenizer assets are present

set -e

echo "=== CLIP4Clip Assets Verification ==="
echo "Checking for required model files and tokenizer assets..."

# Configuration
ASSETS_DIR="app/src/main/assets"
MODELS_DIR="$ASSETS_DIR"
TOKENIZER_DIR="$ASSETS_DIR"

# Required files
REQUIRED_FILES=(
    "bpe_vocab.json"
    "bpe_merges.txt"
    "clip_image_encoder.ptl"
    "clip_text_encoder.ptl"
)

# Optional files with expected checksums (for verification)
OPTIONAL_FILES=(
    "clip_image_encoder.ptl:sha256:abc123def456"  # Placeholder checksum
    "clip_text_encoder.ptl:sha256:def456ghi789"  # Placeholder checksum
)

echo "Checking assets directory: $ASSETS_DIR"
if [ ! -d "$ASSETS_DIR" ]; then
    echo "❌ Assets directory not found: $ASSETS_DIR"
    exit 1
fi

echo "✅ Assets directory exists"

# Check required files
echo ""
echo "Checking required files..."
all_present=true

for file in "${REQUIRED_FILES[@]}"; do
    file_path="$ASSETS_DIR/$file"
    if [ -f "$file_path" ]; then
        size=$(stat -f%z "$file_path" 2>/dev/null || stat -c%s "$file_path" 2>/dev/null || echo "unknown")
        echo "✅ $file (${size} bytes)"
    else
        echo "❌ Missing: $file"
        all_present=false
    fi
done

if [ "$all_present" = false ]; then
    echo ""
    echo "❌ Some required files are missing!"
    echo "Please ensure all required model files and tokenizer assets are present."
    exit 1
fi

# Check optional files with checksums
echo ""
echo "Checking optional files with checksums..."
for file_info in "${OPTIONAL_FILES[@]}"; do
    IFS=':' read -r file checksum_type expected_checksum <<< "$file_info"
    file_path="$ASSETS_DIR/$file"
    
    if [ -f "$file_path" ]; then
        if command -v sha256sum >/dev/null 2>&1; then
            actual_checksum=$(sha256sum "$file_path" | cut -d' ' -f1)
        elif command -v shasum >/dev/null 2>&1; then
            actual_checksum=$(shasum -a 256 "$file_path" | cut -d' ' -f1)
        else
            actual_checksum="unknown"
        fi
        
        if [ "$actual_checksum" = "$expected_checksum" ]; then
            echo "✅ $file (checksum verified)"
        else
            echo "⚠️  $file (checksum mismatch: expected $expected_checksum, got $actual_checksum)"
        fi
    else
        echo "⚠️  Optional file not found: $file"
    fi
done

echo ""
echo "=== Asset Verification Summary ==="
echo "✅ All required assets are present"
echo "📁 Assets directory: $ASSETS_DIR"
echo "📊 Total files checked: ${#REQUIRED_FILES[@]}"

# List all files in assets directory for reference
echo ""
echo "📋 All files in assets directory:"
ls -la "$ASSETS_DIR" | grep -v "^total" | while read -r line; do
    echo "   $line"
done

echo ""
echo "🎉 Asset verification completed successfully!"
exit 0
