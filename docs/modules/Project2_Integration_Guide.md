# Project 2: Integration Guide

## Overview
This guide explains how to integrate Project 2 (Whisper.cpp) with the existing Project 1 (Media3 Video Pipeline) within the same app, creating a content-aware video editing system.

## Integration Strategy

### 1. Modular Integration Within App
Project 2 is designed as a **modular extension** within the existing app structure, not a separate project. This allows:
- Gradual adoption and testing
- Fallback to Project 1 capabilities
- Independent development and deployment
- A/B testing between approaches

### 2. Backward Compatibility
All existing Project 1 functionality remains unchanged. Project 2 adds new capabilities without breaking existing features.

## App Structure Integration

### Current App Structure
```
app/src/main/java/com/mira/videoeditor/
├── MainActivity.kt
├── AutoCutEngine.kt
├── VideoScorer.kt
├── ShotDetector.kt
├── MediaStoreExt.kt
└── VideoScorer.kt
```

### Enhanced App Structure
```
app/src/main/java/com/mira/videoeditor/
├── MainActivity.kt                    # Enhanced with Project 2
├── AutoCutEngine.kt                  # Enhanced with whisper
├── VideoScorer.kt                    # Enhanced with semantic scoring
├── ShotDetector.kt                   # Unchanged
├── MediaStoreExt.kt                  # Unchanged
├── VideoScorer.kt                    # Unchanged
├── whisper/                          # NEW: Project 2 Core Module
│   ├── WhisperEngine.kt
│   ├── SemanticAnalyzer.kt
│   ├── ContentScorer.kt
│   ├── EnhancedPipeline.kt
│   └── ThermalManager.kt
├── enhanced/                          # NEW: Enhanced Components
│   ├── EnhancedAnalyzer.kt
│   ├── EnhancedAutoCutEngine.kt
│   └── HybridPipeline.kt
└── utils/                             # NEW: Shared Utilities
    ├── PerformanceMonitor.kt
    ├── TranscriptionCache.kt
    └── ResourceManager.kt
```

## Integration Points

### 1. Enhanced AutoCutEngine Integration

#### Current AutoCutEngine.kt
```kotlin
// Existing Project 1 functionality
class AutoCutEngine(
    private val ctx: Context,
    private val onProgress: (Float) -> Unit = {}
) {
    suspend fun autoCutAndExport(
        input: Uri,
        outputPath: String,
        targetDurationMs: Long = DEFAULT_TARGET_DURATION_MS,
        segmentMs: Long = DEFAULT_SEGMENT_MS,
        useShotSampling: Boolean = true,
        shotSampleMs: Long = 500L,
        minShotMs: Long = 1500L,
        shotThreshold: Float = 0.28f
    ) {
        // Existing implementation
    }
}
```

#### Enhanced AutoCutEngine.kt
```kotlin
// Enhanced with Project 2 capabilities
class AutoCutEngine(
    private val ctx: Context,
    private val onProgress: (Float) -> Unit = {}
) {
    // Existing Project 1 functionality remains unchanged
    suspend fun autoCutAndExport(
        input: Uri,
        outputPath: String,
        targetDurationMs: Long = DEFAULT_TARGET_DURATION_MS,
        segmentMs: Long = DEFAULT_SEGMENT_MS,
        useShotSampling: Boolean = true,
        shotSampleMs: Long = 500L,
        minShotMs: Long = 1500L,
        shotThreshold: Float = 0.28f,
        // NEW: Project 2 parameters
        enableWhisper: Boolean = true,
        whisperModelSize: ModelSize = ModelSize.BASE
    ) {
        Log.d(TAG, ctxLog(STAGE_INIT, mapOf(
            "input" to "$input",
            "output" to outputPath,
            "enableWhisper" to "$enableWhisper",
            "whisperModelSize" to "$whisperModelSize"
        )))
        
        try {
            // Step 1: Generate candidate intervals (existing)
            onProgress(0.05f)
            val scorer = VideoScorer(ctx)
            val candidateSegments = if (useShotSampling) {
                // Existing shot detection logic
                val detector = ShotDetector(ctx)
                val shots = detector.detectShots(
                    uri = input,
                    sampleMs = shotSampleMs,
                    minShotMs = minShotMs,
                    threshold = shotThreshold
                ) { detP ->
                    val overall = 0.05f + detP * 0.05f
                    onProgress(overall)
                }
                val intervals = shots.map { it.startMs to it.endMs }
                
                // Enhanced scoring with whisper
                if (enableWhisper) {
                    scorer.scoreIntervalsEnhanced(
                        uri = input,
                        intervals = intervals,
                        whisperModelSize = whisperModelSize
                    ) { scoreP ->
                        val overall = 0.10f + scoreP * 0.05f
                        onProgress(overall)
                    }
                } else {
                    // Fallback to existing Project 1 scoring
                    scorer.scoreIntervals(
                        uri = input,
                        intervals = intervals
                    ) { scoreP ->
                        val overall = 0.10f + scoreP * 0.05f
                        onProgress(overall)
                    }
                }
            } else {
                // Existing fixed segment logic
                scorer.scoreSegments(
                    uri = input,
                    segmentMs = segmentMs,
                    maxDurationMs = targetDurationMs
                ) { scoreP ->
                    val overall = 0.10f + scoreP * 0.05f
                    onProgress(overall)
                }
            }
            
            // Rest of existing implementation...
            
        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            throw e
        }
    }
}
```

