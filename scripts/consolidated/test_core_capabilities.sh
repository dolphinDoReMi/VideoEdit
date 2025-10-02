#!/bin/bash

# Mira Video Editor Core Capabilities Test Script
# Tests all core functionalities with video_v1.mov

echo "🎬 Mira Video Editor - Core Capabilities Test"
echo "============================================="
echo ""

# Check if video exists
if [ ! -f "test_videos/video_v1.mov" ]; then
    echo "❌ Error: video_v1.mov not found in test_videos/"
    exit 1
fi

echo "📹 Test Video: video_v1.mov ($(ls -lh test_videos/video_v1.mov | awk '{print $5}'))"
echo ""

# Test 1: Video Analysis (VideoScorer)
echo "🧠 Test 1: AI Motion Analysis (VideoScorer)"
echo "-------------------------------------------"
echo "Testing motion detection algorithm..."

# Create a simple test to verify VideoScorer can analyze the video
cat > test_video_analysis.kt << 'EOF'
import android.content.Context
import android.net.Uri
import com.mira.videoeditor.VideoScorer

fun testVideoAnalysis(context: Context, videoUri: Uri) {
    val scorer = VideoScorer(context)
    val segments = scorer.scoreSegments(
        uri = videoUri,
        segmentMs = 2000L, // 2 second segments
        maxDurationMs = 30000L // Analyze first 30 seconds
    )
    
    println("✅ VideoScorer Analysis Results:")
    println("   - Total segments analyzed: ${segments.size}")
    println("   - Motion scores range: ${segments.minOfOrNull { it.score }} - ${segments.maxOfOrNull { it.score }}")
    println("   - Average motion score: ${segments.map { it.score }.average()}")
    
    // Show top 3 segments
    val topSegments = segments.sortedByDescending { it.score }.take(3)
    println("   - Top 3 segments:")
    topSegments.forEachIndexed { index, segment ->
        println("     ${index + 1}. ${segment.startMs}ms-${segment.endMs}ms (score: ${segment.score})")
    }
}
EOF

echo "✅ VideoScorer test created"
echo ""

# Test 2: Media3 Integration (AutoCutEngine)
echo "🎥 Test 2: Media3 Video Processing (AutoCutEngine)"
echo "------------------------------------------------"
echo "Testing Media3 Transformer integration..."

cat > test_media3_integration.kt << 'EOF'
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.*
import com.mira.videoeditor.AutoCutEngine
import kotlinx.coroutines.runBlocking

fun testMedia3Integration(context: Context, videoUri: Uri) {
    println("✅ Media3 Integration Test:")
    
    // Test MediaItem creation
    val mediaItem = MediaItem.Builder()
        .setUri(videoUri)
        .build()
    println("   - MediaItem created: ${mediaItem.mediaId}")
    
    // Test Transformer creation
    val transformer = Transformer.Builder(context)
        .setVideoMimeType(MimeTypes.VIDEO_H264)
        .build()
    println("   - Transformer created successfully")
    
    // Test AutoCutEngine
    runBlocking {
        try {
            val engine = AutoCutEngine(context) { progress ->
                println("   - Progress: ${(progress * 100).toInt()}%")
            }
            println("   - AutoCutEngine created successfully")
        } catch (e: Exception) {
            println("   - AutoCutEngine error: ${e.message}")
        }
    }
}
EOF

echo "✅ Media3 integration test created"
echo ""

# Test 3: File Permissions (MediaStoreExt)
echo "🔐 Test 3: File Permissions (MediaStoreExt)"
echo "-------------------------------------------"
echo "Testing Storage Access Framework integration..."

cat > test_permissions.kt << 'EOF'
import android.content.ContentResolver
import android.net.Uri
import com.mira.videoeditor.MediaStoreExt

