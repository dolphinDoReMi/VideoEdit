#!/bin/bash
# Release automation script for VideoEdit
# This script automates the release process for Android, iOS, and Web platforms

set -euo pipefail

# Configuration
VERSION=${1:-""}
RELEASE_TYPE=${2:-"patch"} # patch, minor, major
DRY_RUN=${3:-false}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
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

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        log_error "Not in a git repository"
        exit 1
    fi
    
    # Check if working directory is clean
    if ! git diff-index --quiet HEAD --; then
        log_error "Working directory is not clean. Please commit or stash changes."
        exit 1
    fi
    
    # Check if we're on main branch
    current_branch=$(git branch --show-current)
    if [ "$current_branch" != "main" ]; then
        log_error "Not on main branch. Current branch: $current_branch"
        exit 1
    fi
    
    # Check if Android SDK is available
    if ! command -v adb &> /dev/null; then
        log_warning "Android SDK not found. Android builds will be skipped."
    fi
    
    # Check if Node.js is available
    if ! command -v node &> /dev/null; then
        log_warning "Node.js not found. Web builds will be skipped."
    fi
    
    log_success "Prerequisites check completed"
}

# Version management
bump_version() {
    log_info "Bumping version..."
    
    if [ -z "$VERSION" ]; then
        # Auto-increment version based on release type
        case $RELEASE_TYPE in
            "patch")
                VERSION=$(npm version patch --no-git-tag-version | sed 's/v//')
                ;;
            "minor")
                VERSION=$(npm version minor --no-git-tag-version | sed 's/v//')
                ;;
            "major")
                VERSION=$(npm version major --no-git-tag-version | sed 's/v//')
                ;;
            *)
                log_error "Invalid release type: $RELEASE_TYPE"
                exit 1
                ;;
        esac
    fi
    
    # Update version in build.gradle.kts
    sed -i.bak "s/versionName = \".*\"/versionName = \"$VERSION\"/" app/build.gradle.kts
    sed -i.bak "s/versionCode = [0-9]*/versionCode = $(date +%s)/" app/build.gradle.kts
    
    # Update version in package.json
    npm version "$VERSION" --no-git-tag-version
    
    log_success "Version bumped to $VERSION"
}

# Generate release notes
generate_release_notes() {
    log_info "Generating release notes..."
    
    # Get the previous tag
    PREV_TAG=$(git describe --tags --abbrev=0 HEAD~1 2>/dev/null || echo "")
    
    # Create release notes file
    RELEASE_NOTES_FILE="RELEASE_NOTES_$VERSION.md"
    
    echo "# Release $VERSION" > "$RELEASE_NOTES_FILE"
    echo "" >> "$RELEASE_NOTES_FILE"
    echo "## What's Changed" >> "$RELEASE_NOTES_FILE"
    
    if [ -n "$PREV_TAG" ]; then
        git log --oneline --pretty=format:"- %s" "$PREV_TAG"..HEAD >> "$RELEASE_NOTES_FILE"
    else
        echo "- Initial release" >> "$RELEASE_NOTES_FILE"
    fi
    
    echo "" >> "$RELEASE_NOTES_FILE"
    echo "## Download" >> "$RELEASE_NOTES_FILE"
    echo "- **Android APK**: Download from the assets below" >> "$RELEASE_NOTES_FILE"
    echo "- **iOS**: Available on TestFlight" >> "$RELEASE_NOTES_FILE"
    echo "- **Web**: Available at [videoedit.app](https://videoedit.app)" >> "$RELEASE_NOTES_FILE"
    
    log_success "Release notes generated: $RELEASE_NOTES_FILE"
}

