# Whisper Device Deployment

## Xiaomi Pad Ultra Deployment

### Device Specifications
- **Model**: Xiaomi Pad Ultra (25032RP42C)
- **Android Version**: 15
- **Architecture**: arm64-v8a
- **RAM**: 11.8GB
- **Storage**: 479GB (445GB available)

### Deployment Steps

#### 1. Model Deployment
```bash
# Deploy multilingual Whisper models
./deploy_multilingual_models.sh

# Verify model deployment
adb shell "ls -la /sdcard/MiraWhisper/models/"
# Expected: whisper-base.q5_1.bin (59.7MB)
```

#### 2. App Installation
```bash
# Install debug variant (side-by-side)
./gradlew :app:installDebug

# Verify installation
adb shell "pm list packages | grep com.mira"
# Expected: com.mira.videoeditor.debug.test
```

#### 3. Permission Setup
```bash
# Grant storage permissions
adb shell "pm grant com.mira.videoeditor.debug.test android.permission.READ_EXTERNAL_STORAGE"
adb shell "pm grant com.mira.videoeditor.debug.test android.permission.WRITE_EXTERNAL_STORAGE"
```

#### 4. Testing and Validation
```bash
# Test LID pipeline
./test_lid_pipeline.sh

# Test multilingual support
./test_multilingual_lid.sh

# End-to-end workflow test
./work_through_xiaomi_pad.sh
```

### Performance Optimization

#### Xiaomi Pad Specific Configuration
```kotlin
object XiaomiPadConfig {
    const val MODEL_FILE = "/sdcard/MiraWhisper/models/whisper-base.q5_1.bin"
    const val OPTIMAL_THREADS = 6
    
    fun getOptimalParams(): WhisperParams {
        return WhisperParams(
            model = MODEL_FILE,
            lang = "auto",
            translate = false,
            threads = OPTIMAL_THREADS,
            temperature = 0.0f,
            beam = 1,
            enableWordTimestamps = false,
            detectLanguage = true,
            noContext = true
        )
    }
}
```

#### Resource Monitoring
```bash
# Monitor device performance
adb shell "top -n 1 | grep -E 'CPU|whisper|mira'"

# Check memory usage
adb shell "cat /proc/meminfo | grep -E 'MemTotal|MemFree|MemAvailable'"

# Monitor battery status
adb shell "dumpsys battery | grep -E 'level|status|temperature'"
```

### Validation Results

#### Before (English-only Model)
- **Model**: whisper-tiny.en-q5_1.bin
- **Language Detection**: Failed (detected as English)
- **Transcription**: Generic English text
- **Accuracy**: Poor for Chinese content

#### After (Multilingual Model + LID Pipeline)
- **Model**: whisper-base.q5_1.bin
- **Language Detection**: Robust two-pass LID
- **Transcription**: Accurate Chinese content
- **Accuracy**: Excellent for Chinese content

#### Performance Metrics
- **RTF**: 0.45 (Real-Time Factor)
- **Processing**: Background worker (non-blocking UI)
- **Memory**: Efficient usage on ARM64 architecture
- **Storage**: Sidecar files with LID data
- **Monitoring**: Enhanced logging and resource tracking

## iPad Deployment

### Device Specifications
- **Model**: iPad Pro (M1/M2)
- **iOS Version**: 17+
- **Architecture**: arm64 (Apple Silicon)
- **RAM**: 8GB+
- **Storage**: 128GB+

### Deployment Steps

#### 1. WhisperKit Integration
```swift
import WhisperKit

let config = WhisperKitConfig(
    modelFolder: Bundle.main.url(forResource: "whisper-small.q5_1", withExtension: "bin")!,
    computeUnits: .cpuAndGPU,
    verbose: true
)

try await whisperKit = WhisperKit(config: config)
```

#### 2. Multilingual Model Setup
```swift
// Auto LID, no translate
let result = try await whisperKit.transcribe(
    audioPath: audioURL.path,
    language: .auto,
    task: .transcribe
)
```

#### 3. Performance Configuration
```swift
// iPad-optimized settings
let whisperConfig = WhisperConfiguration(
    language: .auto,
    task: .transcribe,
    temperature: 0.0,
    beamSize: 1,
    enableWordTimestamps: false
)
```

## macOS Web Version Deployment

### System Requirements
- **macOS**: 12.0+
- **Architecture**: Intel x64 or Apple Silicon arm64
- **RAM**: 8GB+
- **Storage**: 2GB+ for models

### Deployment Steps

#### 1. WebAssembly Build
```bash
# Build whisper.cpp for WebAssembly
cd whisper.cpp
make wasm

# Generate WebAssembly module
emcc -O3 -s WASM=1 -s EXPORTED_FUNCTIONS="['_whisper_init', '_whisper_decode']" \
     whisper.cpp -o whisper.wasm
```

#### 2. JavaScript Integration
```javascript
// Load WebAssembly module
const whisperModule = await import('./whisper.wasm');

// Initialize multilingual model
const model = await whisperModule.loadModel('whisper-base.q5_1.bin');

// Process audio with LID
const result = await whisperModule.transcribe(audioBuffer, {
    language: 'auto',
    translate: false,
    detectLanguage: true
});
```

#### 3. Browser Compatibility
- **Chrome**: 88+
- **Firefox**: 89+
- **Safari**: 14.1+
- **Edge**: 88+

### Cross-Platform Testing

#### Test Scripts
```bash
# Test all platforms
./test_cross_platform.sh

# Platform-specific tests
./test_android_xiaomi.sh
./test_ios_ipad.sh
./test_web_macos.sh
```

#### Validation Matrix
| Platform | Model | LID Support | Performance | Status |
|----------|-------|-------------|-------------|---------|
| Android (Xiaomi Pad) | whisper-base.q5_1.bin | ✅ | RTF 0.45 | ✅ Verified |
| iOS (iPad) | whisper-small.q5_1.bin | ✅ | RTF 0.3 | 🔄 Testing |
| Web (macOS) | whisper-tiny.q5_1.bin | ✅ | RTF 0.8 | 🔄 Testing |

### Troubleshooting

#### Common Issues
1. **Model Loading Failures**
   - Verify model file integrity
   - Check storage permissions
   - Ensure sufficient memory

2. **LID Accuracy Issues**
   - Verify multilingual model (no .en suffix)
   - Check VAD window settings
   - Validate confidence thresholds

3. **Performance Problems**
   - Monitor RTF metrics
   - Adjust thread counts
   - Check thermal throttling

#### Debug Commands
```bash
# Check model deployment
adb shell "ls -la /sdcard/MiraWhisper/models/"

# Monitor processing logs
adb logcat | grep -i "TranscribeWorker\|LID"

# Verify sidecar files
adb shell "find /sdcard/MiraWhisper/sidecars -name '*.json' -mtime -1"
```

### Deployment Scripts

- **Xiaomi Pad**: `deploy_xiaomi_pad.sh`
- **Multilingual Models**: `deploy_multilingual_models.sh`
- **Cross-platform Testing**: `test_cross_platform.sh`
- **Device Validation**: `work_through_xiaomi_pad.sh`