### 2. Enhanced VideoScorer Integration

#### Current VideoScorer.kt
```kotlin
// Existing Project 1 functionality
class VideoScorer(private val ctx: Context) {
    fun scoreSegments(uri: Uri, segmentMs: Long, maxDurationMs: Long): List<Segment> {
        // Existing implementation
    }
}
```

#### Enhanced VideoScorer.kt
```kotlin
// Enhanced with Project 2 capabilities
class VideoScorer(private val ctx: Context) {
    // Existing Project 1 functionality
    fun scoreSegments(uri: Uri, segmentMs: Long, maxDurationMs: Long): List<Segment> {
        // Existing implementation unchanged
    }
    
    // NEW: Enhanced scoring with whisper
    fun scoreIntervalsEnhanced(
        uri: Uri,
        intervals: List<Pair<Long, Long>>,
        whisperModelSize: ModelSize = ModelSize.BASE,
        onProgress: (Float) -> Unit = {}
    ): List<EnhancedSegment> {
        val whisperEngine = WhisperEngine(ctx)
        val semanticAnalyzer = SemanticAnalyzer()
        val contentScorer = ContentScorer()
        
        return intervals.mapIndexed { index, (startMs, endMs) ->
            onProgress(index.toFloat() / intervals.size)
            
            // Existing motion and face analysis
            val motion = calculateMotionScore(uri, startMs, endMs)
            val faces = detectFaces(uri, startMs, endMs)
            
            // NEW: Whisper-enhanced analysis
            val transcription = whisperEngine.transcribe(uri, startMs, endMs)
            val semanticResult = semanticAnalyzer.analyzeTranscription(transcription)
            
            // Enhanced scoring
            val enhancedScore = contentScorer.calculateEnhancedScore(
                WindowFeat(
                    sMs = startMs,
                    eMs = endMs,
                    motion = motion,
                    speech = calculateSpeechScore(uri, startMs, endMs),
                    faceCt = faces,
                    transcription = transcription,
                    semanticScore = semanticResult.relevanceScore,
                    keywords = semanticResult.keywords,
                    sentiment = semanticResult.sentiment,
                    language = semanticResult.language,
                    confidence = semanticResult.confidence
                )
            )
            
            EnhancedSegment(
                startMs = startMs,
                endMs = endMs,
                score = enhancedScore,
                transcription = transcription,
                semanticResult = semanticResult
            )
        }
    }
}
```

### 3. Enhanced MainActivity Integration

#### Current MainActivity.kt
```kotlin
// Existing Project 1 functionality
class MainActivity : ComponentActivity() {
    private val autoCutEngine by lazy { AutoCutEngine(this) }
    
    private fun startVideoProcessing(uri: Uri) {
        lifecycleScope.launch {
            try {
                autoCutEngine.autoCutAndExport(
                    input = uri,
                    outputPath = getOutputPath()
                )
            } catch (e: Exception) {
                showError("Processing failed: ${e.message}")
            }
        }
    }
}
```

