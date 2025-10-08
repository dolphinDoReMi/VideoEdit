# Feature Branch CI/CD Guide

This guide explains how to use the CI/CD system for feature branches with Work In Progress (WIP) status management.

## Overview

The VideoEdit project uses GitHub Actions for continuous integration and deployment with specialized workflows for feature branches. The system automatically detects changes, builds appropriate artifacts, and manages WIP status.

## Quick Start

### 1. Create a Feature Branch

```bash
# Create and switch to a new feature branch
git checkout -b feature/your-feature-name

# Or use the WIP naming convention
git checkout -b wip/your-feature-name
```

### 2. Deploy with WIP Status

```bash
# Deploy current branch as WIP preview
./scripts/deploy_feature.sh

# Deploy specific branch as WIP
./scripts/deploy_feature.sh feature/your-feature preview true
```

### 3. Mark as Ready for Review

```bash
# Mark current branch as ready
./scripts/wip_status.sh mark-ready

# Mark specific branch as ready
./scripts/wip_status.sh feature/your-feature mark-ready
```

## Workflows

### Feature Branch CI/CD (`feature-branch-ci.yml`)

**Triggers:**
- Push to `feature/**`, `feat/**`, or `wip/**` branches
- Pull requests to main/develop
- Manual dispatch

**Features:**
- **Smart Detection**: Automatically detects changes in different modules
- **WIP Status Management**: Marks branches and PRs as Work In Progress
- **Multi-platform Builds**: Android, iOS, and Web builds
- **Artifact Upload**: Builds and test reports uploaded as artifacts
- **Conditional Deployment**: Preview deployments for non-WIP branches

**Jobs:**
- `detect-changes`: Analyzes changes and determines which tests to run
- `mark-wip-status`: Creates/updates WIP issues and PR labels
- `android-build`: Builds debug APK with thread isolation
- `android-tests`: Runs unit tests for database, ML, and worker changes
- `ios-build`: Builds iOS debug version
- `web-build`: Builds web assets and deploys to preview (if not WIP)
- `docs-validation`: Validates documentation structure
- `whisper-validation`: Validates Whisper implementation
- `summary`: Generates comprehensive build summary

### Main CI Pipeline (`ci.yml`)

**Updated to include:**
- Feature branch triggers (`feature/**`)
- Integration with feature branch workflow

## Scripts

### WIP Status Management (`scripts/wip_status.sh`)

Manages Work In Progress status for feature branches.

```bash
# Show current status
./scripts/wip_status.sh

# Mark branch as WIP
./scripts/wip_status.sh feature/my-feature mark-wip

# Mark branch as ready
./scripts/wip_status.sh feature/my-feature mark-ready

# Show status of specific branch
./scripts/wip_status.sh feature/my-feature status
```

**Features:**
- Creates/updates GitHub issues for WIP tracking
- Manages PR labels (WIP, do-not-merge, ready-for-review)
- Provides status overview
- Integrates with GitHub CLI

### Feature Deployment (`scripts/deploy_feature.sh`)

Handles deployment of feature branches with WIP status management.

```bash
# Deploy current branch as WIP preview
./scripts/deploy_feature.sh

# Deploy specific branch
./scripts/deploy_feature.sh feature/my-feature preview true

# Deploy as ready for review
./scripts/deploy_feature.sh feature/my-feature staging false
```

**Features:**
- Multi-platform deployment (Android, iOS, Web)
- WIP status management
- Deployment artifact generation
- Preview environment deployment
- Comprehensive deployment summary

## Branch Naming Conventions

### Recommended Patterns

- `feature/feature-name` - Standard feature branches
- `feat/feature-name` - Short form for features
- `wip/feature-name` - Work In Progress branches
- `WIP/feature-name` - Alternative WIP naming

### Automatic WIP Detection

Branches are automatically marked as WIP if they contain:
- `wip` or `WIP` in the name
- `work-in-progress` in the name

## GitHub Integration

### Issues

The system automatically creates GitHub issues for WIP tracking:

- **Title**: `🚧 WIP: Feature Branch <branch-name>`
- **Labels**: `WIP`, `feature-branch`
- **Body**: Includes branch info, status, and development checklist

