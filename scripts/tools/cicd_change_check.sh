#!/usr/bin/env bash
set -euo pipefail

# CI/CD Change Detection Script
# Detects and analyzes CI/CD modifications

echo "🔍 CI/CD Change Detection"
echo "========================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get the comparison base (default to HEAD~1, can be overridden)
BASE_COMMIT="${1:-HEAD~1}"
CURRENT_COMMIT="HEAD"

echo "Comparing: $BASE_COMMIT -> $CURRENT_COMMIT"
echo ""

# Critical CI/CD files
CRITICAL_CICD_FILES=(
    ".github/workflows/ci.yml"
    ".github/workflows/whisper-branch-ci.yml"
    ".github/workflows/feature-branch-ci.yml"
    ".github/workflows/release.yml"
)

# Check for CI/CD modifications
CICD_CHANGES=()
CICD_ADDITIONS=()
CICD_MODIFICATIONS=()
CICD_DELETIONS=()

echo "1. Detecting CI/CD Changes"
echo "-------------------------"

for file in "${CRITICAL_CICD_FILES[@]}"; do
    # Check if file exists in current commit
    if git cat-file -e "$CURRENT_COMMIT:$file" 2>/dev/null; then
        # Check if file exists in base commit
        if git cat-file -e "$BASE_COMMIT:$file" 2>/dev/null; then
            # File exists in both commits, check for modifications
            if ! git diff --quiet "$BASE_COMMIT" "$CURRENT_COMMIT" -- "$file"; then
                CICD_MODIFICATIONS+=("$file")
                CICD_CHANGES+=("$file")
                echo -e "${YELLOW}📝 Modified: $file${NC}"
            fi
        else
            # File added
            CICD_ADDITIONS+=("$file")
            CICD_CHANGES+=("$file")
            echo -e "${GREEN}➕ Added: $file${NC}"
        fi
    else
        # Check if file existed in base commit
        if git cat-file -e "$BASE_COMMIT:$file" 2>/dev/null; then
            # File deleted
            CICD_DELETIONS+=("$file")
            CICD_CHANGES+=("$file")
            echo -e "${RED}➖ Deleted: $file${NC}"
        fi
    fi
done

# Check for other CI/CD related changes
echo ""
echo "2. Detecting Related CI/CD Changes"
echo "----------------------------------"

# Check for changes in CI/CD scripts
CICD_SCRIPTS=(
    "scripts/thread_suffix.sh"
    "scripts/thread_suffix_ios.sh"
    "scripts/tools/cicd_protection_hook.sh"
    "scripts/tools/validate_cicd.sh"
    "scripts/tools/install_git_hooks.sh"
)

for script in "${CICD_SCRIPTS[@]}"; do
    if ! git diff --quiet "$BASE_COMMIT" "$CURRENT_COMMIT" -- "$script" 2>/dev/null; then
        echo -e "${BLUE}🔧 CI/CD Script Modified: $script${NC}"
    fi
done

# Check for changes in CI/CD documentation
CICD_DOCS=(
    "docs/ci-cd/"
    "docs/CICD & Release Guide/"
)

for doc_path in "${CICD_DOCS[@]}"; do
    if [ -d "$doc_path" ]; then
        if ! git diff --quiet "$BASE_COMMIT" "$CURRENT_COMMIT" -- "$doc_path" 2>/dev/null; then
            echo -e "${BLUE}📚 CI/CD Documentation Modified: $doc_path${NC}"
        fi
    fi
done

echo ""
echo "3. Change Analysis"
echo "------------------"

