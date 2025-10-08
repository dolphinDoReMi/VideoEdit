#!/bin/bash
# Feature Branch Deployment Script
# This script handles deployment of feature branches with WIP status management

set -euo pipefail

# Configuration
BRANCH_NAME=${1:-$(git branch --show-current)}
DEPLOY_TYPE=${2:-"preview"} # preview, staging, production
MARK_WIP=${3:-"true"} # true, false
REPO_OWNER=${4:-"dolphinDoReMi"}
REPO_NAME=${5:-"VideoEdit"}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}[DEPLOY]${NC} $1"
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

# Validate branch name
validate_branch() {
    local branch=$1
    
    if [[ "$branch" == "main" ]] || [[ "$branch" == "develop" ]]; then
        log_error "Cannot deploy main/develop branches using this script"
        log_info "Use the main CI/CD pipeline for main/develop branches"
        exit 1
    fi
    
    if [[ ! "$branch" =~ ^(feature|feat|wip|WIP)/ ]]; then
        log_warning "Branch '$branch' does not follow feature branch naming convention"
        log_info "Consider renaming to feature/$branch or feat/$branch"
    fi
}

# Deploy Android
deploy_android() {
    local branch=$1
    local deploy_type=$2
    
    log_info "Deploying Android build for branch: $branch"
    
    # Build debug APK
    log_info "Building debug APK..."
    ./gradlew :app:assembleDebug
    
    # Create deployment directory
    local deploy_dir="deployments/android/$branch"
    mkdir -p "$deploy_dir"
    
    # Copy APK
    cp app/build/outputs/apk/debug/app-debug.apk "$deploy_dir/app-debug-$branch.apk"
    
    # Generate deployment info
    cat > "$deploy_dir/deployment-info.json" << EOF
{
  "branch": "$branch",
  "deploy_type": "$deploy_type",
  "timestamp": "$(date -u +"%Y-%m-%d %H:%M:%S UTC")",
  "commit": "$(git rev-parse HEAD)",
  "commit_message": "$(git log -1 --pretty=format:'%s')",
  "platform": "android",
  "build_type": "debug",
  "wip_status": "$MARK_WIP"
}
EOF
    
    log_success "Android deployment ready in $deploy_dir"
}

# Deploy iOS
deploy_ios() {
    local branch=$1
    local deploy_type=$2
    
    log_info "Deploying iOS build for branch: $branch"
    
    # Check if we're on macOS
    if [[ "$OSTYPE" != "darwin"* ]]; then
        log_warning "iOS deployment requires macOS. Skipping iOS build."
        return 0
    fi
    
    # Build web assets
    log_info "Building web assets..."
    npm run build
    
    # Sync iOS
    log_info "Syncing iOS..."
    npx cap sync ios
    
    # Install CocoaPods
    log_info "Installing CocoaPods..."
    cd ios/App
    pod install --repo-update
    cd -
    
    # Build iOS (Debug)
    log_info "Building iOS app..."
    cd ios/App
    xcodebuild -workspace App.xcworkspace \
      -scheme App \
      -configuration Debug \
      -destination 'generic/platform=iOS' \
      -allowProvisioningUpdates \
      build
    cd -
    
    # Create deployment directory
    local deploy_dir="deployments/ios/$branch"
    mkdir -p "$deploy_dir"
    
    # Copy build artifacts
    cp -r ios/App/build/ "$deploy_dir/"
    
    # Generate deployment info
    cat > "$deploy_dir/deployment-info.json" << EOF
{
  "branch": "$branch",
  "deploy_type": "$deploy_type",
  "timestamp": "$(date -u +"%Y-%m-%d %H:%M:%S UTC")",
  "commit": "$(git rev-parse HEAD)",
  "commit_message": "$(git log -1 --pretty=format:'%s')",
  "platform": "ios",
  "build_type": "debug",
  "wip_status": "$MARK_WIP"
}
EOF
    
    log_success "iOS deployment ready in $deploy_dir"
}

# Deploy Web
deploy_web() {
    local branch=$1
    local deploy_type=$2
    
    log_info "Deploying web build for branch: $branch"
    
    # Build web assets
    log_info "Building web assets..."
    npm run build
    
    # Create deployment directory
    local deploy_dir="deployments/web/$branch"
    mkdir -p "$deploy_dir"
    
    # Copy build artifacts
    cp -r dist/ "$deploy_dir/"
    
    # Generate deployment info
    cat > "$deploy_dir/deployment-info.json" << EOF
{
  "branch": "$branch",
  "deploy_type": "$deploy_type",
  "timestamp": "$(date -u +"%Y-%m-%d %H:%M:%S UTC")",
  "commit": "$(git rev-parse HEAD)",
  "commit_message": "$(git log -1 --pretty=format:'%s')",
  "platform": "web",
  "build_type": "production",
  "wip_status": "$MARK_WIP"
}
EOF
    
    # Deploy to preview if not WIP
    if [[ "$MARK_WIP" == "false" ]] && [[ "$deploy_type" == "preview" ]]; then
        log_info "Deploying to preview environment..."
        
        # Deploy to Netlify (if configured)
        if [[ -n "${NETLIFY_AUTH_TOKEN:-}" ]] && [[ -n "${NETLIFY_SITE_ID:-}" ]]; then
            log_info "Deploying to Netlify..."
            npx netlify deploy --dir="$deploy_dir" --message="Feature branch: $branch" --site="$NETLIFY_SITE_ID" || log_warning "Netlify deployment failed"
        fi
        
        # Deploy to Vercel (if configured)
        if [[ -n "${VERCEL_TOKEN:-}" ]] && [[ -n "${VERCEL_PROJECT_ID:-}" ]]; then
            log_info "Deploying to Vercel..."
            npx vercel --token="$VERCEL_TOKEN" --project-id="$VERCEL_PROJECT_ID" --prod=false || log_warning "Vercel deployment failed"
        fi
    else
        log_info "Skipping live deployment (WIP status: $MARK_WIP)"
    fi
    
    log_success "Web deployment ready in $deploy_dir"
}

