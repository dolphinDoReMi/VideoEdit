package com.mira.videoeditor

import android.content.Context
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.util.Log
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.media.MediaScannerConnection
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

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
            Logger.debug(Logger.Category.STORAGE, "Successfully took persistable permission", mapOf(
                "uri" to uri.toString().takeLast(50)
            ))
        } catch (e: SecurityException) {
            Logger.warn(Logger.Category.STORAGE, "Could not take persistable permission", mapOf(
                "uri" to uri.toString().takeLast(50),
                "reason" to "SecurityException"
            ))
            // This is expected on first try for some URI types
        } catch (e: Exception) {
            Logger.logError(Logger.Category.STORAGE, "Taking persistable permission", e.message ?: "Unknown error", mapOf(
                "uri" to uri.toString().takeLast(50)
            ), e)
        }
    }
    
    /**
     * Check if URI permission is available
     */
    fun ContentResolver.hasUriPermission(uri: Uri): Boolean {
        return try {
            val permissions = getPersistedUriPermissions()
            val hasPermission = permissions.any { it.uri == uri }
            Logger.debug(Logger.Category.STORAGE, "Checked URI permission", mapOf(
                "uri" to uri.toString().takeLast(50),
                "hasPermission" to hasPermission
            ))
            hasPermission
        } catch (e: Exception) {
            Logger.logError(Logger.Category.STORAGE, "Checking URI permission", e.message ?: "Unknown error", mapOf(
                "uri" to uri.toString().takeLast(50)
            ), e)
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

    /**
     * Save a local video file into the system Photos/Gallery via MediaStore.
     * Returns the content Uri if successful, otherwise null.
     */
    fun saveVideoToGallery(
        context: Context,
        sourceFile: File,
        displayName: String = defaultVideoName(),
        relativeDir: String = Environment.DIRECTORY_MOVIES + "/Mira"
    ): Uri? {
        return try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                Log.e(TAG, "Source file missing or empty: ${sourceFile.absolutePath}")
                return null
            }

            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val itemUri = resolver.insert(collection, values)
            if (itemUri == null) {
                Log.e(TAG, "Failed to insert into MediaStore: $displayName")
                return null
            }

            resolver.openOutputStream(itemUri)?.use { out: OutputStream ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: run {
                Log.e(TAG, "Failed to open output stream for: $itemUri")
                return null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val finalize = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(itemUri, finalize, null, null)
            } else {
                // Pre-Android Q: request media scan
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(sourceFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
            }

            Log.d(TAG, "Saved video to Photos: $itemUri (from ${sourceFile.absolutePath})")
            itemUri
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving video to Photos", e)
            null
        }
    }

    private fun defaultVideoName(): String {
        val ts = System.currentTimeMillis()
        return "mira_${ts}.mp4"
    }
}
