package com.mira.clip.autoclip

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import java.util.UUID

/**
 * Background service for Auto-Clipper operations
 * Handles the complete pipeline: Auto-Clipper -> Whisper Chain -> Results
 */
class AutoClipperService(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Start the complete Auto-Clipper pipeline
     * 1. Split large video into clips using MediaExtractor/MediaMuxer
     * 2. Chain Whisper processing for each clip
     * 3. Monitor progress and completion
     */
    fun startAutoClipPipeline(
        inputVideoUri: Uri,
        outputFolderUri: Uri,
        segmentSeconds: Int = 600, // 10 minutes
        whisperModel: String = "whisper-base.q5_1.bin",
        whisperThreads: Int = 4,
        whisperLanguage: String = "auto",
        whisperTranslate: Boolean = false
    ): WorkRequest {
        
        Log.d("AutoClipperService", "Starting Auto-Clipper pipeline for: $inputVideoUri")
        
        // Step 1: Auto-Clipper Worker
        val autoClipData = workDataOf(
            AutoClipperWorker.KEY_INPUT_URI to inputVideoUri.toString(),
            AutoClipperWorker.KEY_OUT_TREE_URI to outputFolderUri.toString(),
            AutoClipperWorker.KEY_SEG_SEC to segmentSeconds
        )
        
        val autoClipRequest = OneTimeWorkRequestBuilder<AutoClipperWorker>()
            .setInputData(autoClipData)
            .addTag("AutoClipper")
            .addTag("TennisInterview")
            .build()
        
        // Step 2: Whisper Chain Worker (depends on Auto-Clipper)
        val whisperChainData = workDataOf(
            WhisperChainWorker.KEY_MANIFEST_URI to "", // Will be set by Auto-Clipper output
            WhisperChainWorker.KEY_MODEL to whisperModel,
            WhisperChainWorker.KEY_THREADS to whisperThreads,
            WhisperChainWorker.KEY_LANGUAGE to whisperLanguage,
            WhisperChainWorker.KEY_TRANSLATE to whisperTranslate
        )
        
        val whisperChainRequest = OneTimeWorkRequestBuilder<WhisperChainWorker>()
            .setInputData(whisperChainData)
            .addTag("WhisperChain")
            .addTag("TennisInterview")
            .build()
        
        // Chain the workers: Auto-Clipper -> Whisper Chain
        val chain = workManager.beginWith(autoClipRequest)
            .then(whisperChainRequest)
            .enqueue()
        
        Log.d("AutoClipperService", "Auto-Clipper pipeline enqueued")
        
        return autoClipRequest
    }
    
    /**
     * Process TennisInterview.mp4 specifically
     * This is a convenience method for the specific use case
     */
    fun processTennisInterview(
        inputVideoUri: Uri,
        outputFolderUri: Uri
    ): WorkRequest {
        return startAutoClipPipeline(
            inputVideoUri = inputVideoUri,
            outputFolderUri = outputFolderUri,
            segmentSeconds = 600, // 10 minutes per clip
            whisperModel = "whisper-base.q5_1.bin",
            whisperThreads = 4,
            whisperLanguage = "auto",
            whisperTranslate = false
        )
    }
    
    /**
     * Monitor the progress of Auto-Clipper operations
     */
    fun observeProgress(workId: UUID, callback: (WorkInfo) -> Unit) {
        workManager.getWorkInfoByIdLiveData(workId).observeForever { workInfo ->
            callback(workInfo)
        }
    }
    
    /**
     * Get all Auto-Clipper related work status
     */
    fun getAutoClipStatus(): List<WorkInfo> {
        return workManager.getWorkInfosByTag("AutoClipper").get()
    }
    
    /**
     * Get all Whisper Chain work status
     */
    fun getWhisperChainStatus(): List<WorkInfo> {
        return workManager.getWorkInfosByTag("WhisperChain").get()
    }
    
    /**
     * Cancel all Auto-Clipper operations
     */
    fun cancelAllAutoClipOperations() {
        workManager.cancelAllWorkByTag("AutoClipper")
        workManager.cancelAllWorkByTag("WhisperChain")
        workManager.cancelAllWorkByTag("TennisInterview")
        Log.d("AutoClipperService", "Cancelled all Auto-Clipper operations")
    }
    
    /**
     * Get completion status for TennisInterview processing
     */
    fun getTennisInterviewStatus(): TennisInterviewStatus {
        val autoClipWork = workManager.getWorkInfosByTag("AutoClipper").get()
        val whisperWork = workManager.getWorkInfosByTag("WhisperChain").get()
        
        val autoClipCompleted = autoClipWork.any { it.state.isFinished }
        val whisperCompleted = whisperWork.any { it.state.isFinished }
        
        val totalClips = autoClipWork.firstOrNull()?.outputData?.getInt("clips", 0) ?: 0
        val completedClips = whisperWork.count { it.state == WorkInfo.State.SUCCEEDED }
        
        return TennisInterviewStatus(
            autoClipCompleted = autoClipCompleted,
            whisperCompleted = whisperCompleted,
            totalClips = totalClips,
            completedClips = completedClips,
            isComplete = autoClipCompleted && whisperCompleted && completedClips == totalClips
        )
    }
    
    data class TennisInterviewStatus(
        val autoClipCompleted: Boolean,
        val whisperCompleted: Boolean,
        val totalClips: Int,
        val completedClips: Int,
        val isComplete: Boolean
    ) {
        val progressPercentage: Int
            get() = if (totalClips > 0) (completedClips * 100) / totalClips else 0
    }
}
