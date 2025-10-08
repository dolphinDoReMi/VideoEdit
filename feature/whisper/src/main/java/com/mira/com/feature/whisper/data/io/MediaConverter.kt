package com.mira.com.feature.whisper.data.io

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import com.mira.com.core.infra.GlobalFileManagementService

/**
 * Media converter that converts all media files to H.264 format before processing.
 * This ensures reliability by using a consistent, well-supported format.
 */
object MediaConverter {
    
    private const val TAG = "MediaConverter"
    
    /**
     * Convert any media file to H.264 format for reliable processing.
     * For now, this is a placeholder that will be implemented with FFmpeg.
     */
    fun convertToH264(
        context: Context,
        inputUri: Uri,
        outputFileName: String? = null,
        removeOriginal: Boolean = false
    ): Uri? {
        return try {
            Log.d(TAG, "TECHNICAL: Starting H.264 conversion for $inputUri")

            // Create output file in Documents folder for easy access
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ConvertedMedia")
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            // First check if a converted version already exists
            val existingConvertedFile = GlobalFileManagementService.findExistingConvertedFile(context, inputUri, outputFileName)
            if (existingConvertedFile != null) {
                Log.d(TAG, "TECHNICAL: Found existing converted file, skipping conversion")
                
                // If removeOriginal is true and we found an existing converted file, 
                // we can still remove the original file
                if (removeOriginal) {
                    GlobalFileManagementService.removeOriginalFile(context, inputUri)
                }
                
                return existingConvertedFile
            }

            val fileName = outputFileName ?: "converted_${System.currentTimeMillis()}.mp4"
            val outputFile = File(documentsDir, fileName)

            Log.d(TAG, "TECHNICAL: Output file: ${outputFile.absolutePath}")

            // For now, just copy the file and rename it to .mp4
            // This is a temporary solution until FFmpeg integration
            val inputStream = context.contentResolver.openInputStream(inputUri)
            if (inputStream != null) {
                val outputStream = FileOutputStream(outputFile)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                Log.d(TAG, "TECHNICAL: File copied to H.264 container: ${outputFile.absolutePath}")
                Log.d(TAG, "TECHNICAL: Converted file saved to Documents/ConvertedMedia/ for easy access")
                
                // Remove original file if requested
                if (removeOriginal) {
                    GlobalFileManagementService.removeOriginalFile(context, inputUri)
                }
                
                Uri.fromFile(outputFile)
            } else {
                Log.e(TAG, "TECHNICAL: Could not open input stream for $inputUri")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "TECHNICAL: H.264 conversion error: ${e.message}", e)
            null
        }
    }
    
    /**
     * Check if a file needs conversion to H.264.
     * Returns true if the file is not already in H.264 format.
     */
    fun needsConversion(uri: Uri): Boolean {
        val path = uri.toString().lowercase()
        return !path.endsWith(".mp4") || path.contains("mkv") || path.contains("avi") || path.contains("mov")
    }
    
    /**
     * Check if a converted version of the file already exists in the converted folder.
     * This prevents unnecessary re-conversion of the same file.
     */
    fun findExistingConvertedFile(context: Context, inputUri: Uri, outputFileName: String? = null): Uri? {
        return GlobalFileManagementService.findExistingConvertedFile(context, inputUri, outputFileName)
    }
    
    /**
     * Remove the original file after successful conversion.
     * This helps save storage space by removing the original file once conversion is complete.
     */
    fun removeOriginalFile(context: Context, inputUri: Uri): Boolean {
        return GlobalFileManagementService.removeOriginalFile(context, inputUri)
    }
    
    /**
     * Remove original files from a batch of conversions.
     * This is useful for batch processing where multiple files are converted.
     */
    fun removeOriginalFilesBatch(context: Context, inputUris: List<Uri>): GlobalFileManagementService.BatchRemovalResult {
        return GlobalFileManagementService.removeOriginalFilesBatch(context, inputUris)
    }
    
    /**
     * Clean up converted files older than specified hours.
     */
    fun cleanupOldFiles(context: Context, maxAgeHours: Long = 24) {
        GlobalFileManagementService.cleanupOldFiles(context, maxAgeHours)
    }
    
    /**
     * Calculate SHA-256 hash of a file for duplicate detection.
     */
    fun calculateFileHash(file: File): String? {
        return GlobalFileManagementService.calculateFileHash(file)
    }
    
    /**
     * Find duplicate files based on SHA-256 hash comparison.
     */
    fun findDuplicateFilesByHash(directory: File): Map<String, List<File>> {
        return GlobalFileManagementService.findDuplicateFilesByHash(directory)
    }
    
    /**
     * Remove duplicate files based on hash comparison, keeping the most recent file.
     */
    fun removeDuplicateFilesByHash(directory: File): GlobalFileManagementService.DuplicateRemovalResult {
        return GlobalFileManagementService.removeDuplicateFilesByHash(directory)
    }
    
    /**
     * Comprehensive global cleanup that removes all converted files and temporary data.
     * This is more aggressive than cleanupOldFiles and removes everything regardless of age.
     * Now includes hash-based duplicate detection and removal.
     */
    fun globalCleanup(context: Context): GlobalFileManagementService.GlobalCleanupResult {
        return GlobalFileManagementService.globalCleanup(context)
    }
    
    /**
     * Result object for cleanup operations.
     */
    data class CleanupResult(
        var success: Boolean = false,
        var cacheFilesDeleted: Int = 0,
        var documentsFilesDeleted: Int = 0,
        var tempFilesDeleted: Int = 0,
        var whisperFilesDeleted: Int = 0,
        var originalFilesDeleted: Int = 0,
        var duplicateFilesRemoved: Int = 0,
        var duplicateSpaceSaved: Long = 0,
        var errors: MutableList<String> = mutableListOf()
    ) {
        val totalFilesDeleted: Int
            get() = cacheFilesDeleted + documentsFilesDeleted + tempFilesDeleted + whisperFilesDeleted + originalFilesDeleted + duplicateFilesRemoved
        
        val totalSpaceSaved: Long
            get() = duplicateSpaceSaved
    }
    
    /**
     * Result object for hash-based duplicate removal operations.
     */
    data class DuplicateRemovalResult(
        var success: Boolean = false,
        var filesRemoved: Int = 0,
        var spaceSaved: Long = 0,
        var duplicateSetsProcessed: Int = 0,
        var errors: MutableList<String> = mutableListOf()
    ) {
        val averageSpacePerFile: Long
            get() = if (filesRemoved > 0) spaceSaved / filesRemoved else 0
    }
    
    /**
     * Result object for batch original file removal operations.
     */
    data class BatchRemovalResult(
        var successfulRemovals: Int = 0,
        var failedRemovals: Int = 0,
        var failedUris: MutableList<Uri> = mutableListOf()
    ) {
        val totalProcessed: Int
            get() = successfulRemovals + failedRemovals
        
        val successRate: Double
            get() = if (totalProcessed > 0) successfulRemovals.toDouble() / totalProcessed else 0.0
    }
}