# Build Android
build_android() {
    log_info "Building Android APKs..."
    
    # Build debug APK
    ./gradlew assembleDebug
    log_success "Debug APK built"
    
    # Build minimal APK
    ./gradlew assembleMinimal
    log_success "Minimal APK built"
    
    # Build release APK
    ./gradlew assembleRelease
    log_success "Release APK built"
    
    # Build release bundle for Play Store
    ./gradlew bundleRelease
    log_success "Release bundle built"
    
    # Get APK sizes
    DEBUG_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    MINIMAL_SIZE=$(du -h app/build/outputs/apk/minimal/app-minimal.apk | cut -f1)
    RELEASE_SIZE=$(du -h app/build/outputs/apk/release/app-release.apk | cut -f1)
    
    log_info "APK Sizes:"
    log_info "  Debug: $DEBUG_SIZE"
    log_info "  Minimal: $MINIMAL_SIZE"
    log_info "  Release: $RELEASE_SIZE"
}

# Build Web
build_web() {
    log_info "Building Web assets..."
    
    # Install dependencies
    npm ci
    
    # Build web assets
    npm run build
    
    log_success "Web assets built"
}

# Run tests
run_tests() {
    log_info "Running tests..."
    
    # Run unit tests
    ./gradlew testDebugUnitTest
    log_success "Unit tests passed"
    
    # Run instrumented tests if device is available
    if adb devices | grep -q "device$"; then
        ./gradlew connectedDebugAndroidTest
        log_success "Instrumented tests passed"
    else
        log_warning "No device connected. Skipping instrumented tests."
    fi
}

# Create release
create_release() {
    log_info "Creating release..."
    
    if [ "$DRY_RUN" = "true" ]; then
        log_info "DRY RUN: Would create release with tag v$VERSION"
        return
    fi
    
    # Create git tag
    git add .
    git commit -m "Release $VERSION"
    git tag "v$VERSION"
    
    # Push to remote
    git push origin main
    git push origin "v$VERSION"
    
    log_success "Release v$VERSION created and pushed"
}

# Deploy
deploy() {
    log_info "Deploying..."
    
    if [ "$DRY_RUN" = "true" ]; then
        log_info "DRY RUN: Would deploy to platforms"
        return
    fi
    
    # Deploy web to Netlify (if configured)
    if [ -n "${NETLIFY_AUTH_TOKEN:-}" ] && [ -n "${NETLIFY_SITE_ID:-}" ]; then
        log_info "Deploying to Netlify..."
        npx netlify deploy --prod --dir=dist
        log_success "Deployed to Netlify"
    fi
    
    # Deploy to Vercel (if configured)
    if [ -n "${VERCEL_TOKEN:-}" ] && [ -n "${VERCEL_PROJECT_ID:-}" ]; then
        log_info "Deploying to Vercel..."
        npx vercel --prod
        log_success "Deployed to Vercel"
    fi
    
    log_success "Deployment completed"
}

# Main execution
main() {
    log_info "Starting release process for VideoEdit..."
    log_info "Version: $VERSION"
    log_info "Release Type: $RELEASE_TYPE"
    log_info "Dry Run: $DRY_RUN"
    
    check_prerequisites
    bump_version
    generate_release_notes
    run_tests
    build_android
    build_web
    create_release
    deploy
    
    log_success "Release process completed successfully!"
    log_info "Release artifacts:"
    log_info "  - Android APKs: app/build/outputs/apk/"
    log_info "  - Android Bundle: app/build/outputs/bundle/"
    log_info "  - Web Assets: dist/"
    log_info "  - Release Notes: RELEASE_NOTES_$VERSION.md"
}

# Show usage
show_usage() {
    echo "Usage: $0 [VERSION] [RELEASE_TYPE] [DRY_RUN]"
    echo ""
    echo "Arguments:"
    echo "  VERSION      - Version number (e.g., 1.0.0). If not provided, auto-increments based on RELEASE_TYPE"
    echo "  RELEASE_TYPE - Type of release: patch, minor, major (default: patch)"
    echo "  DRY_RUN      - Set to 'true' to run without making changes (default: false)"
    echo ""
    echo "Examples:"
    echo "  $0                    # Auto-increment patch version"
    echo "  $0 1.0.0             # Release version 1.0.0"
    echo "  $0 '' minor          # Auto-increment minor version"
    echo "  $0 1.0.0 patch true # Dry run for version 1.0.0"
}

# Parse arguments
if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    show_usage
    exit 0
fi

# Run main function
main
