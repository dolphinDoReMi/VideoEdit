package com.mira.videoeditor

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.min

/**
 * Main engine for automatic video cutting and export using Media3 Transformer
 */
class AutoCutEngine(
    private val ctx: Context,
    private val onProgress: (Float) -> Unit = {}
) {
    
    companion object {
        private const val TAG = "AutoCutEngine"
        private const val DEFAULT_TARGET_DURATION_MS = 30_000L
        private const val DEFAULT_SEGMENT_MS = 2_000L
        private const val MIN_SEGMENTS_FOR_FALLBACK = 3
    }
    
    /**
     * Automatically select clips and export edited video
     * @param input Input video URI
     * @param outputPath Output file path
     * @param targetDurationMs Target duration for final video
     * @param segmentMs Length of each segment to analyze
     */
    suspend fun autoCutAndExport(
        input: Uri,
        outputPath: String,
        targetDurationMs: Long = DEFAULT_TARGET_DURATION_MS,
        segmentMs: Long = DEFAULT_SEGMENT_MS
    ) {
        Log.d(TAG, "Starting auto cut and export")
        Log.d(TAG, "Input: $input")
        Log.d(TAG, "Output: $outputPath")
        Log.d(TAG, "Target duration: ${targetDurationMs}ms")
        Log.d(TAG, "Segment length: ${segmentMs}ms")
        
        try {
            // Step 1: Analyze and score video segments
            onProgress(0.05f)
            val scorer = VideoScorer(ctx)
            val candidateSegments = scorer.scoreSegments(
                uri = input,
                segmentMs = segmentMs,
                maxDurationMs = 0L, // Analyze entire video
                onProgress = { motionProgress ->
                    // Map motion analysis progress (0-1) to overall progress (0.05-0.15)
                    val overallProgress = 0.05f + (motionProgress * 0.10f)
                    Log.d(TAG, "VideoScorer progress: $motionProgress -> overall: $overallProgress")
                    onProgress(overallProgress)
                }
            )
            
            if (candidateSegments.isEmpty()) {
                throw IllegalStateException("No segments could be analyzed from the video")
            }
            
            Log.d(TAG, "Analyzed ${candidateSegments.size} segments")
            onProgress(0.15f)
            
            // Step 2: Select best segments based on motion scores
            val selectedSegments = selectBestSegments(
                candidateSegments, 
                targetDurationMs
            )
            
            Log.d(TAG, "Selected ${selectedSegments.size} segments for export")
            onProgress(0.25f)
            
            // Step 3: Create Media3 composition and export
            exportVideo(input, outputPath, selectedSegments)
            
            Log.d(TAG, "Export completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during auto cut and export", e)
            throw e
        }
    }
    
    /**
     * Select the best segments based on motion scores
     */
    private fun selectBestSegments(
        candidateSegments: List<VideoScorer.Segment>,
        targetDurationMs: Long
    ): List<VideoScorer.Segment> {
        
        // Sort segments by score (highest first)
        val sortedSegments = candidateSegments.sortedByDescending { it.score }
        
        val selectedSegments = mutableListOf<VideoScorer.Segment>()
        var accumulatedDuration = 0L
        
        // Select segments until we reach target duration
        for (segment in sortedSegments) {
            val segmentDuration = segment.endMs - segment.startMs
            
            // Skip if adding this segment would exceed target duration
            if (accumulatedDuration + segmentDuration > targetDurationMs) {
                continue
            }
            
            selectedSegments += segment
            accumulatedDuration += segmentDuration
            
            // Stop if we've reached 90% of target duration
            if (accumulatedDuration >= targetDurationMs * 0.9) {
                break
            }
        }
        
        // Fallback: if no segments selected or too few, take first segments
        if (selectedSegments.size < MIN_SEGMENTS_FOR_FALLBACK) {
            Log.w(TAG, "Using fallback segment selection")
            return createFallbackSegments(candidateSegments, targetDurationMs)
        }
        
        Log.d(TAG, "Selected ${selectedSegments.size} segments with total duration: ${accumulatedDuration}ms")
        return selectedSegments
    }
    
    /**
     * Create fallback segments if motion-based selection fails
     */
    private fun createFallbackSegments(
        candidateSegments: List<VideoScorer.Segment>,
        targetDurationMs: Long
    ): List<VideoScorer.Segment> {
        
        val fallbackSegments = mutableListOf<VideoScorer.Segment>()
        var currentTime = 0L
        
        while (currentTime < targetDurationMs && fallbackSegments.size < candidateSegments.size) {
            val segment = candidateSegments[fallbackSegments.size]
            val segmentDuration = segment.endMs - segment.startMs
            
            if (currentTime + segmentDuration <= targetDurationMs) {
                fallbackSegments += segment
                currentTime += segmentDuration
            } else {
                break
            }
        }
        
        Log.d(TAG, "Created ${fallbackSegments.size} fallback segments")
        return fallbackSegments
    }
    
    /**
     * Export video using Media3 Transformer
     */
    private suspend fun exportVideo(
        inputUri: Uri,
        outputPath: String,
        segments: List<VideoScorer.Segment>
    ) {
        
        Log.d(TAG, "Creating Media3 composition with ${segments.size} segments")
        
        // Create edited media items for each segment
        val editedMediaItems = segments.map { segment ->
            val mediaItem = MediaItem.Builder()
                .setUri(inputUri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(segment.startMs)
                        .setEndPositionMs(segment.endMs)
                        .build()
                )
                .build()
            
            EditedMediaItem.Builder(mediaItem)
                .build()
        }
        
        // Create composition with sequence
        val sequence = EditedMediaItemSequence(editedMediaItems)
        val composition = Composition.Builder(sequence).build()
        
        // Create transformer
        val transformer = Transformer.Builder(ctx)
            .setVideoMimeType(MimeTypes.VIDEO_H264) // Universal compatibility
            .build()
        
        Log.d(TAG, "Starting Media3 export to: $outputPath")
        
        // Export video
        suspendCancellableCoroutine<Unit> { continuation ->
            transformer.start(composition, outputPath)
            
            transformer.addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    Log.d(TAG, "Export completed successfully")
                    onProgress(1.0f)
                    if (!continuation.isCompleted) {
                        continuation.resume(Unit)
                    }
                }
                
                override fun onError(
                    composition: Composition, 
                    result: ExportResult, 
                    exception: ExportException
                ) {
                    Log.e(TAG, "Export failed", exception)
                    if (!continuation.isCompleted) {
                        continuation.resume(Unit)
                    }
                }
            })
            
            // Handle cancellation
            continuation.invokeOnCancellation {
                transformer.cancel()
            }
        }
    }
    
    /**
     * Create progress listener for export updates
     */
    private fun createProgressListener(): Transformer.Listener {
        return object : Transformer.Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                // Handled in main listener
            }
            
            override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                // Handled in main listener
            }
        }
    }
}