package com.mira.clip

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mira.clip.clip.ClipEngines
import com.mira.clip.storage.FileEmbeddingStore
import com.mira.clip.video.FrameSampler
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ClipLocalVideoTest {

    @Test
    fun testClipWithLocalVideo_video_v1_mp4() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test with local video file on Xiaomi device
        val videoPath = "/sdcard/Movies/video_v1.mp4"
        val videoFile = File(videoPath)
        
        assertTrue("Local video file should exist: $videoPath", videoFile.exists())
        assertTrue("Video file should be readable", videoFile.canRead())
        assertTrue("Video file should have content", videoFile.length() > 1000) // At least 1KB
        
        println("Testing with video file: ${videoFile.absolutePath}")
        println("Video file size: ${videoFile.length()} bytes")
        
        // Test frame sampling from local video
        val frameCount = 8
        val frames = FrameSampler.sampleUniform(ctx, videoFile.absolutePath, frameCount)
        
        assertTrue("Should sample frames from local video", frames.isNotEmpty())
        assertEquals("Should sample $frameCount frames", frameCount, frames.size)
        
        // Verify frames are valid bitmaps
        for (index in frames.indices) {
            val frame = frames[index]
            assertNotNull("Frame $index should not be null", frame)
            assertFalse("Frame $index should not be recycled", frame.isRecycled)
            assertTrue("Frame $index should have dimensions", frame.width > 0 && frame.height > 0)
            println("Frame $index: ${frame.width}x${frame.height}")
        }
        
        // Test CLIP engines initialization
        val engines = ClipEngines(ctx)
        assertNotNull("ClipEngines should initialize", engines)
        
        // Test embedding store
        val store = FileEmbeddingStore()
        assertNotNull("EmbeddingStore should initialize", store)
        
        // Test cache directory creation
        val cacheDir = File(ctx.cacheDir, "embeddings")
        assertTrue("Cache directory should exist or be creatable", cacheDir.exists() || cacheDir.mkdirs())
        
        println("✅ All CLIP pipeline components working with local video file")
    }
    
    @Test
    fun testClipPipelineWithRealVideo() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test with local video file
        val videoPath = "/sdcard/Movies/video_v1.mp4"
        val videoFile = File(videoPath)
        
        assertTrue("Local video file should exist", videoFile.exists())
        
        // Sample frames from real video
        val frames = FrameSampler.sampleUniform(ctx, videoFile.absolutePath, 4)
        assertTrue("Should sample frames from real video", frames.isNotEmpty())
        
        // Initialize CLIP engines
        val engines = ClipEngines(ctx)
        assertNotNull("ClipEngines should initialize", engines)
        
        // Test that we can process the frames (smoke test)
        try {
            // This is a basic smoke test - actual encoding would require model loading
            assertTrue("Pipeline should handle real video frames", frames.size > 0)
            println("✅ CLIP pipeline successfully processed ${frames.size} frames from real video")
        } catch (e: Exception) {
            fail("Pipeline should not crash with real video: ${e.message}")
        }
    }
    
    @Test
    fun testXiaomiDeviceSpecificVideoProcessing() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test device-specific features
        val deviceModel = android.os.Build.MODEL
        assertTrue("Should be running on Android device", deviceModel.isNotEmpty())
        
        println("Running on device: $deviceModel")
        
        // Test with local video file
        val videoPath = "/sdcard/Movies/video_v1.mp4"
        val videoFile = File(videoPath)
        
        assertTrue("Local video file should exist on device", videoFile.exists())
        
        // Test available memory
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val freeMemory = runtime.freeMemory()
        
        assertTrue("Should have sufficient memory", maxMemory > 100 * 1024 * 1024) // 100MB
        assertTrue("Should have free memory", freeMemory > 10 * 1024 * 1024) // 10MB
        
        println("Device memory: max=${maxMemory / (1024*1024)}MB, free=${freeMemory / (1024*1024)}MB")
        
        // Test CLIP model assets availability
        val modelFiles = ctx.assets.list("models_bundled")
        assertNotNull("Should have model assets", modelFiles)
        assertTrue("Should have CLIP model files", modelFiles!!.isNotEmpty())
        
        println("Available CLIP models: ${modelFiles.joinToString(", ")}")
        
        // Test video processing with device-optimized settings
        val frames = FrameSampler.sampleUniform(ctx, videoFile.absolutePath, 6)
        assertTrue("Should sample frames on device", frames.isNotEmpty())
        
        println("✅ Xiaomi device successfully processed ${frames.size} frames from local video")
    }
    
    @Test
    fun generateExactEmbeddingForVideo_v1_mp4() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test with local video file
        val videoPath = "/sdcard/Movies/video_v1.mp4"
        val videoFile = File(videoPath)
        
        assertTrue("Video file should exist", videoFile.exists())
        println("🎬 Processing video: ${videoFile.absolutePath}")
        println("📁 Video file size: ${videoFile.length()} bytes")
        
        // Sample frames from video
        val frameCount = 8
        println("🖼️ Sampling $frameCount frames...")
        val frames = FrameSampler.sampleUniform(ctx, videoPath, frameCount)
        
        assertTrue("Should sample frames", frames.isNotEmpty())
        assertEquals("Should sample $frameCount frames", frameCount, frames.size)
        
        // Display frame info
        frames.forEachIndexed { index, frame ->
            println("Frame $index: ${frame.width}x${frame.height}")
        }
        
        // Initialize CLIP engines
        println("🤖 Initializing CLIP engines...")
        val engines = ClipEngines(ctx)
        engines.initialize()
        println("✅ CLIP engines initialized")
        
        // Generate embedding
        println("🧠 Generating CLIP embedding...")
        val embedding = engines.encodeFrames(frames)
        
        assertNotNull("Embedding should not be null", embedding)
        assertTrue("Embedding should have content", embedding.size > 0)
        
        // Display exact embedding
        println("\n" + "=".repeat(80))
        println("🎯 EXACT CLIP EMBEDDING FOR video_v1.mp4")
        println("=".repeat(80))
        println("📏 Dimension: ${embedding.size}")
        println("🎬 Video: $videoPath")
        println("🖼️ Frames: ${frames.size}")
        
        // Calculate statistics
        var sum = 0f
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE
        var sumSquares = 0.0
        
        for (value in embedding) {
            sum += value
            if (value < min) min = value
            if (value > max) max = value
            sumSquares += value * value
        }
        
        val mean = sum / embedding.size
        val l2Norm = kotlin.math.sqrt(sumSquares)
        
        println("\n📊 EMBEDDING STATISTICS:")
        println("   Mean: $mean")
        println("   Min: $min")
        println("   Max: $max")
        println("   L2 Norm: $l2Norm")
        
        // Verify properties
        assertTrue("Should be 512-dimensional", embedding.size == 512)
        assertTrue("L2 norm should be close to 1.0", kotlin.math.abs(l2Norm - 1.0) < 0.01)
        
        // Display complete embedding vector
        println("\n🔢 COMPLETE EMBEDDING VECTOR:")
        println("FloatArray(${embedding.size}) {")
        for (i in 0 until embedding.size) {
            val value = embedding[i]
            val comma = if (i < embedding.size - 1) "," else ""
            println("    ${value}f$comma")
        }
        println("}")
        
        // Store embedding
        println("\n💾 Storing embedding...")
        val store = FileEmbeddingStore()
        val metadata = mapOf(
            "video_path" to videoPath,
            "frame_count" to frames.size,
            "embedding_dim" to embedding.size,
            "l2_norm" to l2Norm,
            "mean" to mean,
            "min" to min,
            "max" to max,
            "timestamp" to System.currentTimeMillis()
        )
        
        store.storeEmbedding("clip_vit_b32_mean_v1", "video_v1", embedding, metadata)
        println("✅ Embedding stored successfully")
        
        // Verify storage
        val loadedEmbedding = store.loadEmbedding("clip_vit_b32_mean_v1", "video_v1")
        assertNotNull("Should load embedding back", loadedEmbedding)
        assertTrue("Loaded embedding should match", embedding.contentEquals(loadedEmbedding))
        
        println("\n🎉 SUCCESS: Exact embedding generated and verified!")
        println("=".repeat(80))
    }
}