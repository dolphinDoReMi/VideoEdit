#!/usr/bin/env bash
set -euo pipefail

# CI/CD Protection Pre-commit Hook
# Prevents accidental modifications to working CI/CD pipeline

echo "🔍 Checking for CI/CD modifications..."

# Get current branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

# Get list of staged files
STAGED_FILES=$(git diff --cached --name-only)

# Critical CI/CD files that require special approval
CRITICAL_CICD_FILES=(
    ".github/workflows/ci.yml"
    ".github/workflows/whisper-branch-ci.yml"
    ".github/workflows/feature-branch-ci.yml"
    ".github/workflows/release.yml"
)

# CI/CD workflow patterns that are protected
CICD_WORKFLOW_PATTERN="\.github/workflows/.*\.yml$"

# Check if any critical CI/CD files are being modified
CICD_MODIFICATIONS=()
for file in "${CRITICAL_CICD_FILES[@]}"; do
    if echo "$STAGED_FILES" | grep -q "^$file$"; then
        CICD_MODIFICATIONS+=("$file")
    fi
done

# Check for NEW CI/CD workflow files being created
NEW_CICD_WORKFLOWS=()
for file in $STAGED_FILES; do
    if echo "$file" | grep -qE "$CICD_WORKFLOW_PATTERN"; then
        # Check if this is a new file (not in critical list)
        is_critical=false
        for critical_file in "${CRITICAL_CICD_FILES[@]}"; do
            if [[ "$file" == "$critical_file" ]]; then
                is_critical=true
                break
            fi
        done
        
        if [ "$is_critical" = false ]; then
            NEW_CICD_WORKFLOWS+=("$file")
        fi
    fi
done