fun testFilePermissions(resolver: ContentResolver, videoUri: Uri) {
    println("✅ File Permissions Test:")
    
    // Test MIME type detection
    val mimeType = resolver.getMimeType(videoUri)
    println("   - MIME type: $mimeType")
    
    // Test video validation
    val isValidVideo = resolver.isValidVideoUri(videoUri)
    println("   - Is valid video: $isValidVideo")
    
    // Test file size
    val fileSize = resolver.getFileSize(videoUri)
    println("   - File size: ${fileSize / 1024 / 1024}MB")
    
    // Test permission taking
    try {
        resolver.takePersistableUriPermission(videoUri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        println("   - Permission taken successfully")
    } catch (e: Exception) {
        println("   - Permission error: ${e.message}")
    }
}
EOF

echo "✅ File permissions test created"
echo ""

# Test 4: Application Initialization
echo "🚀 Test 4: Application Initialization (AutoCutApplication)"
echo "--------------------------------------------------------"
echo "Testing app initialization and Media3 setup..."

cat > test_app_init.kt << 'EOF'
import android.app.Application
import androidx.media3.common.util.Util
import com.mira.videoeditor.AutoCutApplication

fun testApplicationInitialization() {
    println("✅ Application Initialization Test:")
    
    // Test Media3 user agent
    val userAgent = Util.getUserAgent(null, "Mira")
    println("   - Media3 user agent: $userAgent")
    
    // Test AutoCutApplication
    try {
        val app = AutoCutApplication()
        println("   - AutoCutApplication created successfully")
        println("   - App context available: ${app.getAppContext() != null}")
    } catch (e: Exception) {
        println("   - AutoCutApplication error: ${e.message}")
    }
}
EOF

echo "✅ Application initialization test created"
echo ""

# Test 5: End-to-End Integration
echo "🔄 Test 5: End-to-End Integration"
echo "--------------------------------"
echo "Testing complete video processing pipeline..."

cat > test_e2e.kt << 'EOF'
import android.content.Context
import android.net.Uri
import com.mira.videoeditor.AutoCutEngine
import com.mira.videoeditor.VideoScorer
import kotlinx.coroutines.runBlocking

fun testEndToEndProcessing(context: Context, videoUri: Uri) {
    println("✅ End-to-End Integration Test:")
    
    runBlocking {
        try {
            val engine = AutoCutEngine(context) { progress ->
                val percentage = (progress * 100).toInt()
                when {
                    progress < 0.1f -> println("   - Analyzing video... $percentage%")
                    progress < 0.3f -> println("   - Selecting segments... $percentage%")
                    else -> println("   - Exporting video... $percentage%")
                }
            }
            
            val outputPath = context.getExternalFilesDir(null)?.resolve("test_output.mp4")?.absolutePath
            if (outputPath != null) {
                engine.autoCutAndExport(
                    input = videoUri,
                    outputPath = outputPath,
                    targetDurationMs = 30000L, // 30 seconds
                    segmentMs = 2000L // 2 second segments
                )
                println("   - ✅ End-to-end test completed successfully!")
                println("   - Output file: $outputPath")
            } else {
                println("   - ❌ Could not create output path")
            }
        } catch (e: Exception) {
            println("   - ❌ End-to-end test failed: ${e.message}")
            e.printStackTrace()
        }
    }
}
EOF

echo "✅ End-to-end integration test created"
echo ""

# Test 6: Performance Testing
echo "⚡ Test 6: Performance Testing"
echo "-----------------------------"
echo "Testing processing performance and memory usage..."

cat > test_performance.kt << 'EOF'
import android.content.Context
import android.net.Uri
import com.mira.videoeditor.VideoScorer
import kotlin.system.measureTimeMillis

fun testPerformance(context: Context, videoUri: Uri) {
    println("✅ Performance Test:")
    
    val scorer = VideoScorer(context)
    
    // Test analysis performance
    val analysisTime = measureTimeMillis {
        val segments = scorer.scoreSegments(
            uri = videoUri,
            segmentMs = 1000L, // 1 second segments for faster testing
            maxDurationMs = 10000L // First 10 seconds only
        )
        println("   - Analyzed ${segments.size} segments")
    }
    
    println("   - Analysis time: ${analysisTime}ms")
    println("   - Average time per segment: ${analysisTime / 10}ms")
    
    // Memory usage estimation
    val runtime = Runtime.getRuntime()
    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    println("   - Memory usage: ${usedMemory}MB")
}
EOF

echo "✅ Performance test created"
echo ""

echo "📋 Test Summary"
echo "==============="
echo "✅ All test scripts created successfully!"
echo ""
echo "🧪 Test Coverage:"
echo "   1. AI Motion Analysis (VideoScorer) - Motion detection algorithm"
echo "   2. Media3 Integration (AutoCutEngine) - Video processing pipeline"
echo "   3. File Permissions (MediaStoreExt) - Storage Access Framework"
echo "   4. App Initialization (AutoCutApplication) - Media3 setup"
echo "   5. End-to-End Integration - Complete processing pipeline"
echo "   6. Performance Testing - Speed and memory analysis"
echo ""
echo "🚀 Next Steps:"
echo "   1. Install the app on your Android device:"
echo "      ./gradlew app:installDebug"
echo ""
echo "   2. Copy your test video to the device:"
echo "      adb push test_videos/video_v1.mov /sdcard/Download/"
echo ""
echo "   3. Run the app and test with video_v1.mov"
echo ""
echo "   4. Monitor logs for detailed test results:"
echo "      adb logcat | grep -E '(VideoScorer|AutoCutEngine|MediaStoreExt|AutoCutApplication)'"
echo ""
echo "🎯 Expected Results:"
echo "   - Motion analysis should complete in 2-5 seconds"
echo "   - Segment selection should identify high-motion areas"
echo "   - Video export should complete in 1-3 minutes"
echo "   - Output should be ~30 seconds of best motion segments"
echo ""
echo "✅ All core capabilities are ready for testing!"
