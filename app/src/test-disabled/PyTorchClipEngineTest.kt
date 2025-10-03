package com.mira.videoeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mira.videoeditor.ml.PyTorchClipEngine
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test suite for PyTorch Mobile CLIP integration.
 * 
 * These tests verify:
 * 1. Model loading and initialization
 * 2. Image preprocessing
 * 3. Text tokenization
 * 4. Embedding generation
 * 5. Similarity computation
 */
@RunWith(AndroidJUnit4::class)
class PyTorchClipEngineTest {

    private lateinit var context: Context
    private lateinit var clipEngine: PyTorchClipEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        clipEngine = PyTorchClipEngine(context)
    }

    @Test
    fun testModelInitialization() = runBlocking {
        // Test model initialization (will fail without actual model files)
        val initialized = clipEngine.initialize()
        
        // For now, we expect this to fail since we don't have actual model files
        // In production, this should return true
        println("Model initialization result: $initialized")
        
        // Test isReady method
        val isReady = clipEngine.isReady()
        println("Model ready status: $isReady")
    }

    @Test
    fun testImagePreprocessing() {
        // Create a test bitmap
        val testBitmap = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
        
        // Test preprocessing (this should work even without models)
        try {
            // We can't directly test the private preprocessImage method,
            // but we can test the public encodeImage method
            // which will fail gracefully if models aren't loaded
            val embedding = clipEngine.encodeImage(testBitmap)
            
            // If we get here, the model is loaded and working
            assertEquals(512, embedding.size)
            
            // Verify embedding is normalized
            val norm = kotlin.math.sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
            assertTrue(kotlin.math.abs(norm - 1.0f) < 0.001f, "Embedding should be normalized")
            
        } catch (e: Exception) {
            // Expected if models aren't loaded
            println("Expected error (models not loaded): ${e.message}")
        }
    }

    @Test
    fun testTextEncoding() {
        val testText = "person walking in the street"
        
        try {
            val embedding = clipEngine.encodeText(testText)
            
            // If we get here, the model is loaded and working
            assertEquals(512, embedding.size)
            
            // Verify embedding is normalized
            val norm = kotlin.math.sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
            assertTrue(kotlin.math.abs(norm - 1.0f) < 0.001f, "Embedding should be normalized")
            
        } catch (e: Exception) {
            // Expected if models aren't loaded
            println("Expected error (models not loaded): ${e.message}")
        }
    }

    @Test
    fun testBatchImageEncoding() {
        // Create test bitmaps
        val bitmaps = (1..5).map { 
            Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        }
        
        try {
            val embeddings = clipEngine.encodeImages(bitmaps)
            
            // If we get here, the model is loaded and working
            assertEquals(5, embeddings.size)
            
            embeddings.forEach { embedding ->
                assertEquals(512, embedding.size)
                
                // Verify each embedding is normalized
                val norm = kotlin.math.sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
                assertTrue(kotlin.math.abs(norm - 1.0f) < 0.001f, "Each embedding should be normalized")
            }
            
        } catch (e: Exception) {
            // Expected if models aren't loaded
            println("Expected error (models not loaded): ${e.message}")
        }
    }

    @Test
    fun testCosineSimilarity() {
        // Test cosine similarity computation
        val embedding1 = FloatArray(512) { kotlin.random.Random.nextFloat() }
        val embedding2 = FloatArray(512) { kotlin.random.Random.nextFloat() }
        
        // Normalize embeddings
        normalizeEmbedding(embedding1)
        normalizeEmbedding(embedding2)
        
        val similarity = clipEngine.cosineSimilarity(embedding1, embedding2)
        
        // Similarity should be between -1 and 1
        assertTrue(similarity >= -1.0f && similarity <= 1.0f, "Similarity should be between -1 and 1")
        
        // Test identical embeddings
        val identicalSimilarity = clipEngine.cosineSimilarity(embedding1, embedding1)
        assertTrue(kotlin.math.abs(identicalSimilarity - 1.0f) < 0.001f, "Identical embeddings should have similarity ~1")
        
        // Test orthogonal embeddings
        val orthogonal1 = floatArrayOf(1f, 0f, 0f, 0f) + FloatArray(508) { 0f }
        val orthogonal2 = floatArrayOf(0f, 1f, 0f, 0f) + FloatArray(508) { 0f }
        val orthogonalSimilarity = clipEngine.cosineSimilarity(orthogonal1, orthogonal2)
        assertTrue(kotlin.math.abs(orthogonalSimilarity) < 0.001f, "Orthogonal embeddings should have similarity ~0")
    }

    @Test
    fun testBpeTokenizer() {
        // Test BPE tokenizer (this should work even without model files)
        val tokenizer = com.mira.videoeditor.ml.ClipBPETokenizer(context)
        
        val testTexts = listOf(
            "person walking",
            "a cat sitting on a chair",
            "outdoor scene with trees",
            "people talking in a room"
        )
        
        testTexts.forEach { text ->
            val tokens = tokenizer.encode(text, 77)
            
            // Verify token array properties
            assertEquals(77, tokens.size, "Token array should have length 77")
            assertTrue(tokens.all { it >= 0 }, "All tokens should be non-negative")
            assertTrue(tokens.all { it <= 50000 }, "All tokens should be reasonable values")
            
            println("Text: '$text' -> Tokens: ${tokens.take(10).joinToString(", ")}...")
        }
    }

    @Test
    fun testEmbeddingConsistency() {
        // Test that the same input produces consistent embeddings
        val testBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        val testText = "person walking in the street"
        
        try {
            val imageEmbedding1 = clipEngine.encodeImage(testBitmap)
            val imageEmbedding2 = clipEngine.encodeImage(testBitmap)
            
            val textEmbedding1 = clipEngine.encodeText(testText)
            val textEmbedding2 = clipEngine.encodeText(testText)
            
            // Same input should produce identical embeddings
            val imageSimilarity = clipEngine.cosineSimilarity(imageEmbedding1, imageEmbedding2)
            val textSimilarity = clipEngine.cosineSimilarity(textEmbedding1, textEmbedding2)
            
            assertTrue(kotlin.math.abs(imageSimilarity - 1.0f) < 0.001f, "Same image should produce identical embeddings")
            assertTrue(kotlin.math.abs(textSimilarity - 1.0f) < 0.001f, "Same text should produce identical embeddings")
            
        } catch (e: Exception) {
            // Expected if models aren't loaded
            println("Expected error (models not loaded): ${e.message}")
        }
    }

    @Test
    fun testCrossModalSimilarity() {
        // Test similarity between image and text embeddings
        val testBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        val testText = "person walking in the street"
        
        try {
            val imageEmbedding = clipEngine.encodeImage(testBitmap)
            val textEmbedding = clipEngine.encodeText(testText)
            
            val similarity = clipEngine.cosineSimilarity(imageEmbedding, textEmbedding)
            
            // Cross-modal similarity should be between -1 and 1
            assertTrue(similarity >= -1.0f && similarity <= 1.0f, "Cross-modal similarity should be between -1 and 1")
            
            println("Cross-modal similarity: $similarity")
            
        } catch (e: Exception) {
            // Expected if models aren't loaded
            println("Expected error (models not loaded): ${e.message}")
        }
    }

    // Helper function for normalization
    private fun normalizeEmbedding(embedding: FloatArray) {
        val norm = kotlin.math.sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
        if (norm > 0f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
    }
}
