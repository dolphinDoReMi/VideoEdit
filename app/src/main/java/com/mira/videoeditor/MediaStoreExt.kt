package com.mira.videoeditor

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Utility extensions for MediaStore and URI handling
 */
object MediaStoreExt {
    
    private const val TAG = "MediaStoreExt"
    
    /**
     * Safely take persistable URI permission
     */
    fun ContentResolver.takePersistableUriPermission(uri: Uri, flags: Int) {
        val takeFlags = flags and
            (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        
        try {
            takePersistableUriPermission(uri, takeFlags)
            Log.d(TAG, "Successfully took persistable permission for: $uri")
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not take persistable permission for: $uri", e)
            // This is expected on first try for some URI types
        } catch (e: Exception) {
            Log.e(TAG, "Error taking persistable permission for: $uri", e)
        }
    }
    
    /**
     * Check if URI permission is available
     */
    fun ContentResolver.hasUriPermission(uri: Uri): Boolean {
        return try {
            val permissions = getPersistedUriPermissions()
            permissions.any { it.uri == uri }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking URI permission", e)
            false
        }
    }
    
    /**
     * Get file size from URI
     */
    fun ContentResolver.getFileSize(uri: Uri): Long {
        return try {
            openInputStream(uri)?.use { inputStream ->
                inputStream.available().toLong()
            } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size for: $uri", e)
            0L
        }
    }
    
    /**
     * Get MIME type from URI
     */
    fun ContentResolver.getMimeType(uri: Uri): String? {
        return try {
            getType(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting MIME type for: $uri", e)
            null
        }
    }
    
    /**
     * Validate video URI
     */
    fun ContentResolver.isValidVideoUri(uri: Uri): Boolean {
        return try {
            val mimeType = getMimeType(uri)
            mimeType?.startsWith("video/") == true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating video URI: $uri", e)
            false
        }
    }
    
    /**
     * Copy URI content to local file for testing
     */
    fun ContentResolver.copyUriToFile(uri: Uri, outputFile: File): Boolean {
        return try {
            openInputStream(uri)?.use { inputStream ->
                FileOutputStream(outputFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d(TAG, "Successfully copied URI to file: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to file", e)
            false
        }
    }
}
