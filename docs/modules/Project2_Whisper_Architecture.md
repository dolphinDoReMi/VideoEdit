# Project 2: Technical Architecture

## Overview
This document details the technical architecture for integrating whisper.cpp into the existing Android video editing app, creating a content-aware editing system that understands speech semantics.

## Integration Architecture

### App Structure Integration
```
app/src/main/java/com/mira/videoeditor/
├── MainActivity.kt                    # Existing - Enhanced with Project 2
├── AutoCutEngine.kt                  # Existing - Enhanced with whisper
├── VideoScorer.kt                    # Existing - Enhanced with semantic scoring
├── ShotDetector.kt                   # Existing - Unchanged
├── MediaStoreExt.kt                  # Existing - Unchanged
├── VideoScorer.kt                    # Existing - Unchanged
├── whisper/                          # NEW: Project 2 Core Module
│   ├── WhisperEngine.kt              # Native whisper.cpp integration
│   ├── SemanticAnalyzer.kt           # Text analysis and scoring
│   ├── ContentScorer.kt               # Enhanced scoring algorithm
│   ├── EnhancedPipeline.kt            # Complete enhanced pipeline
│   └── ThermalManager.kt             # Thermal and battery management
├── enhanced/                          # NEW: Enhanced Components
│   ├── EnhancedAnalyzer.kt            # Enhanced video analysis
│   ├── EnhancedAutoCutEngine.kt       # Enhanced auto-cut engine
│   └── HybridPipeline.kt              # Hybrid processing pipeline
└── utils/                             # NEW: Shared Utilities
    ├── PerformanceMonitor.kt          # Performance monitoring
    ├── TranscriptionCache.kt          # Caching for performance
    └── ResourceManager.kt             # Resource management
```

## Component Architecture

### 1. WhisperEngine (Native Layer)
```kotlin
// app/src/main/java/com/mira/videoeditor/whisper/WhisperEngine.kt
class WhisperEngine(private val context: Context) {
    companion object {
        private const val TAG = "WhisperEngine"
    }
    
    // Model management
    fun initialize(modelSize: ModelSize = ModelSize.BASE): Boolean
    fun isInitialized(): Boolean
    fun cleanup()
    
    // Transcription
    suspend fun transcribe(audioUri: Uri, startMs: Long, endMs: Long): String
    suspend fun transcribe(audioData: ByteArray): String
    
    // Configuration
    fun setLanguage(language: String)
    fun setTemperature(temperature: Float)
}
```

### 2. SemanticAnalyzer (Analysis Layer)
```kotlin
// app/src/main/java/com/mira/videoeditor/whisper/SemanticAnalyzer.kt
class SemanticAnalyzer {
    fun analyzeTranscription(text: String): SemanticResult {
        return SemanticResult(
            relevanceScore = calculateRelevance(text),
            keywords = extractKeywords(text),
            sentiment = analyzeSentiment(text),
            language = detectLanguage(text)
        )
    }
}

data class SemanticResult(
    val text: String,
    val relevanceScore: Float,        // 0.0 to 1.0
    val keywords: List<String>,
    val sentiment: Float,             // -1.0 to 1.0
    val language: String,
    val confidence: Float              // 0.0 to 1.0
)
```

### 3. ContentScorer (Enhanced Scoring)
```kotlin
// app/src/main/java/com/mira/videoeditor/whisper/ContentScorer.kt
class ContentScorer {
    fun calculateEnhancedScore(features: WindowFeat): Float {
        return 0.25f * features.motion +
               0.20f * features.speech +
               0.30f * features.semanticScore +  // NEW: Content relevance
               0.15f * features.sentiment +      // NEW: Emotional tone
               0.10f * features.faceCt
    }
}
```

### 4. Enhanced Pipeline Integration
```kotlin
// app/src/main/java/com/mira/videoeditor/enhanced/EnhancedPipeline.kt
class EnhancedPipeline(
    private val context: Context,
    private val fallbackToProject1: Boolean = true
) {
    private val project1Analyzer = Analyzer(context)
    private val enhancedAnalyzer = EnhancedAnalyzer(context)
    private val contentScorer = ContentScorer()
    
    suspend fun processVideo(uri: Uri): ProcessingResult {
        return try {
            // Try enhanced processing
            processWithWhisper(uri)
        } catch (e: Exception) {
            if (fallbackToProject1) {
                Log.w(TAG, "Enhanced processing failed, using Project 1", e)
                processWithProject1(uri)
            } else {
                throw e
            }
        }
    }
}
```

## Enhanced Data Models

### WindowFeat Enhancement
```kotlin
// Enhanced WindowFeat with Project 2 capabilities
@Serializable
data class WindowFeat(
    val sMs: Long, val eMs: Long,
    val motion: Float,          // 0..1 (existing)
    val speech: Float,          // 0..1 (existing)
    val faceCt: Int = 0,        // existing
    val emb: FloatArray = FloatArray(0), // existing
    val advancedFeatures: AdvancedFeatures? = null, // existing
    val vadResults: List<VADResult> = emptyList(), // existing
    val embedding: VideoEmbedding? = null, // existing
    
    // NEW: Project 2 enhanced features
    val transcription: String? = null,        // Whisper transcription
    val semanticScore: Float = 0.0f,        // Content relevance score
    val keywords: List<String> = emptyList(), // Extracted keywords
    val sentiment: Float = 0.0f,             // Emotional tone
    val language: String = "auto",           // Detected language
    val confidence: Float = 0.0f             // Transcription confidence
)
```

