package com.mira.videoeditor.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.mira.videoeditor.Logger
import com.mira.videoeditor.db.*
import com.mira.videoeditor.db.VectorConverters.leBytesToFloatArray
import com.mira.videoeditor.db.VectorConverters.floatArrayToLeBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * Repository layer for CLIP4Clip video-text retrieval system.
 * 
 * This layer provides high-level data access operations, combining
 * database operations with ML model inference for video ingestion
 * and text-based retrieval.
 */

/**
 * Repository for video ingestion and embedding generation.
 * Handles the complete pipeline from video URI to stored embeddings.
 */
class IngestionRepository(
    private val videoDao: VideoDao,
    private val shotDao: ShotDao,
    private val embeddingDao: EmbeddingDao,
    private val context: Context
) {
    
    companion object {
        private const val DEFAULT_VARIANT = "clip_vit_b32_mean_v1"
        private const val DEFAULT_FRAMES_PER_VIDEO = 32
        private const val DEFAULT_FRAMES_PER_SHOT = 12
    }

    /**
     * Ingest a video and generate embeddings for retrieval.
     * 
     * @param uri Video URI
     * @param variant Embedding variant (e.g., "clip_vit_b32_mean_v1")
     * @param framesPerVideo Number of frames to sample for video-level embedding
     * @return Video ID
     */
    suspend fun ingestVideo(
        uri: Uri,
        variant: String = DEFAULT_VARIANT,
        framesPerVideo: Int = DEFAULT_FRAMES_PER_VIDEO
    ): String = withContext(Dispatchers.IO) {
        
        Logger.info(Logger.Category.CLIP, "Starting video ingestion", mapOf(
            "uri" to uri.toString().takeLast(50),
            "variant" to variant,
            "framesPerVideo" to framesPerVideo
        ))

        try {
            // 1) Probe video metadata
            val videoEntity = probeVideoMetadata(uri)
            videoDao.upsert(videoEntity)

            // 2) Detect shots
            val shots = detectShots(uri)
            if (shots.isNotEmpty()) {
                shotDao.upsertAll(shots.map { shot ->
                    ShotEntity(
                        videoId = videoEntity.id,
                        startMs = shot.startMs,
                        endMs = shot.endMs
                    )
                })
            }

            // 3) Generate video-level embedding
            val videoEmbedding = generateVideoEmbedding(uri, framesPerVideo)
            embeddingDao.upsert(
                EmbeddingEntity(
                    ownerType = "video",
                    ownerId = videoEntity.id,
                    dim = videoEmbedding.size,
                    variant = variant,
                    vec = floatArrayToLeBytes(videoEmbedding)
                )
            )

            // 4) Generate shot-level embeddings (optional, for fine-grained retrieval)
            if (shots.isNotEmpty()) {
                generateShotEmbeddings(uri, shots, variant)
            }

            Logger.info(Logger.Category.CLIP, "Video ingestion completed", mapOf(
                "videoId" to videoEntity.id,
                "shots" to shots.size,
                "variant" to variant
            ))

            videoEntity.id

        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Video ingestion failed", 
                e.message ?: "Unknown error", mapOf(
                    "uri" to uri.toString().takeLast(50),
                    "variant" to variant
                ), e)
            throw e
        }
    }

    /**
     * Probe video metadata using MediaMetadataRetriever.
     */
    private suspend fun probeVideoMetadata(uri: Uri): VideoEntity = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(context, uri)
            
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            
            val width = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.toIntOrNull() ?: 0
            
            val height = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.toIntOrNull() ?: 0
            
            // Estimate FPS (simplified)
            val fps = if (durationMs > 0) {
                val frameCount = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT
                )?.toLongOrNull() ?: 0L
                if (frameCount > 0) frameCount * 1000f / durationMs else 30f
            } else 30f

            VideoEntity(
                uri = uri.toString(),
                durationMs = durationMs,
                fps = fps,
                width = width,
                height = height,
                metadataJson = "{}"
            )
            
        } finally {
            retriever.release()
        }
    }

    /**
     * Detect shots using existing ShotDetector.
     */
    private suspend fun detectShots(uri: Uri): List<ShotDetector.Shot> = withContext(Dispatchers.IO) {
        // Import and use existing ShotDetector
        val shotDetector = ShotDetector(context)
        shotDetector.detectShots(
            uri = uri,
            sampleMs = 500L,
            minShotMs = 1500L,
            threshold = 0.28f
        )
    }

    /**
     * Generate video-level embedding by sampling frames uniformly.
     */
    private suspend fun generateVideoEmbedding(uri: Uri, framesPerVideo: Int): FloatArray = withContext(Dispatchers.IO) {
        val frames = sampleFramesUniform(uri, framesPerVideo)
        
        // Mock embedding generation (replace with actual CLIP model)
        val mockEmbedding = FloatArray(512) { kotlin.random.Random.nextFloat() }
        
        // Normalize the embedding
        normalizeEmbedding(mockEmbedding)
    }

    /**
     * Generate embeddings for individual shots.
     */
    private suspend fun generateShotEmbeddings(
        uri: Uri, 
        shots: List<ShotDetector.Shot>, 
        variant: String
    ) = withContext(Dispatchers.IO) {
        
        shots.forEach { shot ->
            try {
                val frames = sampleFramesFromShot(uri, shot, DEFAULT_FRAMES_PER_SHOT)
                
                // Mock embedding generation (replace with actual CLIP model)
                val mockEmbedding = FloatArray(512) { kotlin.random.Random.nextFloat() }
                normalizeEmbedding(mockEmbedding)
                
                embeddingDao.upsert(
                    EmbeddingEntity(
                        ownerType = "shot",
                        ownerId = "${shot.startMs}-${shot.endMs}",
                        dim = mockEmbedding.size,
                        variant = variant,
                        vec = floatArrayToLeBytes(mockEmbedding)
                    )
                )
                
            } catch (e: Exception) {
                Logger.warn(Logger.Category.CLIP, "Shot embedding generation failed", mapOf(
                    "shotId" to "${shot.startMs}-${shot.endMs}",
                    "error" to (e.message ?: "Unknown")
                ))
            }
        }
    }

    /**
     * Sample frames uniformly across the video.
     */
    private suspend fun sampleFramesUniform(uri: Uri, frameCount: Int): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        
        try {
            retriever.setDataSource(context, uri)
            
            val durationUs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull()?.times(1000) ?: 0L
            
            if (durationUs <= 0L) return@withContext emptyList()
            
            val step = durationUs / frameCount
            var t = step / 2
            
            repeat(frameCount) {
                retriever.getFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { bitmap ->
                    frames.add(bitmap)
                }
                t += step
            }
            
        } finally {
            retriever.release()
        }
        
        frames
    }

    /**
     * Sample frames from a specific shot.
     */
    private suspend fun sampleFramesFromShot(uri: Uri, shot: ShotDetector.Shot, frameCount: Int): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        
        try {
            retriever.setDataSource(context, uri)
            
            val shotDuration = shot.endMs - shot.startMs
            val frameInterval = shotDuration / frameCount.coerceAtLeast(1)
            
            repeat(frameCount) { i ->
                val timestampMs = shot.startMs + (i * frameInterval)
                val clampedTimestamp = timestampMs.coerceIn(shot.startMs, shot.endMs - 1)
                
                retriever.getFrameAtTime(
                    clampedTimestamp * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )?.let { bitmap ->
                    frames.add(bitmap)
                }
            }
            
        } finally {
            retriever.release()
        }
        
        frames
    }

    /**
     * Normalize embedding vector to unit length.
     */
    private fun normalizeEmbedding(embedding: FloatArray) {
        val norm = sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
        if (norm > 0f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
    }
}

