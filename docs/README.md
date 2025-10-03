# Mira Video Editor Documentation

## 📚 Documentation Structure

This documentation is organized into 4 main threads for easy navigation and maintenance:

### 1. 🏗️ Architecture Design (`docs/architecture/`)
- **System Architecture**: Overall system design and components
- **Design Principles**: Core design patterns and guidelines
- **Verification**: Architecture validation and compliance
- **Policy**: Change management and development policies

### 2. 🧩 Modules (`docs/modules/`)
- **Feature Modules**: Individual feature implementations
- **Testing Guides**: Comprehensive testing documentation
- **Progress Reports**: Development progress and status
- **Integration Guides**: Module integration instructions

### 3. 📝 DEV Changelog (`docs/dev-changelog/`)
- **Development History**: Complete version history
- **Feature Tracking**: Major features and implementations
- **Technical Details**: Implementation notes and decisions
- **Version Management**: Release versioning and tracking

### 4. 🚀 Release (`docs/release/`)
- **Deployment Guides**: Production deployment instructions
- **Distribution**: App store submission and distribution
- **Testing**: Release testing and validation
- **Store Management**: Play Store, App Store, and Xiaomi Store

## 🛠️ Scripts Organization

Scripts are organized to match the documentation structure:

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
- **Architecture**: Start with `docs/architecture/README.md`
- **Modules**: Check `docs/modules/README.md` for feature guides
- **Release**: See `docs/release/README.md` for deployment
- **Changelog**: Review `docs/dev-changelog/DEV_CHANGELOG.md`

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

- [Local/Edge Builds Guide](LOCAL_EDGE_BUILDS.md) - Device testing commands
- [CI/CD Developer Guide](architecture/CICD_DEVELOPER_GUIDE.md) - CI/CD setup
- [CLIP Feature Guide](modules/CLIP_FEATURE_README.md) - CLIP implementation
- [Release Guide](release/README.md) - Deployment instructions

---

**Last Updated**: 2025-01-04  
**Version**: v1.0.0  
**Status**: ✅ Production Ready