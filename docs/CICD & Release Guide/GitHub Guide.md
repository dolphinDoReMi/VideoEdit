# GitHub Guide

## Repository Overview

### Main Repository
- **URL**: `https://github.com/dolphinDoReMi/VideoEdit`
- **Description**: Comprehensive video processing platform with AI-powered speech recognition and visual understanding
- **License**: MIT License
- **Language**: Kotlin (Android), Swift (iOS), TypeScript (Web)

### Repository Structure
```
VideoEdit/
├── .github/                    # GitHub Actions workflows
│   ├── workflows/
│   │   ├── ci.yml             # Continuous Integration
│   │   ├── cd.yml             # Continuous Deployment
│   │   ├── release.yml        # Release Pipeline
│   │   └── security.yml       # Security Scanning
├── docs/                       # Documentation
├── app/                        # Android Application
├── feature/                    # Feature Modules
├── core/                       # Shared Core
└── scripts/                    # Build and Test Scripts
```

## Branch Strategy

### GitFlow Model
- **main**: Production-ready code
- **develop**: Integration branch for features
- **feature/**: Feature development branches
- **release/**: Release preparation branches
- **hotfix/**: Critical bug fixes

### Branch Protection Rules

#### Main Branch
- **Requires PR Reviews**: 2 reviewers required
- **Requires Status Checks**: All CI checks must pass
- **Requires Up-to-date**: Branch must be up-to-date
- **Restricts Pushes**: No direct pushes allowed

#### Develop Branch
- **Requires PR Reviews**: 1 reviewer required
- **Requires Status Checks**: All CI checks must pass
- **Requires Up-to-date**: Branch must be up-to-date

## GitHub Actions Workflows

### Continuous Integration (CI)
```yaml
# .github/workflows/ci.yml
name: Continuous Integration

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  android-ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run tests
        run: ./gradlew testDebugUnitTest
      - name: Build
        run: ./gradlew assembleDebug
      - name: Lint
        run: ./gradlew detekt

  ios-ci:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
      - name: Install dependencies
        run: pnpm install --frozen-lockfile
      - name: Build
        run: pnpm build
      - name: Test
        run: pnpm test

  web-ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
      - name: Install dependencies
        run: pnpm install --frozen-lockfile
      - name: Build
        run: pnpm build
      - name: Test
        run: pnpm exec playwright test
```

### Continuous Deployment (CD)
```yaml
# .github/workflows/cd.yml
name: Continuous Deployment

on:
  push:
    branches: [main]
    tags: ['v*']

jobs:
  android-deploy:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build Release
        run: ./gradlew assembleRelease
      - name: Upload to Play Store
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.GOOGLE_PLAY_SERVICE_ACCOUNT }}
          packageName: com.mira.com
          releaseFiles: app/build/outputs/apk/release/app-release.apk

  ios-deploy:
    runs-on: macos-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
      - name: Build
        run: pnpm build
      - name: Sync iOS
        run: pnpm exec cap sync ios
      - name: Build iOS
        run: |
          cd ios/App
          pod install
          xcodebuild -workspace App.xcworkspace -scheme App -configuration Release archive
      - name: Upload to App Store
        uses: apple-actions/upload-app-store@v1
        with:
          app-path: ios/build/App.xcarchive
          api-key: ${{ secrets.APP_STORE_API_KEY }}

  web-deploy:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
      - name: Build
        run: pnpm build
      - name: Deploy to GitHub Pages
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./dist
```

### Security Scanning
```yaml
# .github/workflows/security.yml
name: Security Scanning

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]
  schedule:
    - cron: '0 0 * * 0'  # Weekly

jobs:
  dependency-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'sarif'
          output: 'trivy-results.sarif'
      - name: Upload Trivy scan results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'

  code-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Initialize CodeQL
        uses: github/codeql-action/init@v2
        with:
          languages: kotlin, javascript
      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v2
```

## Issue Management

### Issue Templates
- **Bug Report**: Standardized bug reporting template
- **Feature Request**: Feature request template
- **Documentation**: Documentation improvement template
- **Performance**: Performance issue template

### Issue Labels
- **Priority**: critical, high, medium, low
- **Type**: bug, feature, documentation, performance
- **Platform**: android, ios, web, infra
- **Status**: triaged, in-progress, review, done

### Issue Workflow
1. **Creation**: Issues created with appropriate templates
2. **Triaging**: Issues labeled and assigned
3. **Development**: Issues linked to pull requests
4. **Testing**: Issues tested and validated
5. **Closure**: Issues closed with resolution

## Pull Request Process

### PR Templates
- **Feature PR**: Feature development template
- **Bug Fix PR**: Bug fix template
- **Documentation PR**: Documentation update template
- **Release PR**: Release preparation template

### PR Requirements
- **Description**: Clear description of changes
- **Testing**: Evidence of testing
- **Documentation**: Updated documentation
- **Breaking Changes**: Clear indication of breaking changes

### Review Process
1. **Automated Checks**: CI/CD pipeline validation
2. **Code Review**: Peer review by team members
3. **Testing**: Manual testing if required
4. **Approval**: Required approvals before merge
5. **Merge**: Automated merge after approval

## Release Management

### Release Process
1. **Version Bump**: Update version numbers
2. **Changelog**: Generate changelog
3. **Tag Creation**: Create release tag
4. **Build**: Build release artifacts
5. **Deploy**: Deploy to production
6. **Announce**: Announce release

### Release Automation
```yaml
# .github/workflows/release.yml
name: Release

on:
  push:
    tags: ['v*']

jobs:
  create-release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Create Release
        uses: actions/create-release@v1
        with:
          tag_name: ${{ github.ref }}
          release_name: Release ${{ github.ref }}
          body: |
            ## Changes
            - Automated release notes
            - Performance improvements
            - Bug fixes
          draft: false
          prerelease: false
```

## Secrets Management

### Required Secrets
- **GOOGLE_PLAY_SERVICE_ACCOUNT**: Play Store service account
- **APP_STORE_API_KEY**: App Store Connect API key
- **GITHUB_TOKEN**: GitHub API token
- **NPM_TOKEN**: NPM package registry token

### Secret Security
- **Encryption**: All secrets encrypted at rest
- **Access Control**: Limited access to secrets
- **Rotation**: Regular secret rotation
- **Audit**: Secret access auditing

## Monitoring and Analytics

### Repository Analytics
- **Traffic**: Repository traffic and clone statistics
- **Contributors**: Contributor activity and statistics
- **Issues**: Issue resolution metrics
- **Pull Requests**: PR review and merge metrics

### Performance Metrics
- **Build Time**: CI/CD pipeline performance
- **Test Coverage**: Code coverage metrics
- **Deployment Time**: Deployment performance
- **Error Rate**: Production error rates

## Best Practices

### Development Workflow
1. **Feature Branch**: Create feature branch from develop
2. **Development**: Implement feature with tests
3. **Pull Request**: Create PR with description
4. **Review**: Address review feedback
5. **Merge**: Merge after approval
6. **Cleanup**: Delete feature branch

### Code Quality
- **Linting**: Automated code style checking
- **Testing**: Comprehensive test coverage
- **Documentation**: Clear code documentation
- **Review**: Thorough code review process

### Security
- **Dependencies**: Regular dependency updates
- **Scanning**: Automated security scanning
- **Secrets**: Secure secret management
- **Access**: Limited repository access

---

**Last Updated**: October 7, 2025  
**Version**: 1.0  
**Status**: Production Ready
