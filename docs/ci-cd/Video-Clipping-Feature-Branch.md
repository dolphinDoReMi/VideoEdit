# Video Clipping Feature Branch Workflow

This guide explains how to work with the video clipping feature branch and integrate with the existing CI/CD pipeline.

## Current Status

- **Active Branch**: `whisper-clipping`
- **Purpose**: Video clipping functionality development
- **CI/CD Integration**: Fully integrated with existing workflows

## Existing CI/CD Support

The project already has comprehensive CI/CD workflows that support video clipping development:

### 1. Main CI Pipeline (`ci.yml`)
- **Automatic Detection**: Detects video-related changes in `app/src/main/java/com/mira/videoeditor/video/**`
- **Conditional Testing**: Runs video-specific tests when video code changes
- **Thread Isolation**: Prevents build conflicts with unique suffixes
- **Documentation Validation**: Ensures video clipping docs follow structure

### 2. Code Quality (`code-quality.yml`)
- **Lint Checks**: Validates video clipping code quality
- **Report Generation**: Creates HTML reports for review
- **Artifact Upload**: Saves quality reports for analysis

### 3. Policy Guard (`push-guard.yml`)
- **Policy Enforcement**: Ensures video clipping changes follow project standards
- **Fast Validation**: Quick checks for configuration and code quality
- **Documentation Compliance**: Validates video clipping documentation structure

## Video Clipping Development Workflow

### 1. Working on Feature Branch
```bash
# Ensure you're on the feature branch
git checkout whisper-clipping

# Make your video clipping changes
# ... edit files ...

# Commit with descriptive message
git add .
git commit -m "feat: implement video clipping algorithm

- Add frame extraction logic
- Implement similarity scoring
- Add clip boundary detection
- Update UI for clip preview"

# Push to feature branch
git push origin whisper-clipping
```

### 2. CI/CD Automatic Triggers

When you push to `whisper-clipping`, the following workflows automatically run:

1. **CI Pipeline**: Detects video changes and runs appropriate tests
2. **Code Quality**: Validates code quality and generates reports
3. **Policy Guard**: Ensures compliance with project policies

### 3. Testing Integration

The existing workflows will automatically:
- Run unit tests if database or ML code changes
- Run instrumented tests if workers, ML, or video code changes
- Validate documentation structure
- Check code quality and policy compliance

## Commit Message Guidelines

For video clipping features, use these commit message patterns:

### Feature Implementation
```
feat: implement video clipping core algorithm

- Add frame extraction with uniform sampling
- Implement CLIP-based similarity scoring
- Add clip boundary detection logic
- Update UI for clip preview functionality
```

### Bug Fixes
```
fix: resolve video clipping memory leak

- Fix frame buffer cleanup in AutoClipperWorker
- Add proper resource disposal
- Update memory monitoring
```

### Performance Improvements
```
perf: optimize video clipping processing

- Reduce memory usage by 30%
- Improve frame extraction speed
- Add parallel processing support
```

### Documentation Updates
```
docs: update video clipping architecture

- Add control knots documentation
- Update implementation details
- Add performance benchmarks
```

## Branch Protection Rules

The `whisper-clipping` branch should follow these rules:

1. **Require PR Reviews**: All changes must go through pull request review
2. **Require Status Checks**: All CI/CD workflows must pass
3. **Require Up-to-date**: Branch must be up-to-date with main before merging
4. **Restrict Pushes**: Only allow pushes through pull requests

## Integration with Main Branch

When ready to merge video clipping features:

1. **Create Pull Request**: From `whisper-clipping` to `main`
2. **CI/CD Validation**: All workflows will run automatically
3. **Code Review**: Team reviews the changes
4. **Merge**: Once approved, merge to main
5. **Release**: Tag for release pipeline to build and deploy

## Monitoring CI/CD Results

### GitHub Actions Tab
- View all workflow runs
- Check individual job status
- Download artifacts (test reports, quality reports)
- Review logs for debugging

### Key Artifacts to Monitor
- **APK Builds**: Debug builds for testing
- **Test Reports**: Unit and instrumented test results
- **Quality Reports**: Lint and code quality analysis
- **Documentation Validation**: Structure compliance checks

## Troubleshooting

### Common Issues

1. **Build Failures**: Check thread suffix conflicts
2. **Test Failures**: Review test reports in artifacts
3. **Quality Issues**: Check lint reports for problems
4. **Policy Violations**: Review policy guard output

### Getting Help

- Check workflow logs in GitHub Actions
- Review artifact reports for detailed errors
- Consult CI/CD documentation for guidance
- Use manual dispatch for testing workflows

## Best Practices

1. **Small Commits**: Make focused, atomic commits
2. **Descriptive Messages**: Use conventional commit format
3. **Test Locally**: Run tests before pushing
4. **Monitor CI**: Check workflow results regularly
5. **Update Docs**: Keep video clipping documentation current
6. **Follow Policies**: Ensure compliance with project standards

## Next Steps

1. **Continue Development**: Work on video clipping features in `whisper-clipping` branch
2. **Monitor CI/CD**: Check workflow results after each push
3. **Prepare for Merge**: When ready, create pull request to main
4. **Release Planning**: Plan release strategy for video clipping features

The existing CI/CD infrastructure fully supports video clipping development - no new pipelines needed!
