# Infrastructure - Device Deployment

## Xiaomi Pad Deployment

### Prerequisites
- Xiaomi Pad with Android 11+ and USB debugging enabled
- ADB tools installed and configured
- Sufficient storage space (minimum 2GB free)

### Deployment Steps

#### 1. Device Preparation
```bash
# Check device connection
adb devices

# Enable USB debugging
adb shell settings put global adb_enabled 1

# Verify device capabilities
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
```

#### 2. App Installation
```bash
# Build debug APK
./gradlew assembleDebug

# Install on Xiaomi Pad
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Verify installation
adb shell pm list packages | grep com.mira.com
```

#### 3. Infrastructure Service Testing
```bash
# Launch infrastructure service
adb shell am start -n com.mira.com/com.mira.whisper.WhisperUnifiedActivity

# Test hash-based duplicate detection
adb shell am broadcast -a com.mira.com.action.HASH_DUPLICATE_DETECTION \
  --es directory "/storage/emulated/0/Documents/ConvertedMedia"

# Test global cleanup
adb shell am broadcast -a com.mira.com.action.GLOBAL_CLEANUP
```

#### 4. Performance Monitoring
```bash
# Monitor infrastructure processing
adb logcat | grep -E "(GlobalFileManagement|HashDuplicateDetection|TECHNICAL)"

# Check memory usage
adb shell dumpsys meminfo com.mira.com

# Monitor CPU usage
adb shell top | grep com.mira.com
```

### Xiaomi Pad Specific Considerations

#### Hardware Optimization
- **Memory management**: Optimize for 6GB RAM configuration
- **Storage**: Efficient file management on internal storage
- **Processing**: Background processing to avoid UI blocking
- **Thermal management**: Monitor device temperature during operations

#### Performance Tuning
```kotlin
// Xiaomi Pad optimized configuration
object XiaomiPadInfrastructureConfig {
    const val CHUNK_SIZE = 8192 // Optimized for Xiaomi Pad
    const val BATCH_SIZE = 50 // Memory-optimized batch size
    const val CACHE_SIZE = 800 // Reduced cache for 6GB RAM
    const val BACKGROUND_PROCESSING = true
    const val THERMAL_MONITORING = true
}
```

## iPad Deployment

### Prerequisites
- iPad with iOS 14+ and developer mode enabled
- Xcode with iOS development tools
- Apple Developer account for device provisioning

### Deployment Steps

#### 1. iOS Build Configuration
```bash
# Configure iOS build
cd ios/App
pod install

# Build for iOS
xcodebuild -workspace App.xcworkspace \
  -scheme App \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  build
```

#### 2. iPad Installation
```bash
# Install via Xcode
# Or use TestFlight for distribution
# Or use ad-hoc provisioning for direct installation
```

#### 3. Infrastructure Service Testing
```swift
// Test infrastructure functionality on iPad
let globalFileManagement = GlobalFileManagementService()
let duplicates = globalFileManagement.findDuplicateFilesByHash(directory: documentsDir)
let cleanupResult = globalFileManagement.globalCleanup(context: context)
```

### iPad Specific Considerations

#### Hardware Optimization
- **Memory management**: Optimize for iPad memory architecture
- **Storage**: Efficient file management using Core Data
- **Performance**: Leverage Metal Performance Shaders for hash calculation
- **Background processing**: Use iOS background processing capabilities

#### Performance Tuning
```swift
// iPad optimized configuration
struct IPadInfrastructureConfig {
    static let chunkSize = 8192
    static let batchSize = 100
    static let cacheSize = 1000
    static let useMetalPerformanceShaders = true
    static let backgroundProcessing = true
}
```

## macOS Web Version Deployment

### Prerequisites
- macOS 12+ with Xcode command line tools
- Node.js 16+ and npm/yarn package manager
- Web browser with WebGL support

### Deployment Steps

#### 1. Web Build Configuration
```bash
# Install dependencies
npm install

# Build web version
npm run build

# Start development server
npm run dev
```

