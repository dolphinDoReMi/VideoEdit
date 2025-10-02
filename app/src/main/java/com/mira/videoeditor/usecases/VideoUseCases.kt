package com.mira.videoeditor.usecases

import android.content.Context
import android.net.Uri
import com.mira.videoeditor.Clip4ClipEngineWithStorage
import com.mira.videoeditor.Logger
import com.mira.videoeditor.db.*
import com.mira.videoeditor.workers.VideoWorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Use cases for CLIP4Clip video-text retrieval system.
 * 
 * These use cases provide high-level operations for the UI layer,
 * encapsulating business logic and coordinating between different
 * components (database, ML models, background workers).
 */

/**
 * Use case for video ingestion and processing.
 * 
 * Handles the complete pipeline from video selection to stored embeddings,
 * including background processing and progress tracking.
 */
class VideoIngestionUseCase(private val context: Context) {

    companion object {
        private const val DEFAULT_VARIANT = "clip_vit_b32_mean_v1"
        private const val DEFAULT_FRAMES_PER_VIDEO = 32
        private const val DEFAULT_FRAMES_PER_SHOT = 12
    }

    /**
     * Process a single video and generate embeddings.
     * 
     * @param videoUri Video URI to process
     * @param variant Embedding variant to use
     * @param framesPerVideo Number of frames to sample for video-level embedding
     * @param framesPerShot Number of frames to sample per shot
     * @param aggregationType Aggregation method for shot embeddings
     * @param useBackgroundProcessing Whether to use WorkManager for background processing
     * @return Video ID if successful
     */
    suspend fun processVideo(
        videoUri: Uri,
        variant: String = DEFAULT_VARIANT,
        framesPerVideo: Int = DEFAULT_FRAMES_PER_VIDEO,
        framesPerShot: Int = DEFAULT_FRAMES_PER_SHOT,
        aggregationType: Clip4ClipEngineWithStorage.AggregationType = Clip4ClipEngineWithStorage.AggregationType.MEAN_POOLING,
        useBackgroundProcessing: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        
        Logger.info(Logger.Category.CLIP, "Starting video processing", mapOf(
            "videoUri" to videoUri.toString().takeLast(50),
            "variant" to variant,
            "framesPerVideo" to framesPerVideo,
            "framesPerShot" to framesPerShot,
            "aggregationType" to aggregationType.name,
            "useBackgroundProcessing" to useBackgroundProcessing
        ))

        try {
            if (useBackgroundProcessing) {
                // Enqueue background work
                VideoWorkManager.enqueueVideoIngest(
                    context = context,
                    videoUri = videoUri,
                    variant = variant,
                    framesPerVideo = framesPerVideo,
                    framesPerShot = framesPerShot,
                    aggregationType = aggregationType
                )
                
                // Return a placeholder ID (actual ID will be available after work completes)
                return@withContext "processing_${videoUri.hashCode()}"
            } else {
                // Process synchronously
                val engine = Clip4ClipEngineWithStorage(context)
                return@withContext engine.processVideoWithShots(
                    uri = videoUri,
                    variant = variant,
                    framesPerVideo = framesPerVideo,
                    framesPerShot = framesPerShot,
                    aggregationType = aggregationType
                )
            }
        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Video processing failed", 
                e.message ?: "Unknown error", mapOf(
                    "videoUri" to videoUri.toString().takeLast(50),
                    "variant" to variant
                ), e)
            throw e
        }
    }

    /**
     * Process multiple videos in batch.
     * 
     * @param videoUris List of video URIs to process
     * @param variant Embedding variant to use
     * @param framesPerVideo Number of frames to sample for video-level embedding
     * @param framesPerShot Number of frames to sample per shot
     * @return Number of videos enqueued for processing
     */
    suspend fun processVideosBatch(
        videoUris: List<Uri>,
        variant: String = DEFAULT_VARIANT,
        framesPerVideo: Int = DEFAULT_FRAMES_PER_VIDEO,
        framesPerShot: Int = DEFAULT_FRAMES_PER_SHOT
    ): Int = withContext(Dispatchers.IO) {
        
        Logger.info(Logger.Category.CLIP, "Starting batch video processing", mapOf(
            "videoCount" to videoUris.size,
            "variant" to variant,
            "framesPerVideo" to framesPerVideo,
            "framesPerShot" to framesPerShot
        ))

        try {
            VideoWorkManager.enqueueBatchVideoIngest(
                context = context,
                videoUris = videoUris,
                variant = variant,
                framesPerVideo = framesPerVideo,
                framesPerShot = framesPerShot
            )
            
            return@withContext videoUris.size
        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Batch video processing failed", 
                e.message ?: "Unknown error", mapOf(
                    "videoCount" to videoUris.size,
                    "variant" to variant
                ), e)
            throw e
        }
    }

    /**
     * Get processing status for a video.
     * 
     * @param videoUri Video URI to check
     * @return Processing status information
     */
    suspend fun getProcessingStatus(videoUri: Uri): ProcessingStatus = withContext(Dispatchers.IO) {
        val database = AppDatabase.get(context)
        val videoDao = database.videoDao()
        
        val video = videoDao.getByUri(videoUri.toString())
        val embeddingDao = database.embeddingDao()
        
        if (video != null) {
            val embedding = embeddingDao.forOwnerAndVariant(video.id, DEFAULT_VARIANT)
            if (embedding != null) {
                ProcessingStatus.COMPLETED
            } else {
                ProcessingStatus.PROCESSING
            }
        } else {
            ProcessingStatus.NOT_STARTED
        }
    }

    /**
     * Delete a video and all associated data.
     * 
     * @param videoId Video ID to delete
     */
    suspend fun deleteVideo(videoId: String) = withContext(Dispatchers.IO) {
        val engine = Clip4ClipEngineWithStorage(context)
        engine.deleteVideo(videoId)
        
        Logger.info(Logger.Category.CLIP, "Video deleted", mapOf("videoId" to videoId))
    }
}

