package com.mira.clip.services

import android.content.Context
import com.mira.clip.clip.ClipEngines
import com.mira.clip.core.Config
import com.mira.clip.storage.EmbeddingStore
import com.mira.clip.storage.FileEmbeddingStore
import com.mira.clip.video.FrameSampler
import org.json.JSONObject
import java.io.File

/**
 * Video ingestion service for CLIP4Clip.
 * 
 * Processes videos from manifest and generates embeddings.
 */
class VideoIngestService(private val context: Context) {
    
    private val clipEngines = ClipEngines(context)
    private val embeddingStore: EmbeddingStore = FileEmbeddingStore()
    
    /**
     * Run video ingestion from manifest file.
     * 
     * @param manifestPath Path to ingestion manifest JSON
     */
    fun run(manifestPath: String) {
        val manifest = loadManifest(manifestPath)
        
        // Initialize CLIP engines
        clipEngines.initialize()
        
        val variant = manifest.getString("variant")
        val frameCount = manifest.optInt("frame_count", Config.DEFAULT_FRAME_COUNT)
        val videos = manifest.getJSONArray("videos")
        
        for (i in 0 until videos.length()) {
            val video = videos.getJSONObject(i)
            val videoId = video.getString("id")
            val videoPath = video.optString("path", Config.DEFAULT_VIDEO_PATH)
            
            try {
                processVideo(variant, videoId, videoPath, frameCount)
            } catch (e: Exception) {
                println("Error processing video $videoId: ${e.message}")
            }
        }
    }
    
    /**
     * Process a single video and generate embedding.
     */
    private fun processVideo(variant: String, videoId: String, videoPath: String, frameCount: Int) {
        println("Processing video: $videoId from $videoPath")
        
        // Sample frames from video
        val frames = FrameSampler.sampleUniform(context, videoPath, frameCount)
        println("Sampled ${frames.size} frames")
        
        // Generate embedding
        val embedding = clipEngines.encodeFrames(frames)
        println("Generated embedding with dimension: ${embedding.size}")
        
        // Create metadata
        val metadata = mapOf(
            "video_id" to videoId,
            "video_path" to videoPath,
            "frame_count" to frames.size,
            "variant" to variant,
            "embedding_dimension" to embedding.size,
            "timestamp" to System.currentTimeMillis()
        )
        
        // Store embedding and metadata
        embeddingStore.storeEmbedding(variant, videoId, embedding, metadata)
        println("Stored embedding for video: $videoId")
    }
    
    /**
     * Load ingestion manifest from JSON file.
     */
    private fun loadManifest(path: String): JSONObject {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("Manifest file does not exist: $path")
        }
        
        return JSONObject(file.readText())
    }
}
