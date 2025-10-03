# Mira Video Editor - Android Application

## 🎬 Overview
Mira Video Editor is an Android application that provides AI-powered automatic video cutting and editing capabilities using Media3 Transformer. The application analyzes video content, detects motion patterns, and automatically creates engaging short-form videos.

> **📚 Documentation**: For comprehensive documentation, see [📖 Documentation Index](docs/INDEX.md)
> 
> **🚀 Quick Start**: See [Architecture Design](docs/architecture/) for setup and implementation
> 
> **🧪 Testing**: Use [Module Scripts](scripts/modules/) for comprehensive testing
> 
> **📋 Release**: See [Release Documentation](docs/release/) for deployment guides

## ✨ Key Features

### 🧠 AI-Powered Video Analysis
- **Motion Detection**: Analyzes video segments for motion intensity
- **Shot Detection**: Automatically detects shot boundaries using histogram analysis
- **Smart Selection**: Selects the most engaging segments based on motion scores
- **Content Understanding**: Advanced video content analysis

### 🎥 Media3 Integration
- **Hardware Acceleration**: Leverages Media3 Transformer for efficient video processing
- **Multiple Formats**: Supports various video formats and codecs
- **Real-time Processing**: Optimized for mobile device performance
- **Export Quality**: High-quality video export with customizable settings

### 📱 Enhanced User Experience
- **Intuitive Interface**: Clean, modern Material Design UI
- **Real-time Progress**: Live progress updates with descriptive status messages
- **Export Management**: Organized file management with timestamped exports
- **Gallery Integration**: Optional Photos gallery integration

### 🔧 Advanced Logging & Monitoring
- **Comprehensive Logging**: Structured logging system with 10 specialized categories
- **Performance Monitoring**: Automatic timing and memory usage tracking
- **Error Handling**: Enhanced error reporting with context and stack traces
- **Privacy Protection**: Secure URI logging with privacy considerations

## 🏗️ Service Architecture

### Core Service Components
- **Clip4ClipService**: Main service API for video-text retrieval
- **Clip4ClipServiceApplication**: Service application entry point
- **Database Layer**: Room database with vector storage
- **Repository Layer**: Data access and business logic
- **Use Case Layer**: Service operations and workflows
- **Security Layer**: Encryption and security management

### Technology Stack
- **Android**: Native Android development with Kotlin
- **Room**: SQLite database with vector storage
- **Hilt**: Dependency injection framework
- **PyTorch Mobile**: CLIP model inference
- **WorkManager**: Background processing
- **SQLCipher**: Database encryption
- **Coroutines**: Asynchronous programming for smooth performance
- **Firebase**: Backend services and analytics (optional)

## 📚 Documentation

### 📖 Documentation Structure
- **[📖 Documentation Index](docs/INDEX.md)** - Complete documentation overview
- **[🏗️ Architecture Design](docs/architecture/README.md)** - System architecture and design
- **[📦 Modules](docs/modules/README.md)** - Feature modules and implementations
- **[📝 DEV Changelog](docs/dev-changelog/README.md)** - Development history and changes
- **[🚀 Release](docs/release/README.md)** - Release management and deployment

### 🛠️ Development Guides
- **[Project Context Guide](docs/architecture/PROJECT_CONTEXT_GUIDANCE.md)** - Project structure and purpose
- **[Media3 Video Pipeline](docs/architecture/Project1_Media3_VideoPipeline.md)** - Core implementation guide
- **[CLIP4Clip Integration](docs/modules/CLIP4Clip_Room_Integration_Guide.md)** - CLIP4Clip database integration

### 🧪 Testing & Verification
- **[Xiaomi Pad Testing Guide](docs/modules/XIAOMI_PAD_RESOURCE_MONITORING_GUIDE.md)** - Device testing procedures
- **[Test Results](docs/modules/XIAOMI_PAD_COMPREHENSIVE_TEST_REPORT.md)** - Comprehensive test results
- **[Performance Analysis](docs/modules/MEDIA3_PROCESSING_ANALYSIS.md)** - Performance metrics

### 🚀 Deployment & Operations
- **[CI/CD Developer Guide](docs/architecture/CICD_DEVELOPER_GUIDE.md)** - Development workflow
- **[Firebase Setup Guide](docs/release/FIREBASE_SETUP_GUIDE.md)** - Infrastructure setup
- **[Distribution Release Guide](docs/release/DISTRIBUTION_RELEASE_GUIDE.md)** - Release process

## 🚀 Quick Start

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API 21+
- Gradle 7.0+
- Android device with USB debugging enabled

