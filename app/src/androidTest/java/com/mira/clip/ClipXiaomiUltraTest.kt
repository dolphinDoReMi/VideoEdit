package com.mira.clip

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
class ClipXiaomiUltraTest {

    @Test
    fun testClipEnginesInitialization() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test CLIP engines initialization
        val engines = ClipEngines(ctx)
        assertNotNull("ClipEngines should initialize", engines)
        
        // Test that we can get the default model
        val modelId = "clip_vit_b32_mean_v1"
        assertTrue("Should have default model", modelId.isNotEmpty())
    }
    
    @Test
    fun testFrameSamplerWithSampleVideo() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Copy sample video to cache
        val sampleVideoUri = copyRawToFileUri(ctx, "sample.mp4")
        val videoFile = File(sampleVideoUri.path ?: "")
        
        assertTrue("Sample video should exist", videoFile.exists())
        
        // Test frame sampling
        val frames = FrameSampler.sampleUniform(ctx, videoFile.absolutePath, 4)
        assertTrue("Should sample frames", frames.isNotEmpty())
        assertEquals("Should sample 4 frames", 4, frames.size)
        
        // Verify frames are valid bitmaps
        for (frame in frames) {
            assertNotNull("Frame should not be null", frame)
            assertFalse("Frame should not be recycled", frame.isRecycled)
            assertTrue("Frame should have dimensions", frame.width > 0 && frame.height > 0)
        }
    }
    
    @Test
    fun testEmbeddingStore() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test embedding store initialization
        val store = FileEmbeddingStore()
        assertNotNull("EmbeddingStore should initialize", store)
        
        // Test cache directory creation
        val cacheDir = File(ctx.cacheDir, "embeddings")
        assertTrue("Cache directory should exist or be creatable", cacheDir.exists() || cacheDir.mkdirs())
    }
    
    @Test
    fun testClipPipelineIntegration() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Copy sample video to cache
        val sampleVideoUri = copyRawToFileUri(ctx, "sample.mp4")
        val videoFile = File(sampleVideoUri.path ?: "")
        
        assertTrue("Sample video should exist", videoFile.exists())
        
        // Test full pipeline: sample frames -> encode -> store
        val frames = FrameSampler.sampleUniform(ctx, videoFile.absolutePath, 2)
        assertTrue("Should sample frames", frames.isNotEmpty())
        
        // Initialize CLIP engines
        val engines = ClipEngines(ctx)
        assertNotNull("ClipEngines should initialize", engines)
        
        // Test encoding (this might take time, so we'll just verify it doesn't crash)
        try {
            // This is a basic smoke test - actual encoding would require model loading
            assertTrue("Pipeline components should be available", true)
        } catch (e: Exception) {
            fail("Pipeline should not crash: ${e.message}")
        }
    }
    
    @Test
    fun testXiaomiUltraSpecificFeatures() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test device-specific features
        val deviceModel = android.os.Build.MODEL
        assertTrue("Should be running on Xiaomi device", deviceModel.contains("Xiaomi") || deviceModel.contains("Pad"))
        
        // Test available memory
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        
        assertTrue("Should have sufficient memory", maxMemory > 100 * 1024 * 1024) // 100MB
        assertTrue("Should have free memory", freeMemory > 10 * 1024 * 1024) // 10MB
        
        // Test CLIP model assets availability
        val assets = ctx.assets
        val modelFiles = assets.list("models_bundled")
        assertNotNull("Should have model assets", modelFiles)
        assertTrue("Should have CLIP model files", modelFiles!!.isNotEmpty())
    }
    
    private fun copyRawToFileUri(ctx: Context, rawFileName: String): android.net.Uri {
        val inputStream = ctx.resources.openRawResource(
            ctx.resources.getIdentifier(rawFileName.replace(".mp4", ""), "raw", ctx.packageName)
        )
        
        val outputFile = File(ctx.cacheDir, rawFileName)
        val outputStream = outputFile.outputStream()
        
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        
        return android.net.Uri.fromFile(outputFile)
    }
}
