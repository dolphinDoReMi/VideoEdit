# VideoEdit Documentation

This documentation provides comprehensive guides for the VideoEdit application across three main feature areas: **CLIP**, **Whisper**, and **UI**.

## 📁 Documentation Structure

```
Doc/
├── clip/                    # CLIP feature documentation
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── Device Deployment.md
│   ├── Release.md
│   ├── README.md
│   └── scripts/            # CLIP-related scripts
├── whisper/                # Whisper ASR documentation
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── Device Deployment.md
│   ├── CI/CD Guide & Release.md
│   ├── README.md
│   └── scripts/            # Whisper-related scripts
├── ui/                     # UI/UX documentation
│   ├── Architecture Design and Control Knot.md
│   ├── Full scale implementation Details.md
│   ├── Device Deployment.md
│   ├── Release.md
│   ├── README.md
│   └── scripts/            # UI-related scripts
└── cursor_rule.md          # Development rules and guidelines
```

## 🎯 Key Design Decisions

### For Content Understanding Experts
- **Primitive Output**: `{t0Ms, t1Ms, text}` spans provide exact anchors for highlights, topic segmentation, summarization, safety tagging, and retrieval-augmented QA
- **Segmentation Quality**: Phrase-level segments are stable for content understanding; word timestamps available when needed
- **Multimodal Integration**: Transcripts align with video frames/shots by time for better retrieval and summarization

### For Video Experts
- **Deterministic Pipeline**: Fixed preprocessing, same model assets, deterministic sampling for reproducible results
- **Format Support**: WAV/PCM16 files, MP4/AAC containers with hardware-accelerated decoding
- **Quality Control**: SHA-256 hash comparison for verification, sample-accurate timestamps

### For Recommendation System Experts
- **Indexing Contract**: One immutable transcript JSON per (asset, variant) with SHA verification
- **Online Latency**: Text retrieval over transcripts with time-coded jumps back to media
- **Feature Stability**: Fixed resample/downmix and pinned decode params ensure deterministic transcripts

## 🚀 Quick Start

1. **CLIP Features**: See [CLIP README](clip/README.md) for video understanding capabilities
2. **Whisper ASR**: See [Whisper README](whisper/README.md) for speech-to-text functionality  
3. **UI Components**: See [UI README](ui/README.md) for user interface documentation

## 📋 Development Guidelines

- **Architecture**: Each feature follows the "Architecture Design and Control Knot" pattern
- **Implementation**: Detailed technical specifications in "Full scale implementation Details"
- **Deployment**: Device-specific deployment guides for Xiaomi Pad and iPad
- **Release**: Platform-specific release procedures for iOS, Android, and macOS Web

## 🔧 Control Knots

Key configuration points that control system behavior:

- **Seedless Pipeline**: Deterministic sampling for reproducible results
- **Fixed Preprocessing**: No random crop, center-crop only
- **Model Assets**: Fixed hypothesis f_θ for consistency
- **Resource Monitoring**: Real-time CPU, memory, battery, temperature tracking

## 📊 Verification

Each feature includes verification scripts and hash comparison tools to ensure:
- Deterministic outputs
- Sample-accurate timing
- Reproducible results
- Performance benchmarks

---

*For detailed implementation guides, see the specific feature documentation in each subfolder.*