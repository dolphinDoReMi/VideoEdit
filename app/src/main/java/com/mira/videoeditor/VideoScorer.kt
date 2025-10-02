package com.mira.videoeditor

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Motion-based video segment scoring for automatic clip selection
 * Analyzes video segments for motion intensity using frame difference calculations
 */
class VideoScorer(private val ctx: Context) {
    
    companion object {
        private const val TAG = "VideoScorer"
        private const val FRAME_WIDTH = 96
        private const val FRAME_HEIGHT = 54
        private const val SAMPLING_STEP = 4
    }
    
    data class Segment(val startMs: Long, val endMs: Long, val score: Float)
    
    /**
     * Score video segments based on motion intensity
     * @param uri Video URI
     * @param segmentMs Segment length in milliseconds
     * @param maxDurationMs Maximum duration to analyze (0 = no limit)
     * @param onProgress Progress callback (0.0f to 1.0f)
     * @return List of scored segments
     */
    fun scoreSegments(uri: Uri, segmentMs: Long, maxDurationMs: Long, onProgress: (Float) -> Unit = {}): List<Segment> {
        Logger.info(Logger.Category.MOTION, "Starting segment scoring", mapOf(
            "uri" to uri.toString().takeLast(50),
            "segmentMs" to segmentMs,
            "maxDurationMs" to maxDurationMs
        ))
        
        val retriever = MediaMetadataRetriever()
        
        return try {
            retriever.setDataSource(ctx, uri)
            
            val totalMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L
            
            val useMs = if (maxDurationMs > 0) min(totalMs, maxDurationMs) else totalMs
            val segments = mutableListOf<Segment>()
            
            // Calculate total number of segments for progress tracking
            val totalSegments = (useMs / segmentMs).toInt() + if (useMs % segmentMs > 0) 1 else 0
            
            Logger.info(Logger.Category.MOTION, "Video analysis parameters", mapOf(
                "totalMs" to totalMs,
                "useMs" to useMs,
                "totalSegments" to totalSegments,
                "segmentMs" to segmentMs
            ))
            
            var currentTime = 0L
            var segmentIndex = 0
            while (currentTime < useMs) {
                val startTime = currentTime
                val endTime = min(currentTime + segmentMs, useMs)
                val score = calculateMotionScore(retriever, startTime, endTime)
                segments += Segment(startTime, endTime, score)
                currentTime += segmentMs
                
                // Report progress
                segmentIndex++
                val progress = segmentIndex.toFloat() / totalSegments.toFloat()
                Log.v(TAG, "Segment $segmentIndex/$totalSegments completed, progress: $progress")
                onProgress(progress.coerceIn(0f, 1f))
            }
            
            val averageScore = segments.map { it.score }.average()
            val topScore = segments.maxOfOrNull { it.score } ?: 0f
            
            Logger.logMotionAnalysis(segments.size, averageScore.toFloat(), topScore, mapOf(
                "totalMs" to totalMs,
                "segmentMs" to segmentMs
            ))
            
            segments
            
        } catch (e: Exception) {
            Logger.logError(Logger.Category.MOTION, "Segment scoring", e.message ?: "Unknown error", mapOf(
                "uri" to uri.toString().takeLast(50),
                "segmentMs" to segmentMs
            ), e)
            emptyList()
        } finally {
            retriever.release()
        }
    }
    