#### Enhanced MainActivity.kt
```kotlin
// Enhanced with Project 2 capabilities
class MainActivity : ComponentActivity() {
    private val autoCutEngine by lazy { AutoCutEngine(this) }
    
    // NEW: Project 2 configuration
    private var enableWhisper = true
    private var whisperModelSize = ModelSize.BASE
    
    private fun startVideoProcessing(uri: Uri) {
        lifecycleScope.launch {
            try {
                // Enhanced processing with whisper
                autoCutEngine.autoCutAndExport(
                    input = uri,
                    outputPath = getOutputPath(),
                    enableWhisper = enableWhisper,  // NEW: Enable Project 2
                    whisperModelSize = whisperModelSize
                )
            } catch (e: Exception) {
                Log.e(TAG, "Processing failed", e)
                showError("Processing failed: ${e.message}")
            }
        }
    }
    
    // NEW: Project 2 configuration methods
    private fun toggleWhisper(enabled: Boolean) {
        enableWhisper = enabled
        Log.d(TAG, "Whisper enabled: $enabled")
    }
    
    private fun setWhisperModelSize(size: ModelSize) {
        whisperModelSize = size
        Log.d(TAG, "Whisper model size: $size")
    }
}
```

## Implementation Steps

### Step 1: Add Project 2 Dependencies

#### app/build.gradle.kts (Enhanced)
```kotlin
android {
    // Existing configuration
    
    defaultConfig {
        // Existing configuration
        
        // NEW: Project 2 configuration
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
    
    // NEW: Native library configuration
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {
    // Existing Project 1 dependencies
    implementation("androidx.media3:media3-transformer:1.2.1")
    implementation("androidx.media3:media3-effect:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.mlkit:face-detection:16.1.7")
    
    // NEW: Project 2 dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.core:core-ktx:1.12.0")
    
    // Native whisper.cpp integration
    implementation("com.github.ggerganov:whisper.cpp:1.5.4")
}
```

### Step 2: Create Project 2 Modules

#### Create whisper/ Module
```kotlin
// app/src/main/java/com/mira/videoeditor/whisper/WhisperEngine.kt
class WhisperEngine(private val context: Context) {
    fun initialize(modelSize: ModelSize = ModelSize.BASE): Boolean
    suspend fun transcribe(audioUri: Uri, startMs: Long, endMs: Long): String
    fun cleanup()
}

// app/src/main/java/com/mira/videoeditor/whisper/SemanticAnalyzer.kt
class SemanticAnalyzer {
    fun analyzeTranscription(text: String): SemanticResult
}

// app/src/main/java/com/mira/videoeditor/whisper/ContentScorer.kt
class ContentScorer {
    fun calculateEnhancedScore(features: WindowFeat): Float
}
```

#### Create enhanced/ Module
```kotlin
// app/src/main/java/com/mira/videoeditor/enhanced/EnhancedAnalyzer.kt
class EnhancedAnalyzer(private val ctx: Context) {
    suspend fun analyze(uri: Uri, windowMs: Long = 2000L): List<WindowFeat> {
        // Enhanced analysis with whisper
    }
}

// app/src/main/java/com/mira/videoeditor/enhanced/HybridPipeline.kt
class HybridPipeline(
    private val context: Context,
    private val fallbackToProject1: Boolean = true
) {
    suspend fun processVideo(uri: Uri): ProcessingResult {
        // Hybrid processing with fallback
    }
}
```

### Step 3: Update Existing Components

#### Update AutoCutEngine.kt
```kotlin
// Add Project 2 parameters to existing method
suspend fun autoCutAndExport(
    input: Uri,
    outputPath: String,
    targetDurationMs: Long = DEFAULT_TARGET_DURATION_MS,
    segmentMs: Long = DEFAULT_SEGMENT_MS,
    useShotSampling: Boolean = true,
    shotSampleMs: Long = 500L,
    minShotMs: Long = 1500L,
    shotThreshold: Float = 0.28f,
    // NEW: Project 2 parameters
    enableWhisper: Boolean = true,
    whisperModelSize: ModelSize = ModelSize.BASE
) {
    // Enhanced implementation
}
```