# Initialize arrays if empty
if [ ${#CICD_MODIFICATIONS[@]} -eq 0 ]; then
    CICD_MODIFICATIONS=()
fi
if [ ${#NEW_CICD_WORKFLOWS[@]} -eq 0 ]; then
    NEW_CICD_WORKFLOWS=()
fi

# Check for NEW CI/CD workflow files first (highest priority)
if [ ${#NEW_CICD_WORKFLOWS[@]} -gt 0 ]; then
    echo "🚨 CRITICAL: New CI/CD workflow files are being created!"
    echo ""
    echo "New workflow files:"
    for file in "${NEW_CICD_WORKFLOWS[@]}"; do
        echo "  - $file"
    done
    echo ""
    echo "❌ BLOCKING COMMIT: New CI/CD workflow creation is NOT ALLOWED!"
    echo ""
    echo "Creating new CI/CD workflows requires:"
    echo "  1. Feature branch with detailed justification"
    echo "  2. Pull request with comprehensive explanation"
    echo "  3. Approval from CI/CD maintainers"
    echo "  4. Full testing and validation"
    echo "  5. Documentation updates"
    echo ""
    echo "Current branch: $CURRENT_BRANCH"
    echo ""
    
    if [[ "$CURRENT_BRANCH" == "main" ]]; then
        echo "❌ CRITICAL ERROR: Attempting to create CI/CD workflow on main branch!"
        echo "This is strictly prohibited and poses a security risk."
        exit 1
    fi
    
    echo "Please:"
    echo "  1. Remove the new CI/CD workflow files"
    echo "  2. Create a feature branch: git checkout -b feature/cicd-workflow-name"
    echo "  3. Document the need for the new workflow"
    echo "  4. Create a pull request with detailed justification"
    echo "  5. Get approval from CI/CD maintainers"
    echo ""
    echo "Emergency override (NOT RECOMMENDED):"
    echo "  CICD_CHANGE_APPROVED=true git commit"
    echo ""
    
    # Block the commit
    exit 1
fi

# If CI/CD files are being modified, check for approval
if [ ${#CICD_MODIFICATIONS[@]} -gt 0 ]; then
    echo "⚠️  WARNING: Critical CI/CD files are being modified!"
    echo ""
    echo "Modified files:"
    for file in "${CICD_MODIFICATIONS[@]}"; do
        echo "  - $file"
    done
    echo ""
    
    # Check if this is a feature branch (allowed for feature-branch-ci.yml)
    CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    ALLOWED_FEATURE_MODIFICATIONS=(
        ".github/workflows/feature-branch-ci.yml"
    )
    
    # Allow feature branch CI modifications on feature branches
    if [[ "$CURRENT_BRANCH" == feature/* ]] || [[ "$CURRENT_BRANCH" == feat/* ]]; then
        ALLOWED_MODIFICATIONS=()
        for file in "${CICD_MODIFICATIONS[@]}"; do
            for allowed in "${ALLOWED_FEATURE_MODIFICATIONS[@]}"; do
                if [[ "$file" == "$allowed" ]]; then
                    ALLOWED_MODIFICATIONS+=("$file")
                fi
            done
        done
        
        if [ ${#ALLOWED_MODIFICATIONS[@]} -gt 0 ]; then
            echo "✅ Feature branch CI modifications allowed on feature branch: $CURRENT_BRANCH"
            echo "   Allowed modifications: ${ALLOWED_MODIFICATIONS[*]}"
            echo ""
        fi
    fi
    
    # Check for main branch CI modifications (most critical)
    MAIN_CICD_MODIFICATIONS=()
    for file in "${CICD_MODIFICATIONS[@]}"; do
        if [[ "$file" == ".github/workflows/ci.yml" ]]; then
            MAIN_CICD_MODIFICATIONS+=("$file")
        fi
    done
    
    if [ ${#MAIN_CICD_MODIFICATIONS[@]} -gt 0 ]; then
        echo "🚨 CRITICAL: Main branch CI/CD is being modified!"
        echo ""
        echo "Main branch CI/CD modifications require:"
        echo "  1. Feature branch (not direct main branch modification)"
        echo "  2. Pull request with detailed justification"
        echo "  3. Code review from CI/CD maintainers"
        echo "  4. Full CI/CD pipeline testing"
        echo ""
        echo "Current branch: $CURRENT_BRANCH"
        echo ""
        
        if [[ "$CURRENT_BRANCH" == "main" ]]; then
            echo "❌ BLOCKING COMMIT: Direct main branch CI/CD modification detected!"
            echo ""
            echo "Please:"
            echo "  1. Create a feature branch: git checkout -b feature/cicd-improvement"
            echo "  2. Make your changes on the feature branch"
            echo "  3. Create a pull request with detailed justification"
            echo "  4. Get approval from CI/CD maintainers"
            echo ""
            exit 1
        fi
    fi
    
    # Check for approval environment variable
    if [[ "${CICD_CHANGE_APPROVED:-}" != "true" ]]; then
        echo "⚠️  CI/CD modifications detected without approval!"
        echo ""
        echo "To proceed with CI/CD changes, set:"
        echo "  export CICD_CHANGE_APPROVED=true"
        echo ""
        echo "Or use the approved commit command:"
        echo "  CICD_CHANGE_APPROVED=true git commit"
        echo ""
        echo "Make sure you have:"
        echo "  ✅ Created a feature branch"
        echo "  ✅ Documented the changes"
        echo "  ✅ Tested the CI/CD pipeline"
        echo "  ✅ Ready for code review"
        echo ""
        
        # Allow with warning for feature branches
        if [[ "$CURRENT_BRANCH" == feature/* ]] || [[ "$CURRENT_BRANCH" == feat/* ]]; then
            echo "⚠️  Proceeding with warning (feature branch detected)"
            echo "   Please ensure proper testing and review process"
        else
            echo "❌ BLOCKING COMMIT: CI/CD modifications require approval"
            exit 1
        fi
    else
        echo "✅ CI/CD modifications approved"
    fi
fi

# Check for removal of critical CI/CD files
if [ ${#CICD_MODIFICATIONS[@]} -gt 0 ]; then
    for file in "${CRITICAL_CICD_FILES[@]}"; do
        if git diff --cached --name-status | grep -q "^D.*$file$"; then
            echo "❌ BLOCKING COMMIT: Attempting to delete critical CI/CD file: $file"
            echo ""
            echo "Critical CI/CD files cannot be deleted without:"
            echo "  1. Feature branch with proper justification"
            echo "  2. Pull request with detailed explanation"
            echo "  3. Approval from CI/CD maintainers"
            echo "  4. Migration plan for any dependencies"
            echo ""
            exit 1
        fi
    done
fi

# Check for CI/CD syntax errors
if [ ${#CICD_MODIFICATIONS[@]} -gt 0 ]; then
    for file in "${CICD_MODIFICATIONS[@]}"; do
        if [[ "$file" == *.yml ]] || [[ "$file" == *.yaml ]]; then
            echo "🔍 Validating CI/CD syntax for: $file"
            
            # Basic YAML syntax check
            if ! python3 -c "import yaml; yaml.safe_load(open('$file'))" 2>/dev/null; then
                echo "❌ YAML syntax error in: $file"
                echo "Please fix syntax errors before committing"
                exit 1
            fi
            
            # Check for required GitHub Actions structure
            if ! grep -q "name:" "$file" || ! grep -q "on:" "$file" || ! grep -q "jobs:" "$file"; then
                echo "❌ Invalid GitHub Actions workflow structure in: $file"
                echo "Workflow must have 'name:', 'on:', and 'jobs:' sections"
                exit 1
            fi
            
            echo "✅ CI/CD syntax validation passed for: $file"
        fi
    done
fi

echo "✅ CI/CD protection check passed"
echo ""
echo "📋 CI/CD Change Summary:"
if [ ${#NEW_CICD_WORKFLOWS[@]} -gt 0 ]; then
    echo "  🚨 NEW WORKFLOW FILES BLOCKED: ${NEW_CICD_WORKFLOWS[*]}"
    echo "  Current branch: $CURRENT_BRANCH"
    echo "  Status: BLOCKED - New CI/CD workflows not allowed"
elif [ ${#CICD_MODIFICATIONS[@]} -gt 0 ]; then
    echo "  Modified files: ${CICD_MODIFICATIONS[*]}"
    echo "  Current branch: $CURRENT_BRANCH"
    echo "  Approval status: ${CICD_CHANGE_APPROVED:-not approved}"
    echo ""
    echo "⚠️  Remember to:"
    echo "  - Test CI/CD pipeline thoroughly"
    echo "  - Create pull request with justification"
    echo "  - Get code review from CI/CD maintainers"
    echo "  - Update CI/CD documentation if needed"
else
    echo "  No CI/CD modifications detected"
fi
