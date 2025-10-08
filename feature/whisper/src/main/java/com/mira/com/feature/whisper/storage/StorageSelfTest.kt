package com.mira.com.feature.whisper.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import android.util.Log

/**
 * Storage self-test utilities to verify writability before processing.
 * Helps catch storage issues early and provides better error messages.
 */
object StorageSelfTest {
    
    private const val TAG = "StorageSelfTest"
    
    /**
     * Verify that a sidecar store is writable by performing a test write/delete cycle.
     * @param store The sidecar store to test
     * @param jobId Test job ID (will be cleaned up)
     * @throws Exception if storage is not writable
     */
    suspend fun assertWritable(store: SidecarStore, jobId: String) {
        val testContent = """{"test":true,"timestamp":${System.currentTimeMillis()},"store":"${store::class.simpleName}"}"""
        
        try {
            // Test write
            val uri = store.writeJson(jobId, "probe.json", testContent)
            Log.d(TAG, "Storage test write successful: $uri")
            
            // Test cleanup (optional - SAF may not allow deletion)
            runCatching {
                when (store) {
                    is AppScopedSidecarStore -> {
                        // For app-scoped storage, we can delete via File API
                        val dir = store.ensureJobDir(jobId)
                        dir.file?.let { file ->
                            val probeFile = java.io.File(file, "probe.json")
                            if (probeFile.exists()) {
                                probeFile.delete()
                                Log.d(TAG, "Storage test cleanup successful")
                            }
                        }
                    }
                    is SafSidecarStore -> {
                        // For SAF, try to delete via DocumentFile
                        val dir = store.ensureJobDir(jobId)
                        val parent = DocumentFile.fromTreeUri(store.ctx, dir.uri)
                        parent?.findFile("probe.json")?.delete()
                        Log.d(TAG, "Storage test cleanup attempted (SAF)")
                    }
                }
            }.onFailure { e ->
                Log.w(TAG, "Storage test cleanup failed (non-critical): ${e.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Storage test failed: ${e.message}", e)
            throw Exception("Storage not writable: ${e.message}", e)
        }
    }
    
    /**
     * Log persisted URI permissions for debugging.
     * Call this to verify SAF grants are properly persisted.
     */
    fun logPersistedGrants(ctx: Context) {
        val grants = ctx.contentResolver.persistedUriPermissions
        Log.i(TAG, "Persisted URI permissions: ${grants.size} grants")
        grants.forEach { permission ->
            Log.i(TAG, "Grant: uri=${permission.uri} read=${permission.isReadPermission} write=${permission.isWritePermission}")
        }
    }
    
    /**
     * Verify app-scoped storage path exists and is writable.
     * @param ctx Application context
     * @return true if app-scoped storage is available
     */
    fun verifyAppScopedStorage(ctx: Context): Boolean {
        return try {
            val baseDir = ctx.getExternalFilesDir(null)
            val workDir = java.io.File(baseDir, "MiraWhisper/out")
            workDir.mkdirs()
            workDir.canWrite()
        } catch (e: Exception) {
            Log.e(TAG, "App-scoped storage verification failed: ${e.message}", e)
            false
        }
    }
}
