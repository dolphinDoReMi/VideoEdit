package com.mira.clip

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExactEmbeddingVectorTest {

    @Test
    fun generateExactEmbeddingVector() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Use the embedding generator utility
        val generator = EmbeddingGenerator(ctx)
        
        try {
            // Generate the exact embedding vector
            val embedding = generator.generateExactEmbedding()
            
            // Verify the embedding
            assertNotNull("Embedding should not be null", embedding)
            assertTrue("Embedding should have content", embedding.size > 0)
            assertEquals("Should be 512-dimensional", 512, embedding.size)
            
            // Print the exact embedding vector
            generator.printExactEmbeddingVector(embedding)
            
            // Verify L2 normalization
            var sumSquares = 0.0
            for (value in embedding) {
                sumSquares += value * value
            }
            val l2Norm = kotlin.math.sqrt(sumSquares)
            assertTrue("L2 norm should be close to 1.0", kotlin.math.abs(l2Norm - 1.0) < 0.01)
            
            println("\n✅ SUCCESS: Exact embedding vector generated and verified!")
            
        } catch (e: Exception) {
            fail("Failed to generate embedding: ${e.message}")
        }
    }
}