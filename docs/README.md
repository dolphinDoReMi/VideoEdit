# Mira Video Editor Documentation

Welcome to the Mira Video Editor documentation. This project implements AI-powered video editing with automatic clip selection using CLIP (Contrastive Language-Image Pre-training) technology.

## 🎯 Project Overview

Mira Video Editor is an Android application that uses CLIP ViT-B/32 models to:
- Extract video frames and generate embeddings
- Perform text-to-video search and retrieval
- Automatically select and edit video clips
- Provide background processing with no UI dependencies

## 📁 Documentation Structure

The documentation is organized into **4 main threads** for easy navigation:

### 1. 🏗️ [Architecture Design](architecture/)
**System architecture, design principles, and verification**
- System architecture and design patterns
- Development guidelines and policies
- Architecture verification procedures
- Performance analysis and optimization

### 2. 🔧 [Modules](modules/)
**Feature modules, implementations, and testing guides**
- CLIP feature implementation
- Whisper integration
- FAISS vector indexing
- Temporal sampling system
- Database integration
- Device-specific testing

### 3. 📝 [DEV Changelog](dev-changelog/)
**Development history and version tracking**
- Complete development history
- Version-by-version feature additions
- Implementation details and decisions
- Testing results and validation

### 4. 🚀 [Release](release/)
**Release management, deployment, and distribution**
- Release lifecycle management
- Firebase App Distribution
- Store submission procedures
- Internal testing workflows

## 🚀 Quick Start

### For New Developers
1. **Start Here**: Read [Project Context Guidance](architecture/PROJECT_CONTEXT_GUIDANCE.md)
2. **Architecture**: Review [CLIP4Clip Service Architecture](architecture/CLIP4CLIP_SERVICE_ARCHITECTURE.md)
3. **Implementation**: Follow [CLIP Feature README](modules/CLIP_FEATURE_README.md)
4. **Testing**: Use [Step-by-Step Testing Guide](modules/CLIP4Clip_Step_by_Step_Testing_Guide.md)

### For Release Managers
1. **Release Process**: Follow [Distribution Release Guide](release/DISTRIBUTION_RELEASE_GUIDE.md)
2. **Deployment**: Use [Production Deployment Checklist](release/CLIP4Clip_Production_Deployment_Checklist.md)
3. **Testing**: Execute [Internal Testing Action Plan](release/INTERNAL_TESTING_ACTION_PLAN.md)

### For Testers
1. **Testing Guide**: Follow [Step-by-Step Testing Guide](modules/CLIP4Clip_Step_by_Step_Testing_Guide.md)
2. **Device Testing**: Use [Xiaomi Pad Testing Guide](modules/XIAOMI_PAD_COMPREHENSIVE_TEST_REPORT.md)
3. **Performance**: Monitor with [Resource Monitoring Guide](modules/XIAOMI_PAD_RESOURCE_MONITORING_GUIDE.md)

## 🔧 Key Features

### CLIP Integration
- **ViT-B/32 Model**: 512-dimensional embeddings
- **BPE Tokenizer**: Real byte-level tokenization
- **Background Processing**: No UI dependencies
- **Stable Broadcasts**: CI/CD-friendly action names

### Video Processing
- **Temporal Sampling**: Deterministic frame extraction
- **Media3 Integration**: Hardware-accelerated processing
- **FAISS Indexing**: Vector search and retrieval
- **Database Storage**: Room database integration

### Development Features
- **Frozen App ID**: `com.mira.com` across all variants
- **Debug Isolation**: Separate debug package
- **Unified Configuration**: Single source of truth
- **CI/CD Integration**: GitHub Actions workflows

## 📊 Current Status

### ✅ Completed Features
- CLIP ViT-B/32 implementation with real model
- Temporal sampling system with multiple policies
- FAISS vector indexing with multiple backends
- Background processing with WorkManager
- Firebase App Distribution integration
- Production-grade configuration management

### 🚧 In Progress
- CLIP feature orchestration integration
- Performance optimization
- Store submission preparation

### 🔮 Planned Features
- Real-time video processing
- Advanced search capabilities
- Performance monitoring
- User analytics

## 🛠️ Development Workflow

### Build Commands
```bash
# Build debug APK
./gradlew :app:assembleDebug

# Run tests
./gradlew :app:testDebugUnitTest

# Run lint checks
./gradlew :app:lintDebug
```

### Testing Commands
```bash
# Test orchestration broadcast
adb shell am broadcast \
  -n com.mira.com/.orch.OrchestrationReceiver \
  -a com.mira.clip.ORCHESTRATE

# Test debug CLIP broadcast
adb shell am broadcast \
  -n com.mira.com/.feature.clip.debug.DebugClipReceiver \
  -a com.mira.clip.CLIP.RUN
```

## 📚 Additional Resources

### Scripts
- **Architecture Scripts**: `scripts/architecture/` - Architecture validation
- **Module Scripts**: `scripts/modules/` - Testing and verification
- **Release Scripts**: `scripts/release/` - Build and deployment

### Configuration
- **Build Configuration**: `app/build.gradle.kts` - Gradle build configuration
- **Manifests**: `manifests/` - Test manifests and configurations
- **Assets**: `assets/` - Model files and test assets

## 🤝 Contributing

1. **Read Guidelines**: Review [Cursor Workspace Rules](architecture/Cursor-Workspace-Rules.md)
2. **Follow Architecture**: Adhere to [CLIP4Clip Service Architecture](architecture/CLIP4CLIP_SERVICE_ARCHITECTURE.md)
3. **Test Thoroughly**: Use [Step-by-Step Testing Guide](modules/CLIP4Clip_Step_by_Step_Testing_Guide.md)
4. **Update Documentation**: Keep documentation current with changes

## 📞 Support

- **Issues**: Report issues through the project's issue tracker
- **Documentation**: Check the relevant thread documentation
- **Testing**: Use the comprehensive testing guides
- **Release**: Follow the release management procedures

---

**Last Updated**: January 3, 2025  
**Version**: v0.9.0  
**Status**: Production Ready ✅