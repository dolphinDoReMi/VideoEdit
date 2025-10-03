package com.mira.clip

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mira.clip.ml.ClipEngines
import com.mira.clip.ml.ClipBPETokenizer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class TextEncoderInstrumentedTest {
    
    @Test
    fun tokenizer_length_eot_and_ids_valid() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val tok = ClipBPETokenizer(ctx)
        val ids = tok.encode("a photo of a cat")
        
        assertTrue("Token count should be <= 77", ids.size <= 77)
        val eot = ids.last()
        assertTrue("EOT should be non-negative", eot >= 0)
        
        // Verify EOT token is present
        assertTrue("Should contain EOT token", ids.contains(49407L))
    }
    
    @Test
    fun text_embedding_dim_and_norm() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val engines = ClipEngines(ctx)
        
        val initialized = engines.initialize()
        if (!initialized) {
            println("Skipping text encoder test - models not available")
            return@runBlocking
        }
        
        val z = engines.encodeText("a photo of a cat")
        assertTrue("Embedding should have expected dimension", z.size == 512 || z.size == 768)
        
        val n = sqrt(z.fold(0f) { a, b -> a + b * b })
        assertTrue("Norm should be ~ 1.0", abs(n - 1f) < 1e-2)
    }
    
    @Test
    fun tokenizer_consistency() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val tok = ClipBPETokenizer(ctx)
        
        val text = "a photo of a cat"
        val ids1 = tok.encode(text)
        val ids2 = tok.encode(text)
        
        // Should get identical tokenization
        assertEquals("Tokenization should be consistent", ids1.size, ids2.size)
        for (i in ids1.indices) {
            assertEquals("Token IDs should be identical", ids1[i], ids2[i])
        }
    }
    
    @Test
    fun tokenizer_different_texts() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val tok = ClipBPETokenizer(ctx)
        
        val text1 = "a photo of a cat"
        val text2 = "a photo of a dog"
        val text3 = "a video of a person"
        
        val ids1 = tok.encode(text1)
        val ids2 = tok.encode(text2)
        val ids3 = tok.encode(text3)
        
        // Different texts should produce different tokenizations
        assertFalse("Different texts should have different tokenizations", 
            ids1.contentEquals(ids2))
        assertFalse("Different texts should have different tokenizations", 
            ids2.contentEquals(ids3))
        
        // All should have same length (padded/truncated to max length)
        assertEquals("All tokenizations should have same length", ids1.size, ids2.size)
        assertEquals("All tokenizations should have same length", ids2.size, ids3.size)
    }
    
    @Test
    fun text_embedding_consistency() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val engines = ClipEngines(ctx)
        
        val initialized = engines.initialize()
        if (!initialized) {
            println("Skipping text embedding consistency test - models not available")
            return@runBlocking
        }
        
        val text = "a photo of a cat"
        val z1 = engines.encodeText(text)
        val z2 = engines.encodeText(text)
        
        // Should get identical embeddings
        assertEquals("Embeddings should be identical", z1.size, z2.size)
        for (i in z1.indices) {
            assertEquals("Embedding values should be identical", z1[i], z2[i], 1e-6f)
        }
    }
    
    @Test
    fun text_embedding_different_texts() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val engines = ClipEngines(ctx)
        
        val initialized = engines.initialize()
        if (!initialized) {
            println("Skipping text embedding difference test - models not available")
            return@runBlocking
        }
        
        val text1 = "a photo of a cat"
        val text2 = "a photo of a dog"
        val text3 = "a video of a person"
        
        val z1 = engines.encodeText(text1)
        val z2 = engines.encodeText(text2)
        val z3 = engines.encodeText(text3)
        
        // Should get different embeddings
        assertEquals("Embeddings should have same dimension", z1.size, z2.size)
        assertEquals("Embeddings should have same dimension", z2.size, z3.size)
        
        // Calculate similarities
        val sim12 = engines.cosineSimilarity(z1, z2)
        val sim13 = engines.cosineSimilarity(z1, z3)
        val sim23 = engines.cosineSimilarity(z2, z3)
        
        // Similar texts should have higher similarity
        assertTrue("Similar texts should have higher similarity", sim12 > sim13)
        assertTrue("Similar texts should have higher similarity", sim12 > sim23)
    }
    
    @Test
    fun text_embedding_normalization() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val engines = ClipEngines(ctx)
        
        val initialized = engines.initialize()
        if (!initialized) {
            println("Skipping text embedding normalization test - models not available")
            return@runBlocking
        }
        
        val text = "a photo of a cat"
        val z = engines.encodeText(text)
        
        // Check L2 normalization
        val norm = sqrt(z.fold(0f) { a, b -> a + b * b })
        assertEquals("Embedding should be L2 normalized", 1.0f, norm, 1e-5f)
        
        // Check that embedding is not all zeros
        val hasNonZero = z.any { abs(it) > 1e-6f }
        assertTrue("Embedding should have non-zero values", hasNonZero)
    }
    
    @Test
    fun tokenizer_edge_cases() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val tok = ClipBPETokenizer(ctx)
        
        // Test empty string
        val emptyIds = tok.encode("")
        assertTrue("Empty string should produce valid tokens", emptyIds.isNotEmpty())
        assertTrue("Empty string should have EOT token", emptyIds.contains(49407L))
        
        // Test very long string
        val longText = "a photo of a cat ".repeat(20)
        val longIds = tok.encode(longText)
        assertTrue("Long text should be truncated to max length", longIds.size <= 77)
        
        // Test special characters
        val specialText = "a photo of a cat! @#$%^&*()"
        val specialIds = tok.encode(specialText)
        assertTrue("Special characters should be handled", specialIds.isNotEmpty())
        assertTrue("Special characters should have EOT token", specialIds.contains(49407L))
    }
    
    @Test
    fun text_image_similarity() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val engines = ClipEngines(ctx)
        
        val initialized = engines.initialize()
        if (!initialized) {
            println("Skipping text-image similarity test - models not available")
            return@runBlocking
        }
        
        // Encode text
        val textEmbedding = engines.encodeText("a photo of a cat")
        
        // Create a simple image (black bitmap)
        val image = android.graphics.Bitmap.createBitmap(224, 224, android.graphics.Bitmap.Config.ARGB_8888)
        val imageEmbedding = engines.encodeFrames(listOf(image), batch = 1)
        
        // Calculate similarity
        val similarity = engines.cosineSimilarity(textEmbedding, imageEmbedding)
        
        // Similarity should be in valid range
        assertTrue("Similarity should be >= -1", similarity >= -1f)
        assertTrue("Similarity should be <= 1", similarity <= 1f)
        
        // Embeddings should have same dimension
        assertEquals("Text and image embeddings should have same dimension", 
            textEmbedding.size, imageEmbedding.size)
    }
}