### Installation
1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd VideoEdit
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the VideoEdit directory

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

### First Run
1. **Launch the app** on your Android device
2. **Select a video** using the "Select Video" button
3. **Tap "Auto Cut"** to start processing
4. **Monitor progress** in the real-time log console
5. **Find your export** in Documents/Mira/exports/

## 🧪 Testing

### Automated Testing
```bash
# Run all tests
./scripts/modules/run_all_tests.sh

# Run Xiaomi Pad specific tests
./scripts/modules/xiaomi_pad_comprehensive_test.sh

# Performance benchmark
./scripts/modules/performance_benchmark.sh
```

### Manual Testing
1. **Device Testing**: Use [Xiaomi Pad Testing Guide](docs/modules/XIAOMI_PAD_TESTING_READY.md)
2. **Performance Monitoring**: Use [Resource Monitoring Guide](docs/modules/XIAOMI_PAD_RESOURCE_MONITORING_GUIDE.md)
3. **Real Video Testing**: Follow [Real Video Walkthrough](docs/modules/REAL_VIDEO_PROCESSING_ANALYSIS.md)

## 🔧 Development

### Project Structure
```
VideoEdit/
├── app/                    # Main Android application
├── docs/                   # Comprehensive documentation
├── scripts/                # Build, test, and deployment scripts
├── test/                   # Test assets and unit tests
├── releases/               # Release builds and distribution
└── README.md              # This file
```

### Key Directories
- **`app/src/main/java/com/mira/videoeditor/`** - Core application code
- **`docs/architecture/`** - System architecture and design documentation
- **`docs/modules/`** - Feature modules and implementation guides
- **`scripts/modules/`** - Testing scripts and automation
- **`scripts/release/`** - Build and release scripts

### Development Workflow
1. **Setup**: Follow [Project Context Guide](docs/architecture/PROJECT_CONTEXT_GUIDANCE.md)
2. **Implementation**: Use [Media3 Video Pipeline](docs/architecture/Project1_Media3_VideoPipeline.md)
3. **Testing**: Run comprehensive tests using scripts
4. **Deployment**: Follow [Distribution Release Guide](docs/release/DISTRIBUTION_RELEASE_GUIDE.md)

## 📊 Performance

### Optimizations
- **Hardware Acceleration**: Media3 Transformer with GPU acceleration
- **Memory Management**: Efficient memory usage with proper cleanup
- **Thermal Management**: Thermal-aware processing for device safety
- **Background Processing**: Non-blocking UI with coroutines

### Monitoring
- **Real-time Logging**: Comprehensive logging system with performance metrics
- **Resource Monitoring**: CPU, memory, and thermal monitoring
- **Progress Tracking**: Detailed progress reporting with stage information
- **Error Reporting**: Enhanced error handling with context

## 🔒 Security & Privacy

### Data Protection
- **Local Processing**: All video processing happens locally on device
- **Privacy Logging**: URI logging shows only last 50 characters
- **Secure Storage**: Files stored in app-specific directories
- **Permission Management**: Minimal required permissions

### File Management
- **Organized Exports**: Videos saved to Documents/Mira/exports/
- **Timestamped Files**: Unique filenames prevent conflicts
- **Clean Gallery**: No automatic Photos gallery saving
- **User Control**: Optional Photos integration with user consent

## 🤝 Contributing

### Development Guidelines
1. **Code Style**: Follow Kotlin coding conventions
2. **Documentation**: Update relevant documentation for changes
3. **Testing**: Add tests for new features
4. **Logging**: Use the enhanced logging system
5. **Performance**: Consider performance implications

### Documentation Standards
- Use clear, descriptive titles
- Include overview sections
- Provide code examples
- Link to related documents
- Keep content up-to-date

## 📞 Support

### Getting Help
- **Documentation**: Check [Documentation Index](docs/INDEX.md)
- **Issues**: Review verification reports for known issues
- **Testing**: Use testing guides for troubleshooting
- **Performance**: Consult analysis reports for insights

### Resources
- **Architecture**: [docs/architecture/](docs/architecture/) - System architecture and design
- **Modules**: [docs/modules/](docs/modules/) - Feature modules and implementations
- **Scripts**: [scripts/](scripts/) - Build, test, and deployment scripts
- **Tests**: [test/](test/) - Test assets and unit tests

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- **Google Media3**: For the powerful video processing framework
- **Android Team**: For the excellent development platform
- **Open Source Community**: For various libraries and tools used

---

*Last updated: $(date)*
*Version: 1.0*

**👉 For complete documentation, see [📖 Documentation Index](docs/INDEX.md)**# CI/CD Trigger
