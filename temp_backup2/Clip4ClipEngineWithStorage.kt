package com.mira.videoeditor

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.mira.videoeditor.db.*
import com.mira.videoeditor.db.VectorConverters.leBytesToFloatArray
import com.mira.videoeditor.db.VectorConverters.floatArrayToLeBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * Enhanced CLIP4Clip engine with Room database integration.
 * 
 * This implementation extends the original CLIP4Clip engine with:
 * 1. Persistent storage of embeddings in Room database
 * 2. Integration with video ingestion pipeline
 * 3. Efficient similarity search with database queries
 * 4. Support for multiple embedding variants
 */
class Clip4ClipEngineWithStorage(private val ctx: Context) {

    companion object {
        private const val TAG = "Clip4ClipEngineWithStorage"
        
        // Frame sampling parameters
        private const val DEFAULT_FRAMES_PER_SHOT = 12
        private const val DEFAULT_FRAMES_PER_VIDEO = 32
        private const val MIN_FRAMES_PER_SHOT = 4
        private const val MAX_FRAMES_PER_SHOT = 24
        
        // CLIP model parameters
        private const val EMBEDDING_DIM = 512
        private const val FRAME_WIDTH = 224
        private const val FRAME_HEIGHT = 224
        
        // Default embedding variant
        private const val DEFAULT_VARIANT = "clip_vit_b32_mean_v1"
        
        // Aggregation types
        enum class AggregationType {
            MEAN_POOLING,      // Parameter-free mean pooling
            SEQUENTIAL,        // Transformer-based temporal modeling
            TIGHT              // Cross-modal attention with text
        }
    }

    // Database access
    private val database = AppDatabase.get(ctx)
    private val videoDao = database.videoDao()
    private val shotDao = database.shotDao()
    private val embeddingDao = database.embeddingDao()
    private val readDao = database.readModelsDao()

    // Mock CLIP encoder (replace with actual CLIP implementation)
    private val mockClipEncoder = MockClipEncoder()

