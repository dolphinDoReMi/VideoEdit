#!/bin/bash
# WIP Status Management Script for Feature Branches
# This script helps manage Work In Progress status for feature branches

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
BRANCH_NAME=${1:-$(git branch --show-current)}
ACTION=${2:-"status"}
REPO_OWNER=${3:-"dolphinDoReMi"}
REPO_NAME=${4:-"VideoEdit"}

# Helper functions
log_info() {
    echo -e "${BLUE}[WIP]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_wip() {
    echo -e "${PURPLE}[WIP]${NC} $1"
}

# Check if branch is WIP
is_wip_branch() {
    local branch=$1
    if [[ "$branch" == *"wip"* ]] || [[ "$branch" == *"WIP"* ]] || [[ "$branch" == *"work-in-progress"* ]]; then
        return 0
    fi
    return 1
}

# Mark branch as WIP
mark_wip() {
    local branch=$1
    
    log_wip "Marking branch '$branch' as Work In Progress"
    
    # Create or update WIP issue
    local issue_title="🚧 WIP: Feature Branch $branch"
    local issue_body="This issue tracks the Work In Progress status of feature branch \`$branch\`.

**Branch**: \`$branch\`
**Status**: Work In Progress
**Last Updated**: $(date -u +"%Y-%m-%d %H:%M:%S UTC")

This feature branch is currently under development and should not be merged until marked as ready.

## Development Notes
- [ ] Feature implementation in progress
- [ ] Testing required
- [ ] Documentation updates needed
- [ ] Code review pending

## Next Steps
1. Complete feature implementation
2. Run comprehensive tests
3. Update documentation
4. Request code review
5. Mark as ready for merge"

    # Check if issue already exists
    local existing_issue=$(gh issue list --repo "$REPO_OWNER/$REPO_NAME" --search "in:title $issue_title" --json number --jq '.[0].number' 2>/dev/null || echo "")
    
    if [ -n "$existing_issue" ] && [ "$existing_issue" != "null" ]; then
        log_info "Updating existing WIP issue #$existing_issue"
        gh issue edit "$existing_issue" --repo "$REPO_OWNER/$REPO_NAME" --body "$issue_body" --add-label "WIP,feature-branch" --remove-label "ready-for-review" 2>/dev/null || true
    else
        log_info "Creating new WIP issue"
        gh issue create --repo "$REPO_OWNER/$REPO_NAME" --title "$issue_title" --body "$issue_body" --label "WIP,feature-branch" 2>/dev/null || true
    fi
    
    # If this is a PR, mark it as WIP
    local pr_number=$(gh pr list --repo "$REPO_OWNER/$REPO_NAME" --head "$branch" --json number --jq '.[0].number' 2>/dev/null || echo "")
    if [ -n "$pr_number" ] && [ "$pr_number" != "null" ]; then
        log_info "Marking PR #$pr_number as WIP"
        gh pr edit "$pr_number" --repo "$REPO_OWNER/$REPO_NAME" --add-label "WIP" --add-label "do-not-merge" --remove-label "ready-for-review" 2>/dev/null || true
    fi
    
    log_success "Branch '$branch' marked as WIP"
}

# Mark branch as ready
mark_ready() {
    local branch=$1
    
    log_success "Marking branch '$branch' as ready for review"
    
    # Update WIP issue
    local issue_title="🚧 WIP: Feature Branch $branch"
    local existing_issue=$(gh issue list --repo "$REPO_OWNER/$REPO_NAME" --search "in:title $issue_title" --json number --jq '.[0].number' 2>/dev/null || echo "")
    
    if [ -n "$existing_issue" ] && [ "$existing_issue" != "null" ]; then
        log_info "Updating WIP issue #$existing_issue to ready status"
        gh issue edit "$existing_issue" --repo "$REPO_OWNER/$REPO_NAME" --add-label "ready-for-review" --remove-label "WIP" 2>/dev/null || true
    fi
    
    # If this is a PR, mark it as ready
    local pr_number=$(gh pr list --repo "$REPO_OWNER/$REPO_NAME" --head "$branch" --json number --jq '.[0].number' 2>/dev/null || echo "")
    if [ -n "$pr_number" ] && [ "$pr_number" != "null" ]; then
        log_info "Marking PR #$pr_number as ready for review"
        gh pr edit "$pr_number" --repo "$REPO_OWNER/$REPO_NAME" --add-label "ready-for-review" --remove-label "WIP" --remove-label "do-not-merge" 2>/dev/null || true
    fi
    
    log_success "Branch '$branch' marked as ready for review"
}

# Show WIP status
show_status() {
    local branch=$1
    
    log_info "WIP Status for branch: $branch"
    echo ""
    
    # Check if branch name indicates WIP
    if is_wip_branch "$branch"; then
        log_wip "Branch name indicates WIP status"
    else
        log_info "Branch name does not indicate WIP status"
    fi
    
    # Check for WIP issue
    local issue_title="🚧 WIP: Feature Branch $branch"
    local existing_issue=$(gh issue list --repo "$REPO_OWNER/$REPO_NAME" --search "in:title $issue_title" --json number,title,labels --jq '.[0]' 2>/dev/null || echo "null")
    
    if [ "$existing_issue" != "null" ] && [ -n "$existing_issue" ]; then
        local issue_number=$(echo "$existing_issue" | jq -r '.number')
        local issue_labels=$(echo "$existing_issue" | jq -r '.labels[].name' | tr '\n' ',' | sed 's/,$//')
        log_info "WIP Issue found: #$issue_number"
        log_info "Labels: $issue_labels"
        
        if echo "$issue_labels" | grep -q "WIP"; then
            log_wip "Status: Work In Progress"
        elif echo "$issue_labels" | grep -q "ready-for-review"; then
            log_success "Status: Ready for Review"
        else
            log_info "Status: Unknown"
        fi
    else
        log_warning "No WIP issue found for this branch"
    fi
    
    # Check for PR
    local pr_number=$(gh pr list --repo "$REPO_OWNER/$REPO_NAME" --head "$branch" --json number,title,labels --jq '.[0]' 2>/dev/null || echo "null")
    
    if [ "$pr_number" != "null" ] && [ -n "$pr_number" ]; then
        local pr_num=$(echo "$pr_number" | jq -r '.number')
        local pr_title=$(echo "$pr_number" | jq -r '.title')
        local pr_labels=$(echo "$pr_number" | jq -r '.labels[].name' | tr '\n' ',' | sed 's/,$//')
        log_info "PR found: #$pr_num - $pr_title"
        log_info "PR Labels: $pr_labels"
        
        if echo "$pr_labels" | grep -q "WIP"; then
            log_wip "PR Status: Work In Progress"
        elif echo "$pr_labels" | grep -q "do-not-merge"; then
            log_warning "PR Status: Do Not Merge"
        elif echo "$pr_labels" | grep -q "ready-for-review"; then
            log_success "PR Status: Ready for Review"
        else
            log_info "PR Status: Unknown"
        fi
    else
        log_info "No PR found for this branch"
    fi
}

# Show help
show_help() {
    echo "WIP Status Management Script"
    echo ""
    echo "Usage: $0 [BRANCH_NAME] [ACTION] [REPO_OWNER] [REPO_NAME]"
    echo ""
    echo "Arguments:"
    echo "  BRANCH_NAME    Branch name (default: current branch)"
    echo "  ACTION         Action to perform (default: status)"
    echo "  REPO_OWNER     Repository owner (default: dolphinDoReMi)"
    echo "  REPO_NAME      Repository name (default: VideoEdit)"
    echo ""
    echo "Actions:"
    echo "  status         Show WIP status (default)"
    echo "  mark-wip       Mark branch as Work In Progress"
    echo "  mark-ready     Mark branch as ready for review"
    echo "  help           Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Show status of current branch"
    echo "  $0 feature/my-feature mark-wip        # Mark feature branch as WIP"
    echo "  $0 feature/my-feature mark-ready      # Mark feature branch as ready"
    echo "  $0 feature/my-feature status         # Show status of specific branch"
}

# Main logic
case "$ACTION" in
    "status")
        show_status "$BRANCH_NAME"
        ;;
    "mark-wip")
        mark_wip "$BRANCH_NAME"
        ;;
    "mark-ready")
        mark_ready "$BRANCH_NAME"
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    *)
        log_error "Unknown action: $ACTION"
        show_help
        exit 1
        ;;
esac
