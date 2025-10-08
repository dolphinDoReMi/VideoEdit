package com.mira.com.feature.whisper.service

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import com.mira.com.feature.whisper.data.io.MediaConverter
import com.mira.com.core.infra.GlobalFileManagementBackgroundService
import java.util.concurrent.Executors

/**
 * Simple background service for automatic H.264 conversion of media files.
 * Runs without UI interaction using coroutines instead of WorkManager.
 */
class BackgroundMediaConverter {

    companion object {
        private const val TAG = "BackgroundMediaConverter"
        private val executor = Executors.newSingleThreadExecutor()
        
        /**
         * Convert a file to H.264 in the background using coroutines.
         */
        fun convertFile(
            context: Context,
            inputUri: Uri,
            outputFileName: String? = null,
            removeOriginal: Boolean = false,
            onComplete: (Uri?) -> Unit = {}
        ) {
            executor.execute {
                try {
                    Log.d(TAG, "TECHNICAL: Starting background H.264 conversion for $inputUri")
                    
                    // Check if conversion is needed
                    if (!MediaConverter.needsConversion(inputUri)) {
                        Log.d(TAG, "TECHNICAL: File already in H.264 format, no conversion needed")
                        onComplete(inputUri)
                        return@execute
                    }
                    
                    // Check if converted version already exists
                    val existingConvertedFile = MediaConverter.findExistingConvertedFile(context, inputUri, outputFileName)
                    if (existingConvertedFile != null) {
                        Log.d(TAG, "TECHNICAL: Found existing converted file, skipping conversion")
                        
                        // If removeOriginal is true and we found an existing converted file, 
                        // we can still remove the original file
                        if (removeOriginal) {
                            MediaConverter.removeOriginalFile(context, inputUri)
                        }
                        
                        onComplete(existingConvertedFile)
                        return@execute
                    }

                    // Perform conversion
                    val convertedUri = MediaConverter.convertToH264(context, inputUri, outputFileName, removeOriginal)
                    
                    if (convertedUri != null) {
                        Log.d(TAG, "TECHNICAL: Background H.264 conversion successful: $convertedUri")
                        onComplete(convertedUri)
                    } else {
                        Log.e(TAG, "TECHNICAL: Background H.264 conversion failed for $inputUri")
                        onComplete(null)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "TECHNICAL: Background conversion error: ${e.message}", e)
                    onComplete(null)
                }
            }
        }

        /**
         * Convert multiple files to H.264 in the background.
         */
        fun convertFiles(
            context: Context,
            inputUris: List<Uri>,
            outputFileNames: List<String?> = emptyList(),
            removeOriginal: Boolean = false,
            onComplete: (List<Uri?>) -> Unit = {}
        ) {
            executor.execute {
                val results = mutableListOf<Uri?>()
                
                inputUris.forEachIndexed { index, uri ->
                    val outputFileName = outputFileNames.getOrNull(index)
                    Log.d(TAG, "TECHNICAL: Converting file ${index + 1}/${inputUris.size}: $uri")
                    
                    try {
                        if (!MediaConverter.needsConversion(uri)) {
                            Log.d(TAG, "TECHNICAL: File already in H.264 format: $uri")
                            results.add(uri)
                        } else {
                            // Check if converted version already exists
                            val existingConvertedFile = MediaConverter.findExistingConvertedFile(context, uri, outputFileName)
                            if (existingConvertedFile != null) {
                                Log.d(TAG, "TECHNICAL: Found existing converted file for batch: $uri")
                                
                                // If removeOriginal is true and we found an existing converted file, 
                                // we can still remove the original file
                                if (removeOriginal) {
                                    MediaConverter.removeOriginalFile(context, uri)
                                }
                                
                                results.add(existingConvertedFile)
                            } else {
                                val convertedUri = MediaConverter.convertToH264(context, uri, outputFileName, removeOriginal)
                                results.add(convertedUri)
                                
                                if (convertedUri != null) {
                                    Log.d(TAG, "TECHNICAL: Conversion successful: $uri -> $convertedUri")
                                } else {
                                    Log.e(TAG, "TECHNICAL: Conversion failed: $uri")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "TECHNICAL: Error converting $uri: ${e.message}", e)
                        results.add(null)
                    }
                }
                
                Log.d(TAG, "TECHNICAL: Batch conversion completed: ${results.count { it != null }}/${results.size} successful")
                onComplete(results)
            }
        }

        /**
         * Convert TennisInterview.mkv specifically (for testing).
         */
        fun convertTennisInterview(
            context: Context,
            removeOriginal: Boolean = false,
            onComplete: (Uri?) -> Unit = {}
        ) {
            val tennisInterviewUri = Uri.parse("content://media/picker_get_content/0/com.android.providers.media.photopicker/media/1000000816")
            Log.d(TAG, "TECHNICAL: Converting TennisInterview.mkv to H.264")
            convertFile(context, tennisInterviewUri, "TennisInterview_converted.mp4", removeOriginal, onComplete)
        }
        
        /**
         * Remove original files from a batch of conversions in the background.
         */
        fun removeOriginalFilesBatch(
            context: Context,
            inputUris: List<Uri>,
            onComplete: (com.mira.com.core.infra.GlobalFileManagementService.BatchRemovalResult) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background batch original file removal")
                val result = MediaConverter.removeOriginalFilesBatch(context, inputUris)
                Log.d(TAG, "TECHNICAL: Background batch removal completed: ${result.successfulRemovals}/${result.totalProcessed} successful")
                onComplete(result)
            }
        }

        /**
         * Clean up old converted files.
         */
        fun cleanupOldFiles(context: Context) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Cleaning up old converted files")
                MediaConverter.cleanupOldFiles(context)
            }
        }
        
        /**
         * Perform comprehensive global cleanup in the background.
         * Now includes hash-based duplicate detection and removal.
         */
        fun globalCleanup(
            context: Context,
            onComplete: (com.mira.com.core.infra.GlobalFileManagementService.GlobalCleanupResult) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background global cleanup with hash-based duplicate detection")
                val result = com.mira.com.core.infra.GlobalFileManagementService.globalCleanup(context)
                Log.d(TAG, "TECHNICAL: Background global cleanup completed: ${result.totalFilesDeleted} files deleted, ${result.totalSpaceSaved} bytes saved")
                Log.d(TAG, "TECHNICAL: Hash-based duplicates removed: ${result.duplicateFilesRemoved} files, ${result.duplicateSpaceSaved} bytes saved")
                onComplete(result)
            }
        }
        
        /**
         * Perform hash-based duplicate detection and removal in the background.
         */
        fun removeDuplicateFilesByHash(
            context: Context,
            directory: java.io.File,
            onComplete: (com.mira.com.core.infra.GlobalFileManagementService.DuplicateRemovalResult) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background hash-based duplicate detection and removal")
                val result = com.mira.com.core.infra.GlobalFileManagementService.removeDuplicateFilesByHash(directory)
                Log.d(TAG, "TECHNICAL: Background hash-based duplicate removal completed: ${result.filesRemoved} files removed, ${result.spaceSaved} bytes saved")
                onComplete(result)
            }
        }
        
        /**
         * Find duplicate files by hash in the background.
         */
        fun findDuplicateFilesByHash(
            directory: java.io.File,
            onComplete: (Map<String, List<java.io.File>>) -> Unit = {}
        ) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Starting background hash-based duplicate detection")
                val duplicates = MediaConverter.findDuplicateFilesByHash(directory)
                Log.d(TAG, "TECHNICAL: Background hash-based duplicate detection completed: ${duplicates.size} duplicate sets found")
                onComplete(duplicates)
            }
        }
    }
}