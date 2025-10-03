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
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class ExactEmbeddingTest {

    @Test
    fun generateExactEmbeddingForVideo_v1_mp4() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test with local video file
        val videoPath = "/sdcard/Movies/video_v1.mp4"
        val videoFile = File(videoPath)
        
        assertTrue("Video file should exist", videoFile.exists())
        println("🎬 Processing video: ${videoFile.absolutePath}")
        println("📁 Video file size: ${videoFile.length()} bytes")
        
        // Sample frames from video
        val frameCount = 8
        println("🖼️ Sampling $frameCount frames...")
        val frames = FrameSampler.sampleUniform(ctx, videoPath, frameCount)
        
        assertTrue("Should sample frames", frames.isNotEmpty())
        assertEquals("Should sample $frameCount frames", frameCount, frames.size)
        
        // Display frame info
        frames.forEachIndexed { index, frame ->
            println("Frame $index: ${frame.width}x${frame.height}")
        }
        
        // Initialize CLIP engines
        println("🤖 Initializing CLIP engines...")
        val engines = ClipEngines(ctx)
        engines.initialize()
        println("✅ CLIP engines initialized")
        
        // Generate embedding
        println("🧠 Generating CLIP embedding...")
        val embedding = engines.encodeFrames(frames)
        
        assertNotNull("Embedding should not be null", embedding)
        assertTrue("Embedding should have content", embedding.size > 0)
        
        // Display exact embedding
        println("\n" + "=".repeat(80))
        println("🎯 EXACT CLIP EMBEDDING FOR video_v1.mp4")
        println("=".repeat(80))
        println("📏 Dimension: ${embedding.size}")
        println("🎬 Video: $videoPath")
        println("🖼️ Frames: ${frames.size}")
        
        // Calculate statistics
        var sum = 0f
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE
        var sumSquares = 0.0
        
        for (value in embedding) {
            sum += value
            if (value < min) min = value
            if (value > max) max = value
            sumSquares += value * value
        }
        
        val mean = sum / embedding.size
        val l2Norm = sqrt(sumSquares)
        
        println("\n📊 EMBEDDING STATISTICS:")
        println("   Mean: $mean")
        println("   Min: $min")
        println("   Max: $max")
        println("   L2 Norm: $l2Norm")
        
        // Verify properties
        assertTrue("Should be 512-dimensional", embedding.size == 512)
        assertTrue("L2 norm should be close to 1.0", kotlin.math.abs(l2Norm - 1.0) < 0.01)
        
        // Display complete embedding vector
        println("\n🔢 COMPLETE EMBEDDING VECTOR:")
        println("FloatArray(${embedding.size}) {")
        for (i in 0 until embedding.size) {
            val value = embedding[i]
            val comma = if (i < embedding.size - 1) "," else ""
            println("    ${value}f$comma")
        }
        println("}")
        
        // Store embedding
        println("\n💾 Storing embedding...")
        val store = FileEmbeddingStore()
        val metadata = mapOf(
            "video_path" to videoPath,
            "frame_count" to frames.size,
            "embedding_dim" to embedding.size,
            "l2_norm" to l2Norm,
            "mean" to mean,
            "min" to min,
            "max" to max,
            "timestamp" to System.currentTimeMillis()
        )
        
        store.storeEmbedding("clip_vit_b32_mean_v1", "video_v1", embedding, metadata)
        println("✅ Embedding stored successfully")
        
        // Verify storage
        val loadedEmbedding = store.loadEmbedding("clip_vit_b32_mean_v1", "video_v1")
        assertNotNull("Should load embedding back", loadedEmbedding)
        assertTrue("Loaded embedding should match", embedding.contentEquals(loadedEmbedding))
        
        println("\n🎉 SUCCESS: Exact embedding generated and verified!")
        println("=".repeat(80))
    }
    
    @Test
    fun verifyEmbeddingQuality() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Load stored embedding
        val store = FileEmbeddingStore()
        val embedding = store.loadEmbedding("clip_vit_b32_mean_v1", "video_v1")
        
        if (embedding != null) {
            println("\n🔍 EMBEDDING QUALITY VERIFICATION:")
            println("=".repeat(60))
            
            // Basic properties
            println("📏 Dimension: ${embedding.size}")
            assertEquals("Should be 512-dimensional", 512, embedding.size)
            
            // Statistical verification
            var sum = 0f
            var sumSquares = 0.0
            var min = Float.MAX_VALUE
            var max = Float.MIN_VALUE
            
            for (value in embedding) {
                sum += value
                sumSquares += value * value
                if (value < min) min = value
                if (value > max) max = value
            }
            
            val mean = sum / embedding.size
            val l2Norm = sqrt(sumSquares)
            
            println("📊 Statistical verification:")
            println("   Mean: $mean")
            println("   Min: $min")
            println("   Max: $max")
            println("   L2 Norm: $l2Norm")
            
            // Quality checks
            assertTrue("Mean should be close to 0", kotlin.math.abs(mean) < 0.1)
            assertTrue("L2 norm should be close to 1.0", kotlin.math.abs(l2Norm - 1.0) < 0.01)
            assertTrue("Min should be reasonable", min > -3.0)
            assertTrue("Max should be reasonable", max < 3.0)
            
            // Check for extreme values
            val extremeCount = embedding.count { kotlin.math.abs(it) > 2.0 }
            println("🚨 Extreme values (>2.0): $extremeCount")
            
            if (extremeCount == 0) {
                println("✅ No extreme values - embedding is healthy")
            } else {
                println("⚠️ Found $extremeCount extreme values")
            }
            
            // Load and display metadata
            val metadata = store.loadMetadata("clip_vit_b32_mean_v1", "video_v1")
            if (metadata != null) {
                println("\n📋 Metadata:")
                metadata.forEach { (key, value) ->
                    println("   $key: $value")
                }
            }
            
            println("\n✅ Embedding quality verification complete")
        } else {
            println("❌ No embedding found - run generateExactEmbeddingForVideo_v1_mp4 first")
        }
    }
}
