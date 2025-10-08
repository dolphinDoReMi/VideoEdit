package com.mira.storage.adapters

import android.content.Context
import android.util.Log
import com.mira.storage.ScopedStorageManager
import java.io.File

/**
 * CLIP Service Storage Adapter
 * 
 * Provides CLIP-specific storage access through the unified scoped storage infrastructure.
 * All CLIP services MUST use this adapter instead of hardcoded paths.
 */
object ClipStorageAdapter {
    
    private const val TAG = "ClipStorageAdapter"
    
    /**
     * Get the default CLIP model path
     */
    fun getDefaultModelPath(context: Context): String {
        val modelFile = File(ScopedStorageManager.getClipModelsDir(context), "clip-model.bin")
        return modelFile.absolutePath
    }
    
    /**
     * Get the CLIP input directory path
     */
    fun getInputDirPath(context: Context): String {
        return ScopedStorageManager.getClipInputDir(context).absolutePath
    }
    
    /**
     * Get the CLIP output directory path
     */
    fun getOutputDirPath(context: Context): String {
        return ScopedStorageManager.getClipOutputDir(context).absolutePath
    }
    
    /**
     * Get the CLIP embeddings directory path
     */
    fun getEmbeddingsDirPath(context: Context): String {
        return ScopedStorageManager.getClipEmbeddingsDir(context).absolutePath
    }
    
    /**
     * Get variant-specific embeddings directory
     */
    fun getVariantEmbeddingsDir(context: Context, variant: String): File {
        return File(ScopedStorageManager.getClipEmbeddingsDir(context), variant)
    }
    
    /**
     * Get job-specific output directory
     */
    fun getJobOutputDir(context: Context, jobId: String): File {
        return ScopedStorageManager.getJobOutputDir(context, "clip", jobId)
    }
    
    /**
     * Get embeddings file for a specific video
     */
    fun getEmbeddingsFile(context: Context, variant: String, videoId: String): File {
        val variantDir = getVariantEmbeddingsDir(context, variant)
        return File(variantDir, "$videoId.f32")
    }
    
    /**
     * Get metadata file for a specific video
     */
    fun getMetadataFile(context: Context, variant: String, videoId: String): File {
        val variantDir = getVariantEmbeddingsDir(context, variant)
        return File(variantDir, "$videoId.json")
    }
    
    /**
     * Get logs directory for CLIP processing logs
     */
    fun getLogsDir(context: Context): File {
        return ScopedStorageManager.getClipLogsDir(context)
    }
    
    /**
     * Ensure CLIP directory structure exists
     */
    fun ensureDirectories(context: Context): Boolean {
        return ScopedStorageManager.ensureDirectoryStructure(context)
    }
    
    /**
     * Get CLIP storage info for debugging
     */
    fun getStorageInfo(context: Context): Map<String, String> {
        return mapOf(
            "serviceDir" to ScopedStorageManager.getClipServiceDir(context).absolutePath,
            "inputDir" to ScopedStorageManager.getClipInputDir(context).absolutePath,
            "outputDir" to ScopedStorageManager.getClipOutputDir(context).absolutePath,
            "modelsDir" to ScopedStorageManager.getClipModelsDir(context).absolutePath,
            "embeddingsDir" to ScopedStorageManager.getClipEmbeddingsDir(context).absolutePath,
            "logsDir" to ScopedStorageManager.getClipLogsDir(context).absolutePath,
            "defaultModelPath" to getDefaultModelPath(context)
        )
    }
}