if [ ${#CICD_CHANGES[@]} -eq 0 ]; then
    echo -e "${GREEN}✅ No critical CI/CD changes detected${NC}"
    echo ""
    echo "Safe to proceed with normal development workflow."
    exit 0
fi

echo -e "${YELLOW}⚠️  CI/CD changes detected!${NC}"
echo ""

# Analyze the type of changes
if [ ${#CICD_DELETIONS[@]} -gt 0 ]; then
    echo -e "${RED}🚨 CRITICAL: CI/CD files deleted!${NC}"
    echo "Deleted files:"
    for file in "${CICD_DELETIONS[@]}"; do
        echo "  - $file"
    done
    echo ""
    echo "This is a critical change that requires:"
    echo "  1. Feature branch with detailed justification"
    echo "  2. Pull request with migration plan"
    echo "  3. Approval from CI/CD maintainers"
    echo "  4. Full testing of remaining CI/CD pipeline"
    echo ""
fi

if [ ${#CICD_ADDITIONS[@]} -gt 0 ]; then
    echo -e "${YELLOW}📝 New CI/CD files added:${NC}"
    for file in "${CICD_ADDITIONS[@]}"; do
        echo "  - $file"
    done
    echo ""
fi

if [ ${#CICD_MODIFICATIONS[@]} -gt 0 ]; then
    echo -e "${YELLOW}📝 Existing CI/CD files modified:${NC}"
    for file in "${CICD_MODIFICATIONS[@]}"; do
        echo "  - $file"
    done
    echo ""
fi

echo "4. Impact Assessment"
echo "--------------------"

# Check current branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "Current branch: $CURRENT_BRANCH"

# Assess impact based on branch type
if [ "$CURRENT_BRANCH" = "main" ]; then
    echo -e "${RED}🚨 HIGH IMPACT: Direct main branch CI/CD modification${NC}"
    echo ""
    echo "This change will:"
    echo "  - Immediately affect all main branch builds"
    echo "  - Impact all pull requests to main"
    echo "  - Affect production deployments"
    echo ""
    echo "Required actions:"
    echo "  1. Create feature branch for CI/CD changes"
    echo "  2. Test thoroughly in feature branch"
    echo "  3. Create pull request with detailed justification"
    echo "  4. Get approval from CI/CD maintainers"
    echo "  5. Monitor CI/CD pipeline after merge"
    echo ""
    
elif [[ "$CURRENT_BRANCH" == feature/* ]] || [[ "$CURRENT_BRANCH" == feat/* ]]; then
    echo -e "${YELLOW}⚠️  MEDIUM IMPACT: Feature branch CI/CD modification${NC}"
    echo ""
    echo "This change will:"
    echo "  - Affect CI/CD for this feature branch"
    echo "  - Require testing before merge to main"
    echo ""
    echo "Required actions:"
    echo "  1. Test CI/CD pipeline thoroughly"
    echo "  2. Document changes and justification"
    echo "  3. Get code review from CI/CD maintainers"
    echo "  4. Ensure no breaking changes to main branch CI/CD"
    echo ""
    
else
    echo -e "${BLUE}ℹ️  LOW IMPACT: Non-main branch CI/CD modification${NC}"
    echo ""
    echo "This change will:"
    echo "  - Only affect CI/CD for this branch"
    echo "  - Not impact main branch until merged"
    echo ""
fi

echo "5. Recommended Actions"
echo "----------------------"

# Check if this is a main branch CI/CD modification
MAIN_CICD_MODIFIED=false
for file in "${CICD_MODIFICATIONS[@]}"; do
    if [[ "$file" == ".github/workflows/ci.yml" ]]; then
        MAIN_CICD_MODIFIED=true
        break
    fi
done

if [ "$MAIN_CICD_MODIFIED" = true ]; then
    echo -e "${RED}🚨 MAIN BRANCH CI/CD MODIFICATION DETECTED${NC}"
    echo ""
    echo "Immediate actions required:"
    echo "  1. Stop current commit process"
    echo "  2. Create feature branch: git checkout -b feature/cicd-improvement"
    echo "  3. Move changes to feature branch"
    echo "  4. Test CI/CD pipeline thoroughly"
    echo "  5. Create pull request with detailed justification"
    echo "  6. Get approval from CI/CD maintainers"
    echo ""
    echo "Emergency override (NOT RECOMMENDED):"
    echo "  CICD_CHANGE_APPROVED=true git commit"
    echo ""
    
    # Exit with error to prevent commit
    exit 1
else
    echo -e "${YELLOW}⚠️  CI/CD modifications require careful review${NC}"
    echo ""
    echo "Recommended actions:"
    echo "  1. Review all changes carefully"
    echo "  2. Test CI/CD pipeline locally if possible"
    echo "  3. Document changes and justification"
    echo "  4. Get code review from CI/CD maintainers"
    echo "  5. Monitor CI/CD pipeline after merge"
    echo ""
    echo "To proceed with approval:"
    echo "  CICD_CHANGE_APPROVED=true git commit"
    echo ""
fi

echo "6. Change Summary"
echo "-----------------"
echo "Total CI/CD changes: ${#CICD_CHANGES[@]}"
echo "  - Additions: ${#CICD_ADDITIONS[@]}"
echo "  - Modifications: ${#CICD_MODIFICATIONS[@]}"
echo "  - Deletions: ${#CICD_DELETIONS[@]}"
echo ""
echo "Current branch: $CURRENT_BRANCH"
echo "Impact level: $([ "$MAIN_CICD_MODIFIED" = true ] && echo "CRITICAL" || echo "MODERATE")"
echo ""

# Show detailed diff for main CI/CD file if modified
if [ "$MAIN_CICD_MODIFIED" = true ]; then
    echo "7. Main CI/CD Diff Preview"
    echo "-------------------------"
    echo "Showing changes to .github/workflows/ci.yml:"
    echo ""
    git diff "$BASE_COMMIT" "$CURRENT_COMMIT" -- ".github/workflows/ci.yml" | head -50
    echo ""
    echo "... (truncated, see full diff with: git diff $BASE_COMMIT $CURRENT_COMMIT -- .github/workflows/ci.yml)"
fi

echo ""
echo "For more information, see: docs/ci-cd/CI-CD-PROTECTION-POLICY.md"
