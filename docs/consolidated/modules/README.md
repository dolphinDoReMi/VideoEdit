# Modules

## Core Modules

### 1. **CLIP Feature Module** (`feature/clip/`)
- **Purpose**: Video frame embedding using CLIP ViT-B/32
- **Key Classes**:
  - `VideoFrameExtractor`: Frame extraction from video files
  - `ClipPreprocess`: Image preprocessing and normalization
  - `ClipEngine`: CLIP model execution and embedding generation
  - `ClipRunner`: End-to-end video processing pipeline
  - `ClipReceiver`: Broadcast receiver for CLIP operations
- **Input**: Video files (MP4, etc.)
- **Output**: 512-dimensional embeddings (.f32) + JSON metadata
- **Communication**: `${applicationId}.CLIP.RUN` broadcast

### 2. **Retrieval Module** (`feature/retrieval/`)
- **Purpose**: Vector similarity search and ranking
- **Key Classes**:
  - `IndexBackend`: Abstract search interface
  - `FlatIndexBackend`: Linear scan with cosine similarity
  - `EmbeddingStore`: Binary vector storage and retrieval
  - `ResultsWriter`: Search results output formatting
- **Input**: Query vectors (512-dimensional)
- **Output**: Ranked results with cosine similarity scores
- **Storage**: Binary .f32 format with Little-Endian encoding

### 3. **Whisper Module** (`feature/whisper/`)
- **Purpose**: Audio transcription and processing
- **Status**: Planned integration
- **Technology**: Whisper.cpp for on-device transcription

### 4. **UI Module** (`feature/ui/`)
- **Purpose**: User interface components
- **Status**: Basic implementation
- **Technology**: Jetpack Compose

## Core Infrastructure

### 1. **Infra Module** (`core/infra/`)
- **Purpose**: Shared infrastructure and configuration
- **Key Classes**:
  - `Config`: Global configuration and control knots
  - `SharedFileProvider`: File sharing utilities
  - `FeatureReceiver`: Broadcast event handling
- **Configuration**: BuildConfig fields, runtime overrides

### 2. **Media Module** (`core/media/`)
- **Purpose**: Media processing and manipulation
- **Key Classes**:
  - `FrameSampler`: Temporal sampling algorithms
  - `TimestampPolicies`: Sampling policy implementations
- **Algorithms**: UNIFORM, TSN_JITTER, SLOWFAST_LITE

### 3. **ML Module** (`core/ml/`)
- **Purpose**: Machine learning utilities and models
- **Status**: Basic framework
- **Technology**: PyTorch Mobile integration

## App Module

### 1. **Main App** (`app/`)
- **Purpose**: Application entry point and coordination
- **Key Components**:
  - `MiraApp`: Application class
  - `MainActivity`: Main user interface
  - Broadcast receivers for feature communication
- **Build Variants**: Debug, Internal, Release
- **Package Names**: `mira.ui`, `mira.ui.debug`, `mira.ui.internal`

## Module Dependencies

```
app
├── core/infra
├── core/media
├── core/ml
├── feature/clip
├── feature/retrieval
├── feature/whisper
└── feature/ui
```

## Communication Patterns

### 1. **Broadcast Communication**
- Feature modules communicate via Android broadcasts
- Namespaced actions: `${applicationId}.FEATURE.ACTION`
- Non-exported receivers for security

### 2. **Background Processing**
- WorkManager for long-running tasks
- Foreground services for user-visible operations
- Crash-resilient job scheduling

### 3. **Data Storage**
- Binary .f32 format for vector embeddings
- JSON metadata for audit trails
- SharedPreferences for configuration overrides

## Module Isolation

### 1. **Package Isolation**
- Debug builds use `.debug` suffix
- Release builds maintain original package name
- Feature modules in separate packages

### 2. **Build Variants**
- Debug: Development and testing
- Internal: Internal testing with some debugging
- Release: Production build with optimizations

### 3. **Security**
- Non-exported components
- Signature permissions for internal communication
- Secure keystore for release builds
