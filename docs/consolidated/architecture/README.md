# Architecture Design

## Overview

Mira is an AI-powered video editing application that automatically selects the most engaging segments from videos using CLIP (Contrastive Language-Image Pre-training) embeddings and vector similarity search.

## Core Architecture

### 1. **CLIP Feature Module**
- **Purpose**: Video frame embedding and text-to-video search
- **Technology**: CLIP ViT-B/32 with 512-dimensional embeddings
- **Components**: VideoFrameExtractor, ClipPreprocess, ClipEngine, ClipRunner, ClipReceiver
- **Communication**: Broadcast-based (`${applicationId}.CLIP.RUN`)

### 2. **Retrieval System**
- **Purpose**: Vector similarity search and ranking
- **Technology**: FAISS indexing with cosine similarity
- **Components**: IndexBackend, FlatIndexBackend, EmbeddingStore, ResultsWriter
- **Storage**: Binary .f32 format with JSON metadata

### 3. **Temporal Sampling**
- **Purpose**: Deterministic frame extraction from videos
- **Technology**: MediaMetadataRetriever with multiple sampling policies
- **Components**: FrameSampler, TimestampPolicies, SamplerService
- **Output**: Fixed-length frame sequences (e.g., 32 frames)

### 4. **Policy Guard System**
- **Purpose**: Code quality and compliance enforcement
- **Technology**: Git hooks + GitHub Actions
- **Components**: push_guard.sh, commit-msg hook, pre-push hook
- **Enforcement**: Conventional Commits, app ID freeze, feature tests

### 5. **Firebase Integration**
- **Purpose**: App distribution and testing
- **Technology**: Firebase App Distribution
- **Components**: Multi-variant support, keystore signing
- **Build Variants**: Debug, Internal, Release

## Data Flow

```
Video Input → Temporal Sampling → Frame Extraction → CLIP Embedding → Vector Storage → Similarity Search → Results
```

## Key Design Principles

1. **Modular Architecture**: Feature-based modules with clear boundaries
2. **Background Processing**: No-UI operations via broadcasts and WorkManager
3. **Deterministic Behavior**: Reproducible results with fixed parameters
4. **Auditable Storage**: Binary format with JSON metadata for verification
5. **Policy Compliance**: Automated enforcement of code quality standards

## Technology Stack

- **Language**: Kotlin
- **ML Framework**: PyTorch Mobile (CLIP)
- **Vector Search**: FAISS (planned), Flat search (current)
- **Background Processing**: WorkManager
- **Communication**: Broadcast receivers
- **Storage**: Binary .f32 + JSON metadata
- **Build System**: Gradle with multi-variant support
- **Distribution**: Firebase App Distribution
