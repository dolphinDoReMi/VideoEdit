package com.mira.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Unified Scoped Storage Manager for All Mira Services
 * 
 * This is the central storage infrastructure that all services must use
 * to access Android device storage. It provides:
 * 
 * 1. App-scoped storage paths using getExternalFilesDir()
 * 2. Unified directory structure for all services
 * 3. URI handling for MediaStore and SAF
 * 4. Context-aware path resolution
 * 5. Xiaomi Pad optimizations
 * 
 * NO SERVICE SHOULD USE HARDCODED PATHS - ALL MUST GO THROUGH THIS MANAGER
 */
object ScopedStorageManager {
    
    private const val TAG = "ScopedStorageManager"
    
    // Base directory structure constants
    private const val MIRA_ROOT = "Mira"
    private const val SERVICES_DIR = "services"
    private const val SHARED_DIR = "shared"
    private const val TEMP_DIR = "temp"
    private const val CACHE_DIR = "cache"
    
    // Service-specific directories
    private const val WHISPER_SERVICE = "whisper"
    private const val CLIP_SERVICE = "clip"
    private const val AUTOCLIP_SERVICE = "autoclip"
    private const val FAISS_SERVICE = "faiss"
    
    // Service subdirectories
    private const val INPUT_DIR = "input"
    private const val OUTPUT_DIR = "output"
    private const val MODELS_DIR = "models"
    private const val LOGS_DIR = "logs"
    private const val METADATA_DIR = "metadata"
    private const val EMBEDDINGS_DIR = "embeddings"
    private const val INDEX_DIR = "index"
    private const val QUERY_DIR = "query"
    private const val RESULTS_DIR = "results"
    private const val SIDECAR_DIR = "sidecar"
    private const val PROCESSING_DIR = "processing"
    private const val CHUNKS_DIR = "chunks"
    private const val INTERMEDIATE_DIR = "intermediate"
    private const val EXPORTS_DIR = "exports"
    
    /**
     * Get the root Mira directory using app-scoped storage
     * This is the single source of truth for all storage paths
     */
    fun getMiraRootDir(context: Context): File {
        return File(context.getExternalFilesDir(null), MIRA_ROOT)
    }
    
    /**
     * Get the services directory for all service-specific storage
     */
    fun getServicesDir(context: Context): File {
        return File(getMiraRootDir(context), SERVICES_DIR)
    }
    
    /**
     * Get the shared directory for cross-service data
     */
    fun getSharedDir(context: Context): File {
        return File(getMiraRootDir(context), SHARED_DIR)
    }
    
    /**
     * Get the temporary directory for processing
     */
    fun getTempDir(context: Context): File {
        return File(getMiraRootDir(context), TEMP_DIR)
    }
    
    /**
     * Get the cache directory for temporary files
     */
    fun getCacheDir(context: Context): File {
        return File(getMiraRootDir(context), CACHE_DIR)
    }
    
    // ===== WHISPER SERVICE STORAGE =====
    
    /**
     * Get Whisper service root directory
     */
    fun getWhisperServiceDir(context: Context): File {
        return File(getServicesDir(context), WHISPER_SERVICE)
    }
    
    /**
     * Get Whisper input directory
     */
    fun getWhisperInputDir(context: Context): File {
        return File(getWhisperServiceDir(context), INPUT_DIR)
    }
    
    /**
     * Get Whisper output directory
     */
    fun getWhisperOutputDir(context: Context): File {
        return File(getWhisperServiceDir(context), OUTPUT_DIR)
    }
    
    /**
     * Get Whisper models directory
     */
    fun getWhisperModelsDir(context: Context): File {
        return File(getWhisperServiceDir(context), MODELS_DIR)
    }
    
    /**
     * Get Whisper logs directory
     */
    fun getWhisperLogsDir(context: Context): File {
        return File(getWhisperServiceDir(context), LOGS_DIR)
    }
    
    /**
     * Get Whisper sidecar directory
     */
    fun getWhisperSidecarDir(context: Context): File {
        return File(getWhisperServiceDir(context), SIDECAR_DIR)
    }
    
    /**
     * Get Whisper processing directory
     */
    fun getWhisperProcessingDir(context: Context): File {
        return File(getWhisperServiceDir(context), PROCESSING_DIR)
    }
    
