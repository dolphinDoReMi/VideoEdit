# Scripts Documentation

## Overview

This directory contains all scripts organized by feature for the VideoEditor application. Scripts are categorized by functionality and maintained with kebab-case naming conventions.

## Directory Structure

```
scripts/
├── clip/                          # CLIP video clipping scripts
├── whisper/                       # Whisper audio transcription scripts
├── infra/                         # Infrastructure and storage scripts
├── cicd/                         # CI/CD and deployment scripts
├── ui/                           # User interface scripts
├── test/                         # Testing and validation scripts
└── README.md                     # This file
```

## Script Categories

### CLIP Scripts (`clip/`)
Scripts related to CLIP video processing and AutoClipper functionality:

- **Verification Scripts**: End-to-end CLIP pipeline verification
- **Processing Scripts**: Video processing and clip generation
- **Tennis-Specific Scripts**: Specialized tennis video processing
- **Demo Scripts**: Demonstration and example scripts

### Whisper Scripts (`whisper/`)
Scripts related to Whisper audio transcription and language detection:

- **Verification Scripts**: Whisper service and functionality verification
- **Processing Scripts**: Audio processing and transcription
- **Integration Scripts**: Complete integration testing
- **Model Management**: Model download and bootstrap scripts

### Infrastructure Scripts (`infra/`)
Scripts related to storage, resource monitoring, and system infrastructure:

- **Verification Scripts**: Comprehensive infrastructure verification (A-M)
- **Storage Scripts**: Scoped storage and duplicate detection
- **Resource Monitoring**: Memory, CPU, and performance monitoring
- **System Scripts**: System-level operations and diagnostics

### CI/CD Scripts (`cicd/`)
Scripts related to continuous integration, deployment, and release management:

- **Build Scripts**: Build automation and configuration
- **Deployment Scripts**: Multi-platform deployment automation
- **Validation Scripts**: Pipeline and system validation
- **Release Scripts**: Release management and versioning

### UI Scripts (`ui/`)
Scripts related to user interface, file selection, and user experience:

- **File Selection Scripts**: SAF integration and file handling
- **UI Testing Scripts**: User interface testing and validation
- **Demo Scripts**: User experience demonstrations

### Test Scripts (`test/`)
Comprehensive testing and validation scripts:

- **Unit Tests**: Individual component testing
- **Integration Tests**: Cross-module integration testing
- **Performance Tests**: Performance and benchmark testing
- **E2E Tests**: End-to-end testing scripts

## Usage Guidelines

### Script Execution
All scripts maintain executable permissions (`+x`) and use kebab-case naming:

```bash
# Execute a script
./scripts/clip/check-autoclip-status.sh

# Execute with parameters
./scripts/whisper/verify-whisper-activity.sh --verbose

# Execute from any directory
cd /path/to/project
./scripts/infra/verify-all.sh
```

### Script Dependencies
Scripts may depend on:
- **Android SDK**: For Android-specific operations
- **Xcode**: For iOS-specific operations
- **Node.js/pnpm**: For web-specific operations
- **System Tools**: Standard Unix tools and utilities

### Error Handling
All scripts include:
- **Error Checking**: Comprehensive error detection and handling
- **Logging**: Detailed logging with configurable levels
- **Exit Codes**: Proper exit codes for automation
- **Cleanup**: Proper cleanup on failure

## Script Maintenance

### Naming Conventions
- **kebab-case**: All script names use kebab-case (e.g., `verify-whisper-activity.sh`)
- **Descriptive Names**: Script names clearly describe their functionality
- **Consistent Suffixes**: Use `.sh` suffix for all shell scripts

### Documentation
Each script includes:
- **Header Comments**: Description, usage, and parameters
- **Inline Comments**: Key operations and logic explanation
- **Error Messages**: Clear, actionable error messages
- **Help Text**: Usage information and examples

### Testing
Scripts are tested for:
- **Functionality**: Correct operation under normal conditions
- **Error Handling**: Proper behavior under error conditions
- **Cross-platform**: Compatibility across different platforms
- **Performance**: Acceptable execution time and resource usage

## Quick Reference

### Most Used Scripts

#### CLIP Operations
```bash
# Check AutoClipper status
./scripts/clip/check-autoclip-status.sh

# Demo AutoClipper functionality
./scripts/clip/demo-autoclip.sh

# Process tennis clips
./scripts/clip/process-tennis-interview.sh
```

#### Whisper Operations
```bash
# Verify Whisper functionality
./scripts/whisper/verify-whisper-activity.sh

# Download Whisper models
./scripts/whisper/download-all-whisper-models.sh

# Test Whisper integration
./scripts/whisper/direct-whisper-test.sh
```

#### Infrastructure Operations
```bash
# Run comprehensive verification
./scripts/infra/verify-all.sh

# Setup scoped storage
./scripts/infra/setup-scoped-storage.sh

# Check storage compliance
./scripts/infra/check-storage-compliance.sh
```

#### CI/CD Operations
```bash
# Run CI/CD pipeline
./scripts/cicd/cicd.sh

# Deploy features
./scripts/cicd/deploy-feature.sh

# Validate pipeline
./scripts/cicd/validate-cicd-pipeline.sh
```

## Contributing

### Adding New Scripts
1. **Choose Category**: Place script in appropriate subdirectory
2. **Follow Naming**: Use kebab-case naming convention
3. **Add Documentation**: Include header comments and help text
4. **Test Thoroughly**: Test under various conditions
5. **Update README**: Update this documentation

### Modifying Existing Scripts
1. **Maintain Compatibility**: Ensure backward compatibility
2. **Update Documentation**: Update comments and help text
3. **Test Changes**: Test modifications thoroughly
4. **Update References**: Update any references to the script

### Script Standards
- **Error Handling**: Comprehensive error checking and handling
- **Logging**: Detailed logging with appropriate levels
- **Cleanup**: Proper cleanup on success and failure
- **Performance**: Efficient execution and resource usage
- **Security**: Secure handling of sensitive data

---

**Last Updated**: October 9, 2025  
**Version**: 1.3  
**Status**: Production Ready