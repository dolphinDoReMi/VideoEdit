package com.mira.com.core.infra

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.DigestInputStream

/**
 * Core infrastructure service for hash-based duplicate detection and removal.
 * This service provides content-based duplicate detection using SHA-256 hashing
 * and is available across all modules in the application.
 */
object HashBasedDuplicateDetectionService {
    
    private const val TAG = "HashDuplicateDetection"
    
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
     * This is the core duplicate removal logic used across all modules.
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
    
    /**
     * Perform hash-based duplicate detection and removal across multiple directories.
     * This is useful for global cleanup operations.
     */
    fun removeDuplicateFilesFromDirectories(directories: List<File>): GlobalDuplicateRemovalResult {
        val result = GlobalDuplicateRemovalResult()
        
        try {
            Log.d(TAG, "TECHNICAL: Starting global hash-based duplicate removal across ${directories.size} directories")
            
            directories.forEach { directory ->
                if (directory.exists() && directory.isDirectory) {
                    Log.d(TAG, "TECHNICAL: Processing directory: ${directory.absolutePath}")
                    val directoryResult = removeDuplicateFilesByHash(directory)
                    
                    result.totalFilesRemoved += directoryResult.filesRemoved
                    result.totalSpaceSaved += directoryResult.spaceSaved
                    result.totalDuplicateSetsProcessed += directoryResult.duplicateSetsProcessed
                    result.errors.addAll(directoryResult.errors)
                    
                    Log.d(TAG, "TECHNICAL: Directory ${directory.name}: ${directoryResult.filesRemoved} files removed, ${directoryResult.spaceSaved} bytes saved")
                } else {
                    Log.w(TAG, "TECHNICAL: Skipping invalid directory: ${directory.absolutePath}")
                }
            }
            
            Log.d(TAG, "TECHNICAL: Global hash-based duplicate removal completed")
            Log.d(TAG, "TECHNICAL: Total files removed: ${result.totalFilesRemoved}")
            Log.d(TAG, "TECHNICAL: Total space saved: ${result.totalSpaceSaved} bytes")
            
            result.success = true
            
        } catch (e: Exception) {
            Log.e(TAG, "TECHNICAL: Error during global hash-based duplicate removal: ${e.message}", e)
            result.errors.add("Global hash-based duplicate removal error: ${e.message}")
            result.success = false
        }
        
        return result
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
     * Result object for global hash-based duplicate removal operations.
     */
    data class GlobalDuplicateRemovalResult(
        var success: Boolean = false,
        var totalFilesRemoved: Int = 0,
        var totalSpaceSaved: Long = 0,
        var totalDuplicateSetsProcessed: Int = 0,
        var errors: MutableList<String> = mutableListOf()
    ) {
        val averageSpacePerFile: Long
            get() = if (totalFilesRemoved > 0) totalSpaceSaved / totalFilesRemoved else 0
        
        val spaceSavedInGB: Double
            get() = totalSpaceSaved / (1024.0 * 1024.0 * 1024.0)
    }
}
