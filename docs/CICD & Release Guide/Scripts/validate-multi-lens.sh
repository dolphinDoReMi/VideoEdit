#!/bin/bash
# docs/CICD & Release Guide/Scripts/validate_multi_lens.sh

echo "🔍 Validating multi-lens expert communication format..."

features=("clip" "whisper" "infra")
for feature in "${features[@]}"; do
    echo "🔍 Checking multi-lens format for: $feature"
    file="docs/$feature/README.md"
    
    # Check for required expert sections
    required_sections=(
        "Plain-text:"
        "For a Recommendation System Expert"
        "For a Deep Learning Expert"
        "For a Content Understanding Expert"
        "For an Audio/Video Expert"
    )
    
    for section in "${required_sections[@]}"; do
        if ! grep -q "$section" "$file"; then
            echo "❌ Missing section '$section' in $file"
            exit 1
        fi
    done
    
    # Check for proper section formatting
    if ! grep -q "### [0-9]/.*Plain-text:" "$file"; then
        echo "❌ Plain-text section format incorrect in $file"
        exit 1
    fi
    
    if ! grep -q "### [0-9]/.*For a Recommendation System Expert" "$file"; then
        echo "❌ Recommendation System Expert section format incorrect in $file"
        exit 1
    fi
    
    if ! grep -q "### [0-9]/.*For a Deep Learning Expert" "$file"; then
        echo "❌ Deep Learning Expert section format incorrect in $file"
        exit 1
    fi
    
    if ! grep -q "### [0-9]/.*For a Content Understanding Expert" "$file"; then
        echo "❌ Content Understanding Expert section format incorrect in $file"
        exit 1
    fi
    
    if ! grep -q "### [0-9]/.*For an Audio/Video Expert" "$file"; then
        echo "❌ Audio/Video Expert section format incorrect in $file"
        exit 1
    fi
    
    echo "✅ Multi-lens format valid for $feature"
done

echo "✅ Multi-lens expert communication validation passed"
