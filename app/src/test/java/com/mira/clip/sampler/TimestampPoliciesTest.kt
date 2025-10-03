package com.mira.clip.sampler

import org.junit.Test
import org.junit.Assert.*

class TimestampPoliciesTest {

    @Test
    fun testUniformSampling() {
        val duration = 10000L // 10 seconds
        val count = 5
        val timestamps = TimestampPolicies.uniform(duration, count)
        
        assertEquals(count, timestamps.size)
        assertEquals(0L, timestamps[0])
        assertEquals(duration, timestamps[count - 1])
        
        // Check monotonicity
        for (i in 1 until timestamps.size) {
            assertTrue("Timestamps should be monotonic", timestamps[i] >= timestamps[i - 1])
        }
    }

    @Test
    fun testUniformSamplingEdgeCases() {
        // Single frame - should throw exception since n >= 2 is required
        try {
            TimestampPolicies.uniform(1000L, 1)
            fail("Should throw IllegalArgumentException for n < 2")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
        
        // Zero duration
        val zero = TimestampPolicies.uniform(0L, 3)
        assertEquals(3, zero.size)
        assertTrue(zero.all { it == 0L })
    }

    @Test
    fun testTsnJitterSampling() {
        val duration = 10000L
        val count = 5
        val timestamps = TimestampPolicies.tsnJitter(duration, count)
        
        assertEquals(count, timestamps.size)
        
        // Check bounds
        assertTrue("First timestamp should be >= 0", timestamps[0] >= 0)
        assertTrue("Last timestamp should be <= duration", timestamps[count - 1] <= duration)
        
        // Check monotonicity
        for (i in 1 until timestamps.size) {
            assertTrue("Timestamps should be monotonic", timestamps[i] >= timestamps[i - 1])
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUniformSamplingInvalidCount() {
        TimestampPolicies.uniform(1000L, 1) // Should throw for n < 2
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTsnJitterInvalidCount() {
        TimestampPolicies.tsnJitter(1000L, 1) // Should throw for n < 2
    }
}