#### Update VideoScorer.kt
```kotlin
// Add enhanced scoring method
fun scoreIntervalsEnhanced(
    uri: Uri,
    intervals: List<Pair<Long, Long>>,
    whisperModelSize: ModelSize = ModelSize.BASE,
    onProgress: (Float) -> Unit = {}
): List<EnhancedSegment> {
    // Enhanced implementation with whisper
}
```

## Configuration Options

### 1. Whisper Configuration
```kotlin
data class WhisperConfig(
    val enabled: Boolean = true,
    val modelSize: ModelSize = ModelSize.BASE,
    val language: String = "auto",
    val enableFallback: Boolean = true,
    val thermalThreshold: Float = 45.0f,
    val batteryThreshold: Int = 20
)
```

### 2. Processing Modes
```kotlin
enum class ProcessingMode {
    ENHANCED,    // Use whisper.cpp
    BASIC,       // Use Project 1 only
    HYBRID       // Use both, compare results
}
```

### 3. Feature Flags
```kotlin
data class FeatureFlags(
    val enableWhisper: Boolean = true,
    val enableSemanticAnalysis: Boolean = true,
    val enableSentimentAnalysis: Boolean = true,
    val enableKeywordExtraction: Boolean = true,
    val enableMultiLanguage: Boolean = false
)
```

## Migration Strategy

### Phase 1: Parallel Development
- Develop Project 2 modules alongside existing code
- Maintain backward compatibility
- Test integration points

### Phase 2: Gradual Integration
- Add Project 2 as optional feature
- A/B test with subset of users
- Monitor performance and accuracy

### Phase 3: Full Integration
- Make Project 2 default for capable devices
- Keep Project 1 as fallback
- Optimize based on usage data

### Phase 4: Advanced Features
- Add multi-language support
- Implement custom models
- Add advanced semantic features

## Testing Integration

### 1. Unit Tests
```kotlin
class EnhancedAutoCutEngineTest {
    @Test
    fun `should fallback to Project 1 when whisper fails`() {
        val engine = EnhancedAutoCutEngine(mockContext)
        val result = engine.autoCutAndExportWithFallback(testUri, testPath)
        
        assertTrue(result.features.isNotEmpty())
        assertTrue(result.mode in listOf(ProcessingMode.ENHANCED, ProcessingMode.BASIC))
    }
}
```

### 2. Integration Tests
```kotlin
class PipelineIntegrationTest {
    @Test
    fun `should process video with enhanced pipeline`() {
        val pipeline = EnhancedPipeline(testContext)
        val result = runBlocking { pipeline.processVideo(testVideoUri) }
        
        assertNotNull(result)
        assertTrue(result.features.isNotEmpty())
    }
}
```

### 3. Performance Tests
```kotlin
class PerformanceTest {
    @Test
    fun `enhanced processing should complete within time limit`() {
        val startTime = System.currentTimeMillis()
        val result = runBlocking { enhancedPipeline.processVideo(testUri) }
        val duration = System.currentTimeMillis() - startTime
        
        assertTrue(duration < MAX_PROCESSING_TIME_MS)
    }
}
```

## Deployment Considerations

### 1. App Size Impact
- Base model: +39MB
- Large model: +155MB
- Total impact: Manageable with dynamic loading

### 2. Performance Impact
- Processing time: 2-3x increase
- Memory usage: +100-200MB
- Battery impact: Significant, needs thermal management

### 3. Device Compatibility
- Minimum: Android API 24
- Recommended: 4GB+ RAM, ARM64
- Optimal: 6GB+ RAM, modern CPU

## Monitoring and Analytics

### 1. Performance Metrics
```kotlin
class PerformanceMonitor {
    fun trackProcessingTime(mode: ProcessingMode, duration: Long) {
        analytics.track("processing_time", mapOf(
            "mode" to mode.name,
            "duration_ms" to duration
        ))
    }
    
    fun trackFallbackUsage(reason: String) {
        analytics.track("fallback_used", mapOf(
            "reason" to reason
        ))
    }
}
```

### 2. Quality Metrics
```kotlin
class QualityMonitor {
    fun trackAccuracy(enhancedScore: Float, basicScore: Float) {
        val improvement = enhancedScore - basicScore
        analytics.track("score_improvement", mapOf(
            "improvement" to improvement
        ))
    }
}
```

This integration guide provides a comprehensive approach to adding whisper.cpp capabilities to the existing video editing app while maintaining reliability and performance.