    /**
     * Get Whisper exports directory
     */
    fun getWhisperExportsDir(context: Context): File {
        return File(getWhisperServiceDir(context), EXPORTS_DIR)
    }
    
    // ===== CLIP SERVICE STORAGE =====
    
    /**
     * Get CLIP service root directory
     */
    fun getClipServiceDir(context: Context): File {
        return File(getServicesDir(context), CLIP_SERVICE)
    }
    
    /**
     * Get CLIP input directory
     */
    fun getClipInputDir(context: Context): File {
        return File(getClipServiceDir(context), INPUT_DIR)
    }
    
    /**
     * Get CLIP output directory
     */
    fun getClipOutputDir(context: Context): File {
        return File(getClipServiceDir(context), OUTPUT_DIR)
    }
    
    /**
     * Get CLIP models directory
     */
    fun getClipModelsDir(context: Context): File {
        return File(getClipServiceDir(context), MODELS_DIR)
    }
    
    /**
     * Get CLIP embeddings directory
     */
    fun getClipEmbeddingsDir(context: Context): File {
        return File(getClipServiceDir(context), EMBEDDINGS_DIR)
    }
    
    /**
     * Get CLIP logs directory
     */
    fun getClipLogsDir(context: Context): File {
        return File(getClipServiceDir(context), LOGS_DIR)
    }
    
    // ===== AUTOCLIP SERVICE STORAGE =====
    
    /**
     * Get AutoClipper service root directory
     */
    fun getAutoClipServiceDir(context: Context): File {
        return File(getServicesDir(context), AUTOCLIP_SERVICE)
    }
    
    /**
     * Get AutoClipper input directory
     */
    fun getAutoClipInputDir(context: Context): File {
        return File(getAutoClipServiceDir(context), INPUT_DIR)
    }
    
    /**
     * Get AutoClipper output directory
     */
    fun getAutoClipOutputDir(context: Context): File {
        return File(getAutoClipServiceDir(context), OUTPUT_DIR)
    }
    
    /**
     * Get AutoClipper processing directory
     */
    fun getAutoClipProcessingDir(context: Context): File {
        return File(getAutoClipServiceDir(context), PROCESSING_DIR)
    }
    
    /**
     * Get AutoClipper chunks directory
     */
    fun getAutoClipChunksDir(context: Context): File {
        return File(getAutoClipProcessingDir(context), CHUNKS_DIR)
    }
    
    /**
     * Get AutoClipper intermediate directory
     */
    fun getAutoClipIntermediateDir(context: Context): File {
        return File(getAutoClipProcessingDir(context), INTERMEDIATE_DIR)
    }
    
    /**
     * Get AutoClipper logs directory
     */
    fun getAutoClipLogsDir(context: Context): File {
        return File(getAutoClipServiceDir(context), LOGS_DIR)
    }
    
    // ===== FAISS SERVICE STORAGE =====
    
    /**
     * Get FAISS service root directory
     */
    fun getFaissServiceDir(context: Context): File {
        return File(getServicesDir(context), FAISS_SERVICE)
    }
    
    /**
     * Get FAISS index directory
     */
    fun getFaissIndexDir(context: Context): File {
        return File(getFaissServiceDir(context), INDEX_DIR)
    }
    
    /**
     * Get FAISS query directory
     */
    fun getFaissQueryDir(context: Context): File {
        return File(getFaissServiceDir(context), QUERY_DIR)
    }
    
    /**
     * Get FAISS results directory
     */
    fun getFaissResultsDir(context: Context): File {
        return File(getFaissServiceDir(context), RESULTS_DIR)
    }
    
    /**
     * Get FAISS logs directory
     */
    fun getFaissLogsDir(context: Context): File {
        return File(getFaissServiceDir(context), LOGS_DIR)
    }
    
    // ===== SHARED STORAGE =====
    
    /**
     * Get shared models directory (for models used by multiple services)
     */
    fun getSharedModelsDir(context: Context): File {
        return File(getSharedDir(context), MODELS_DIR)
    }
    
    /**
     * Get shared metadata directory
     */
    fun getSharedMetadataDir(context: Context): File {
        return File(getSharedDir(context), METADATA_DIR)
    }
    
