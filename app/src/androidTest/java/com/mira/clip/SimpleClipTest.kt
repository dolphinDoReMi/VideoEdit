package com.mira.clip

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mira.clip.clip.ClipEngines
import com.mira.clip.storage.FileEmbeddingStore
import com.mira.clip.video.FrameSampler
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class SimpleClipTest {

    @Test
    fun testFrameSampler_withSampleVideo() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Copy sample video to cache
        val sampleVideoUri = copyRawToFileUri(ctx, "sample.mp4")
        val videoFile = File(sampleVideoUri.path ?: "")
        
        assertTrue("Sample video should exist", videoFile.exists())
        
        // Test frame sampling
        val frames = FrameSampler.sampleUniform(ctx, videoFile.absolutePath, 4)
        assertTrue("Should sample frames", frames.isNotEmpty())
        assertEquals("Should sample 4 frames", 4, frames.size)
        
        // Verify frames are valid
        frames.forEach { frame ->
            assertNotNull("Frame should not be null", frame)
            assertTrue("Frame should have width > 0", frame.width > 0)
            assertTrue("Frame should have height > 0", frame.height > 0)
        }
    }

    @Test
    fun testClipEngines_initialization() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test CLIP engines initialization
        val clipEngines = ClipEngines(ctx)
        
        // This should not throw an exception
        clipEngines.initialize()
        
        // Test embedding dimension
        val dim = clipEngines.getEmbeddingDimension()
        assertEquals("Should be 512 dimensions", 512, dim)
    }

    @Test
    fun testEmbeddingStore_basicOperations() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        val embeddingStore = FileEmbeddingStore()
        val testId = "test_embedding_${System.currentTimeMillis()}"
        val testModel = "test_model"
        
        // Create a simple test vector
        val testVector = FloatArray(512) { 0.1f }
        
        // Test storing
        val metadata = mapOf(
            "test" to true,
            "dimension" to 512,
            "timestamp" to System.currentTimeMillis()
        )
        
        embeddingStore.storeEmbedding(testModel, testId, testVector, metadata)
        
        // Test loading
        assertTrue("Should have embedding", embeddingStore.hasEmbedding(testModel, testId))
        
        val loaded = embeddingStore.loadEmbedding(testModel, testId)
        assertNotNull("Should load embedding", loaded)
        assertEquals("Should have correct dimension", 512, loaded!!.size)
        
        // Test metadata
        val loadedMetadata = embeddingStore.loadMetadata(testModel, testId)
        assertNotNull("Should load metadata", loadedMetadata)
        assertEquals("Should have test flag", true, loadedMetadata!!["test"])
    }

    @Test
    fun testClipEngines_withSyntheticFrames() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Create synthetic frames
        val frames = listOf(
            Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply { 
                eraseColor(Color.RED) 
            },
            Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply { 
                eraseColor(Color.GREEN) 
            },
            Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply { 
                eraseColor(Color.BLUE) 
            }
        )
        
        val clipEngines = ClipEngines(ctx)
        clipEngines.initialize()
        
        // Test encoding frames
        val embedding = clipEngines.encodeFrames(frames)
        
        assertEquals("Should have correct dimension", 512, embedding.size)
        
        // Test L2 normalization
        val norm = l2(embedding)
        assertTrue("Should be L2 normalized: norm=$norm", abs(norm - 1f) < 1e-2)
        
        // Test no NaN or Inf values
        assertFalse("Should not contain NaN", embedding.any { it.isNaN() })
        assertFalse("Should not contain Inf", embedding.any { 
            it == Float.POSITIVE_INFINITY || it == Float.NEGATIVE_INFINITY 
        })
    }

    private fun copyRawToFileUri(ctx: Context, name: String): android.net.Uri {
        val cache = File(ctx.cacheDir, "test_videos")
        if (!cache.exists()) cache.mkdirs()
        val out = File(cache, name)
        
        ctx.resources.openRawResource(
            ctx.resources.getIdentifier(name.substringBefore("."), "raw", ctx.packageName)
        ).use { input ->
            out.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        return android.net.Uri.fromFile(out)
    }

    private fun l2(v: FloatArray): Float {
        var s = 0f
        for (x in v) s += x * x
        return sqrt(s)
    }
}