    /**
     * Generate and store embeddings for a video with shots.
     * 
     * @param uri Video URI
     * @param variant Embedding variant
     * @param framesPerVideo Frames to sample for video-level embedding
     * @param framesPerShot Frames to sample per shot
     * @param aggregationType Aggregation method for shot embeddings
     * @param onProgress Progress callback
     * @return Video ID
     */
    suspend fun processVideoWithShots(
        uri: Uri,
        variant: String = DEFAULT_VARIANT,
        framesPerVideo: Int = DEFAULT_FRAMES_PER_VIDEO,
        framesPerShot: Int = DEFAULT_FRAMES_PER_SHOT,
        aggregationType: AggregationType = AggregationType.MEAN_POOLING,
        onProgress: (Float) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        
        Logger.info(Logger.Category.CLIP, "Processing video with shots", mapOf(
            "uri" to uri.toString().takeLast(50),
            "variant" to variant,
            "framesPerVideo" to framesPerVideo,
            "framesPerShot" to framesPerShot,
            "aggregationType" to aggregationType.name
        ))

        try {
            // 1) Check if video already exists
            val existingVideo = videoDao.getByUri(uri.toString())
            val videoEntity = existingVideo ?: probeVideoMetadata(uri)
            
            if (existingVideo == null) {
                videoDao.upsert(videoEntity)
            }

            onProgress(0.1f)

            // 2) Detect shots
            val shotDetector = ShotDetector(ctx)
            val shots = shotDetector.detectShots(
                uri = uri,
                sampleMs = 500L,
                minShotMs = 1500L,
                threshold = 0.28f
            )

            if (shots.isNotEmpty()) {
                val shotEntities = shots.map { shot ->
                    ShotEntity(
                        videoId = videoEntity.id,
                        startMs = shot.startMs,
                        endMs = shot.endMs
                    )
                }
                shotDao.upsertAll(shotEntities)
            }

            onProgress(0.3f)

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

            onProgress(0.6f)

            // 4) Generate shot-level embeddings
            if (shots.isNotEmpty()) {
                generateShotEmbeddings(uri, shots, variant, framesPerShot, aggregationType) { shotProgress ->
                    val overallProgress = 0.6f + (shotProgress * 0.4f)
                    onProgress(overallProgress)
                }
            }

            onProgress(1.0f)

            Logger.info(Logger.Category.CLIP, "Video processing completed", mapOf(
                "videoId" to videoEntity.id,
                "shots" to shots.size,
                "variant" to variant
            ))

            videoEntity.id

        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Video processing failed", 
                e.message ?: "Unknown error", mapOf(
                    "uri" to uri.toString().takeLast(50),
                    "variant" to variant
                ), e)
            throw e
        }
    }

    /**
     * Search videos by text query using stored embeddings.
     * 
     * @param query Text query
     * @param variant Embedding variant to use
     * @param topK Number of results
     * @param searchLevel "video" or "shot" for different granularity
     * @return List of similarity results
     */
    suspend fun searchByText(
        query: String,
        variant: String = DEFAULT_VARIANT,
        topK: Int = 10,
        searchLevel: String = "video"
    ): List<SimilarityResult> = withContext(Dispatchers.IO) {
        
        Logger.info(Logger.Category.CLIP, "Starting text-based search", mapOf(
            "query" to query.take(50),
            "variant" to variant,
            "topK" to topK,
            "searchLevel" to searchLevel
        ))

        try {
            // 1) Generate text embedding
            val textEmbedding = mockClipEncoder.encodeText(query)
            normalizeEmbedding(textEmbedding)

            // 2) Get embeddings based on search level
            val embeddings = when (searchLevel) {
                "video" -> embeddingDao.listAllVideoEmbeddings(variant)
                "shot" -> embeddingDao.listAllShotEmbeddings(variant)
                else -> embeddingDao.listAllVideoEmbeddings(variant)
            }

            // 3) Compute similarities
            val similarities = embeddings.mapNotNull { embedding ->
                val embeddingVec = leBytesToFloatArray(embedding.vec)
                val similarity = cosineSimilarity(textEmbedding, embeddingVec)
                
                when (searchLevel) {
                    "video" -> {
                        val video = videoDao.getById(embedding.ownerId)
                        if (video != null) {
                            SimilarityResult(
                                shotId = embedding.ownerId,
                                similarity = similarity,
                                startMs = 0L,
                                endMs = video.durationMs,
                                metadata = mapOf(
                                    "type" to "video",
                                    "uri" to video.uri,
                                    "variant" to variant
                                )
                            )
                        } else null
                    }
                    "shot" -> {
                        val shot = shotDao.getById(embedding.ownerId)
                        if (shot != null) {
                            SimilarityResult(
                                shotId = embedding.ownerId,
                                similarity = similarity,
                                startMs = shot.startMs,
                                endMs = shot.endMs,
                                metadata = mapOf(
                                    "type" to "shot",
                                    "videoId" to shot.videoId,
                                    "variant" to variant
                                )
                            )
                        } else null
                    }
                    else -> null
                }
            }

            // 4) Sort by similarity and return top K
            val results = similarities
                .sortedByDescending { it.similarity }
                .take(topK)

            Logger.info(Logger.Category.CLIP, "Text search completed", mapOf(
                "query" to query.take(50),
                "results" to results.size,
                "maxSimilarity" to (results.firstOrNull()?.similarity ?: 0f)
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
     * Get all videos with their embeddings for a specific variant.
     */
    suspend fun getAllVideosWithEmbeddings(variant: String = DEFAULT_VARIANT): List<VideoWithEmbedding> = withContext(Dispatchers.IO) {
        readDao.videosWithEmbeddings(variant)
    }

    /**
     * Get shots for a video with their embeddings.
     */
    suspend fun getShotsWithEmbeddings(videoId: String, variant: String = DEFAULT_VARIANT): List<ShotWithEmbedding> = withContext(Dispatchers.IO) {
        readDao.shotsWithEmbeddings(videoId, variant)
    }

    /**
     * Delete video and all associated data.
     */
    suspend fun deleteVideo(videoId: String) = withContext(Dispatchers.IO) {
        // Delete embeddings
        embeddingDao.deleteByOwnerTypeAndId("video", videoId)
        embeddingDao.deleteByOwnerTypeAndId("shot", videoId)
        
        // Delete shots
        shotDao.deleteByVideoId(videoId)
        
        // Delete video
        videoDao.deleteById(videoId)
        
        Logger.info(Logger.Category.CLIP, "Video deleted", mapOf("videoId" to videoId))
    }

    /**
     * Get embedding statistics for monitoring.
     */
    suspend fun getEmbeddingStats(variant: String = DEFAULT_VARIANT): Map<String, Int> = withContext(Dispatchers.IO) {
        mapOf(
            "videoEmbeddings" to embeddingDao.countByVariant(variant),
            "shotEmbeddings" to embeddingDao.listAllShotEmbeddings(variant).size,
            "textEmbeddings" to embeddingDao.listAllTextEmbeddings(variant).size
        )
    }

    // Private helper methods

    private suspend fun probeVideoMetadata(uri: Uri): VideoEntity = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(ctx, uri)
            
            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            
            val width = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.toIntOrNull() ?: 0
            
            val height = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.toIntOrNull() ?: 0
            
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

    private suspend fun generateVideoEmbedding(uri: Uri, framesPerVideo: Int): FloatArray = withContext(Dispatchers.IO) {
        val frames = sampleFramesUniform(uri, framesPerVideo)
        
        // Mock embedding generation (replace with actual CLIP model)
        val mockEmbedding = FloatArray(EMBEDDING_DIM) { kotlin.random.Random.nextFloat() }
        normalizeEmbedding(mockEmbedding)
        mockEmbedding
    }

    private suspend fun generateShotEmbeddings(
        uri: Uri,
        shots: List<ShotDetector.Shot>,
        variant: String,
        framesPerShot: Int,
        aggregationType: AggregationType,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        
        shots.forEachIndexed { index, shot ->
            try {
                val frames = sampleFramesFromShot(uri, shot, framesPerShot)
                
                // Mock embedding generation (replace with actual CLIP model)
                val mockEmbedding = FloatArray(EMBEDDING_DIM) { kotlin.random.Random.nextFloat() }
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
                
                onProgress((index + 1).toFloat() / shots.size)
                
            } catch (e: Exception) {
                Logger.warn(Logger.Category.CLIP, "Shot embedding generation failed", mapOf(
                    "shotId" to "${shot.startMs}-${shot.endMs}",
                    "error" to (e.message ?: "Unknown")
                ))
            }
        }
    }

    private suspend fun sampleFramesUniform(uri: Uri, frameCount: Int): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        
        try {
            retriever.setDataSource(ctx, uri)
            
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

    private suspend fun sampleFramesFromShot(uri: Uri, shot: ShotDetector.Shot, frameCount: Int): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        
        try {
            retriever.setDataSource(ctx, uri)
            
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

    private fun normalizeEmbedding(embedding: FloatArray) {
        val norm = sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }).toFloat()
        if (norm > 0f) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        
        var dotProduct = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        
        return dotProduct // Both vectors are normalized
    }
}

/**
 * Mock CLIP encoder for demonstration purposes.
 * Replace with actual CLIP implementation (e.g., using PyTorch Mobile)
 */
class MockClipEncoder {
    
    companion object {
        private const val EMBEDDING_DIM = 512
    }
    
    fun encodeImage(bitmap: Bitmap): FloatArray {
        // Mock implementation - replace with actual CLIP image encoder
        return FloatArray(EMBEDDING_DIM) { kotlin.random.Random.nextFloat() }
    }
    
    fun encodeText(text: String): FloatArray {
        // Mock implementation - replace with actual CLIP text encoder
        return FloatArray(EMBEDDING_DIM) { kotlin.random.Random.nextFloat() }
    }
}