    /**
     * Score arbitrary intervals (e.g., detected shots) for motion intensity.
     * @param uri Video URI
     * @param intervals List of Pair(startMs, endMs)
     * @param onProgress Progress callback (0.0f to 1.0f)
     */
    fun scoreIntervals(
        uri: Uri,
        intervals: List<Pair<Long, Long>>,
        onProgress: (Float) -> Unit = {}
    ): List<Segment> {
        if (intervals.isEmpty()) return emptyList()
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(ctx, uri)
            val result = mutableListOf<Segment>()
            var idx = 0
            val total = intervals.size
            for ((startMs, endMs) in intervals) {
                val safeStart = max(0L, startMs)
                val safeEnd = max(safeStart + 1, endMs)
                val score = calculateMotionScore(retriever, safeStart, safeEnd)
                result += Segment(safeStart, safeEnd, score)
                idx++
                onProgress((idx.toFloat() / total.toFloat()).coerceIn(0f, 1f))
            }
            val averageScore = result.map { it.score }.average()
            val topScore = result.maxOfOrNull { it.score } ?: 0f
            
            Logger.logMotionAnalysis(result.size, averageScore.toFloat(), topScore, mapOf(
                "mode" to "intervals",
                "intervalCount" to intervals.size
            ))
            result
        } catch (e: Exception) {
            Logger.logError(Logger.Category.MOTION, "Interval scoring", e.message ?: "Unknown error", mapOf(
                "uri" to uri.toString().takeLast(50),
                "intervalCount" to intervals.size
            ), e)
            emptyList()
        } finally {
            retriever.release()
        }
    }
    
    /**
     * Calculate motion score for a segment by comparing frames
     */
    private fun calculateMotionScore(retriever: MediaMetadataRetriever, startMs: Long, endMs: Long): Float {
        try {
            // Extract frames at start, middle, and end of segment
            val frame1Time = startMs
            val frame2Time = (startMs + endMs) / 2
            val frame3Time = max(endMs - 33, startMs) // Prevent out of bounds
            
            val frame1 = retriever.getFrameAtTime(frame1Time * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: return 0f
            val frame2 = retriever.getFrameAtTime(frame2Time * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: frame1
            val frame3 = retriever.getFrameAtTime(frame3Time * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                ?: frame2
            
            // Calculate differences between consecutive frames
            val diff12 = calculateFrameDifference(frame1, frame2)
            val diff23 = calculateFrameDifference(frame2, frame3)
            
            // Average the differences for motion score
            val motionScore = ((diff12 + diff23) / 2f).coerceIn(0f, 1f)
            
            Log.v(TAG, "Motion score for segment [$startMs-$endMs]: $motionScore")
            return motionScore
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating motion score", e)
            return 0f
        }
    }
    
    /**
     * Calculate difference between two frames using grayscale histogram comparison
     */
    private fun calculateFrameDifference(frame1: Bitmap, frame2: Bitmap): Float {
        try {
            // Scale frames to small size for performance
            val scaled1 = Bitmap.createScaledBitmap(frame1, FRAME_WIDTH, FRAME_HEIGHT, false)
            val scaled2 = Bitmap.createScaledBitmap(frame2, FRAME_WIDTH, FRAME_HEIGHT, false)
            
            var totalDifference = 0L
            var sampleCount = 0
            
            // Sample pixels with step to reduce computation
            for (y in 0 until FRAME_HEIGHT step SAMPLING_STEP) {
                for (x in 0 until FRAME_WIDTH step SAMPLING_STEP) {
                    val pixel1 = scaled1.getPixel(x, y)
                    val pixel2 = scaled2.getPixel(x, y)
                    
                    // Convert to grayscale using luminance formula
                    val gray1 = calculateGrayscale(pixel1)
                    val gray2 = calculateGrayscale(pixel2)
                    
                    totalDifference += abs(gray1 - gray2)
                    sampleCount++
                }
            }
            
            // Normalize to 0-1 range
            val averageDifference = totalDifference.toFloat() / sampleCount
            return (averageDifference / 255f).coerceIn(0f, 1f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating frame difference", e)
            return 0f
        }
    }
    
    /**
     * Convert RGB pixel to grayscale using luminance formula
     */
    private fun calculateGrayscale(pixel: Int): Int {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        
        // Use standard luminance formula: 0.299*R + 0.587*G + 0.114*B
        return (red * 0.299 + green * 0.587 + blue * 0.114).toInt()
    }
}
