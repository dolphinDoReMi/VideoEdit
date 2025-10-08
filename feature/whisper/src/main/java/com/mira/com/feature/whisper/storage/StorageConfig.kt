package com.mira.com.feature.whisper.storage

import android.content.Context
import android.net.Uri

/**
 * Global storage configuration for sidecar operations.
 * 
 * P0: All worker writes go to app-scoped storage (no permissions needed)
 * P1: Optional export to user-picked SAF tree (requires persisted grant)
 */
object StorageConfig {
    
    /**
     * Get the primary sidecar store for worker operations.
     * This uses app-scoped storage which requires no runtime permissions.
     */
    fun workSidecarStore(ctx: Context): SidecarStore =
        AppScopedSidecarStore(ctx)
    
    /**
     * Optional export tree URI (user-picked via SAF).
     * Set when user explicitly chooses an export location.
     */
    @Volatile var exportTreeUri: Uri? = null
    
    /**
     * Get export sidecar store if available.
     * Returns null if no export tree has been set.
     */
    fun exportSidecarStore(ctx: Context): SidecarStore? {
        val treeUri = exportTreeUri ?: return null
        return SafSidecarStore(ctx, treeUri)
    }
}
