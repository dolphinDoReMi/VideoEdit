package com.mira.com.core.infra

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.DigestInputStream

/**
 * Core infrastructure service for comprehensive file management including:
 * - Duplicate detection (filename-based and hash-based)
 * - Global cleanup operations
 * - Original file removal after conversion
 * - Storage optimization
 * 
 * This service is available across all modules in the application.
 */
object GlobalFileManagementService {
    
    private const val TAG = "GlobalFileManagement"
    
    // ==================== HASH-BASED DUPLICATE DETECTION ====================
    
    /**
     * Calculate SHA-256 hash of a file for duplicate detection.
     * Uses chunk-based processing for memory efficiency with large files.
     */
    fun calculateFileHash(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val inputStream = FileInputStream(file)
            val digestStream = DigestInputStream(inputStream, digest)
            
            // Read file in chunks to handle large files efficiently
            val buffer = ByteArray(8192)
            while (digestStream.read(buffer) != -1) {
                // Hash is calculated automatically by DigestInputStream
            }
            
            digestStream.close()
            inputStream.close()
            
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "TECHNICAL: Error calculating file hash for ${file.name}: ${e.message}", e)
            null
        }
    }
    
    /**
     * Find duplicate files based on SHA-256 hash comparison.
     * Returns a map of hash to list of files with that hash.
     */
    fun findDuplicateFilesByHash(directory: File): Map<String, List<File>> {
        val hashToFiles = mutableMapOf<String, MutableList<File>>()
        
        try {
            Log.d(TAG, "TECHNICAL: Starting hash-based duplicate detection in ${directory.absolutePath}")
            
            if (!directory.exists() || !directory.isDirectory) {
                Log.w(TAG, "TECHNICAL: Directory does not exist or is not a directory: ${directory.absolutePath}")
                return emptyMap()
            }
            
            val files = directory.listFiles { file -> file.isFile }
            if (files.isNullOrEmpty()) {
                Log.d(TAG, "TECHNICAL: No files found in directory: ${directory.absolutePath}")
                return emptyMap()
            }
            
            Log.d(TAG, "TECHNICAL: Processing ${files.size} files for hash-based duplicate detection")
            
            files.forEach { file ->
                val hash = calculateFileHash(file)
                if (hash != null) {
                    hashToFiles.getOrPut(hash) { mutableListOf() }.add(file)
                    Log.d(TAG, "TECHNICAL: Calculated hash for ${file.name}: ${hash.take(8)}...")
                } else {
                    Log.w(TAG, "TECHNICAL: Failed to calculate hash for ${file.name}")
                }
            }
            
            // Filter to only include hashes with multiple files (duplicates)
            val duplicates = hashToFiles.filter { it.value.size > 1 }
            
            Log.d(TAG, "TECHNICAL: Hash-based duplicate detection completed")
            Log.d(TAG, "TECHNICAL: Found ${duplicates.size} sets of duplicate files")
            duplicates.forEach { (hash, files) ->
                Log.d(TAG, "TECHNICAL: Duplicate set (${hash.take(8)}...): ${files.map { it.name }}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "TECHNICAL: Error during hash-based duplicate detection: ${e.message}", e)
        }
        
        return hashToFiles.filter { it.value.size > 1 }
    }
    
    /**
     * Remove duplicate files based on hash comparison, keeping the most recent file.
     */
    fun removeDuplicateFilesByHash(directory: File): DuplicateRemovalResult {
        val result = DuplicateRemovalResult()
        
        try {
            Log.d(TAG, "TECHNICAL: Starting hash-based duplicate file removal in ${directory.absolutePath}")
            
            val duplicates = findDuplicateFilesByHash(directory)
            
            duplicates.forEach { (hash, files) ->
                Log.d(TAG, "TECHNICAL: Processing duplicate set with hash ${hash.take(8)}...")
                
                // Sort files by last modified time (most recent first)
                val sortedFiles = files.sortedByDescending { it.lastModified() }
                val keepFile = sortedFiles.first()
                val removeFiles = sortedFiles.drop(1)
                
                Log.d(TAG, "TECHNICAL: Keeping file: ${keepFile.name} (most recent)")
                Log.d(TAG, "TECHNICAL: Removing ${removeFiles.size} duplicate files")
                
                removeFiles.forEach { file ->
                    try {
                        if (file.delete()) {
                            result.filesRemoved++
                            result.spaceSaved += file.length()
                            Log.d(TAG, "TECHNICAL: Removed duplicate file: ${file.name}")
                        } else {
                            result.errors.add("Failed to delete duplicate file: ${file.name}")
                            Log.w(TAG, "TECHNICAL: Failed to delete duplicate file: ${file.name}")
                        }
                    } catch (e: Exception) {
                        result.errors.add("Error deleting duplicate file ${file.name}: ${e.message}")
                        Log.e(TAG, "TECHNICAL: Error deleting duplicate file ${file.name}: ${e.message}", e)
                    }
                }
                
                result.duplicateSetsProcessed++
            }
            
            Log.d(TAG, "TECHNICAL: Hash-based duplicate removal completed")
            Log.d(TAG, "TECHNICAL: Removed ${result.filesRemoved} duplicate files")
            Log.d(TAG, "TECHNICAL: Space saved: ${result.spaceSaved} bytes")
            
            result.success = true
            
        } catch (e: Exception) {
            Log.e(TAG, "TECHNICAL: Error during hash-based duplicate removal: ${e.message}", e)
            result.errors.add("Hash-based duplicate removal error: ${e.message}")
            result.success = false
        }
        
        return result
    }
    
    // ==================== FILENAME-BASED DUPLICATE DETECTION ====================
    
    /**
     * Check if a converted version of the file already exists in the converted folder.
     * This prevents unnecessary re-conversion of the same file.
     */
    fun findExistingConvertedFile(context: Context, inputUri: Uri, outputFileName: String? = null): Uri? {
        try {
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ConvertedMedia")
            if (!documentsDir.exists()) {
                return null
            }
            
            // If specific output filename is provided, check for exact match
            if (outputFileName != null) {
                val existingFile = File(documentsDir, outputFileName)
                if (existingFile.exists()) {
                    Log.d(TAG, "TECHNICAL: Found existing converted file with exact name: ${existingFile.name}")
                    return Uri.fromFile(existingFile)
                }
            }
            
            // Extract base filename from input URI for pattern matching
            val inputFileName = getFileNameFromUri(inputUri)
            val baseName = inputFileName.substringBeforeLast(".")
            
            // Look for files that match the pattern: baseName_converted.* or converted_baseName.*
            val files = documentsDir.listFiles { file ->
                val fileName = file.name.lowercase()
                fileName.startsWith("${baseName.lowercase()}_converted") ||
                fileName.startsWith("converted_${baseName.lowercase()}") ||
                fileName.startsWith("${baseName.lowercase()}_") ||
                fileName.contains(baseName.lowercase())
            }
            
            // Return the most recent matching file
            files?.maxByOrNull { it.lastModified() }?.let { file ->
                Log.d(TAG, "TECHNICAL: Found existing converted file by pattern: ${file.name}")
                return Uri.fromFile(file)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "TECHNICAL: Error checking for existing converted file: ${e.message}", e)
        }
        
        return null
    }
    
    /**
     * Extract filename from URI, handling different URI schemes.
     */
    private fun getFileNameFromUri(uri: Uri): String {
        return when (uri.scheme) {
            "file" -> File(uri.path ?: "").name
            "content" -> uri.lastPathSegment ?: "unknown_file"
            else -> uri.lastPathSegment ?: "unknown_file"
        }
    }
    
    // ==================== ORIGINAL FILE REMOVAL ====================
    
    /**
     * Remove the original file after successful conversion.
     * This helps save storage space by removing the original file once conversion is complete.
     */
    fun removeOriginalFile(context: Context, inputUri: Uri): Boolean {
        return try {
            Log.d(TAG, "TECHNICAL: Attempting to remove original file: $inputUri")
            
            when (inputUri.scheme) {
                "file" -> {
                    val file = File(inputUri.path ?: "")
                    if (file.exists() && file.delete()) {
                        Log.d(TAG, "TECHNICAL: Successfully removed original file: ${file.name}")
                        true
                    } else {
                        Log.w(TAG, "TECHNICAL: Could not remove original file: ${file.name}")
                        false
                    }
                }
                "content" -> {
                    // For content URIs, we can't directly delete the file
                    // This would require special permissions and user interaction
                    Log.w(TAG, "TECHNICAL: Cannot remove content URI file directly: $inputUri")
                    Log.d(TAG, "TECHNICAL: Content URI files require user permission to delete")
                    false
                }
                else -> {
                    Log.w(TAG, "TECHNICAL: Unknown URI scheme, cannot remove file: $inputUri")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TECHNICAL: Error removing original file: ${e.message}", e)
            false
        }
    }
    
    /**
     * Remove original files from a batch of conversions.
     * This is useful for batch processing where multiple files are converted.
     */
    fun removeOriginalFilesBatch(context: Context, inputUris: List<Uri>): BatchRemovalResult {
        val result = BatchRemovalResult()
        
        inputUris.forEach { uri ->
            if (removeOriginalFile(context, uri)) {
                result.successfulRemovals++
                Log.d(TAG, "TECHNICAL: Successfully removed original file: $uri")
            } else {
                result.failedRemovals++
                result.failedUris.add(uri)
                Log.w(TAG, "TECHNICAL: Failed to remove original file: $uri")
            }
        }
        
        Log.d(TAG, "TECHNICAL: Batch removal completed: ${result.successfulRemovals} successful, ${result.failedRemovals} failed")
        return result
    }
    
    // ==================== GLOBAL CLEANUP OPERATIONS ====================
    
    /**
     * Clean up converted files older than specified hours.
     */
    fun cleanupOldFiles(context: Context, maxAgeHours: Long = 24) {
        try {
            // Clean up cache directory
            val cacheDir = File(context.cacheDir, "converted_media")
            if (cacheDir.exists()) {
                val files = cacheDir.listFiles()
                val currentTime = System.currentTimeMillis()
                val maxAge = maxAgeHours * 60 * 60 * 1000L

                files?.forEach { file ->
                    if (currentTime - file.lastModified() > maxAge) {
                        Log.d(TAG, "TECHNICAL: Cleaning up old converted file: ${file.name}")
                        file.delete()
                    }
                }
            }
            
            // Clean up Documents/ConvertedMedia directory
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ConvertedMedia")
            if (documentsDir.exists()) {
                val files = documentsDir.listFiles()
                val currentTime = System.currentTimeMillis()
                val maxAge = maxAgeHours * 60 * 60 * 1000L

                files?.forEach { file ->
                    if (currentTime - file.lastModified() > maxAge) {
                        Log.d(TAG, "TECHNICAL: Cleaning up old converted file in Documents: ${file.name}")
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TECHNICAL: Error cleaning up old files: ${e.message}", e)
        }
    }
    
    /**
     * Comprehensive global cleanup that removes all converted files and temporary data.
     * This is more aggressive than cleanupOldFiles and removes everything regardless of age.
     * Now includes hash-based duplicate detection and removal.
     */
    fun globalCleanup(context: Context): GlobalCleanupResult {
        val result = GlobalCleanupResult()
        
        try {
            Log.d(TAG, "TECHNICAL: Starting global cleanup with hash-based duplicate detection")
            
            // First, perform hash-based duplicate detection and removal
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ConvertedMedia")
            if (documentsDir.exists()) {
                Log.d(TAG, "TECHNICAL: Performing hash-based duplicate detection in Documents/ConvertedMedia")
                val duplicateResult = removeDuplicateFilesByHash(documentsDir)
                result.duplicateFilesRemoved = duplicateResult.filesRemoved
                result.duplicateSpaceSaved = duplicateResult.spaceSaved
                result.errors.addAll(duplicateResult.errors)
                
                Log.d(TAG, "TECHNICAL: Hash-based duplicate removal: ${duplicateResult.filesRemoved} files removed, ${duplicateResult.spaceSaved} bytes saved")
            }
            
            // Clean up cache directory completely
            val cacheDir = File(context.cacheDir, "converted_media")
            if (cacheDir.exists()) {
                val files = cacheDir.listFiles()
                files?.forEach { file ->
                    if (file.delete()) {
                        result.cacheFilesDeleted++
                        Log.d(TAG, "TECHNICAL: Deleted cache file: ${file.name}")
                    } else {
                        result.errors.add("Failed to delete cache file: ${file.name}")
                    }
                }
            }
            
            // Clean up remaining files in Documents/ConvertedMedia directory
            if (documentsDir.exists()) {
                val files = documentsDir.listFiles()
                files?.forEach { file ->
                    if (file.delete()) {
                        result.documentsFilesDeleted++
                        Log.d(TAG, "TECHNICAL: Deleted Documents file: ${file.name}")
                    } else {
                        result.errors.add("Failed to delete Documents file: ${file.name}")
                    }
                }
            }
            
            // Clean up app-specific temporary files
            val tempDir = File(context.cacheDir, "temp")
            if (tempDir.exists()) {
                val files = tempDir.listFiles()
                files?.forEach { file ->
                    if (file.delete()) {
                        result.tempFilesDeleted++
                        Log.d(TAG, "TECHNICAL: Deleted temp file: ${file.name}")
                    } else {
                        result.errors.add("Failed to delete temp file: ${file.name}")
                    }
                }
            }
            
            // Clean up whisper-specific cache files
            val whisperCacheDir = File(context.cacheDir, "whisper")
            if (whisperCacheDir.exists()) {
                val files = whisperCacheDir.listFiles()
                files?.forEach { file ->
                    if (file.delete()) {
                        result.whisperFilesDeleted++
                        Log.d(TAG, "TECHNICAL: Deleted whisper cache file: ${file.name}")
                    } else {
                        result.errors.add("Failed to delete whisper cache file: ${file.name}")
                    }
                }
            }
            
            // Clean up CLIP-specific cache files
            val clipCacheDir = File(context.cacheDir, "clip")
            if (clipCacheDir.exists()) {
                val files = clipCacheDir.listFiles()
                files?.forEach { file ->
                    if (file.delete()) {
                        result.clipFilesDeleted++
                        Log.d(TAG, "TECHNICAL: Deleted CLIP cache file: ${file.name}")
                    } else {
                        result.errors.add("Failed to delete CLIP cache file: ${file.name}")
                    }
                }
            }
            
            Log.d(TAG, "TECHNICAL: Global cleanup with hash-based duplicate detection completed successfully")
            result.success = true
            
        } catch (e: Exception) {
            Log.e(TAG, "TECHNICAL: Error during global cleanup: ${e.message}", e)
            result.errors.add("Global cleanup error: ${e.message}")
            result.success = false
        }
        
        return result
    }
    
    // ==================== RESULT DATA CLASSES ====================
    
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
    
    /**
     * Result object for comprehensive global cleanup operations.
     */
    data class GlobalCleanupResult(
        var success: Boolean = false,
        var cacheFilesDeleted: Int = 0,
        var documentsFilesDeleted: Int = 0,
        var tempFilesDeleted: Int = 0,
        var whisperFilesDeleted: Int = 0,
        var clipFilesDeleted: Int = 0,
        var originalFilesDeleted: Int = 0,
        var duplicateFilesRemoved: Int = 0,
        var duplicateSpaceSaved: Long = 0,
        var errors: MutableList<String> = mutableListOf()
    ) {
        val totalFilesDeleted: Int
            get() = cacheFilesDeleted + documentsFilesDeleted + tempFilesDeleted + whisperFilesDeleted + clipFilesDeleted + originalFilesDeleted + duplicateFilesRemoved
        
        val totalSpaceSaved: Long
            get() = duplicateSpaceSaved
        
        val spaceSavedInGB: Double
            get() = totalSpaceSaved / (1024.0 * 1024.0 * 1024.0)
    }
}
