package com.mira.com.feature.whisper.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mira.com.feature.whisper.data.io.MediaConverter
import com.mira.com.core.infra.GlobalFileManagementService

/**
 * Simple media conversion manager that coordinates H.264 conversions.
 * Provides a unified interface for scheduling and managing conversions.
 */
class MediaConversionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MediaConversionManager"
        
        @Volatile
        private var INSTANCE: MediaConversionManager? = null

        fun getInstance(context: Context): MediaConversionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaConversionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Convert a single file to H.264 in the background.
     */
    fun convertFile(
        inputUri: Uri,
        outputFileName: String? = null,
        removeOriginal: Boolean = false,
        onComplete: (Uri?) -> Unit = {}
    ) {
        Log.d(TAG, "TECHNICAL: Scheduling H.264 conversion for $inputUri")
        BackgroundMediaConverter.convertFile(context, inputUri, outputFileName, removeOriginal, onComplete)
    }

    /**
     * Convert multiple files to H.264 in the background.
     */
    fun convertFiles(
        inputUris: List<Uri>,
        outputFileNames: List<String?> = emptyList(),
        removeOriginal: Boolean = false,
        onComplete: (List<Uri?>) -> Unit = {}
    ) {
        Log.d(TAG, "TECHNICAL: Scheduling batch H.264 conversion for ${inputUris.size} files")
        BackgroundMediaConverter.convertFiles(context, inputUris, outputFileNames, removeOriginal, onComplete)
    }

    /**
     * Check if a file needs H.264 conversion.
     */
    fun needsConversion(uri: Uri): Boolean {
        return MediaConverter.needsConversion(uri)
    }

    /**
     * Clean up old converted files.
     */
    fun cleanupOldFiles() {
        Log.d(TAG, "TECHNICAL: Cleaning up old converted files")
        BackgroundMediaConverter.cleanupOldFiles(context)
    }
    
    /**
     * Perform comprehensive global cleanup of all converted files and temporary data.
     * This removes all files regardless of age and provides detailed results.
     * Now includes hash-based duplicate detection and removal.
     */
    fun globalCleanup(): GlobalFileManagementService.GlobalCleanupResult {
        Log.d(TAG, "TECHNICAL: Starting global cleanup with hash-based duplicate detection")
        return GlobalFileManagementService.globalCleanup(context)
    }
    
    /**
     * Find duplicate files based on SHA-256 hash comparison.
     */
    fun findDuplicateFilesByHash(directory: java.io.File): Map<String, List<java.io.File>> {
        Log.d(TAG, "TECHNICAL: Finding duplicate files by hash in ${directory.absolutePath}")
        return MediaConverter.findDuplicateFilesByHash(directory)
    }
    
    /**
     * Remove duplicate files based on hash comparison, keeping the most recent file.
     */
    fun removeDuplicateFilesByHash(directory: java.io.File): GlobalFileManagementService.DuplicateRemovalResult {
        Log.d(TAG, "TECHNICAL: Removing duplicate files by hash in ${directory.absolutePath}")
        return GlobalFileManagementService.removeDuplicateFilesByHash(directory)
    }
    
    /**
     * Calculate SHA-256 hash of a file for duplicate detection.
     */
    fun calculateFileHash(file: java.io.File): String? {
        Log.d(TAG, "TECHNICAL: Calculating hash for ${file.name}")
        return MediaConverter.calculateFileHash(file)
    }

    /**
     * Convert TennisInterview.mkv specifically (for testing).
     */
    fun convertTennisInterview(removeOriginal: Boolean = false, onComplete: (Uri?) -> Unit = {}) {
        Log.d(TAG, "TECHNICAL: Converting TennisInterview.mkv to H.264")
        BackgroundMediaConverter.convertTennisInterview(context, removeOriginal, onComplete)
    }
    
    /**
     * Remove original files from a batch of conversions.
     */
    fun removeOriginalFilesBatch(
        inputUris: List<Uri>,
        onComplete: (GlobalFileManagementService.BatchRemovalResult) -> Unit = {}
    ) {
        Log.d(TAG, "TECHNICAL: Scheduling batch original file removal for ${inputUris.size} files")
        BackgroundMediaConverter.removeOriginalFilesBatch(context, inputUris, onComplete)
    }
    
    /**
     * Remove a single original file after conversion.
     */
    fun removeOriginalFile(inputUri: Uri): Boolean {
        Log.d(TAG, "TECHNICAL: Removing original file: $inputUri")
        return MediaConverter.removeOriginalFile(context, inputUri)
    }
}