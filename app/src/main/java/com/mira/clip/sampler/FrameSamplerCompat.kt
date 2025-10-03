package com.mira.clip.sampler

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File

/**
 * Compatibility wrapper for the old FrameSampler API
 * Provides backward compatibility for existing code
 */
object FrameSamplerCompat {
    
    /**
     * Sample frames uniformly from a video file (legacy API compatibility)
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
            
            // Calculate frame timestamps using new policies
            val timestamps = TimestampPolicies.uniform(duration, count)
            
            // Extract frames
            val frames = mutableListOf<Bitmap>()
            
            for (timestamp in timestamps) {
                val frame = retriever.getFrameAtTime(timestamp * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
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
     * Get video metadata (legacy API compatibility)
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
