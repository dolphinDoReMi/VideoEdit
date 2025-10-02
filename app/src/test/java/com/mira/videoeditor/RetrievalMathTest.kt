package com.mira.videoeditor

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class RetrievalMathTest {
    private fun l2(x: FloatArray): Float = sqrt(x.fold(0f) { a, b -> a + b * b })
    
    private fun cos(a: FloatArray, b: FloatArray): Float =
        a.zip(b).sumOf { it.first * it.second }.toFloat() / (l2(a) * l2(b) + 1e-8f)

    @Test
    fun cosine_ordering_monotonic() {
        val q = floatArrayOf(1f, 0f, 0f)
        val a = floatArrayOf(1f, 0f, 0f)    // 1.0
        val b = floatArrayOf(0.9f, 0.1f, 0f) // ~0.9
        val c = floatArrayOf(0f, 1f, 0f)    // 0.0
        
        val cosQA = cos(q, a)
        val cosQB = cos(q, b)
        val cosQC = cos(q, c)
        
        assertTrue("cos(q,a) > cos(q,b)", cosQA > cosQB)
        assertTrue("cos(q,b) > cos(q,c)", cosQB > cosQC)
        assertTrue("cos(q,a) > cos(q,c)", cosQA > cosQC)
    }

    @Test
    fun cosine_similarity_properties() {
        val a = floatArrayOf(1f, 0f, 0f)
        val b = floatArrayOf(0f, 1f, 0f)
        val c = floatArrayOf(0f, 0f, 1f)
        
        // Identical vectors should have cosine similarity of 1.0
        val cosAA = cos(a, a)
        assertEquals(1.0f, cosAA, 1e-6f)
        
        // Orthogonal vectors should have cosine similarity of 0.0
        val cosAB = cos(a, b)
        assertEquals(0.0f, cosAB, 1e-6f)
        
        val cosAC = cos(a, c)
        assertEquals(0.0f, cosAC, 1e-6f)
        
        val cosBC = cos(b, c)
        assertEquals(0.0f, cosBC, 1e-6f)
    }

    @Test
    fun cosine_similarity_range() {
        val a = floatArrayOf(1f, 0f, 0f)
        val b = floatArrayOf(-1f, 0f, 0f)  // Opposite direction
        val c = floatArrayOf(0.5f, 0.5f, 0f)  // 45 degrees
        
        val cosAB = cos(a, b)
        val cosAC = cos(a, c)
        
        // Cosine similarity should be in range [-1, 1]
        assertTrue("cos(a,b) >= -1", cosAB >= -1f)
        assertTrue("cos(a,b) <= 1", cosAB <= 1f)
        assertTrue("cos(a,c) >= -1", cosAC >= -1f)
        assertTrue("cos(a,c) <= 1", cosAC <= 1f)
        
        // Opposite vectors should have cosine similarity of -1.0
        assertEquals(-1.0f, cosAB, 1e-6f)
        
        // 45-degree angle should have cosine similarity of ~0.707
        assertEquals(0.7071068f, cosAC, 1e-5f)
    }

    @Test
    fun l2_norm_properties() {
        val a = floatArrayOf(3f, 4f, 0f)
        val b = floatArrayOf(1f, 1f, 1f)
        val c = floatArrayOf(0f, 0f, 0f)
        
        // L2 norm should be positive for non-zero vectors
        assertTrue("l2(a) > 0", l2(a) > 0f)
        assertTrue("l2(b) > 0", l2(b) > 0f)
        
        // L2 norm should be 0 for zero vector
        assertEquals(0.0f, l2(c), 1e-6f)
        
        // L2 norm should match expected values
        assertEquals(5.0f, l2(a), 1e-6f)  // sqrt(3^2 + 4^2 + 0^2) = 5
        assertEquals(sqrt(3.0f), l2(b), 1e-6f)  // sqrt(1^2 + 1^2 + 1^2) = sqrt(3)
    }

    @Test
    fun ranking_consistency() {
        val query = floatArrayOf(1f, 0f, 0f)
        val candidates = listOf(
            floatArrayOf(0.9f, 0.1f, 0f) to "high_similarity",
            floatArrayOf(0.5f, 0.5f, 0f) to "medium_similarity", 
            floatArrayOf(0f, 1f, 0f) to "low_similarity",
            floatArrayOf(-0.5f, 0.5f, 0f) to "negative_similarity"
        )
        
        // Calculate similarities
        val similarities = candidates.map { (vec, name) ->
            cos(query, vec) to name
        }.sortedByDescending { it.first }
        
        // Verify ranking order
        assertEquals("high_similarity", similarities[0].second)
        assertEquals("medium_similarity", similarities[1].second)
        assertEquals("low_similarity", similarities[2].second)
        assertEquals("negative_similarity", similarities[3].second)
        
        // Verify similarity values are in descending order
        assertTrue("Similarities should be in descending order", 
            similarities[0].first > similarities[1].first)
        assertTrue("Similarities should be in descending order", 
            similarities[1].first > similarities[2].first)
        assertTrue("Similarities should be in descending order", 
            similarities[2].first > similarities[3].first)
    }

    @Test
    fun normalized_vector_properties() {
        val raw = floatArrayOf(3f, 4f, 0f)
        val norm = l2(raw)
        val normalized = raw.map { it / norm }.toFloatArray()
        
        // Normalized vector should have L2 norm of 1.0
        assertEquals(1.0f, l2(normalized), 1e-6f)
        
        // Cosine similarity between original and normalized should be 1.0
        assertEquals(1.0f, cos(raw, normalized), 1e-6f)
    }
}
