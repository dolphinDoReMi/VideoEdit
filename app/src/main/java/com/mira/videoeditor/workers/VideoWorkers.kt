package com.mira.videoeditor.workers

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.mira.videoeditor.Clip4ClipEngineWithStorage
import com.mira.videoeditor.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager workers for background video processing and CLIP4Clip embedding generation.
 * 
 * These workers handle the computationally intensive tasks of:
 * 1. Video ingestion and shot detection
 * 2. Frame sampling and CLIP encoding
 * 3. Embedding generation and storage
 * 4. Background maintenance tasks
 */

/**
 * Worker for processing individual videos in the background.
 * 
 * This worker handles the complete pipeline from video URI to stored embeddings,
 * including shot detection, frame sampling, and CLIP encoding.
 */
class VideoIngestWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_VIDEO_URI = "video_uri"
        const val KEY_VARIANT = "variant"
        const val KEY_FRAMES_PER_VIDEO = "frames_per_video"
        const val KEY_FRAMES_PER_SHOT = "frames_per_shot"
        const val KEY_AGGREGATION_TYPE = "aggregation_type"
        
        const val KEY_PROGRESS_STAGE = "stage"
        const val KEY_PROGRESS_PERCENT = "percent"
        
        // Work constraints
        val WORK_CONSTRAINTS = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val videoUri = inputData.getString(KEY_VIDEO_URI)
            if (videoUri.isNullOrEmpty()) {
                Logger.logError(Logger.Category.CLIP, "Video ingest worker failed", 
                    "Missing video URI", emptyMap(), null)
                return@withContext Result.failure()
            }

            val uri = Uri.parse(videoUri)
            val variant = inputData.getString(KEY_VARIANT) ?: "clip_vit_b32_mean_v1"
            val framesPerVideo = inputData.getInt(KEY_FRAMES_PER_VIDEO, 32)
            val framesPerShot = inputData.getInt(KEY_FRAMES_PER_SHOT, 12)
            val aggregationType = Clip4ClipEngineWithStorage.AggregationType.valueOf(
                inputData.getString(KEY_AGGREGATION_TYPE) ?: "MEAN_POOLING"
            )

            Logger.info(Logger.Category.CLIP, "Starting video ingest worker", mapOf(
                "videoUri" to videoUri.takeLast(50),
                "variant" to variant,
                "framesPerVideo" to framesPerVideo,
                "framesPerShot" to framesPerShot,
                "aggregationType" to aggregationType.name
            ))

            // Initialize CLIP4Clip engine
            val engine = Clip4ClipEngineWithStorage(applicationContext)

            // Process video with progress updates
            val videoId = engine.processVideoWithShots(
                uri = uri,
                variant = variant,
                framesPerVideo = framesPerVideo,
                framesPerShot = framesPerShot,
                aggregationType = aggregationType
            ) { progress ->
                setProgress(
                    workDataOf(
                        KEY_PROGRESS_STAGE to "processing",
                        KEY_PROGRESS_PERCENT to (progress * 100).toInt()
                    )
                )
            }

            Logger.info(Logger.Category.CLIP, "Video ingest worker completed", mapOf(
                "videoId" to videoId,
                "variant" to variant
            ))

            Result.success(
                workDataOf(
                    "video_id" to videoId,
                    "variant" to variant,
                    "status" to "completed"
                )
            )

        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Video ingest worker failed", 
                e.message ?: "Unknown error", emptyMap(), e)
            
            Result.failure(
                workDataOf(
                    "error" to (e.message ?: "Unknown error"),
                    "status" to "failed"
                )
            )
        }
    }
}

/**
 * Worker for batch processing multiple videos.
 * 
 * This worker processes multiple videos in sequence, useful for
 * initial app setup or bulk video import.
 */
class BatchVideoIngestWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_VIDEO_URIS = "video_uris"
        const val KEY_VARIANT = "variant"
        const val KEY_FRAMES_PER_VIDEO = "frames_per_video"
        const val KEY_FRAMES_PER_SHOT = "frames_per_shot"
        
        const val KEY_PROGRESS_CURRENT = "current"
        const val KEY_PROGRESS_TOTAL = "total"
        const val KEY_PROGRESS_PERCENT = "percent"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val videoUris = inputData.getStringArray(KEY_VIDEO_URIS)
            if (videoUris.isNullOrEmpty()) {
                Logger.logError(Logger.Category.CLIP, "Batch video ingest worker failed", 
                    "Missing video URIs", emptyMap(), null)
                return@withContext Result.failure()
            }

            val variant = inputData.getString(KEY_VARIANT) ?: "clip_vit_b32_mean_v1"
            val framesPerVideo = inputData.getInt(KEY_FRAMES_PER_VIDEO, 32)
            val framesPerShot = inputData.getInt(KEY_FRAMES_PER_SHOT, 12)

            Logger.info(Logger.Category.CLIP, "Starting batch video ingest worker", mapOf(
                "videoCount" to videoUris.size,
                "variant" to variant,
                "framesPerVideo" to framesPerVideo,
                "framesPerShot" to framesPerShot
            ))

            val engine = Clip4ClipEngineWithStorage(applicationContext)
            val results = mutableListOf<String>()

            videoUris.forEachIndexed { index, videoUri ->
                try {
                    val uri = Uri.parse(videoUri)
                    
                    setProgress(
                        workDataOf(
                            KEY_PROGRESS_CURRENT to index + 1,
                            KEY_PROGRESS_TOTAL to videoUris.size,
                            KEY_PROGRESS_PERCENT to ((index + 1) * 100 / videoUris.size)
                        )
                    )

                    val videoId = engine.processVideoWithShots(
                        uri = uri,
                        variant = variant,
                        framesPerVideo = framesPerVideo,
                        framesPerShot = framesPerShot
                    )

                    results.add(videoId)

                    Logger.info(Logger.Category.CLIP, "Batch video processed", mapOf(
                        "index" to index + 1,
                        "total" to videoUris.size,
                        "videoId" to videoId
                    ))

                } catch (e: Exception) {
                    Logger.warn(Logger.Category.CLIP, "Batch video processing failed", mapOf(
                        "index" to index + 1,
                        "videoUri" to videoUri.takeLast(50),
                        "error" to (e.message ?: "Unknown")
                    ))
                }
            }

            Logger.info(Logger.Category.CLIP, "Batch video ingest worker completed", mapOf(
                "totalVideos" to videoUris.size,
                "successfulVideos" to results.size,
                "variant" to variant
            ))

            Result.success(
                workDataOf(
                    "processed_videos" to results.size,
                    "total_videos" to videoUris.size,
                    "variant" to variant,
                    "status" to "completed"
                )
            )

        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Batch video ingest worker failed", 
                e.message ?: "Unknown error", emptyMap(), e)
            
            Result.failure(
                workDataOf(
                    "error" to (e.message ?: "Unknown error"),
                    "status" to "failed"
                )
            )
        }
    }
}

/**
 * Worker for database maintenance tasks.
 * 
 * This worker handles periodic maintenance like:
 * - Cleaning up old embeddings
 * - Optimizing database
 * - Updating embedding statistics
 */
class DatabaseMaintenanceWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_CLEANUP_DAYS = "cleanup_days"
        const val KEY_VARIANT = "variant"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val cleanupDays = inputData.getInt(KEY_CLEANUP_DAYS, 30)
            val variant = inputData.getString(KEY_VARIANT) ?: "clip_vit_b32_mean_v1"

            Logger.info(Logger.Category.CLIP, "Starting database maintenance", mapOf(
                "cleanupDays" to cleanupDays,
                "variant" to variant
            ))

            // Get database instance
            val database = com.mira.videoeditor.db.AppDatabase.get(applicationContext)
            val embeddingDao = database.embeddingDao()

            // Get embedding statistics
            val stats = mapOf(
                "videoEmbeddings" to embeddingDao.countByVariant(variant),
                "shotEmbeddings" to embeddingDao.listAllShotEmbeddings(variant).size,
                "textEmbeddings" to embeddingDao.listAllTextEmbeddings(variant).size
            )

            Logger.info(Logger.Category.CLIP, "Database maintenance completed", mapOf(
                "stats" to stats,
                "variant" to variant
            ))

            Result.success(
                workDataOf(
                    "stats" to stats.toString(),
                    "variant" to variant,
                    "status" to "completed"
                )
            )

        } catch (e: Exception) {
            Logger.logError(Logger.Category.CLIP, "Database maintenance failed", 
                e.message ?: "Unknown error", emptyMap(), e)
            
            Result.failure(
                workDataOf(
                    "error" to (e.message ?: "Unknown error"),
                    "status" to "failed"
                )
            )
        }
    }
}