## Integration Points

### 1. Enhanced AutoCutEngine
```kotlin
// app/src/main/java/com/mira/videoeditor/AutoCutEngine.kt (Enhanced)
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
                val shots = detector.detectShots(/* existing parameters */)
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
                    scorer.scoreIntervals(/* existing parameters */)
                }
            } else {
                // Existing fixed segment logic
            }
            
            // Rest of existing implementation...
            
        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            throw e
        }
    }
}
```

### 2. Enhanced VideoScorer
```kotlin
// app/src/main/java/com/mira/videoeditor/VideoScorer.kt (Enhanced)
class VideoScorer(private val ctx: Context) {
    // Existing Project 1 functionality
    fun scoreIntervals(
        uri: Uri,
        intervals: List<Pair<Long, Long>>,
        onProgress: (Float) -> Unit = {}
    ): List<Segment> {
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

## Memory Management

### Model Loading Strategy
```kotlin
// app/src/main/java/com/mira/videoeditor/whisper/WhisperEngine.kt
class WhisperEngine(private val context: Context) {
    private var model: WhisperModel? = null
    private var isInitialized = false
    
    fun initialize(modelSize: ModelSize = ModelSize.BASE): Boolean {
        return try {
            if (model == null) {
                model = WhisperModel.load(context.assets, modelSize)
                isInitialized = true
                Log.d(TAG, "Whisper model loaded: $modelSize")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            false
        }
    }
    
    fun cleanup() {
        model?.close()
        model = null
        isInitialized = false
    }
}
```

## Performance Optimization

### 1. Thermal Management
```kotlin
// app/src/main/java/com/mira/videoeditor/whisper/ThermalManager.kt
class ThermalManager {
    fun shouldThrottleProcessing(): Boolean {
        val temperature = getDeviceTemperature()
        val batteryLevel = getBatteryLevel()
        
        return temperature > THERMAL_THRESHOLD || batteryLevel < BATTERY_THRESHOLD
    }
    
    fun getProcessingMode(): ProcessingMode {
        return when {
            shouldThrottleProcessing() -> ProcessingMode.CONSERVATIVE
            else -> ProcessingMode.AGGRESSIVE
        }
    }
}
```

### 2. Caching Strategy
```kotlin
// app/src/main/java/com/mira/videoeditor/utils/TranscriptionCache.kt
class TranscriptionCache {
    private val cache = LruCache<String, String>(MAX_CACHE_SIZE)
    
    fun getTranscription(audioHash: String): String? {
        return cache.get(audioHash)
    }
    
    fun putTranscription(audioHash: String, transcription: String) {
        cache.put(audioHash, transcription)
    }
}
```

## Error Handling and Fallback

### 1. Graceful Degradation
```kotlin
// app/src/main/java/com/mira/videoeditor/enhanced/HybridPipeline.kt
class HybridPipeline(
    private val context: Context,
    private val fallbackToProject1: Boolean = true
) {
    suspend fun processVideo(uri: Uri): ProcessingResult {
        return try {
            // Try enhanced processing
            processWithWhisper(uri)
        } catch (e: WhisperException) {
            when (e.message) {
                WhisperException.MODEL_LOAD_FAILED -> {
                    Log.w(TAG, "Model load failed, using Project 1")
                    processWithProject1(uri)
                }
                WhisperException.INSUFFICIENT_MEMORY -> {
                    Log.w(TAG, "Insufficient memory, using Project 1")
                    processWithProject1(uri)
                }
                WhisperException.THERMAL_THROTTLE -> {
                    Log.w(TAG, "Thermal throttling, using Project 1")
                    processWithProject1(uri)
                }
                else -> throw e
            }
        }
    }
}
```

## Build Configuration

### app/build.gradle.kts (Enhanced)
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

## Testing Strategy

### 1. Unit Tests
```kotlin
// app/src/test/java/com/mira/videoeditor/whisper/WhisperEngineTest.kt
class WhisperEngineTest {
    @Test
    fun `should initialize with base model`() {
        val engine = WhisperEngine(mockContext)
        val success = engine.initialize(ModelSize.BASE)
        assertTrue(success)
    }
    
    @Test
    fun `should fallback to Project 1 when whisper fails`() {
        val pipeline = HybridPipeline(mockContext)
        val result = runBlocking { pipeline.processVideo(testUri) }
        assertTrue(result.mode in listOf(ProcessingMode.ENHANCED, ProcessingMode.BASIC))
    }
}
```

### 2. Integration Tests
```kotlin
// app/src/androidTest/java/com/mira/videoeditor/EnhancedPipelineTest.kt
class EnhancedPipelineTest {
    @Test
    fun `should process video with enhanced pipeline`() {
        val pipeline = EnhancedPipeline(testContext)
        val result = runBlocking { pipeline.processVideo(testVideoUri) }
        
        assertNotNull(result)
        assertTrue(result.features.isNotEmpty())
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
- Required: ARM64, sufficient RAM
- Optimal: 4GB+ RAM, modern CPU

This architecture provides a robust foundation for integrating whisper.cpp into the existing video editing app while maintaining performance, privacy, and reliability.