#### 2. Infrastructure Web Service
```javascript
// Web-based infrastructure service
class InfrastructureWebService {
    async calculateFileHash(file) {
        // Calculate SHA-256 hash using Web Crypto API
        const buffer = await file.arrayBuffer();
        const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
        return Array.from(new Uint8Array(hashBuffer))
            .map(b => b.toString(16).padStart(2, '0'))
            .join('');
    }
    
    async findDuplicateFiles(files) {
        // Find duplicate files based on hash comparison
        const hashMap = new Map();
        const duplicates = new Map();
        
        for (const file of files) {
            const hash = await this.calculateFileHash(file);
            if (hashMap.has(hash)) {
                if (!duplicates.has(hash)) {
                    duplicates.set(hash, [hashMap.get(hash)]);
                }
                duplicates.get(hash).push(file);
            } else {
                hashMap.set(hash, file);
            }
        }
        
        return duplicates;
    }
}
```

#### 3. Browser Testing
```bash
# Test in different browsers
open -a "Google Chrome" http://localhost:3000
open -a "Safari" http://localhost:3000
open -a "Firefox" http://localhost:3000
```

### macOS Web Specific Considerations

#### Browser Optimization
- **Web Crypto API**: Utilize Web Crypto API for hash calculation
- **Memory management**: Efficient memory usage in browser context
- **Storage**: Use IndexedDB for file metadata storage
- **Performance**: Optimize for different browser engines

#### Performance Tuning
```javascript
// macOS Web optimized configuration
const macOSWebInfrastructureConfig = {
    chunkSize: 8192,
    batchSize: 50,
    cacheSize: 500,
    useWebCryptoAPI: true,
    useIndexedDB: true,
    compressionLevel: 'medium'
};
```

## Testing and Validation

### Device-Specific Testing

#### Xiaomi Pad Testing
```bash
# Run comprehensive tests
./scripts/test_xiaomi_pad_infrastructure.sh

# Performance benchmarks
./scripts/benchmark_xiaomi_pad_infrastructure.sh

# Memory stress tests
./scripts/stress_test_xiaomi_pad_infrastructure.sh
```

#### iPad Testing
```bash
# Run iOS tests
./scripts/test_ipad_infrastructure.sh

# Performance benchmarks
./scripts/benchmark_ipad_infrastructure.sh

# Memory stress tests
./scripts/stress_test_ipad_infrastructure.sh
```

#### macOS Web Testing
```bash
# Run web tests
./scripts/test_macos_web_infrastructure.sh

# Performance benchmarks
./scripts/benchmark_macos_web_infrastructure.sh

# Cross-browser testing
./scripts/cross_browser_infrastructure_test.sh
```

### Performance Metrics

#### Target Performance
- **Xiaomi Pad**: <1GB memory usage, <5s processing time
- **iPad**: <2GB memory usage, <3s processing time
- **macOS Web**: <500MB memory usage, <10s processing time

#### Monitoring Scripts
- **Performance monitoring**: `scripts/monitor_infrastructure_performance.sh`
- **Memory tracking**: `scripts/track_infrastructure_memory.sh`
- **Storage monitoring**: `scripts/monitor_infrastructure_storage.sh`

## Troubleshooting

### Common Issues

#### Xiaomi Pad Issues
- **Memory pressure**: Reduce batch size and cache size
- **Storage issues**: Check available storage space
- **Performance issues**: Enable background processing

#### iPad Issues
- **Memory limits**: Optimize for iPad memory architecture
- **Performance**: Use Metal Performance Shaders
- **Background processing**: Ensure proper iOS background processing

#### macOS Web Issues
- **Web Crypto API**: Check browser Web Crypto API compatibility
- **Memory limits**: Optimize for browser memory constraints
- **Performance**: Use efficient JavaScript algorithms

### Debug Commands
```bash
# Debug infrastructure processing
adb logcat | grep -E "(GlobalFileManagement|ERROR|WARN)"

# Check device status
adb shell dumpsys meminfo com.mira.com
adb shell dumpsys cpuinfo | grep com.mira.com

# Monitor performance
adb shell top | grep com.mira.com
```

## Code Pointers

### Deployment Scripts
- **Xiaomi Pad deployment**: `scripts/deploy_xiaomi_pad_infrastructure.sh`
- **iPad deployment**: `scripts/deploy_ipad_infrastructure.sh`
- **macOS Web deployment**: `scripts/deploy_macos_web_infrastructure.sh`

### Testing Scripts
- **Device testing**: `scripts/test_device_infrastructure_deployment.sh`
- **Performance testing**: `scripts/test_infrastructure_performance.sh`
- **Integration testing**: `scripts/test_infrastructure_integration.sh`

### Configuration Files
- **Device configs**: `configs/infrastructure/device/`
- **Performance configs**: `configs/infrastructure/performance/`
- **Deployment configs**: `configs/infrastructure/deployment/`
