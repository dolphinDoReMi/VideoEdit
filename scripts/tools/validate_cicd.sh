#!/usr/bin/env bash
set -euo pipefail

# CI/CD Validation Script
# Validates CI/CD configuration integrity and detects issues

echo "🔍 CI/CD Configuration Validation"
echo "================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Validation results
VALIDATION_PASSED=true
ISSUES_FOUND=0

# Function to report issues
report_issue() {
    echo -e "${RED}❌ $1${NC}"
    VALIDATION_PASSED=false
    ((ISSUES_FOUND++))
}

# Function to report warnings
report_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Function to report success
report_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    report_issue "Not in a git repository"
    exit 1
fi

echo ""
echo "1. Checking CI/CD File Structure"
echo "--------------------------------"

# Required CI/CD files
REQUIRED_FILES=(
    ".github/workflows/ci.yml"
    ".github/workflows/whisper-branch-ci.yml"
    ".github/workflows/feature-branch-ci.yml"
    ".github/workflows/release.yml"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        report_success "Found: $file"
    else
        report_issue "Missing required file: $file"
    fi
done

echo ""
echo "2. Validating YAML Syntax"
echo "-------------------------"

# Check YAML syntax for all workflow files
for file in .github/workflows/*.yml; do
    if [ -f "$file" ]; then
        echo "Validating: $file"
        
        # Basic YAML syntax check
        if python3 -c "import yaml; yaml.safe_load(open('$file'))" 2>/dev/null; then
            report_success "YAML syntax valid: $file"
        else
            report_issue "YAML syntax error in: $file"
        fi
        
        # Check for required GitHub Actions structure
        if grep -q "name:" "$file" && grep -q "on:" "$file" && grep -q "jobs:" "$file"; then
            report_success "GitHub Actions structure valid: $file"
        else
            report_issue "Invalid GitHub Actions structure in: $file"
        fi
    fi
done

echo ""
echo "3. Checking CI/CD Dependencies"
echo "-----------------------------"

# Check for required scripts
REQUIRED_SCRIPTS=(
    "scripts/thread_suffix.sh"
    "scripts/thread_suffix_ios.sh"
    "scripts/tools/cicd_protection_hook.sh"
)

for script in "${REQUIRED_SCRIPTS[@]}"; do
    if [ -f "$script" ]; then
        if [ -x "$script" ]; then
            report_success "Executable script found: $script"
        else
            report_warning "Script not executable: $script"
            chmod +x "$script"
            report_success "Made executable: $script"
        fi
    else
        report_issue "Missing required script: $script"
    fi
done

echo ""
echo "4. Validating Main Branch CI/CD"
echo "-------------------------------"

# Check main branch CI/CD specific requirements
MAIN_CICD_FILE=".github/workflows/ci.yml"
if [ -f "$MAIN_CICD_FILE" ]; then
    echo "Validating main branch CI/CD configuration..."
    
    # Check for required jobs
    REQUIRED_JOBS=(
        "detect"
        "android-build"
        "android-unit-tests"
        "android-instrumented-tests"
        "whisper-validation"
        "deployment-status"
    )
    
    for job in "${REQUIRED_JOBS[@]}"; do
        if grep -q "  $job:" "$MAIN_CICD_FILE"; then
            report_success "Required job found: $job"
        else
            report_issue "Missing required job: $job"
        fi
    done
    
    # Check for Android SDK setup
    if grep -q "android-actions/setup-android" "$MAIN_CICD_FILE"; then
        report_success "Android SDK setup configured"
    else
        report_issue "Android SDK setup missing"
    fi
    
    # Check for Gradle caching
    if grep -q "actions/cache" "$MAIN_CICD_FILE"; then
        report_success "Gradle caching configured"
    else
        report_warning "Gradle caching not configured (performance impact)"
    fi
    
    # Check for artifact uploads
    if grep -q "actions/upload-artifact" "$MAIN_CICD_FILE"; then
        report_success "Artifact uploads configured"
    else
        report_warning "Artifact uploads not configured"
    fi
fi

echo ""
echo "5. Checking Git Hooks"
echo "--------------------"

# Check if git hooks are installed
if [ -f ".git/hooks/pre-commit" ]; then
    if grep -q "cicd_protection_hook.sh" ".git/hooks/pre-commit"; then
        report_success "CI/CD protection hook installed"
    else
        report_warning "CI/CD protection hook not found in pre-commit"
    fi
else
    report_warning "Pre-commit hook not installed"
    echo "  Run: ./scripts/tools/install_git_hooks.sh"
fi

if [ -f ".git/hooks/commit-msg" ]; then
    report_success "Commit message hook installed"
else
    report_warning "Commit message hook not installed"
fi

if [ -f ".git/hooks/pre-push" ]; then
    report_success "Pre-push hook installed"
else
    report_warning "Pre-push hook not installed"
fi

echo ""
echo "6. Checking Branch Protection"
echo "-----------------------------"

# Check current branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "Current branch: $CURRENT_BRANCH"

if [ "$CURRENT_BRANCH" = "main" ]; then
    report_success "On main branch (protected)"
    
    # Check for uncommitted changes
    if git diff --quiet && git diff --cached --quiet; then
        report_success "No uncommitted changes"
    else
        report_warning "Uncommitted changes detected"
    fi
else
    report_success "On feature branch: $CURRENT_BRANCH"
fi

echo ""
echo "7. Performance Checks"
echo "--------------------"

# Check for potential performance issues
for file in .github/workflows/*.yml; do
    if [ -f "$file" ]; then
        # Check for timeout settings
        if grep -q "timeout-minutes" "$file"; then
            report_success "Timeout configured in: $file"
        else
            report_warning "No timeout configured in: $file"
        fi
        
        # Check for concurrency settings
        if grep -q "concurrency:" "$file"; then
            report_success "Concurrency configured in: $file"
        else
            report_warning "No concurrency configured in: $file"
        fi
    fi
done

echo ""
echo "8. Security Checks"
echo "------------------"

# Check for security best practices
for file in .github/workflows/*.yml; do
    if [ -f "$file" ]; then
        # Check for secret usage
        if grep -q "secrets\." "$file"; then
            report_success "Secrets properly referenced in: $file"
        else
            report_warning "No secrets found in: $file (may be intentional)"
        fi
        
        # Check for GITHUB_TOKEN usage
        if grep -q "GITHUB_TOKEN" "$file"; then
            report_success "GITHUB_TOKEN properly used in: $file"
        fi
    fi
done

echo ""
echo "================================="
echo "Validation Summary"
echo "================================="

if [ "$VALIDATION_PASSED" = true ]; then
    echo -e "${GREEN}✅ CI/CD validation PASSED${NC}"
    echo -e "${GREEN}   All checks completed successfully${NC}"
    exit 0
else
    echo -e "${RED}❌ CI/CD validation FAILED${NC}"
    echo -e "${RED}   Issues found: $ISSUES_FOUND${NC}"
    echo ""
    echo "Please fix the issues above before proceeding."
    echo "Refer to docs/ci-cd/CI-CD-PROTECTION-POLICY.md for guidance."
    exit 1
fi
