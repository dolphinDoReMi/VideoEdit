# CI/CD Configuration Guide

This document explains the CI/CD pipeline setup for the AutoCutPad Android project.

## Overview

The project uses GitHub Actions for continuous integration and deployment with three main workflows:

1. **Android CI/CD Pipeline** (`android-ci.yml`) - Main build and test pipeline
2. **Automated Testing Pipeline** (`automated-testing.yml`) - Scheduled and manual testing
3. **Release Deployment Pipeline** (`release-deployment.yml`) - Release management

## Workflows

### 1. Android CI/CD Pipeline

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main`
- Release publication

**Jobs:**
- **Test**: Unit tests, lint checks
- **Build Debug**: Debug APK build
- **Build Release**: Release AAB build (main branch only)
- **Firebase Distribution**: Deploy to Firebase App Distribution
- **Security Scan**: Vulnerability scanning with Trivy

### 2. Automated Testing Pipeline

**Triggers:**
- Daily at 2 AM UTC (scheduled)
- Manual dispatch with test type selection

**Test Types:**
- **Comprehensive**: Full test suite
- **Xiaomi-specific**: Xiaomi device testing
- **Performance**: Performance testing
- **E2E**: End-to-end testing

### 3. Release Deployment Pipeline

**Triggers:**
- Release publication
- Manual dispatch with release type selection

**Release Types:**
- **Internal**: Internal testing release
- **Store**: Store submission release
- **Firebase**: Firebase distribution release

## Required Secrets

Configure these secrets in your GitHub repository settings:

### Build Secrets
```
KEYSTORE_BASE64          # Base64 encoded keystore file
KEYSTORE_PASSWORD        # Keystore password
KEY_ALIAS               # Key alias
KEY_PASSWORD            # Key password
```

### Firebase Secrets
```
FIREBASE_APP_ID         # Firebase app ID
FIREBASE_TOKEN          # Firebase service account token
```

## Setup Instructions

### 1. Configure Repository Secrets

1. Go to your GitHub repository
2. Navigate to Settings → Secrets and variables → Actions
3. Add the required secrets listed above

### 2. Firebase Setup

1. Create a Firebase project
2. Enable App Distribution
3. Create a service account and download the JSON key
4. Generate a Firebase token:
   ```bash
   firebase login:ci
   ```
5. Add the token to GitHub secrets

### 3. Keystore Setup

1. Generate your release keystore:
   ```bash
   keytool -genkey -v -keystore keystore/autocutpad-release.keystore -alias autocutpad -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Convert keystore to base64:
   ```bash
   base64 -i keystore/autocutpad-release.keystore -o keystore-base64.txt
   ```

3. Add the base64 content to `KEYSTORE_BASE64` secret

### 4. Firebase App Distribution Groups

Create these groups in Firebase App Distribution:
- `testers` - General testers
- `internal-testers` - Internal team
- `beta-testers` - Beta testers

## Usage

### Manual Workflow Triggers

1. **Automated Testing**: Go to Actions → Automated Testing Pipeline → Run workflow
2. **Release Deployment**: Go to Actions → Release Deployment Pipeline → Run workflow

### Release Process

1. **Create Release**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **GitHub Release**: Create a release on GitHub with the tag

3. **Automatic Deployment**: The release deployment pipeline will automatically:
   - Build the release AAB
   - Upload to GitHub release
   - Deploy to Firebase App Distribution

### Local Development

For local development, use the organized scripts:

```bash
# Run tests
./scripts/test/local_test.sh

# Build release
./scripts/build/build-release.sh

# Setup Firebase
./scripts/firebase/setup-firebase.sh
```

## Monitoring

### Build Status
- Check the Actions tab for workflow status
- Monitor build logs for issues
- Review test results and artifacts

### Notifications
- GitHub will send notifications for failed builds
- Firebase App Distribution will notify testers of new releases

## Troubleshooting

### Common Issues

1. **Build Failures**:
   - Check Gradle version compatibility
   - Verify Android SDK setup
   - Review build logs

2. **Signing Issues**:
   - Verify keystore secrets are correct
   - Check keystore file format
   - Ensure base64 encoding is correct

3. **Firebase Issues**:
   - Verify Firebase token is valid
   - Check app ID configuration
   - Ensure App Distribution is enabled

### Debug Steps

1. Check workflow logs in GitHub Actions
2. Verify secrets are properly configured
3. Test locally with the same commands
4. Review Firebase console for distribution issues

## Best Practices

1. **Branch Protection**: Enable branch protection for `main` branch
2. **Required Checks**: Require CI checks to pass before merging
3. **Review Process**: Require pull request reviews
4. **Release Notes**: Always include detailed release notes
5. **Testing**: Run comprehensive tests before releases

## Security

1. **Secrets Management**: Never commit secrets to repository
2. **Access Control**: Limit access to sensitive workflows
3. **Audit Logs**: Monitor workflow execution logs
4. **Dependency Updates**: Regularly update GitHub Actions

## Performance Optimization

1. **Caching**: Gradle dependencies are cached for faster builds
2. **Parallel Jobs**: Tests and builds run in parallel when possible
3. **Conditional Execution**: Jobs only run when necessary
4. **Artifact Management**: Clean up old artifacts regularly
