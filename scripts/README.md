# Scripts Directory

This directory contains all build, test, and deployment scripts for the VideoEdit Android application.

## Directory Structure

- **`build/`** - Build and compilation scripts
- **`test/`** - Testing scripts and test automation
- **`deployment/`** - Deployment and release scripts
- **`firebase/`** - Firebase setup and configuration scripts

## Build Scripts (`build/`)

- `build-release.sh` - Build release APK/AAB files

## Test Scripts (`test/`)

- `local_test.sh` - Local testing script
- `test-e2e.sh` - End-to-end testing
- `test_core_capabilities.sh` - Core functionality testing
- `test-xiaomi-device.sh` - Xiaomi device specific testing
- `comprehensive_test_simulation.sh` - Comprehensive test simulation
- `comprehensive_xiaomi_test.sh` - Xiaomi comprehensive testing
- `advanced_local_test.sh` - Advanced local testing
- `debug_progress.sh` - Debug progress tracking
- `generate_test_videos.sh` - Generate test video assets

## Deployment Scripts (`deployment/`)

- `demo-workflow.sh` - Demo workflow automation
- `mira-workflow-demo.sh` - Mira workflow demonstration

## Firebase Scripts (`firebase/`)

- `firebase-auto-setup.sh` - Automated Firebase setup
- `firebase-complete-setup.sh` - Complete Firebase configuration
- `setup-firebase.sh` - Firebase setup script

## Usage

All scripts should be run from the project root directory:

```bash
# Example: Run local tests
./scripts/test/local_test.sh

# Example: Build release
./scripts/build/build-release.sh

# Example: Setup Firebase
./scripts/firebase/setup-firebase.sh
```

## Prerequisites

- Android SDK installed
- Gradle wrapper available
- Appropriate permissions for file operations
- Firebase CLI (for Firebase scripts)