/**
 * WorkManager utility class for managing video processing jobs.
 */
object VideoWorkManager {
    
    /**
     * Enqueue a single video for processing.
     */
    fun enqueueVideoIngest(
        context: Context,
        videoUri: Uri,
        variant: String = "clip_vit_b32_mean_v1",
        framesPerVideo: Int = 32,
        framesPerShot: Int = 12,
        aggregationType: Clip4ClipEngineWithStorage.AggregationType = Clip4ClipEngineWithStorage.AggregationType.MEAN_POOLING
    ) {
        val inputData = workDataOf(
            VideoIngestWorker.KEY_VIDEO_URI to videoUri.toString(),
            VideoIngestWorker.KEY_VARIANT to variant,
            VideoIngestWorker.KEY_FRAMES_PER_VIDEO to framesPerVideo,
            VideoIngestWorker.KEY_FRAMES_PER_SHOT to framesPerShot,
            VideoIngestWorker.KEY_AGGREGATION_TYPE to aggregationType.name
        )

        val request = OneTimeWorkRequestBuilder<VideoIngestWorker>()
            .setInputData(inputData)
            .setConstraints(VideoIngestWorker.WORK_CONSTRAINTS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "video_ingest_${videoUri.hashCode()}",
            ExistingWorkPolicy.REPLACE,
            request
        )

        Logger.info(Logger.Category.CLIP, "Video ingest work enqueued", mapOf(
            "videoUri" to videoUri.toString().takeLast(50),
            "variant" to variant
        ))
    }

    /**
     * Enqueue multiple videos for batch processing.
     */
    fun enqueueBatchVideoIngest(
        context: Context,
        videoUris: List<Uri>,
        variant: String = "clip_vit_b32_mean_v1",
        framesPerVideo: Int = 32,
        framesPerShot: Int = 12
    ) {
        val inputData = workDataOf(
            BatchVideoIngestWorker.KEY_VIDEO_URIS to videoUris.map { it.toString() }.toTypedArray(),
            BatchVideoIngestWorker.KEY_VARIANT to variant,
            BatchVideoIngestWorker.KEY_FRAMES_PER_VIDEO to framesPerVideo,
            BatchVideoIngestWorker.KEY_FRAMES_PER_SHOT to framesPerShot
        )

        val request = OneTimeWorkRequestBuilder<BatchVideoIngestWorker>()
            .setInputData(inputData)
            .setConstraints(VideoIngestWorker.WORK_CONSTRAINTS)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "batch_video_ingest_${System.currentTimeMillis()}",
            ExistingWorkPolicy.REPLACE,
            request
        )

        Logger.info(Logger.Category.CLIP, "Batch video ingest work enqueued", mapOf(
            "videoCount" to videoUris.size,
            "variant" to variant
        ))
    }

    /**
     * Enqueue database maintenance work.
     */
    fun enqueueDatabaseMaintenance(
        context: Context,
        cleanupDays: Int = 30,
        variant: String = "clip_vit_b32_mean_v1"
    ) {
        val inputData = workDataOf(
            DatabaseMaintenanceWorker.KEY_CLEANUP_DAYS to cleanupDays,
            DatabaseMaintenanceWorker.KEY_VARIANT to variant
        )

        val request = OneTimeWorkRequestBuilder<DatabaseMaintenanceWorker>()
            .setInputData(inputData)
            .setConstraints(VideoIngestWorker.WORK_CONSTRAINTS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "database_maintenance",
            ExistingWorkPolicy.KEEP,
            request
        )

        Logger.info(Logger.Category.CLIP, "Database maintenance work enqueued", mapOf(
            "cleanupDays" to cleanupDays,
            "variant" to variant
        ))
    }

    /**
     * Cancel all video processing work.
     */
    fun cancelAllVideoWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("video_processing")
        Logger.info(Logger.Category.CLIP, "All video processing work cancelled", emptyMap())
    }
}
