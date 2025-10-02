package com.mira.clip.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.mira.clip.core.Config
import java.io.File

/**
 * Frame sampler for extracting frames from video files.
 * 
 * Samples a fixed number of frames uniformly from a video file path
 * using MediaMetadataRetriever.
 */
object FrameSampler {
    
    /**
     * Sample frames uniformly from a video file.
     * 
     * @param ctx Android context
     * @param path Video file path
     * @param count Number of frames to sample
     * @return List of sampled frames as bitmaps
     */
    fun sampleUniform(ctx: Context, path: String, count: Int): List<Bitmap> {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("Video file does not exist: $path")
        }
        
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(path)
            
            // Get video duration
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            
            if (duration <= 0) {
                throw IllegalArgumentException("Invalid video duration: $duration")
            }
            
            // Calculate frame timestamps
            val timestamps = calculateFrameTimestamps(duration, count)
            
            // Extract frames
            val frames = mutableListOf<Bitmap>()
            
            for (timestamp in timestamps) {
                val frame = retriever.getFrameAtTime(timestamp * 1000) // Convert to microseconds
                if (frame != null) {
                    frames.add(frame)
                }
            }
            
            return frames
            
        } finally {
            retriever.release()
        }
    }
    
    /**
     * Calculate uniform frame timestamps.
     * 
     * @param duration Video duration in milliseconds
     * @param count Number of frames to sample
     * @return List of timestamps in milliseconds
     */
    private fun calculateFrameTimestamps(duration: Long, count: Int): List<Long> {
        if (count <= 0) {
            return emptyList()
        }
        
        if (count == 1) {
            return listOf(duration / 2) // Middle of video
        }
        
        val timestamps = mutableListOf<Long>()
        val interval = duration / (count - 1)
        
        for (i in 0 until count) {
            val timestamp = i * interval
            timestamps.add(timestamp)
        }
        
        return timestamps
    }
    
    /**
     * Sample frames from default video path.
     * 
     * @param ctx Android context
     * @param count Number of frames to sample
     * @return List of sampled frames
     */
    fun sampleDefaultVideo(ctx: Context, count: Int = Config.DEFAULT_FRAME_COUNT): List<Bitmap> {
        return sampleUniform(ctx, Config.DEFAULT_VIDEO_PATH, count)
    }
    
    /**
     * Get video metadata.
     * 
     * @param path Video file path
     * @return Video metadata map
     */
    fun getVideoMetadata(path: String): Map<String, String> {
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(path)
            
            val metadata = mutableMapOf<String, String>()
            
            // Extract common metadata
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                metadata["duration"] = it
            }
            
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.let {
                metadata["width"] = it
            }
            
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.let {
                metadata["height"] = it
            }
            
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.let {
                metadata["bitrate"] = it
            }
            
            return metadata
            
        } finally {
            retriever.release()
        }
    }
}
