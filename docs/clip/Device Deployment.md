# CLIP Device Deployment

## Xiaomi Pad Ultra Deployment

### Device Specifications
- **Model**: Xiaomi Pad Ultra (25032RP42C)
- **Android Version**: 15
- **Architecture**: arm64-v8a
- **RAM**: 11.8GB
- **Storage**: 479GB (445GB available)
- **GPU**: Adreno 650

### Deployment Steps

#### 1. Model Deployment
```bash
# Deploy CLIP model
./deploy_clip_model.sh

# Verify model deployment
adb shell "ls -la /sdcard/MiraClip/models/"
# Expected: clip-vit-base-patch32.tflite
```

#### 2. App Installation
```bash
# Install debug variant
./gradlew :app:installDebug

# Verify installation
adb shell "pm list packages | grep com.mira"
```

#### 3. Permission Setup
```bash
# Grant storage permissions
adb shell "pm grant com.mira.videoeditor.debug.test android.permission.READ_EXTERNAL_STORAGE"
adb shell "pm grant com.mira.videoeditor.debug.test android.permission.WRITE_EXTERNAL_STORAGE"
```

#### 4. Testing and Validation
```bash
# Test CLIP pipeline
./test_clip_pipeline.sh

# Test embedding generation
./test_embedding_generation.sh

# End-to-end workflow test
./work_through_clip_xiaomi.sh
```

### Performance Optimization

#### Xiaomi Pad Specific Configuration
```kotlin
object XiaomiPadCLIPConfig {
    const val MODEL_FILE = "/sdcard/MiraClip/models/clip-vit-base-patch32.tflite"
    const val OPTIMAL_BATCH_SIZE = 16
    const val MEMORY_LIMIT = "2GB"
    
    fun getOptimalParams(): CLIPParams {
        return CLIPParams(
            modelPath = MODEL_FILE,
            batchSize = OPTIMAL_BATCH_SIZE,
            samplingRate = 1.0f,
            preprocessingSize = Pair(224, 224),
            quantization = "fp16"
        )
    }
}
```

#### Resource Monitoring
```bash
# Monitor GPU usage
adb shell "cat /sys/class/kgsl/kgsl-3d0/gpubusy"

# Check memory usage
adb shell "cat /proc/meminfo | grep -E 'MemTotal|MemFree|MemAvailable'"

# Monitor thermal status
adb shell "cat /sys/class/thermal/thermal_zone*/temp"
```

## iPad Deployment

### Device Specifications
- **Model**: iPad Pro (M1/M2)
- **iOS Version**: 17+
- **Architecture**: arm64 (Apple Silicon)
- **RAM**: 8GB+
- **Storage**: 128GB+
- **GPU**: Apple GPU

### Deployment Steps

#### 1. Core ML Integration
```swift
import CoreML
import Vision

class CLIPModel {
    private var model: VNCoreMLModel?
    
    func loadModel() throws {
        guard let modelURL = Bundle.main.url(forResource: "clip-vit-base-patch32", withExtension: "mlmodelc") else {
            throw CLIPError.modelNotFound
        }
        
        model = try VNCoreMLModel(for: MLModel(contentsOf: modelURL))
    }
}
```

#### 2. Frame Processing
```swift
func processVideo(url: URL) async throws -> [Float] {
    let asset = AVAsset(url: url)
    let generator = AVAssetImageGenerator(asset: asset)
    
    let duration = try await asset.load(.duration)
    let frameCount = Int(duration.seconds * samplingRate)
    
    var embeddings: [Float] = []
    
    for i in 0..<frameCount {
        let time = CMTime(seconds: Double(i) / samplingRate, preferredTimescale: 600)
        let image = try await generator.image(at: time)
        
        let embedding = try await generateEmbedding(from: image)
        embeddings.append(contentsOf: embedding)
    }
    
    return embeddings
}
```

#### 3. Performance Configuration
```swift
// iPad-optimized settings
let config = CLIPConfiguration(
    batchSize: 32,
    samplingRate: 1.0,
    preprocessingSize: CGSize(width: 224, height: 224),
    quantization: .fp16,
    computeUnits: .cpuAndGPU
)
```

## macOS Web Version Deployment

### System Requirements
- **macOS**: 12.0+
- **Architecture**: Intel x64 or Apple Silicon arm64
- **RAM**: 8GB+
- **Storage**: 1GB+ for models

### Deployment Steps

#### 1. WebAssembly Build
```bash
# Build CLIP for WebAssembly
cd clip-web
make wasm

# Generate WebAssembly module
emcc -O3 -s WASM=1 -s EXPORTED_FUNCTIONS="['_clip_init', '_clip_embed']" \
     clip.cpp -o clip.wasm
```

#### 2. JavaScript Integration
```javascript
// Load WebAssembly module
const clipModule = await import('./clip.wasm');

// Initialize CLIP model
const model = await clipModule.loadModel('clip-vit-base-patch32.bin');

// Process video frames
const embeddings = await clipModule.processVideo(videoElement, {
    samplingRate: 1.0,
    batchSize: 8
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
./test_clip_cross_platform.sh

# Platform-specific tests
./test_android_clip.sh
./test_ios_clip.sh
./test_web_clip.sh
```

#### Validation Matrix
| Platform | Model | Batch Size | Performance | Status |
|----------|-------|------------|-------------|---------|
| Android (Xiaomi Pad) | clip-vit-base-patch32.tflite | 16 | 0.1s/frame | ✅ Verified |
| iOS (iPad) | clip-vit-base-patch32.mlmodelc | 32 | 0.05s/frame | 🔄 Testing |
| Web (macOS) | clip-vit-base-patch32.wasm | 8 | 0.2s/frame | 🔄 Testing |

### Troubleshooting

#### Common Issues
1. **Model Loading Failures**
   - Verify model file integrity
   - Check storage permissions
   - Ensure sufficient memory

2. **Performance Issues**
   - Monitor GPU utilization
   - Check thermal throttling
   - Adjust batch sizes

3. **Memory Problems**
   - Reduce batch size
   - Enable memory optimization
   - Check available RAM

#### Debug Commands
```bash
# Check model deployment
adb shell "ls -la /sdcard/MiraClip/models/"

# Monitor processing logs
adb logcat | grep -i "CLIP\|Embed"

# Verify embedding files
adb shell "find /sdcard/MiraClip/embeddings -name '*.json' -mtime -1"
```

### Deployment Scripts

- **Xiaomi Pad**: `deploy_clip_xiaomi.sh`
- **CLIP Model**: `deploy_clip_model.sh`
- **Cross-platform Testing**: `test_clip_cross_platform.sh`
- **Device Validation**: `work_through_clip_xiaomi.sh`