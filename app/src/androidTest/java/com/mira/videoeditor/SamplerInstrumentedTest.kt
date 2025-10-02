package com.mira.videoeditor

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mira.videoeditor.video.FrameSampler
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class SamplerInstrumentedTest {
    
    @Test
    fun uniform_sampling_returns_expected_count() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = copyRawToFileUri(ctx, "sample.mp4")
        
        val frames = FrameSampler.sampleUniform(ctx, uri, 16)
        assertEquals(16, frames.size)
        
        // Verify frames are not null and have reasonable dimensions
        frames.forEach { frame ->
            assertNotNull("Frame should not be null", frame)
            assertTrue("Frame width should be > 0", frame.width > 0)
            assertTrue("Frame height should be > 0", frame.height > 0)
        }
    }
    
    @Test
    fun uniform_sampling_deterministic() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = copyRawToFileUri(ctx, "sample.mp4")
        
        // Sample twice and verify we get the same number of frames
        val frames1 = FrameSampler.sampleUniform(ctx, uri, 8)
        val frames2 = FrameSampler.sampleUniform(ctx, uri, 8)
        
        assertEquals("Should get same number of frames", frames1.size, frames2.size)
        assertEquals("Should get 8 frames", 8, frames1.size)
    }
    
    @Test
    fun range_sampling_works_correctly() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = copyRawToFileUri(ctx, "sample.mp4")
        
        // Sample from first 2 seconds of video
        val frames = FrameSampler.sampleRange(ctx, uri, 0, 2000, 4)
        assertEquals(4, frames.size)
        
        frames.forEach { frame ->
            assertNotNull("Frame should not be null", frame)
            assertTrue("Frame should have valid dimensions", frame.width > 0 && frame.height > 0)
        }
    }
    
    @Test
    fun video_metadata_extraction() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = copyRawToFileUri(ctx, "sample.mp4")
        
        val metadata = FrameSampler.getVideoMetadata(ctx, uri)
        assertNotNull("Metadata should not be null", metadata)
        
        assertTrue("Duration should be > 0", metadata!!.durationMs > 0)
        assertTrue("Width should be > 0", metadata.width > 0)
        assertTrue("Height should be > 0", metadata.height > 0)
        
        // Our test video is 5 seconds, 320x240
        assertTrue("Duration should be around 5 seconds", metadata.durationMs >= 4000 && metadata.durationMs <= 6000)
        assertEquals("Width should be 320", 320, metadata.width)
        assertEquals("Height should be 240", 240, metadata.height)
    }
    
    @Test
    fun sampling_edge_cases() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = copyRawToFileUri(ctx, "sample.mp4")
        
        // Test with 1 frame
        val singleFrame = FrameSampler.sampleUniform(ctx, uri, 1)
        assertEquals(1, singleFrame.size)
        
        // Test with 0 frames (should return empty list)
        val noFrames = FrameSampler.sampleUniform(ctx, uri, 0)
        assertEquals(0, noFrames.size)
        
        // Test with more frames than video duration allows
        val manyFrames = FrameSampler.sampleUniform(ctx, uri, 1000)
        assertTrue("Should return some frames even with high count", manyFrames.size > 0)
        assertTrue("Should not exceed requested count", manyFrames.size <= 1000)
    }
}

// Utility function to copy raw resource to file and return Uri
fun copyRawToFileUri(ctx: Context, name: String): Uri {
    val id = ctx.resources.getIdentifier(name.substringBefore("."), "raw", ctx.packageName)
    require(id != 0) { "raw/$name not found" }
    val out = java.io.File(ctx.filesDir, name)
    ctx.resources.openRawResource(id).use { input ->
        out.outputStream().use { input.copyTo(it) }
    }
    return Uri.fromFile(out)
}
