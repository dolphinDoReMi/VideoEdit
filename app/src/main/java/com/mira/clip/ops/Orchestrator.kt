package com.mira.clip.ops

import android.content.Context
import android.util.Log
import com.mira.clip.core.Config
import com.mira.clip.services.RetrievalService
import com.mira.clip.services.VideoIngestService
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
}
