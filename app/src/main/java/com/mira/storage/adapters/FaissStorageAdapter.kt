package com.mira.storage.adapters

import android.content.Context
import android.util.Log
import com.mira.storage.ScopedStorageManager
import java.io.File

/**
 * FAISS Service Storage Adapter
 * 
 * Provides FAISS-specific storage access through the unified scoped storage infrastructure.
 * All FAISS services MUST use this adapter instead of hardcoded paths.
 */
object FaissStorageAdapter {
    
    private const val TAG = "FaissStorageAdapter"
    
    /**
     * Get the FAISS index directory path
     */
    fun getIndexDirPath(context: Context): String {
        return ScopedStorageManager.getFaissIndexDir(context).absolutePath
    }
    
    /**
     * Get the FAISS query directory path
     */
    fun getQueryDirPath(context: Context): String {
        return ScopedStorageManager.getFaissQueryDir(context).absolutePath
    }
    
    /**
     * Get the FAISS results directory path
     */
    fun getResultsDirPath(context: Context): String {
        return ScopedStorageManager.getFaissResultsDir(context).absolutePath
    }
    
    /**
     * Get variant-specific index directory
     */
    fun getVariantIndexDir(context: Context, variant: String): File {
        return File(ScopedStorageManager.getFaissIndexDir(context), variant)
    }
    
    /**
     * Get job-specific results directory
     */
    fun getJobResultsDir(context: Context, jobId: String): File {
        return ScopedStorageManager.getJobOutputDir(context, "faiss", jobId)
    }
    
    /**
     * Get index file for a specific variant
     */
    fun getIndexFile(context: Context, variant: String, indexType: String): File {
        val variantDir = getVariantIndexDir(context, variant)
        return File(variantDir, "index_$indexType.bin")
    }
    
    /**
     * Get query vector file
     */
    fun getQueryVectorFile(context: Context, queryId: String): File {
        return File(ScopedStorageManager.getFaissQueryDir(context), "$queryId.f32")
    }
    
    /**
     * Get results file for a specific query
     */
    fun getResultsFile(context: Context, queryId: String): File {
        return File(ScopedStorageManager.getFaissResultsDir(context), "$queryId.json")
    }
    
    /**
     * Get bind file for index-embedding binding
     */
    fun getBindFile(context: Context, variant: String): File {
        val variantDir = getVariantIndexDir(context, variant)
        return File(variantDir, "BIND.txt")
    }
    
    /**
     * Get logs directory for FAISS processing logs
     */
    fun getLogsDir(context: Context): File {
        return ScopedStorageManager.getFaissLogsDir(context)
    }
    
    /**
     * Ensure FAISS directory structure exists
     */
    fun ensureDirectories(context: Context): Boolean {
        return ScopedStorageManager.ensureDirectoryStructure(context)
    }
    
    /**
     * Get FAISS storage info for debugging
     */
    fun getStorageInfo(context: Context): Map<String, String> {
        return mapOf(
            "serviceDir" to ScopedStorageManager.getFaissServiceDir(context).absolutePath,
            "indexDir" to ScopedStorageManager.getFaissIndexDir(context).absolutePath,
            "queryDir" to ScopedStorageManager.getFaissQueryDir(context).absolutePath,
            "resultsDir" to ScopedStorageManager.getFaissResultsDir(context).absolutePath,
            "logsDir" to ScopedStorageManager.getFaissLogsDir(context).absolutePath
        )
    }
}