### Pull Requests

PRs are automatically labeled based on WIP status:

- **WIP Branches**: `WIP`, `do-not-merge`
- **Ready Branches**: `ready-for-review`

### Artifacts

Build artifacts are uploaded with descriptive names:

- `app-debug-<suffix>-<branch-name>.apk`
- `android-build-reports-<suffix>-<branch-name>`
- `unit-test-reports-<suffix>-<branch-name>`
- `ios-build-<branch-name>`
- `web-build-<branch-name>`

## Environment Variables

### Required for Deployment

```bash
# Netlify (for web preview deployment)
export NETLIFY_AUTH_TOKEN="your-netlify-token"
export NETLIFY_SITE_ID="your-site-id"

# Vercel (for web preview deployment)
export VERCEL_TOKEN="your-vercel-token"
export VERCEL_PROJECT_ID="your-project-id"
```

### GitHub Secrets

The following secrets should be configured in your GitHub repository:

- `GITHUB_TOKEN` - Automatically provided
- `NETLIFY_AUTH_TOKEN` - For web deployment
- `NETLIFY_SITE_ID` - Netlify site identifier
- `VERCEL_TOKEN` - For Vercel deployment
- `VERCEL_PROJECT_ID` - Vercel project identifier

## Workflow Examples

### Development Workflow

1. **Create Feature Branch**
   ```bash
   git checkout -b feature/new-video-filter
   ```

2. **Make Changes**
   ```bash
   # Make your changes
   git add .
   git commit -m "Add new video filter feature"
   ```

3. **Deploy as WIP**
   ```bash
   ./scripts/deploy_feature.sh feature/new-video-filter preview true
   ```

4. **Push to GitHub**
   ```bash
   git push origin feature/new-video-filter
   ```

5. **Create Pull Request**
   - The CI/CD will automatically run
   - Branch will be marked as WIP
   - Artifacts will be uploaded

6. **Mark as Ready**
   ```bash
   ./scripts/wip_status.sh feature/new-video-filter mark-ready
   ```

### Testing Workflow

1. **Check Status**
   ```bash
   ./scripts/wip_status.sh feature/my-feature status
   ```

2. **Download Artifacts**
   - Go to GitHub Actions
   - Find your workflow run
   - Download artifacts from the Actions tab

3. **Test Locally**
   ```bash
   # Install Android APK
   adb install deployments/android/feature/my-feature/app-debug-feature-my-feature.apk
   
   # Test web build locally
   cd deployments/web/feature/my-feature
   python -m http.server 8000
   ```

## Troubleshooting

### Common Issues

1. **WIP Status Not Updating**
   - Ensure GitHub CLI is installed and authenticated
   - Check repository permissions
   - Verify branch name follows conventions

2. **Build Failures**
   - Check the Actions tab for detailed logs
   - Ensure all dependencies are properly configured
   - Verify environment variables are set

3. **Deployment Issues**
   - Check environment variables for deployment services
   - Verify API tokens are valid
   - Check service-specific logs

### Debug Commands

```bash
# Check GitHub CLI authentication
gh auth status

# List recent workflow runs
gh run list

# View workflow logs
gh run view <run-id>

# Check branch status
./scripts/wip_status.sh <branch-name> status
```

## Best Practices

### Branch Management

- Use descriptive branch names
- Keep branches focused on single features
- Regularly sync with main/develop
- Delete merged branches

### WIP Management

- Mark branches as WIP during active development
- Use WIP issues to track progress
- Mark as ready only when complete
- Include comprehensive commit messages

### Testing

- Test locally before pushing
- Use the CI/CD artifacts for testing
- Verify all platforms work correctly
- Update documentation as needed

## Integration with Existing Workflows

The feature branch CI/CD integrates seamlessly with existing workflows:

- **Main CI**: Updated to include feature branch triggers
- **Release Pipeline**: Unchanged, handles tagged releases
- **Code Quality**: Runs on all branches including features
- **Documentation**: Validates docs on feature branches

This ensures consistent quality and testing across all development workflows.
