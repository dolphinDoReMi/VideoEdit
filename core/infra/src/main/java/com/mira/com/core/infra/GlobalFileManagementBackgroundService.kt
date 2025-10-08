package com.mira.com.core.infra

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.concurrent.Executors

/**
 * Background service wrapper for global file management operations.
 * Provides asynchronous execution of all file management operations.
 */
class GlobalFileManagementBackgroundService {
    
    companion object {
        private const val TAG = "GlobalFileManagementBackground"
        private val executor = Executors.newSingleThreadExecutor()
        
        // ==================== HASH-BASED DUPLICATE DETECTION ====================
        
        /**
         * Perform hash-based duplicate detection and removal in the background.
         */
        fun removeDuplicateFilesByHash(
            directory: File,
            onComplete: (GlobalFileManagementService.DuplicateRemovalResult) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background hash-based duplicate detection and removal")
                val result = GlobalFileManagementService.removeDuplicateFilesByHash(directory)
                Log.d(TAG, "TECHNICAL: Background hash-based duplicate removal completed: ${result.filesRemoved} files removed, ${result.spaceSaved} bytes saved")
                onComplete(result)
            }
        }
        
        /**
         * Find duplicate files by hash in the background.
         */
        fun findDuplicateFilesByHash(
            directory: File,
            onComplete: (Map<String, List<File>>) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background hash-based duplicate detection")
                val duplicates = GlobalFileManagementService.findDuplicateFilesByHash(directory)
                Log.d(TAG, "TECHNICAL: Background hash-based duplicate detection completed: ${duplicates.size} duplicate sets found")
                onComplete(duplicates)
            }
        }
        
        /**
         * Calculate file hash in the background.
         */
        fun calculateFileHash(
            file: File,
            onComplete: (String?) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background hash calculation for ${file.name}")
                val hash = GlobalFileManagementService.calculateFileHash(file)
                Log.d(TAG, "TECHNICAL: Background hash calculation completed for ${file.name}: ${hash?.take(8)}...")
                onComplete(hash)
            }
        }
        
        // ==================== FILENAME-BASED DUPLICATE DETECTION ====================
        
        /**
         * Find existing converted file in the background.
         */
        fun findExistingConvertedFile(
            context: Context,
            inputUri: Uri,
            outputFileName: String? = null,
            onComplete: (Uri?) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background existing file detection")
                val result = GlobalFileManagementService.findExistingConvertedFile(context, inputUri, outputFileName)
                Log.d(TAG, "TECHNICAL: Background existing file detection completed: ${result != null}")
                onComplete(result)
            }
        }
        
        // ==================== ORIGINAL FILE REMOVAL ====================
        
        /**
         * Remove original file in the background.
         */
        fun removeOriginalFile(
            context: Context,
            inputUri: Uri,
            onComplete: (Boolean) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background original file removal")
                val result = GlobalFileManagementService.removeOriginalFile(context, inputUri)
                Log.d(TAG, "TECHNICAL: Background original file removal completed: $result")
                onComplete(result)
            }
        }
        
        /**
         * Remove original files from a batch in the background.
         */
        fun removeOriginalFilesBatch(
            context: Context,
            inputUris: List<Uri>,
            onComplete: (GlobalFileManagementService.BatchRemovalResult) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background batch original file removal")
                val result = GlobalFileManagementService.removeOriginalFilesBatch(context, inputUris)
                Log.d(TAG, "TECHNICAL: Background batch original file removal completed: ${result.successfulRemovals}/${result.totalProcessed} successful")
                onComplete(result)
            }
        }
        
        // ==================== GLOBAL CLEANUP OPERATIONS ====================
        
        /**
         * Clean up old files in the background.
         */
        fun cleanupOldFiles(
            context: Context,
            maxAgeHours: Long = 24
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background cleanup of old files")
                GlobalFileManagementService.cleanupOldFiles(context, maxAgeHours)
                Log.d(TAG, "TECHNICAL: Background cleanup of old files completed")
            }
        }
        
        /**
         * Perform comprehensive global cleanup in the background.
         * Now includes hash-based duplicate detection and removal.
         */
        fun globalCleanup(
            context: Context,
            onComplete: (GlobalFileManagementService.GlobalCleanupResult) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background global cleanup with hash-based duplicate detection")
                val result = GlobalFileManagementService.globalCleanup(context)
                Log.d(TAG, "TECHNICAL: Background global cleanup completed: ${result.totalFilesDeleted} files deleted, ${result.totalSpaceSaved} bytes saved")
                Log.d(TAG, "TECHNICAL: Hash-based duplicates removed: ${result.duplicateFilesRemoved} files, ${result.duplicateSpaceSaved} bytes saved")
                onComplete(result)
            }
        }
        
        // ==================== COMPREHENSIVE FILE MANAGEMENT ====================
        
        /**
         * Perform comprehensive file management operations in sequence.
         * This includes duplicate detection, original file removal, and global cleanup.
         */
        fun performComprehensiveFileManagement(
            context: Context,
            directories: List<File> = emptyList(),
            onComplete: (ComprehensiveFileManagementResult) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting comprehensive file management operations")
                val result = ComprehensiveFileManagementResult()
                
                try {
                    // Step 1: Hash-based duplicate detection and removal
                    Log.d(TAG, "TECHNICAL: Step 1: Hash-based duplicate detection")
                    directories.forEach { directory ->
                        if (directory.exists() && directory.isDirectory) {
                            val duplicateResult = GlobalFileManagementService.removeDuplicateFilesByHash(directory)
                            result.totalDuplicateFilesRemoved += duplicateResult.filesRemoved
                            result.totalDuplicateSpaceSaved += duplicateResult.spaceSaved
                            result.errors.addAll(duplicateResult.errors)
                        }
                    }
                    
                    // Step 2: Global cleanup
                    Log.d(TAG, "TECHNICAL: Step 2: Global cleanup")
                    val cleanupResult = GlobalFileManagementService.globalCleanup(context)
                    result.totalFilesDeleted += cleanupResult.totalFilesDeleted
                    result.totalSpaceSaved += cleanupResult.totalSpaceSaved
                    result.errors.addAll(cleanupResult.errors)
                    
                    Log.d(TAG, "TECHNICAL: Comprehensive file management completed successfully")
                    result.success = true
                    
                } catch (e: Exception) {
                    Log.e(TAG, "TECHNICAL: Error during comprehensive file management: ${e.message}", e)
                    result.errors.add("Comprehensive file management error: ${e.message}")
                    result.success = false
                }
                
                Log.d(TAG, "TECHNICAL: Comprehensive file management result: ${result.totalFilesDeleted} files deleted, ${result.totalSpaceSaved} bytes saved")
                onComplete(result)
            }
        }
        
        /**
         * Result object for comprehensive file management operations.
         */
        data class ComprehensiveFileManagementResult(
            var success: Boolean = false,
            var totalFilesDeleted: Int = 0,
            var totalSpaceSaved: Long = 0,
            var totalDuplicateFilesRemoved: Int = 0,
            var totalDuplicateSpaceSaved: Long = 0,
            var errors: MutableList<String> = mutableListOf()
        ) {
            val spaceSavedInGB: Double
                get() = totalSpaceSaved / (1024.0 * 1024.0 * 1024.0)
            
            val duplicateSpaceSavedInGB: Double
                get() = totalDuplicateSpaceSaved / (1024.0 * 1024.0 * 1024.0)
        }
    }
}
