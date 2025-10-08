package com.mira.com.core.infra

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.Executors

/**
 * Background service wrapper for hash-based duplicate detection.
 * Provides asynchronous execution of duplicate detection operations.
 */
class HashBasedDuplicateDetectionBackgroundService {
    
    companion object {
        private const val TAG = "HashDuplicateBackground"
        private val executor = Executors.newSingleThreadExecutor()
        
        /**
         * Perform hash-based duplicate detection and removal in the background.
         */
        fun removeDuplicateFilesByHash(
            directory: File,
            onComplete: (HashBasedDuplicateDetectionService.DuplicateRemovalResult) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background hash-based duplicate detection and removal")
                val result = HashBasedDuplicateDetectionService.removeDuplicateFilesByHash(directory)
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
                val duplicates = HashBasedDuplicateDetectionService.findDuplicateFilesByHash(directory)
                Log.d(TAG, "TECHNICAL: Background hash-based duplicate detection completed: ${duplicates.size} duplicate sets found")
                onComplete(duplicates)
            }
        }
        
        /**
         * Perform global hash-based duplicate removal across multiple directories in the background.
         */
        fun removeDuplicateFilesFromDirectories(
            directories: List<File>,
            onComplete: (HashBasedDuplicateDetectionService.GlobalDuplicateRemovalResult) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background global hash-based duplicate removal")
                val result = HashBasedDuplicateDetectionService.removeDuplicateFilesFromDirectories(directories)
                Log.d(TAG, "TECHNICAL: Background global hash-based duplicate removal completed: ${result.totalFilesRemoved} files removed, ${result.totalSpaceSaved} bytes saved")
                onComplete(result)
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
                val hash = HashBasedDuplicateDetectionService.calculateFileHash(file)
                Log.d(TAG, "TECHNICAL: Background hash calculation completed for ${file.name}: ${hash?.take(8)}...")
                onComplete(hash)
            }
        }
    }
}
