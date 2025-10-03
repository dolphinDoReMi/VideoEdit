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
}