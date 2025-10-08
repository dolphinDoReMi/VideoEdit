package com.mira.storage.adapters

import android.content.Context
import android.util.Log
import com.mira.storage.ScopedStorageManager
import java.io.File

/**
 * AutoClipper Service Storage Adapter
 * 
 * Provides AutoClipper-specific storage access through the unified scoped storage infrastructure.
 * All AutoClipper services MUST use this adapter instead of hardcoded paths.
 */
object AutoClipStorageAdapter {
    
    private const val TAG = "AutoClipStorageAdapter"
    
    /**
     * Get the AutoClipper input directory path
     */
    fun getInputDirPath(context: Context): String {
        return ScopedStorageManager.getAutoClipInputDir(context).absolutePath
    }
    
    /**
     * Get the AutoClipper output directory path
     */
    fun getOutputDirPath(context: Context): String {
        return ScopedStorageManager.getAutoClipOutputDir(context).absolutePath
    }
    
    /**
     * Get the AutoClipper processing directory path
     */
    fun getProcessingDirPath(context: Context): String {
        return ScopedStorageManager.getAutoClipProcessingDir(context).absolutePath
    }
    
    /**
     * Get the AutoClipper chunks directory
     */
    fun getChunksDir(context: Context): File {
        return ScopedStorageManager.getAutoClipChunksDir(context)
    }
    
    /**
     * Get the AutoClipper intermediate directory
     */
    fun getIntermediateDir(context: Context): File {
        return ScopedStorageManager.getAutoClipIntermediateDir(context)
    }
    
    /**
     * Get job-specific output directory
     */
    fun getJobOutputDir(context: Context, jobId: String): File {
        return ScopedStorageManager.getJobOutputDir(context, "autoclip", jobId)
    }
    
    /**
     * Get chunk file for a specific segment
     */
    fun getChunkFile(context: Context, jobId: String, segmentIndex: Int): File {
        val chunksDir = getChunksDir(context)
        val jobDir = File(chunksDir, jobId)
        return File(jobDir, "chunk_${segmentIndex.toString().padStart(3, '0')}.mp4")
    }
    
    /**
     * Get intermediate processing file
     */
    fun getIntermediateFile(context: Context, jobId: String, filename: String): File {
        val intermediateDir = getIntermediateDir(context)
        val jobDir = File(intermediateDir, jobId)
        return File(jobDir, filename)
    }
    
    /**
     * Get logs directory for AutoClipper processing logs
     */
    fun getLogsDir(context: Context): File {
        return ScopedStorageManager.getAutoClipLogsDir(context)
    }
    
    /**
     * Ensure AutoClipper directory structure exists
     */
    fun ensureDirectories(context: Context): Boolean {
        return ScopedStorageManager.ensureDirectoryStructure(context)
    }
    
    /**
     * Get AutoClipper storage info for debugging
     */
    fun getStorageInfo(context: Context): Map<String, String> {
        return mapOf(
            "serviceDir" to ScopedStorageManager.getAutoClipServiceDir(context).absolutePath,
            "inputDir" to ScopedStorageManager.getAutoClipInputDir(context).absolutePath,
            "outputDir" to ScopedStorageManager.getAutoClipOutputDir(context).absolutePath,
            "processingDir" to ScopedStorageManager.getAutoClipProcessingDir(context).absolutePath,
            "chunksDir" to ScopedStorageManager.getAutoClipChunksDir(context).absolutePath,
            "intermediateDir" to ScopedStorageManager.getAutoClipIntermediateDir(context).absolutePath,
            "logsDir" to ScopedStorageManager.getAutoClipLogsDir(context).absolutePath
        )
    }
}
