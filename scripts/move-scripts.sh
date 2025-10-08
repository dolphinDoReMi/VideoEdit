#!/bin/bash

# Script to move and rename all .sh files to scripts/ directory with kebab-case naming

# Function to convert to kebab-case
to_kebab_case() {
    echo "$1" | sed 's/_/-/g' | tr '[:upper:]' '[:lower:]'
}

# Create scripts directory if it doesn't exist
mkdir -p scripts

# Counter for tracking
count=0
moved=0
skipped=0

echo "Moving and renaming shell scripts to scripts/ directory..."

# Find all .sh files excluding temp directories and docs
while IFS= read -r -d '' file; do
    count=$((count + 1))
    
    # Get the filename without path
    filename=$(basename "$file")
    
    # Convert to kebab-case
    new_name=$(to_kebab_case "$filename")
    
    # Skip if already in scripts directory
    if [[ "$file" == "./scripts/"* ]]; then
        echo "Skipping (already in scripts): $file"
        skipped=$((skipped + 1))
        continue
    fi
    
    # Skip temp directories
    if [[ "$file" == *"/temp_whisper_cpp/"* ]] || [[ "$file" == *"/whisper_real/"* ]]; then
        echo "Skipping (temp directory): $file"
        skipped=$((skipped + 1))
        continue
    fi
    
    # Skip if already exists in scripts
    if [[ -f "scripts/$new_name" ]]; then
        echo "Skipping (already exists): $file -> scripts/$new_name"
        skipped=$((skipped + 1))
        continue
    fi
    
    # Move and preserve permissions
    if cp "$file" "scripts/$new_name" && chmod +x "scripts/$new_name"; then
        echo "Moved: $file -> scripts/$new_name"
        moved=$((moved + 1))
    else
        echo "Failed to move: $file"
    fi
    
done < <(find . -name "*.sh" -type f -print0)

echo ""
echo "Summary:"
echo "Total scripts found: $count"
echo "Moved: $moved"
echo "Skipped: $skipped"
