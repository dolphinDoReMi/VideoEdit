# Architecture Design

This directory contains the core architecture design documentation for the VideoEdit project.

## Contents

- **Project1_Media3_VideoPipeline.md** - Media3 video processing pipeline architecture
- **Android_VideoEdit_Template_Context.md** - Android template context and setup
- **PROJECT_CONTEXT_GUIDANCE.md** - Project context and architectural guidance
- **CICD_DEVELOPER_GUIDE.md** - CI/CD architecture and developer workflow
- **AUTOCUTAPPLICATION_VERIFICATION.md** - AutoCut application architecture verification
- **AUTOCUTENGINE_VERIFICATION.md** - AutoCut engine architecture verification
- **MAINACTIVITY_VERIFICATION.md** - MainActivity architecture verification
- **MEDIASTOREEXT_VERIFICATION.md** - MediaStore extension architecture verification
- **VIDEOSCORER_VERIFICATION.md** - VideoScorer architecture verification
- **FINAL_OUTPUT_ANALYSIS.md** - Final output analysis and architecture review
- **REAL_VIDEO_PROCESSING_ANALYSIS.md** - Real video processing architecture analysis
- **MEDIA3_PROCESSING_ANALYSIS.md** - Media3 processing architecture analysis
- **MEDIA3_STEP_BY_STEP_WORKFLOW.md** - Media3 step-by-step workflow architecture

## Architecture Overview

The VideoEdit project follows a modular architecture with the following key components:

1. **Core Modules** (`core/`)
   - `infra/` - Infrastructure and utilities
   - `media/` - Media processing core
   - `ml/` - Machine learning core

2. **Feature Modules** (`feature/`)
   - `clip/` - CLIP-based video analysis
   - `retrieval/` - Video retrieval system
   - `ui/` - User interface components
   - `whisper/` - Audio transcription (build files preserved)

3. **Application Layer** (`app/`)
   - Main application with build variants (debug, internal, release)
   - Firebase integration and distribution
   - Policy guard system

## Design Principles

- **Modularity**: Each feature is encapsulated in its own module
- **Separation of Concerns**: Clear boundaries between core, feature, and app layers
- **Testability**: Comprehensive testing at unit, integration, and e2e levels
- **Policy Enforcement**: Automated checks for code quality and architectural constraints
- **CI/CD Integration**: Automated build, test, and deployment pipelines