/**
 * Use case for text-based video search.
 * 
 * Provides high-level search operations with different granularities
 * (video-level vs shot-level) and result formatting.
 */
class VideoSearchUseCase(private val context: Context) {

    companion object {
        private const val DEFAULT_VARIANT = "clip_vit_b32_mean_v1"
        private const val DEFAULT_TOP_K = 10
    }

    /**
     * Search videos by text query.
     * 
     * @param query Text query
     * @param variant Embedding variant to use
     * @param topK Number of results to return
     * @param searchLevel "video" or "shot" for different granularity
     * @return List of search results
     */
    suspend fun searchVideos(
        query: String,
        variant: String = DEFAULT_VARIANT,
        topK: Int = DEFAULT_TOP_K,
        searchLevel: String = "video"
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        
        Logger.info(Logger.Category.CLIP, "Starting video search", mapOf(
            "query" to query.take(50),
            "variant" to variant,
            "topK" to topK,
            "searchLevel" to searchLevel
        ))

        try {
            val engine = Clip4ClipEngineWithStorage(context)
            val similarityResults = engine.searchByText(
                query = query,
                variant = variant,
                topK = topK,
                searchLevel = searchLevel
            )

            val searchResults = similarityResults.map { result ->
                SearchResult(
                    id = result.shotId,
                    similarity = result.similarity,
                    startMs = result.startMs,
                    endMs = result.endMs,
                    metadata = result.metadata,
                    searchLevel = searchLevel
                )
            }

            Logger.info(Logger.Category.CLIP, "Video search completed", mapOf(
                "query" to query.take(50),
                "results" to searchResults.size,
                "maxSimilarity" to (searchResults.firstOrNull()?.similarity ?: 0f)
            ))

            searchResults

        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Video search failed", 
                e.message ?: "Unknown error", mapOf(
                    "query" to query.take(50),
                    "variant" to variant
                ), e)
            throw e
        }
    }

    /**
     * Get all processed videos with their embeddings.
     * 
     * @param variant Embedding variant to filter by
     * @return Flow of videos with embeddings
     */
    fun getAllVideosWithEmbeddings(variant: String = DEFAULT_VARIANT): Flow<List<VideoWithEmbedding>> = flow {
        val database = AppDatabase.get(context)
        val readDao = database.readModelsDao()
        
        val videos = readDao.videosWithEmbeddings(variant)
        emit(videos)
    }

    /**
     * Get shots for a specific video with their embeddings.
     * 
     * @param videoId Video ID
     * @param variant Embedding variant to filter by
     * @return List of shots with embeddings
     */
    suspend fun getShotsWithEmbeddings(
        videoId: String,
        variant: String = DEFAULT_VARIANT
    ): List<ShotWithEmbedding> = withContext(Dispatchers.IO) {
        val database = AppDatabase.get(context)
        val readDao = database.readModelsDao()
        
        readDao.shotsWithEmbeddings(videoId, variant)
    }

    /**
     * Get embedding statistics for monitoring.
     * 
     * @param variant Embedding variant to check
     * @return Statistics map
     */
    suspend fun getEmbeddingStats(variant: String = DEFAULT_VARIANT): Map<String, Int> = withContext(Dispatchers.IO) {
        val engine = Clip4ClipEngineWithStorage(context)
        engine.getEmbeddingStats(variant)
    }
}

/**
 * Use case for database management and maintenance.
 * 
 * Provides operations for database cleanup, optimization, and monitoring.
 */
class DatabaseManagementUseCase(private val context: Context) {

    /**
     * Perform database maintenance tasks.
     * 
     * @param cleanupDays Number of days to keep old data
     * @param variant Embedding variant to maintain
     */
    suspend fun performMaintenance(
        cleanupDays: Int = 30,
        variant: String = "clip_vit_b32_mean_v1"
    ) = withContext(Dispatchers.IO) {
        
        Logger.info(Logger.Category.CLIP, "Starting database maintenance", mapOf(
            "cleanupDays" to cleanupDays,
            "variant" to variant
        ))

        try {
            VideoWorkManager.enqueueDatabaseMaintenance(
                context = context,
                cleanupDays = cleanupDays,
                variant = variant
            )
        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Database maintenance failed", 
                e.message ?: "Unknown error", emptyMap(), e)
            throw e
        }
    }

    /**
     * Get database statistics.
     * 
     * @return Database statistics
     */
    suspend fun getDatabaseStats(): DatabaseStats = withContext(Dispatchers.IO) {
        val database = AppDatabase.get(context)
        val videoDao = database.videoDao()
        val shotDao = database.shotDao()
        val embeddingDao = database.embeddingDao()

        val videoCount = videoDao.getLatest(Int.MAX_VALUE).size
        val embeddingCount = embeddingDao.countByVariant("clip_vit_b32_mean_v1")
        
        DatabaseStats(
            videoCount = videoCount,
            embeddingCount = embeddingCount,
            databaseSize = 0L // Would need to calculate actual database file size
        )
    }
}

// Data classes for use case results

enum class ProcessingStatus {
    NOT_STARTED,
    PROCESSING,
    COMPLETED,
    FAILED
}

data class SearchResult(
    val id: String,
    val similarity: Float,
    val startMs: Long,
    val endMs: Long,
    val metadata: Map<String, Any>,
    val searchLevel: String
)

data class DatabaseStats(
    val videoCount: Int,
    val embeddingCount: Int,
    val databaseSize: Long
)