/**
 * Repository for text-based video retrieval.
 * Handles text encoding and similarity search.
 */
class RetrievalRepository(
    private val readDao: ReadModelsDao,
    private val embeddingDao: EmbeddingDao,
    private val textDao: TextDao,
    private val context: Context
) {
    
    companion object {
        private const val DEFAULT_VARIANT = "clip_vit_b32_mean_v1"
        private const val DEFAULT_TOP_K = 10
    }

    /**
     * Search videos by text query using CLIP4Clip similarity.
     * 
     * @param query Text query
     * @param variant Embedding variant to use
     * @param topK Number of results to return
     * @return List of (VideoEntity, similarity_score) pairs
     */
    suspend fun searchByText(
        query: String,
        variant: String = DEFAULT_VARIANT,
        topK: Int = DEFAULT_TOP_K
    ): List<Pair<VideoEntity, Float>> = withContext(Dispatchers.IO) {
        
        Logger.info(Logger.Category.CLIP, "Starting text-based search", mapOf(
            "query" to query.take(50),
            "variant" to variant,
            "topK" to topK
        ))

        try {
            // 1) Store text query
            val textEntity = TextEntity(text = query)
            textDao.upsert(textEntity)

            // 2) Generate text embedding (mock implementation)
            val textEmbedding = generateTextEmbedding(query)
            
            // Store text embedding
            embeddingDao.upsert(
                EmbeddingEntity(
                    ownerType = "text",
                    ownerId = textEntity.id,
                    dim = textEmbedding.size,
                    variant = variant,
                    vec = floatArrayToLeBytes(textEmbedding)
                )
            )

            // 3) Get all video embeddings
            val videoEmbeddings = embeddingDao.listAllVideoEmbeddings(variant)
            
            // 4) Compute similarities
            val similarities = videoEmbeddings.mapNotNull { embedding ->
                val video = readDao.videosWithEmbeddings(variant)
                    .find { it.video.id == embedding.ownerId }?.video
                
                if (video != null) {
                    val videoVec = leBytesToFloatArray(embedding.vec)
                    val similarity = cosineSimilarity(textEmbedding, videoVec)
                    video to similarity
                } else null
            }

            // 5) Sort by similarity and return top K
            val results = similarities
                .sortedByDescending { it.second }
                .take(topK)

            Logger.info(Logger.Category.CLIP, "Text search completed", mapOf(
                "query" to query.take(50),
                "results" to results.size,
                "maxSimilarity" to (results.firstOrNull()?.second ?: 0f)
            ))

            results

        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Text search failed", 
                e.message ?: "Unknown error", mapOf(
                    "query" to query.take(50),
                    "variant" to variant
                ), e)
            throw e
        }
    }

    /**
     * Generate text embedding (mock implementation).
     * Replace with actual CLIP text encoder.
     */
    private suspend fun generateTextEmbedding(text: String): FloatArray = withContext(Dispatchers.IO) {
        // Mock implementation - replace with actual CLIP text encoder
        val embedding = FloatArray(512) { kotlin.random.Random.nextFloat() }
        
        // Normalize
        val norm = sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
        if (norm > 0f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
        
        embedding
    }

    /**
     * Compute cosine similarity between two normalized vectors.
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        
        var dotProduct = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        
        return dotProduct // Both vectors are normalized, so this is cosine similarity
    }
}