# Generate deployment summary
generate_summary() {
    local branch=$1
    local deploy_type=$2
    
    log_info "Generating deployment summary..."
    
    local summary_file="deployments/DEPLOYMENT_SUMMARY.md"
    mkdir -p deployments
    
    cat > "$summary_file" << EOF
# Deployment Summary

**Branch**: \`$branch\`
**Deploy Type**: $deploy_type
**WIP Status**: $MARK_WIP
**Timestamp**: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
**Commit**: \`$(git rev-parse HEAD)\`
**Commit Message**: $(git log -1 --pretty=format:'%s')

## Deployed Platforms

EOF
    
    # Add platform-specific information
    if [ -d "deployments/android/$branch" ]; then
        echo "- ✅ **Android**: Debug APK available" >> "$summary_file"
    fi
    
    if [ -d "deployments/ios/$branch" ]; then
        echo "- ✅ **iOS**: Debug build available" >> "$summary_file"
    fi
    
    if [ -d "deployments/web/$branch" ]; then
        echo "- ✅ **Web**: Production build available" >> "$summary_file"
    fi
    
    cat >> "$summary_file" << EOF

## Next Steps

EOF
    
    if [[ "$MARK_WIP" == "true" ]]; then
        cat >> "$summary_file" << EOF
- [ ] Complete feature implementation
- [ ] Run comprehensive tests
- [ ] Update documentation
- [ ] Mark as ready for review using: \`./scripts/wip_status.sh $branch mark-ready\`
EOF
    else
        cat >> "$summary_file" << EOF
- [ ] Review deployment
- [ ] Test functionality
- [ ] Create pull request if ready
- [ ] Request code review
EOF
    fi
    
    log_success "Deployment summary generated: $summary_file"
}

# Main deployment logic
main() {
    log_info "Starting deployment for branch: $BRANCH_NAME"
    log_info "Deploy type: $DEPLOY_TYPE"
    log_info "WIP status: $MARK_WIP"
    
    # Validate branch
    validate_branch "$BRANCH_NAME"
    
    # Mark as WIP if requested
    if [[ "$MARK_WIP" == "true" ]]; then
        log_wip "Marking branch as WIP..."
        ./scripts/wip_status.sh "$BRANCH_NAME" mark-wip "$REPO_OWNER" "$REPO_NAME" || log_warning "Failed to mark as WIP"
    fi
    
    # Deploy all platforms
    deploy_android "$BRANCH_NAME" "$DEPLOY_TYPE"
    deploy_ios "$BRANCH_NAME" "$DEPLOY_TYPE"
    deploy_web "$BRANCH_NAME" "$DEPLOY_TYPE"
    
    # Generate summary
    generate_summary "$BRANCH_NAME" "$DEPLOY_TYPE"
    
    log_success "Deployment completed successfully!"
    log_info "Check deployments/ directory for build artifacts"
    
    if [[ "$MARK_WIP" == "true" ]]; then
        log_wip "Branch is marked as Work In Progress"
        log_info "Use './scripts/wip_status.sh $BRANCH_NAME mark-ready' when ready for review"
    fi
}

# Show help
show_help() {
    echo "Feature Branch Deployment Script"
    echo ""
    echo "Usage: $0 [BRANCH_NAME] [DEPLOY_TYPE] [MARK_WIP] [REPO_OWNER] [REPO_NAME]"
    echo ""
    echo "Arguments:"
    echo "  BRANCH_NAME    Branch name (default: current branch)"
    echo "  DEPLOY_TYPE    Deploy type: preview, staging, production (default: preview)"
    echo "  MARK_WIP       Mark as WIP: true, false (default: true)"
    echo "  REPO_OWNER     Repository owner (default: dolphinDoReMi)"
    echo "  REPO_NAME      Repository name (default: VideoEdit)"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Deploy current branch as WIP preview"
    echo "  $0 feature/my-feature preview true   # Deploy feature branch as WIP preview"
    echo "  $0 feature/my-feature staging false  # Deploy feature branch as ready staging"
    echo ""
    echo "Environment Variables:"
    echo "  NETLIFY_AUTH_TOKEN    Netlify authentication token"
    echo "  NETLIFY_SITE_ID       Netlify site ID"
    echo "  VERCEL_TOKEN          Vercel authentication token"
    echo "  VERCEL_PROJECT_ID     Vercel project ID"
}

# Parse arguments
case "${1:-}" in
    "help"|"-h"|"--help")
        show_help
        exit 0
        ;;
    *)
        main
        ;;
esac
