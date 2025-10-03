# Architecture Design Thread

This directory contains all architecture-related documentation, design principles, and verification guides for the Mira Video Editor project.

## 📁 Directory Structure

### Core Architecture Documents
- **`README.md`** - This overview document
- **`DOCUMENTATION_INDEX.md`** - Complete documentation index
- **`PROJECT_CONTEXT_GUIDANCE.md`** - Project context and guidance
- **`Android_VideoEdit_Template_Context.md`** - Android video editing template context

### System Architecture
- **`CLIP4CLIP_SERVICE_ARCHITECTURE.md`** - CLIP4Clip service architecture
- **`Project1_Media3_VideoPipeline.md`** - Media3 video pipeline architecture
- **`CONTROL_KNOTS.md`** - Control system documentation
- **`POLICY_GUARD_SYSTEM.md`** - Policy enforcement system

### Development Guidelines
- **`Cursor-Workspace-Rules.md`** - Development guidelines and rules
- **`Change-Policy.md`** - Change management policy
- **`CICD_DEVELOPER_GUIDE.md`** - CI/CD developer guide
- **`DEVELOPER_INVITATION_SYSTEM.md`** - Developer invitation system

### Verification & Testing
- **`AUTOCUTAPPLICATION_VERIFICATION.md`** - AutoCut application verification
- **`AUTOCUTENGINE_VERIFICATION.md`** - AutoCut engine verification
- **`MAINACTIVITY_VERIFICATION.md`** - MainActivity verification
- **`MEDIASTOREEXT_VERIFICATION.md`** - MediaStore extension verification
- **`VIDEOSCORER_VERIFICATION.md`** - Video scorer verification

### Analysis & Reports
- **`FINAL_OUTPUT_ANALYSIS.md`** - Final output analysis
- **`REAL_VIDEO_PROCESSING_ANALYSIS.md`** - Real video processing analysis
- **`MEDIA3_PROCESSING_ANALYSIS.md`** - Media3 processing analysis

### Firebase & Setup
- **`COMPLETE_FIREBASE_SETUP.md`** - Complete Firebase setup guide
- **`FIREBASE_KEYSTORE_SETUP_COMPLETE.md`** - Firebase keystore setup completion

### Communication & Testing
- **`TESTER_COMMUNICATION_TEMPLATES.md`** - Tester communication templates

## 🎯 Purpose

This thread focuses on:
- **System Architecture**: High-level design and architectural decisions
- **Design Principles**: Core principles and patterns used throughout the system
- **Verification**: Architecture compliance and verification procedures
- **Development Guidelines**: Rules and policies for development
- **Analysis**: Performance and processing analysis

## 🔗 Related Threads

- **Modules** (`../modules/`): Implementation details and feature modules
- **DEV Changelog** (`../dev-changelog/`): Development history and version tracking
- **Release** (`../release/`): Release management and deployment

## 📚 Key Documents

### For New Developers
1. Start with `PROJECT_CONTEXT_GUIDANCE.md`
2. Read `CLIP4CLIP_SERVICE_ARCHITECTURE.md` for system overview
3. Review `Cursor-Workspace-Rules.md` for development guidelines
4. Check `CONTROL_KNOTS.md` for configuration management

### For Architecture Reviews
1. `CLIP4CLIP_SERVICE_ARCHITECTURE.md` - System architecture
2. `Project1_Media3_VideoPipeline.md` - Video processing pipeline
3. `POLICY_GUARD_SYSTEM.md` - Policy enforcement
4. `CONTROL_KNOTS.md` - Configuration management

### For Verification
1. `AUTOCUTAPPLICATION_VERIFICATION.md` - Application verification
2. `AUTOCUTENGINE_VERIFICATION.md` - Engine verification
3. `MAINACTIVITY_VERIFICATION.md` - MainActivity verification
4. `VIDEOSCORER_VERIFICATION.md` - Video scorer verification

## 🛠️ Scripts

Related scripts are located in `scripts/architecture/`:
- Architecture validation scripts
- Compliance checking tools
- Verification automation

## 📝 Maintenance

This directory is maintained by the architecture team and should be updated when:
- New architectural decisions are made
- System components are added or modified
- Verification procedures change
- Development guidelines are updated