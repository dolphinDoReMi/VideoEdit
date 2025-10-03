# Release

This directory contains release documentation for iOS, Android, and macOS Web versions, including policy guidelines.

## Contents

### Android Release
- **CLIP4Clip_Production_Deployment_Checklist.md** - Production deployment checklist
- **DISTRIBUTION_RELEASE_GUIDE.md** - Distribution and release guide
- **ANDROID_TESTFLIGHT_ALTERNATIVES.md** - Android testing alternatives

### Firebase Integration
- **FIREBASE_APP_DISTRIBUTION_SETUP.md** - Firebase App Distribution setup
- **FIREBASE_SETUP_COMPLETE.md** - Firebase setup completion
- **FIREBASE_SETUP_GUIDE.md** - Firebase setup guide
- **FIREBASE_STATUS_REPORT.md** - Firebase status report
- **COMPLETE_FIREBASE_SETUP.md** - Complete Firebase setup guide

### Internal Testing
- **INTERNAL_TESTING_ACTION_PLAN.md** - Internal testing action plan
- **INTERNAL_TESTING_LAUNCH_PLAN.md** - Internal testing launch plan
- **INTERNAL_TESTING_SUBMISSION.md** - Internal testing submission
- **INTERNAL_TESTING.md** - Internal testing overview
- **TESTER_COMMUNICATION_TEMPLATES.md** - Tester communication templates
- **DEVELOPER_INVITATION_SYSTEM.md** - Developer invitation system

### Public Release
- **PUBLIC_RELEASE.md** - Public release guide
- **RELEASE_NOTES_v0.1.0.md** - Release notes for v0.1.0

## Release Strategy

### Build Variants
- **Debug**: `com.mira.com.debug` - Development and testing
- **Internal**: `com.mira.com.internal` - Internal testing and QA
- **Release**: `com.mira.com` - Production release

### Release Pipeline

1. **Development** → Debug builds for development
2. **Internal Testing** → Internal builds for QA and testing
3. **Beta Testing** → Firebase App Distribution for beta testers
4. **Production** → Google Play Store release

### Policy Guidelines

#### Code Quality
- All tests must pass (unit, integration, e2e)
- Policy guard checks must pass
- Code review required for all changes
- Conventional Commits format required

#### Release Process
- Automated CI/CD pipeline
- Firebase App Distribution for testing
- Keystore signing for release builds
- Version bumping and changelog updates

#### Security
- Keystore protection and rotation
- Firebase security rules
- Code signing verification
- Dependency vulnerability scanning

## Release Checklist

### Pre-Release
- [ ] All tests passing
- [ ] Policy guard checks passing
- [ ] Code review completed
- [ ] Documentation updated
- [ ] Changelog updated
- [ ] Version bumped

### Release
- [ ] Build signed with release keystore
- [ ] Firebase App Distribution updated
- [ ] Internal testing completed
- [ ] Beta testing completed
- [ ] Production release approved

### Post-Release
- [ ] Release notes published
- [ ] Monitoring and analytics active
- [ ] User feedback collection
- [ ] Bug tracking and resolution

## Platform-Specific Notes

### Android
- Target SDK: 34
- Minimum SDK: 26
- Architecture: arm64-v8a, armeabi-v7a, x86_64
- Signing: Release keystore with 25-year validity

### iOS (Future)
- Target iOS: 15.0+
- Architecture: arm64
- Distribution: App Store Connect
- Signing: Apple Developer certificates

### macOS Web (Future)
- Target: Modern browsers
- Framework: Capacitor
- Distribution: Web hosting
- Signing: Code signing certificates
