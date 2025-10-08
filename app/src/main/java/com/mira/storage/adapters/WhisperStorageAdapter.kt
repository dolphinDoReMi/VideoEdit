package com.mira.storage.adapters

import android.content.Context
import android.util.Log
import com.mira.storage.ScopedStorageManager
import java.io.File

/**
 * Whisper Service Storage Adapter
 * 
 * Provides Whisper-specific storage access through the unified scoped storage infrastructure.
 * All Whisper services MUST use this adapter instead of hardcoded paths.
 */
object WhisperStorageAdapter {
    
    private const val TAG = "WhisperStorageAdapter"
    
    /**
     * Get the default Whisper model path
     */
    fun getDefaultModelPath(context: Context): String {
        val modelFile = File(ScopedStorageManager.getWhisperModelsDir(context), "whisper-base.q5_1.bin")
        return modelFile.absolutePath
    }
    
    /**
     * Get the Whisper output directory path
     */
    fun getOutputDirPath(context: Context): String {
        return ScopedStorageManager.getWhisperOutputDir(context).absolutePath
    }
    
    /**
     * Get the Whisper sidecar directory path
     */
    fun getSidecarDirPath(context: Context): String {
        return ScopedStorageManager.getWhisperSidecarDir(context).absolutePath
    }
    
    /**
     * Get job-specific output directory
     */
    fun getJobOutputDir(context: Context, jobId: String): File {
        return ScopedStorageManager.getJobOutputDir(context, "whisper", jobId)
    }
    
    /**
     * Get job-specific sidecar file
     */
    fun getJobSidecarFile(context: Context, jobId: String): File {
        return File(ScopedStorageManager.getWhisperSidecarDir(context), "$jobId.json")
    }
    
    /**
     * Get processing directory for temporary files
     */
    fun getProcessingDir(context: Context): File {
        return ScopedStorageManager.getWhisperProcessingDir(context)
    }
    
    /**
     * Get exports directory for final outputs
     */
    fun getExportsDir(context: Context): File {
        return ScopedStorageManager.getWhisperExportsDir(context)
    }
    
    /**
     * Get logs directory for Whisper processing logs
     */
    fun getLogsDir(context: Context): File {
        return ScopedStorageManager.getWhisperLogsDir(context)
    }
    
    /**
     * Ensure Whisper directory structure exists
     */
    fun ensureDirectories(context: Context): Boolean {
        return ScopedStorageManager.ensureDirectoryStructure(context)
    }
    
    /**
     * Get Whisper storage info for debugging
     */
    fun getStorageInfo(context: Context): Map<String, String> {
        return mapOf(
            "serviceDir" to ScopedStorageManager.getWhisperServiceDir(context).absolutePath,
            "inputDir" to ScopedStorageManager.getWhisperInputDir(context).absolutePath,
            "outputDir" to ScopedStorageManager.getWhisperOutputDir(context).absolutePath,
            "modelsDir" to ScopedStorageManager.getWhisperModelsDir(context).absolutePath,
            "sidecarDir" to ScopedStorageManager.getWhisperSidecarDir(context).absolutePath,
            "processingDir" to ScopedStorageManager.getWhisperProcessingDir(context).absolutePath,
            "exportsDir" to ScopedStorageManager.getWhisperExportsDir(context).absolutePath,
            "logsDir" to ScopedStorageManager.getWhisperLogsDir(context).absolutePath,
            "defaultModelPath" to getDefaultModelPath(context)
        )
    }
}
