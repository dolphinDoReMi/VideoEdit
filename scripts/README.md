# Mira Video Editor - Scripts Directory

## 📁 Overview
This directory contains all build, test, deployment, and utility scripts for the Mira Video Editor Android application. Scripts are organized by function for easy navigation and maintenance.

> **📋 Scripts Index**: For quick access to all scripts, see [📖 Scripts Index](INDEX.md)

## 🗂️ Directory Structure

```
scripts/
├── architecture/     # Architecture validation and compliance
├── dev-changelog/    # Changelog helpers and versioning
├── modules/          # Feature/module testing & workflows
├── release/          # Build & release automation (Android, iOS, macOS Web)
├── test/             # End-to-end and device-specific tests
└── tools/            # (Removed) Use top-level `tools/` for helpers
```

## 🧭 Thread Mapping (Docs ↔ Scripts)

- Architecture Design → `docs/architecture/` ↔ `scripts/architecture/`
- Modules → `docs/modules/` ↔ `scripts/modules/`
- DEV Change Log → `docs/dev-changelog/` ↔ `scripts/dev-changelog/`
- Release (iOS, Android, macOS Web) → `docs/release/` ↔ `scripts/release/`

## 🚀 Quick Start

### Essential Scripts
- **Build**: `./scripts/build/build-release.sh` - Build release APK/AAB
- **Test**: `./scripts/test/run_all_tests.sh` - Run comprehensive tests
- **Deploy**: `./scripts/deployment/demo-workflow.sh` - Demo deployment
- **Monitor**: `./scripts/monitoring/xiaomi_resource_monitor.sh` - Resource monitoring

## 📋 Script Categories

### 🔨 Build Scripts (`build/`)
- `build-release.sh` - Build release APK/AAB files with signing

### 🧪 Test Scripts (`test/`)

#### Core Testing
- `run_all_tests.sh` - Execute all test suites
- `run_step_by_step_tests.sh` - Run tests in sequence
- `automated_testing_workflow.sh` - Automated testing workflow

#### Device-Specific Testing
- `xiaomi_pad_comprehensive_test.sh` - Comprehensive Xiaomi Pad testing
- `xiaomi_pad_real_video_test.sh` - Real video testing on Xiaomi Pad
- `comprehensive_xiaomi_test.sh` - Xiaomi device comprehensive testing

#### Component Testing
- `media3_autocut_test.sh` - Media3 AutoCut engine testing
- `media3_detailed_test.sh` - Detailed Media3 testing
- `test_core_capabilities.sh` - Core functionality testing
- `e2e_integration_test.sh` - End-to-end integration testing

#### Performance Testing
- `performance_benchmark.sh` - Performance benchmarking
- `enhanced_real_video_test.sh` - Enhanced real video testing
- `real_video_step_by_step_test.sh` - Step-by-step real video testing
- `real_video_walkthrough.sh` - Real video walkthrough testing

#### Test Utilities
- `create_test_videos.sh` - Generate test video assets
- `generate_test_videos.sh` - Generate test videos
- `debug_progress.sh` - Debug progress tracking
- `advanced_local_test.sh` - Advanced local testing
- `comprehensive_test_simulation.sh` - Test simulation
- `local_test.sh` - Local testing script

#### Step-by-Step Testing
- `step1_video_input_test.sh` - Video input testing
- `step2_thermal_test.sh` - Thermal testing
- `step3_content_analysis_test.sh` - Content analysis testing
- `step4_edl_generation_test.sh` - EDL generation testing
- `step5_thumbnail_gen_test.sh` - Thumbnail generation testing
- `step6_cropping_test.sh` - Cropping testing
- `step7_export_test.sh` - Export testing
- `step8_upload_test.sh` - Upload testing
- `step9_performance_test.sh` - Performance testing
- `step10_verification_test.sh` - Verification testing

#### Legacy Testing
- `test-e2e.sh` - Legacy end-to-end testing
- `test-xiaomi-device.sh` - Legacy Xiaomi device testing
- `thermal_verification.sh` - Thermal verification testing

### 🚀 Deployment Scripts (`deployment/`)
- `demo-workflow.sh` - Demo workflow automation
- `mira-workflow-demo.sh` - Mira workflow demonstration

### 🔥 Release & Firebase Scripts (`release/`)
- `build-release.sh` - Build signed artifacts
- `README.md` - Release process overview (Android, iOS, macOS Web)
- Firebase helpers are centralized under this thread

### 📊 Monitoring Scripts (`monitoring/`)
- `xiaomi_resource_monitor.sh` - Xiaomi device resource monitoring
- `xiaomi_performance_analyzer.sh` - Xiaomi performance analysis
- `xiaomi_resource_log_*.txt` - Resource monitoring logs

### 🛠️ Utilities
- Helper utilities live in top-level `tools/` (canonical location)
- The former `scripts/tools/` duplicates were removed to avoid drift

## 📖 Usage Examples

### Build and Test Workflow
```bash
# Build release
./scripts/build/build-release.sh

# Run comprehensive tests
./scripts/test/run_all_tests.sh

# Run Xiaomi Pad specific tests
./scripts/test/xiaomi_pad_comprehensive_test.sh

# Monitor performance
./scripts/monitoring/xiaomi_resource_monitor.sh
```

### Development Workflow
```bash
# Run local tests
./scripts/test/local_test.sh

# Debug progress
./scripts/test/debug_progress.sh

# Performance benchmark
./scripts/test/performance_benchmark.sh
```

### Deployment Workflow
```bash
# Setup Firebase
./scripts/firebase/setup-firebase.sh

# Run demo workflow
./scripts/deployment/demo-workflow.sh
```

## 🔧 Prerequisites

### System Requirements
- Android SDK installed
- Gradle wrapper available
- Appropriate permissions for file operations
- Firebase CLI (for Firebase scripts)

### Device Requirements
- Android device with USB debugging enabled
- Xiaomi Pad Ultra (for device-specific tests)
- Sufficient storage space for test videos

## 📋 Script Standards

### Naming Conventions
- Use descriptive names with underscores
- Include function in name (e.g., `test_`, `build_`, `deploy_`)
- Use lowercase with underscores for consistency

### Documentation
- Include header comments with purpose and usage
- Document prerequisites and dependencies
- Provide usage examples
- Include error handling and cleanup

### Error Handling
- Check prerequisites before execution
- Provide clear error messages
- Clean up temporary files
- Exit with appropriate codes

## 🔄 Maintenance

### Adding New Scripts
1. Place in appropriate category directory
2. Follow naming conventions
3. Include proper documentation
4. Update this README with description
5. Test thoroughly before committing

### Updating Scripts
1. Maintain backward compatibility
2. Update documentation
3. Test changes thoroughly
4. Update this README if needed

## 📞 Support

For script issues:
- Check prerequisites and dependencies
- Review script documentation
- Check device connectivity
- Verify file permissions
- Consult test reports for known issues

---

*Last updated: $(date)*
*Version: 1.0*