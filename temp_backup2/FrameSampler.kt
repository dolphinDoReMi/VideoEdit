package com.mira.videoeditor.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FrameSampler - Utility for sampling frames from video files
 * 
 * Provides deterministic frame sampling for video processing and analysis.
 */
object FrameSampler {
    
    /**
     * Sample frames uniformly across the video duration.
     * 
     * @param context Android context
     * @param uri Video URI
     * @param frameCount Number of frames to sample
     * @return List of sampled frames as Bitmaps
     */
    suspend fun sampleUniform(context: Context, uri: Uri, frameCount: Int): List<Bitmap> = 
        withContext(Dispatchers.IO) {
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
     * Sample frames from a specific time range.
     * 
     * @param context Android context
     * @param uri Video URI
     * @param startMs Start time in milliseconds
     * @param endMs End time in milliseconds
     * @param frameCount Number of frames to sample
     * @return List of sampled frames as Bitmaps
     */
    suspend fun sampleRange(
        context: Context, 
        uri: Uri, 
        startMs: Long, 
        endMs: Long, 
        frameCount: Int
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        
        try {
            retriever.setDataSource(context, uri)
            
            val rangeDuration = endMs - startMs
            val frameInterval = rangeDuration / frameCount.coerceAtLeast(1)
            
            repeat(frameCount) { i ->
                val timestampMs = startMs + (i * frameInterval)
                val clampedTimestamp = timestampMs.coerceIn(startMs, endMs - 1)
                
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
     * Get video metadata including duration and dimensions.
     * 
     * @param context Android context
     * @param uri Video URI
     * @return VideoMetadata or null if unable to extract
     */
    suspend fun getVideoMetadata(context: Context, uri: Uri): VideoMetadata? = 
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            
            try {
                retriever.setDataSource(context, uri)
                
                val durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull()
                
                val width = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull()
                
                val height = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull()
                
                if (durationMs != null && width != null && height != null) {
                    VideoMetadata(durationMs, width, height)
                } else {
                    null
                }
                
            } finally {
                retriever.release()
            }
        }
}

/**
 * Video metadata container
 */
data class VideoMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int
)
