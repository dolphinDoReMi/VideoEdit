package com.mira.clip

import android.content.Context
import android.media.MediaMetadataRetriever
import com.mira.clip.clip.ClipEngines
import com.mira.clip.storage.FileEmbeddingStore
import com.mira.clip.video.FrameSampler
import java.io.File
import kotlin.math.sqrt

/**
 * Simple utility to generate exact CLIP embedding vector for video_v1.mp4
 */
class EmbeddingGenerator(private val context: Context) {
    
    fun generateExactEmbedding(videoPathOverride: String? = null): FloatArray {
        val videoPath = videoPathOverride ?: "/sdcard/Movies/video_v1.mp4"
        val videoFile = File(videoPath)
        
        if (!videoFile.exists()) {
            throw RuntimeException("Video file not found: $videoPath")
        }
        
        println("🎬 Processing video: ${videoFile.absolutePath}")
        println("📁 Video file size: ${videoFile.length()} bytes")
        
        // Validate duration against expected 8:54 (≈534000 ms)
        val durationMs = readDurationMs(videoPath)
        val hh = durationMs / 3_600_000
        val mm = (durationMs % 3_600_000) / 60_000
        val ss = (durationMs % 60_000) / 1000
        println("⏱️ Duration: %02d:%02d:%02d (%d ms)".format(hh, mm, ss, durationMs))
        
        // Accept within a small tolerance window
        val expectedMs = 534_000L // 8:54 minutes
        val toleranceMs = 5_000L
        if (kotlin.math.abs(durationMs - expectedMs) > toleranceMs) {
            throw RuntimeException(
                "Unexpected video duration. Expected ~534000ms (8:54), got ${durationMs}ms. " +
                "Please provide the correct 8:54 file or pass an explicit path to generateExactEmbedding(path)."
            )
        }
        
        // Sample frames from video
        val frameCount = 8
        println("🖼️ Sampling $frameCount frames...")
        val frames = FrameSampler.sampleUniform(context, videoPath, frameCount)
        
        if (frames.isEmpty()) {
            throw RuntimeException("Failed to sample frames from video")
        }
        
        println("✅ Sampled ${frames.size} frames")
        
        // Initialize CLIP engines
        println("🤖 Initializing CLIP engines...")
        val engines = ClipEngines(context)
        engines.initialize()
        println("✅ CLIP engines initialized")
        
        // Generate embedding
        println("🧠 Generating CLIP embedding...")
        val embedding = engines.encodeFrames(frames)
        
        if (embedding.isEmpty()) {
            throw RuntimeException("Failed to generate embedding")
        }
        
        println("✅ Generated embedding with ${embedding.size} dimensions")
        
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
        
        // Store embedding
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
        
        return embedding
    }
    
    private fun readDurationMs(path: String): Long {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(path)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            mmr.release()
        }
    }
    
    fun printExactEmbeddingVector(embedding: FloatArray) {
        println("\n" + "=".repeat(100))
        println("🎯 EXACT CLIP EMBEDDING VECTOR FOR video_v1.mp4")
        println("=".repeat(100))
        println("📏 Dimension: ${embedding.size}")
        
        // Display complete embedding vector
        println("\n🔢 COMPLETE EMBEDDING VECTOR:")
        println("FloatArray(${embedding.size}) {")
        for (i in 0 until embedding.size) {
            val value = embedding[i]
            val comma = if (i < embedding.size - 1) "," else ""
            println("    ${value}f$comma")
        }
        println("}")
        
        // Also display as array literal
        println("\n📋 EMBEDDING AS ARRAY LITERAL:")
        println("val embedding = floatArrayOf(")
        for (i in 0 until embedding.size) {
            val value = embedding[i]
            val comma = if (i < embedding.size - 1) "," else ""
            println("    ${value}f$comma")
        }
        println(")")
        
        println("\n🎉 SUCCESS: Exact embedding vector generated!")
        println("=".repeat(100))
    }
}
