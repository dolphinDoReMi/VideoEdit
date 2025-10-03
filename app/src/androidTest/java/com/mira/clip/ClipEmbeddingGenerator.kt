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
class ClipEmbeddingGenerator {

    @Test
    fun generateEmbeddingForVideo_v1_mp4() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Test with local video file on Xiaomi device
        val videoPath = "/sdcard/Movies/video_v1.mp4"
        val videoFile = File(videoPath)
        
        assertTrue("Local video file should exist: $videoPath", videoFile.exists())
        assertTrue("Video file should be readable", videoFile.canRead())
        assertTrue("Video file should have content", videoFile.length() > 1000)
        
        println("🎬 Processing video: ${videoFile.absolutePath}")
        println("📁 Video file size: ${videoFile.length()} bytes")
        
        // Sample frames from video
        val frameCount = 8
        println("🖼️ Sampling $frameCount frames from video...")
        val frames = FrameSampler.sampleUniform(ctx, videoPath, frameCount)
        
        assertTrue("Should sample frames from video", frames.isNotEmpty())
        assertEquals("Should sample $frameCount frames", frameCount, frames.size)
        
        // Display frame information
        frames.forEachIndexed { index, frame ->
            println("Frame $index: ${frame.width}x${frame.height} pixels")
        }
        
        // Initialize CLIP engines
        println("🤖 Initializing CLIP engines...")
        val engines = ClipEngines(ctx)
        assertNotNull("ClipEngines should initialize", engines)
        
        try {
            engines.initialize()
            println("✅ CLIP engines initialized successfully")
        } catch (e: Exception) {
            println("❌ Failed to initialize CLIP engines: ${e.message}")
            fail("CLIP engines should initialize: ${e.message}")
        }
        
        // Generate embedding from frames
        println("🧠 Generating CLIP embedding from frames...")
        val embedding = try {
            engines.encodeFrames(frames)
        } catch (e: Exception) {
            println("❌ Failed to generate embedding: ${e.message}")
            fail("Should generate embedding: ${e.message}")
        }
        
        assertNotNull("Embedding should not be null", embedding)
        assertTrue("Embedding should have content", embedding.size > 0)
        
        // Display embedding information
        println("\n📊 EMBEDDING RESULTS:")
        println("=".repeat(50))
        println("📏 Embedding dimension: ${embedding.size}")
        println("🎯 Expected dimension: 512 (ViT-B/32)")
        
        // Calculate embedding statistics
        val mean = embedding.sum() / embedding.size.toFloat()
        val min = embedding.min()
        val max = embedding.max()
        val l2Norm = sqrt(embedding.map { it * it }.sum().toDouble())
        
        println("📈 Embedding statistics:")
        println("   Mean: $mean")
        println("   Min: $min")
        println("   Max: $max")
        println("   L2 Norm: $l2Norm")
        
        // Verify L2 normalization (should be close to 1.0)
        assertTrue("Embedding should be L2-normalized (norm ≈ 1.0)", 
                  kotlin.math.abs(l2Norm - 1.0) < 0.01)
        
        // Display first 20 values of the embedding
        println("\n🔢 First 20 embedding values:")
        for (i in 0 until minOf(20, embedding.size)) {
            println("   [$i]: ${embedding[i]}")
        }
        
        // Store embedding using FileEmbeddingStore
        println("\n💾 Storing embedding...")
        val store = FileEmbeddingStore()
        val metadata = mapOf(
            "video_path" to videoPath,
            "frame_count" to frameCount,
            "embedding_dim" to embedding.size,
            "l2_norm" to l2Norm,
            "timestamp" to System.currentTimeMillis()
        )
        
        try {
            store.storeEmbedding("clip_vit_b32_mean_v1", "video_v1", embedding, metadata)
            println("✅ Embedding stored successfully")
        } catch (e: Exception) {
            println("❌ Failed to store embedding: ${e.message}")
        }
        
        // Verify embedding can be loaded back
        val loadedEmbedding = store.loadEmbedding("clip_vit_b32_mean_v1", "video_v1")
        assertNotNull("Should be able to load embedding back", loadedEmbedding)
        assertTrue("Loaded embedding should match original", 
                  embedding.contentEquals(loadedEmbedding))
        
        println("\n🎉 SUCCESS: CLIP embedding generated and stored for video_v1.mp4")
        println("=".repeat(50))
        
        // Display the complete embedding vector
        println("\n🔢 COMPLETE EMBEDDING VECTOR:")
        println("FloatArray(${embedding.size}) {")
        for (i in 0 until embedding.size) {
            val value = embedding[i]
            println("    $value${if (i < embedding.size - 1) "," else ""}")
        }
        println("}")
    }
}