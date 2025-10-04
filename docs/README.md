# Mira Video Editor Documentation

## 📚 Documentation Structure

This documentation is organized into 6 main feature-based documents for easy navigation and maintenance:

### 1. 🎯 CLIP Feature (`CLIP_ARCHITECTURE.md` & `CLIP_IMPLEMENTATION.md`)
- **Architecture Design**: CLIP service architecture, control knots, and verification system
- **Implementation Details**: Complete Android/Kotlin implementation with production-ready code
- **Control Knots**: Deterministic sampling, fixed preprocessing, model assets
- **Verification**: SHA-256 hash comparison for deterministic pipeline validation
- **Code Pointers**: Direct links to implementation files and scripts

### 2. 🎤 Whisper Feature (Coming Soon)
- **Architecture Design**: Whisper integration architecture and control systems
- **Implementation Details**: Full-scale whisper.cpp integration for content-aware video editing

### 3. 🎨 UI Feature (Coming Soon)
- **Architecture Design**: User interface architecture and design patterns
- **Implementation Details**: Complete UI implementation with Compose and Material Design

### 4. 📝 DEV Changelog (Coming Soon)
- **Development History**: Complete version history and technical implementation details
- **Feature Tracking**: Major features and implementations across all modules

### 5. 🚀 Release (Coming Soon)
- **Deployment Guides**: Production deployment instructions for all platforms
- **Distribution**: App store submission and distribution for iOS, Android, and macOS Web

### 6. 🏗️ Architecture (Coming Soon)
- **System Architecture**: Overall system design and components
- **Design Principles**: Core design patterns and guidelines

## 🛠️ Scripts Organization

Scripts are organized to support the consolidated documentation structure:

### Architecture Scripts (`scripts/architecture/`)
- Architecture validation and compliance scripts

### Module Scripts (`scripts/modules/`)
- Testing, verification, and operations scripts
- Comprehensive test suites and validation

### DEV Changelog Scripts (`scripts/dev-changelog/`)
- Changelog management and versioning scripts

### Release Scripts (`scripts/release/`)
- Build, deployment, and distribution automation

### Tools (`scripts/tools/`)
- Utility scripts and helper tools
- Thread suffix generation for CI/CD

Note: This covers platform releases across iOS (Capacitor), Android, and macOS Web. See `RELEASE.md` for platform-specific workflows.

## 🚀 Quick Start

### Local Development
```bash
# Get thread suffix for isolated builds
THREAD=$(./scripts/thread_suffix.sh)

# Build with per-thread app ID
./gradlew :app:installDebug -PappIdSuffix="$THREAD"

# Run tests
./gradlew :app:connectedDebugAndroidTest -PappIdSuffix="$THREAD"
```

### CI/CD
- **GitHub Actions**: Automated builds with per-thread app ID suffixing
- **Conditional Testing**: Runs relevant tests based on changed files
- **Artifact Management**: Uploads builds with thread suffix naming

### Documentation
- **CLIP Architecture**: Start with `CLIP_ARCHITECTURE.md`
- **CLIP Implementation**: Check `CLIP_IMPLEMENTATION.md` for complete implementation
- **Feature Guides**: Each feature has dedicated architecture and implementation docs
- **Coming Soon**: Whisper, UI, DEV Changelog, Release, and Architecture docs

## 📋 Key Features

### Per-Thread App ID Isolation
- **Android**: `com.mira.com.t.<THREAD>` for parallel installs
- **iOS**: `com.mira.com.t.<THREAD>` for parallel installs
- **Deterministic**: Branch+commit SHA for consistent IDs

### CI/CD Pipeline
- **Automated Builds**: GitHub Actions with conditional testing
- **Artifact Management**: Suffix-based naming for easy mapping
- **Cross-Platform**: Android (active), iOS (stubbed)

### Documentation Consolidation
- **4-Thread Structure**: Logical organization
- **Duplicate Removal**: Single source of truth
- **Copy-Pasteable Commands**: Ready-to-use examples

## 🔗 Related Documentation

- [CLIP Architecture](CLIP_ARCHITECTURE.md) - CLIP service architecture and control knots
- [CLIP Implementation](CLIP_IMPLEMENTATION.md) - Complete CLIP implementation details
- **Coming Soon**: Whisper, UI, DEV Changelog, Release, and Architecture documentation

---

**Last Updated**: 2025-01-04  
**Version**: v1.0.0  
**Status**: ✅ Production Ready