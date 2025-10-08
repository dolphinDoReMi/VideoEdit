# CI/CD Workflows Documentation

This document provides a comprehensive overview of all CI/CD workflows in the VideoEdit project, making them easily discoverable and understandable.

## Overview

The project uses GitHub Actions for continuous integration and deployment. All workflows are located in `.github/workflows/` and are designed to support the video clipping feature development workflow.

## Workflow Inventory

### 1. Main CI Pipeline (`ci.yml`)
**Purpose**: Primary continuous integration pipeline for all branches
**Triggers**: 
- Pull requests to `main` and `develop`
- Pushes to `main`, `develop`, and `feature/ui-v3`
- Manual dispatch

**Key Features**:
- **Smart Detection**: Automatically detects changes in different modules (db, workers, ml, video, docs)
- **Conditional Testing**: Runs different test suites based on what changed
- **Thread Isolation**: Uses thread suffixes for parallel builds
- **Documentation Validation**: Ensures comprehensive feature-based documentation structure

**Jobs**:
- `detect`: Analyzes changes and determines which tests to run
- `docs-validation`: Validates documentation structure when docs change
- `android-build`: Builds debug APK with thread isolation
- `android-unit-tests`: Runs unit tests for database and ML changes
- `android-instrumented-tests`: Runs instrumented tests for workers, ML, and video changes
- `whisper-lid-validation`: Validates Whisper Language Detection implementation

### 2. Release Pipeline (`release.yml`)
**Purpose**: Automated release process for tagged versions
**Triggers**: Push to tags matching `v*` pattern

**Features**:
- **Multi-platform Builds**: Android APK/AAB, iOS IPA, Web deployment
- **Automated Release Notes**: Generates changelog from git commits
- **Artifact Distribution**: Uploads builds to GitHub Releases
- **Cloud Deployment**: Deploys web version to Netlify and Vercel

**Jobs**:
- `create-release`: Creates GitHub release with automated notes
- `build-android`: Builds and signs Android release artifacts
- `build-ios`: Archives and exports iOS app for TestFlight
- `deploy-web`: Builds and deploys web version to multiple platforms

### 3. Code Quality Checks (`code-quality.yml`)
**Purpose**: Ensures code quality and consistency
**Triggers**: Push to main branches, pull requests

**Features**:
- **Lint Analysis**: Runs Android lint checks
- **Report Generation**: Generates HTML reports for review
- **Artifact Upload**: Saves quality reports for analysis

### 4. Android Instrumented Tests (`android-instrumented-tests.yml`)
**Purpose**: Runs comprehensive instrumented tests
**Triggers**: Push to main branches, pull requests

**Features**:
- **Emulator Testing**: Uses macOS runners for Android emulator
- **Test Reports**: Generates detailed test reports
- **Artifact Collection**: Saves test results for analysis

### 5. Dependency Updates (`dependency-updates.yml`)
**Purpose**: Automated dependency maintenance
**Triggers**: Weekly schedule, manual dispatch

**Features**:
- **Gradle Updates**: Updates Gradle wrapper and dependencies
- **Action Updates**: Checks for outdated GitHub Actions
- **Automated PRs**: Creates pull requests with dependency updates
- **Selective Updates**: Supports different update types (gradle, dependencies, actions)

### 6. Policy Guard (`push-guard.yml`)
**Purpose**: Enforces project policies and standards
**Triggers**: Pull requests, pushes to main

**Features**:
- **Policy Enforcement**: Runs custom policy checks
- **Fast Gradle Checks**: Quick validation of configuration and code quality
- **Documentation Validation**: Ensures documentation structure compliance
- **Code Quality**: Runs ktlint and detekt checks

### 7. Android E2E Tests (`android-e2e.yml`)
**Purpose**: End-to-end testing (currently disabled)
**Triggers**: Push to main, manual dispatch

**Note**: This workflow is currently disabled due to reliability issues with emulators in GitHub Actions. E2E tests should be run locally.

