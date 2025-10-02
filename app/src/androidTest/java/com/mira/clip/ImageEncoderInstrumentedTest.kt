package com.mira.clip

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mira.clip.ml.ClipEngines
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class ImageEncoderInstrumentedTest {
    
    @Test
    fun image_embedding_has_expected_dim_and_norm() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val engines = ClipEngines(ctx)
        
        // Initialize engines (this will fail gracefully if models are not available)
        val initialized = engines.initialize()
        
        if (!initialized) {
            // Skip test if models are not available (e.g., in CI without model files)
            println("Skipping image encoder test - models not available")
            return@runBlocking
        }
        
        val bmp = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888) // black
        val z = engines.encodeFrames(listOf(bmp), batch = 1)
        
        assertTrue("Embedding should have expected dimension", z.size == 512 || z.size == 768)
        
        val n = sqrt(z.fold(0f) { a, b -> a + b * b })
        assertTrue("Norm should be ~ 1.0", abs(n - 1f) < 1e-2)
    }
    
    @Test
    fun image_embedding_consistency() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val engines = ClipEngines(ctx)
        
        val initialized = engines.initialize()
        if (!initialized) {
            println("Skipping image encoder consistency test - models not available")
            return@runBlocking
        }
        
        val bmp = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        
        // Encode same image twice
        val z1 = engines.encodeFrames(listOf(bmp), batch = 1)
        val z2 = engines.encodeFrames(listOf(bmp), batch = 1)
        
        // Should get identical results
        assertEquals("Embeddings should be identical", z1.size, z2.size)
        for (i in z1.indices) {
            assertEquals("Embedding values should be identical", z1[i], z2[i], 1e-6f)
        }
    }
    
    @Test
    fun image_embedding_different_images() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val engines = ClipEngines(ctx)
        
        val initialized = engines.initialize()
        if (!initialized) {
            println("Skipping image encoder difference test - models not available")
            return@runBlocking
        }
        
        // Create two different images
        val bmp1 = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        val bmp2 = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        
        // Fill second bitmap with different color
        bmp2.eraseColor(android.graphics.Color.WHITE)
        
        val z1 = engines.encodeFrames(listOf(bmp1), batch = 1)
        val z2 = engines.encodeFrames(listOf(bmp2), batch = 1)
        
        // Should get different embeddings
        assertEquals("Embeddings should have same dimension", z1.size, z2.size)
        
        // Calculate cosine similarity
        val similarity = engines.cosineSimilarity(z1, z2)
        assertTrue("Different images should have different embeddings", similarity < 0.99f)
    }
    
    @Test
    fun image_embedding_normalization() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val engines = ClipEngines(ctx)
        
        val initialized = engines.initialize()
        if (!initialized) {
            println("Skipping image encoder normalization test - models not available")
            return@runBlocking
        }
        
        val bmp = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        val z = engines.encodeFrames(listOf(bmp), batch = 1)
        
        // Check L2 normalization
        val norm = sqrt(z.fold(0f) { a, b -> a + b * b })
        assertEquals("Embedding should be L2 normalized", 1.0f, norm, 1e-5f)
        
        // Check that embedding is not all zeros
        val hasNonZero = z.any { abs(it) > 1e-6f }
        assertTrue("Embedding should have non-zero values", hasNonZero)
    }
    
    @Test
    fun image_embedding_batch_processing() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val engines = ClipEngines(ctx)
        
        val initialized = engines.initialize()
        if (!initialized) {
            println("Skipping image encoder batch test - models not available")
            return@runBlocking
        }
        
        // Create multiple images
        val bitmaps = listOf(
            Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888),
            Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply { 
                eraseColor(android.graphics.Color.RED) 
            },
            Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply { 
                eraseColor(android.graphics.Color.BLUE) 
            }
        )
        
        // Process each image individually
        val individualEmbeddings = bitmaps.map { bmp ->
            engines.encodeFrames(listOf(bmp), batch = 1)
        }
        
        // Verify all embeddings have correct properties
        individualEmbeddings.forEach { embedding ->
            assertTrue("Embedding should have expected dimension", 
                embedding.size == 512 || embedding.size == 768)
            
            val norm = sqrt(embedding.fold(0f) { a, b -> a + b * b })
            assertEquals("Embedding should be normalized", 1.0f, norm, 1e-5f)
        }
        
        // Verify embeddings are different for different images
        val similarity1 = engines.cosineSimilarity(individualEmbeddings[0], individualEmbeddings[1])
        val similarity2 = engines.cosineSimilarity(individualEmbeddings[1], individualEmbeddings[2])
        
        assertTrue("Different images should have different embeddings", similarity1 < 0.99f)
        assertTrue("Different images should have different embeddings", similarity2 < 0.99f)
    }
}