    /**
     * Get shared logs directory
     */
    fun getSharedLogsDir(context: Context): File {
        return File(getSharedDir(context), LOGS_DIR)
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Ensure all directory structure exists
     */
    fun ensureDirectoryStructure(context: Context): Boolean {
        return try {
            val directories = listOf(
                getMiraRootDir(context),
                getServicesDir(context),
                getSharedDir(context),
                getTempDir(context),
                getCacheDir(context),
                
                // Whisper directories
                getWhisperServiceDir(context),
                getWhisperInputDir(context),
                getWhisperOutputDir(context),
                getWhisperModelsDir(context),
                getWhisperLogsDir(context),
                getWhisperSidecarDir(context),
                getWhisperProcessingDir(context),
                getWhisperExportsDir(context),
                
                // CLIP directories
                getClipServiceDir(context),
                getClipInputDir(context),
                getClipOutputDir(context),
                getClipModelsDir(context),
                getClipEmbeddingsDir(context),
                getClipLogsDir(context),
                
                // AutoClipper directories
                getAutoClipServiceDir(context),
                getAutoClipInputDir(context),
                getAutoClipOutputDir(context),
                getAutoClipProcessingDir(context),
                getAutoClipChunksDir(context),
                getAutoClipIntermediateDir(context),
                getAutoClipLogsDir(context),
                
                // FAISS directories
                getFaissServiceDir(context),
                getFaissIndexDir(context),
                getFaissQueryDir(context),
                getFaissResultsDir(context),
                getFaissLogsDir(context),
                
                // Shared directories
                getSharedModelsDir(context),
                getSharedMetadataDir(context),
                getSharedLogsDir(context)
            )
            
            directories.forEach { dir ->
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        Log.e(TAG, "Failed to create directory: ${dir.absolutePath}")
                        return false
                    }
                }
                
                if (!dir.canWrite()) {
                    Log.e(TAG, "Directory not writable: ${dir.absolutePath}")
                    return false
                }
            }
            
            Log.i(TAG, "Directory structure created successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating directory structure: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get job-specific output directory for any service
     */
    fun getJobOutputDir(context: Context, service: String, jobId: String): File {
        val serviceDir = when (service.lowercase()) {
            "whisper" -> getWhisperOutputDir(context)
            "clip" -> getClipOutputDir(context)
            "autoclip" -> getAutoClipOutputDir(context)
            "faiss" -> getFaissResultsDir(context)
            else -> getSharedDir(context)
        }
        return File(serviceDir, jobId)
    }
    
    /**
     * Copy URI content to scoped storage
     */
    fun copyUriToScopedStorage(context: Context, uri: Uri, targetFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "Successfully copied URI to scoped storage: ${targetFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to scoped storage", e)
            false
        }
    }
    
    /**
     * Get directory structure info for debugging
     */
    fun getDirectoryInfo(context: Context): Map<String, String> {
        return mapOf(
            "miraRoot" to getMiraRootDir(context).absolutePath,
            "services" to getServicesDir(context).absolutePath,
            "shared" to getSharedDir(context).absolutePath,
            "temp" to getTempDir(context).absolutePath,
            "cache" to getCacheDir(context).absolutePath,
            
            "whisperService" to getWhisperServiceDir(context).absolutePath,
            "whisperInput" to getWhisperInputDir(context).absolutePath,
            "whisperOutput" to getWhisperOutputDir(context).absolutePath,
            "whisperModels" to getWhisperModelsDir(context).absolutePath,
            
            "clipService" to getClipServiceDir(context).absolutePath,
            "clipInput" to getClipInputDir(context).absolutePath,
            "clipOutput" to getClipOutputDir(context).absolutePath,
            "clipEmbeddings" to getClipEmbeddingsDir(context).absolutePath,
            
            "autoclipService" to getAutoClipServiceDir(context).absolutePath,
            "autoclipInput" to getAutoClipInputDir(context).absolutePath,
            "autoclipOutput" to getAutoClipOutputDir(context).absolutePath,
            
            "faissService" to getFaissServiceDir(context).absolutePath,
            "faissIndex" to getFaissIndexDir(context).absolutePath,
            "faissQuery" to getFaissQueryDir(context).absolutePath,
            "faissResults" to getFaissResultsDir(context).absolutePath
        )
    }
    
    /**
     * Clean up temporary files
     */
    fun cleanupTempFiles(context: Context): Boolean {
        return try {
            val tempDir = getTempDir(context)
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up temp files: ${e.message}", e)
            false
        }
    }
}