### 8. Artifact Cleanup (`artifact-cleanup.yml`)
**Purpose**: Maintains repository cleanliness
**Triggers**: Scheduled cleanup

**Features**:
- **Artifact Management**: Cleans up old build artifacts
- **Storage Optimization**: Reduces repository size
- **Automated Maintenance**: Runs on schedule

## Video Clipping Feature Branch Workflow

### Current Branch: `whisper-clipping`

The project is currently on the `whisper-clipping` feature branch, which is designed for video clipping functionality development.

### Workflow Integration

All existing workflows support feature branch development:

1. **CI Pipeline**: Automatically detects video-related changes and runs appropriate tests
2. **Code Quality**: Ensures video clipping code meets quality standards
3. **Policy Guard**: Validates that video clipping changes follow project policies
4. **Release Pipeline**: Will include video clipping features when merged to main

### Branch-Specific Features

- **Thread Isolation**: Each build gets a unique suffix to prevent conflicts
- **Conditional Testing**: Only runs tests relevant to changed components
- **Documentation Validation**: Ensures video clipping documentation is properly structured

## Usage Guidelines

### For Video Clipping Development

1. **Feature Branch**: Work on `whisper-clipping` branch
2. **Automatic Testing**: CI will automatically detect video-related changes
3. **Quality Checks**: All code quality workflows will run on your changes
4. **Documentation**: Ensure video clipping docs follow the established structure

### For Releases

1. **Tag Creation**: Create a version tag (e.g., `v1.0.0`)
2. **Automatic Release**: Release pipeline will build and deploy all platforms
3. **Artifact Distribution**: APK, AAB, IPA, and web versions will be created
4. **Release Notes**: Changelog will be automatically generated

### For Maintenance

1. **Dependency Updates**: Run weekly or manually via workflow dispatch
2. **Code Quality**: Monitor quality reports from CI runs
3. **Policy Compliance**: Ensure all changes pass policy guard checks

## Workflow Triggers Summary

| Workflow | Push | PR | Schedule | Manual |
|----------|------|----|---------|---------| 
| CI | ✅ | ✅ | ❌ | ✅ |
| Release | ✅ (tags) | ❌ | ❌ | ❌ |
| Code Quality | ✅ | ✅ | ❌ | ❌ |
| Instrumented Tests | ✅ | ✅ | ❌ | ❌ |
| Dependency Updates | ❌ | ❌ | ✅ | ✅ |
| Policy Guard | ✅ | ✅ | ❌ | ❌ |
| E2E Tests | ✅ | ❌ | ❌ | ✅ |
| Artifact Cleanup | ❌ | ❌ | ✅ | ❌ |

## Best Practices

1. **Use Feature Branches**: Develop video clipping features in dedicated branches
2. **Follow Naming Conventions**: Use descriptive branch names (e.g., `feature/video-clipping`)
3. **Monitor CI Results**: Check workflow results before merging
4. **Update Documentation**: Keep video clipping documentation current
5. **Test Locally**: Run tests locally before pushing to avoid CI failures
6. **Review Quality Reports**: Check code quality artifacts for issues

## Troubleshooting

### Common Issues

1. **Build Failures**: Check thread suffix conflicts in parallel builds
2. **Test Failures**: Review test reports in workflow artifacts
3. **Quality Issues**: Check lint reports for code quality problems
4. **Policy Violations**: Review policy guard output for compliance issues

### Getting Help

- Check workflow logs in GitHub Actions tab
- Review artifact reports for detailed error information
- Consult this documentation for workflow-specific guidance
- Use manual dispatch for testing workflows locally

## Future Enhancements

- **Enhanced E2E Testing**: Improve emulator reliability for automated E2E tests
- **Performance Testing**: Add performance benchmarks to CI pipeline
- **Security Scanning**: Integrate security vulnerability scanning
- **Multi-Environment**: Support for staging and production environments
