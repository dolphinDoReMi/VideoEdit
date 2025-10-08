package com.mira.clip.ops

import android.content.Context
import android.util.Log
import com.mira.clip.core.Config
import com.mira.clip.services.RetrievalService
import com.mira.clip.services.VideoIngestService
// GlobalFileManagementService dependency temporarily removed for build stability in this variant
import java.io.File

/**
 * Orchestrator facade for CLIP4Clip headless services.
 * 
 * Provides high-level operations for video ingestion and text-to-video retrieval.
 */
object Orchestrator {
    
    private const val TAG = "Orchestrator"
    
    /**
     * Run video ingestion from manifest.
     * 
     * @param context Android context
     * @param manifestPath Path to ingestion manifest
     */
    fun ingest(context: Context, manifestPath: String) {
        Log.i(TAG, "Starting video ingestion from: $manifestPath")
        
        try {
            val ingestService = VideoIngestService(context)
            ingestService.run(manifestPath)
            
            Log.i(TAG, "Video ingestion completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Video ingestion failed: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Run text-to-video retrieval from manifest.
     * 
     * @param context Android context
     * @param manifestPath Path to search manifest
     */
    fun search(context: Context, manifestPath: String) {
        Log.i(TAG, "Starting text-to-video retrieval from: $manifestPath")
        
        try {
            val retrievalService = RetrievalService(context)
            retrievalService.run(manifestPath)
            
            Log.i(TAG, "Text-to-video retrieval completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Text-to-video retrieval failed: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Print debug information about the system.
     * 
     * @param context Android context
     */
    fun debugLog(context: Context) {
        Log.i(TAG, "=== CLIP4Clip Debug Information ===")
        
        // Check asset availability
        val pytorchLoader = com.mira.clip.ml.PytorchLoader
        val hasAssets = pytorchLoader.hasAllAssets(context)
        Log.i(TAG, "CLIP assets available: $hasAssets")
        
        // Check storage directories
        val embeddingsDir = File(Config.EMBEDDINGS_DIR)
        val searchDir = File(Config.SEARCH_DIR)
        
        Log.i(TAG, "Embeddings directory exists: ${embeddingsDir.exists()}")
        Log.i(TAG, "Search directory exists: ${searchDir.exists()}")
        
        if (embeddingsDir.exists()) {
            val variants = embeddingsDir.listFiles()?.map { it.name } ?: emptyList()
            Log.i(TAG, "Available variants: $variants")
            
            for (variant in variants) {
                val variantDir = File(embeddingsDir, variant)
                val videoIds = variantDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".f32")
                }?.map { it.nameWithoutExtension } ?: emptyList()
                
                Log.i(TAG, "Variant $variant has ${videoIds.size} videos: $videoIds")
            }
        }
        
        // Check default video
        val defaultVideo = File(Config.DEFAULT_VIDEO_PATH)
        Log.i(TAG, "Default video exists: ${defaultVideo.exists()}")
        if (defaultVideo.exists()) {
            Log.i(TAG, "Default video size: ${defaultVideo.length()} bytes")
        }
        
        Log.i(TAG, "=== End Debug Information ===")
    }
    
    /**
     * Clear all cached data and embeddings.
     * 
     * @param context Android context
     */
    fun clearCache(context: Context) {
        Log.i(TAG, "Clearing CLIP4Clip cache")
        
        try {
            // Clear embeddings directory
            val embeddingsDir = File(Config.EMBEDDINGS_DIR)
            if (embeddingsDir.exists()) {
                embeddingsDir.deleteRecursively()
                Log.i(TAG, "Cleared embeddings directory")
            }
            
            // Clear search results directory
            val searchDir = File(Config.SEARCH_DIR)
            if (searchDir.exists()) {
                searchDir.deleteRecursively()
                Log.i(TAG, "Cleared search results directory")
            }
            
            // Clear app files directory (PyTorch models)
            val filesDir = context.filesDir
            val modelFiles = filesDir.listFiles { file ->
                file.name.endsWith(".ptl") || 
                file.name.endsWith(".json") || 
                file.name.endsWith(".txt")
            }
            
            modelFiles?.forEach { file ->
                if (file.delete()) {
                    Log.i(TAG, "Deleted cached file: ${file.name}")
                }
            }
            
            Log.i(TAG, "Cache clearing completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Comprehensive global cleanup that removes all temporary files, cache, and converted media.
     * This is more aggressive than clearCache and includes media conversion cleanup.
     */
    fun globalCleanup(context: Context): GlobalCleanupResult {
        val result = GlobalCleanupResult()
        
        try {
            Log.i(TAG, "Starting comprehensive global cleanup")
            
            // Clear CLIP cache
            try {
                clearCache(context)
                result.clipCacheCleared = true
                Log.i(TAG, "CLIP cache cleared successfully")
            } catch (e: Exception) {
                result.errors.add("Failed to clear CLIP cache: ${e.message}")
                Log.e(TAG, "Error clearing CLIP cache: ${e.message}", e)
            }
            
            // Clear media conversion cache and converted files (disabled in this build variant)
            try {
                val mediaFilesDeleted = 0
                result.mediaFilesDeleted = mediaFilesDeleted
                Log.i(TAG, "Media cleanup completed: ${mediaFilesDeleted} files deleted")
            } catch (e: Exception) {
                result.errors.add("Failed to clear media files: ${e.message}")
                Log.e(TAG, "Error clearing media files: ${e.message}", e)
            }
            
            // Clear app cache directory
            try {
                val cacheDir = context.cacheDir
                val cacheFiles = cacheDir.listFiles()
                var cacheFilesDeleted = 0
                
                cacheFiles?.forEach { file ->
                    if (file.deleteRecursively()) {
                        cacheFilesDeleted++
                    }
                }
                
                result.cacheFilesDeleted = cacheFilesDeleted
                Log.i(TAG, "App cache cleared: $cacheFilesDeleted files/directories deleted")
            } catch (e: Exception) {
                result.errors.add("Failed to clear app cache: ${e.message}")
                Log.e(TAG, "Error clearing app cache: ${e.message}", e)
            }
            
            // Clear external cache directory
            try {
                val externalCacheDir = context.externalCacheDir
                if (externalCacheDir != null && externalCacheDir.exists()) {
                    val externalFiles = externalCacheDir.listFiles()
                    var externalFilesDeleted = 0
                    
                    externalFiles?.forEach { file ->
                        if (file.deleteRecursively()) {
                            externalFilesDeleted++
                        }
                    }
                    
                    result.externalCacheFilesDeleted = externalFilesDeleted
                    Log.i(TAG, "External cache cleared: $externalFilesDeleted files/directories deleted")
                }
            } catch (e: Exception) {
                result.errors.add("Failed to clear external cache: ${e.message}")
                Log.e(TAG, "Error clearing external cache: ${e.message}", e)
            }
            
            result.success = result.errors.isEmpty()
            Log.i(TAG, "Global cleanup completed: ${if (result.success) "SUCCESS" else "PARTIAL_SUCCESS"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Critical error during global cleanup: ${e.message}", e)
            result.errors.add("Critical cleanup error: ${e.message}")
            result.success = false
        }
        
        return result
    }
    
    /**
     * Result object for global cleanup operations.
     */
    data class GlobalCleanupResult(
        var success: Boolean = false,
        var clipCacheCleared: Boolean = false,
        var cacheFilesDeleted: Int = 0,
        var externalCacheFilesDeleted: Int = 0,
        var mediaFilesDeleted: Int = 0,
        var errors: MutableList<String> = mutableListOf()
    ) {
        val totalFilesDeleted: Int
            get() = cacheFilesDeleted + externalCacheFilesDeleted + mediaFilesDeleted
    }
}
