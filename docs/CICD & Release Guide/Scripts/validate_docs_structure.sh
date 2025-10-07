#!/bin/bash
# docs/CICD & Release Guide/Scripts/validate_docs_structure.sh

echo "🔍 Validating documentation structure..."

# Check required folders
required_folders=("clip" "whisper" "infra" "CICD & Release Guide")
for folder in "${required_folders[@]}"; do
    if [ ! -d "docs/$folder" ]; then
        echo "❌ Missing required folder: docs/$folder"
        exit 1
    fi
    echo "✅ Found folder: docs/$folder"
done

# Check required files in each feature folder
features=("clip" "whisper" "infra")
for feature in "${features[@]}"; do
    echo "🔍 Checking feature: $feature"
    
    required_files=(
        "Architecture Design and Control Knot.md"
        "Full scale implementation Details.md"
        "XiaoMi Pad Inference Optimization.md"
        "iPad Inference Optimization.md"
        "README.md"
    )
    
    for file in "${required_files[@]}"; do
        if [ ! -f "docs/$feature/$file" ]; then
            echo "❌ Missing required file: docs/$feature/$file"
            exit 1
        fi
        echo "✅ Found file: docs/$feature/$file"
    done
    
    # Check Scripts folder
    if [ ! -d "docs/$feature/Scripts" ]; then
        echo "❌ Missing Scripts folder: docs/$feature/Scripts"
        exit 1
    fi
    echo "✅ Found Scripts folder: docs/$feature/Scripts"
done

# Check CI/CD folder files
cicd_files=(
    "GitHub Guide.md"
    "XiaoMi Pad Inference Optimization.md"
    "iPad Inference Optimization.md"
    "README.md"
)

for file in "${cicd_files[@]}"; do
    if [ ! -f "docs/CICD & Release Guide/$file" ]; then
        echo "❌ Missing required file: docs/CICD & Release Guide/$file"
        exit 1
    fi
    echo "✅ Found file: docs/CICD & Release Guide/$file"
done

# Check root documentation files
root_files=("cursor_rule.md" "README.md")
for file in "${root_files[@]}"; do
    if [ ! -f "docs/$file" ]; then
        echo "❌ Missing required file: docs/$file"
        exit 1
    fi
    echo "✅ Found file: docs/$file"
done

echo "✅ Documentation structure validation passed"
