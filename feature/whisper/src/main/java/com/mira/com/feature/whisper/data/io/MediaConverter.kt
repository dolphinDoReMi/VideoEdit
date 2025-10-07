package com.mira.com.feature.whisper.data.io

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Media converter that converts all media files to H.264 format before processing.
 * This ensures reliability by using a consistent, well-supported format.
 */
object MediaConverter {
    
    private const val TAG = "MediaConverter"
    
    /**
     * Convert any media file to H.264 format for reliable processing.
     * For now, this is a placeholder that will be implemented with FFmpeg.
     */
    fun convertToH264(
        context: Context,
        inputUri: Uri,
        outputFileName: String? = null
    ): Uri? {
        return try {
            Log.d(TAG, "TECHNICAL: Starting H.264 conversion for $inputUri")

            // Create output file in Documents folder for easy access
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ConvertedMedia")
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            val fileName = outputFileName ?: "converted_${System.currentTimeMillis()}.mp4"
            val outputFile = File(documentsDir, fileName)

            // Delete existing file if it exists
            if (outputFile.exists()) {
                outputFile.delete()
            }

            Log.d(TAG, "TECHNICAL: Output file: ${outputFile.absolutePath}")

            // For now, just copy the file and rename it to .mp4
            // This is a temporary solution until FFmpeg integration
            val inputStream = context.contentResolver.openInputStream(inputUri)
            if (inputStream != null) {
                val outputStream = FileOutputStream(outputFile)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                Log.d(TAG, "TECHNICAL: File copied to H.264 container: ${outputFile.absolutePath}")
                Log.d(TAG, "TECHNICAL: Converted file saved to Documents/ConvertedMedia/ for easy access")
                Uri.fromFile(outputFile)
            } else {
                Log.e(TAG, "TECHNICAL: Could not open input stream for $inputUri")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "TECHNICAL: H.264 conversion error: ${e.message}", e)
            null
        }
    }
    
    /**
     * Check if a file needs conversion to H.264.
     * Returns true if the file is not already in H.264 format.
     */
    fun needsConversion(uri: Uri): Boolean {
        val path = uri.toString().lowercase()
        return !path.endsWith(".mp4") || path.contains("mkv") || path.contains("avi") || path.contains("mov")
    }
    
    /**
     * Clean up converted files older than 24 hours.
     */
    fun cleanupOldFiles(context: Context) {
        try {
            // Clean up cache directory
            val cacheDir = File(context.cacheDir, "converted_media")
            if (cacheDir.exists()) {
                val files = cacheDir.listFiles()
                val currentTime = System.currentTimeMillis()
                val maxAge = 24 * 60 * 60 * 1000L // 24 hours

                files?.forEach { file ->
                    if (currentTime - file.lastModified() > maxAge) {
                        Log.d(TAG, "TECHNICAL: Cleaning up old converted file: ${file.name}")
                        file.delete()
                    }
                }
            }
            
            // Clean up Documents/ConvertedMedia directory
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ConvertedMedia")
            if (documentsDir.exists()) {
                val files = documentsDir.listFiles()
                val currentTime = System.currentTimeMillis()
                val maxAge = 24 * 60 * 60 * 1000L // 24 hours

                files?.forEach { file ->
                    if (currentTime - file.lastModified() > maxAge) {
                        Log.d(TAG, "TECHNICAL: Cleaning up old converted file in Documents: ${file.name}")
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TECHNICAL: Error cleaning up old files: ${e.message}", e)
        }
    }
}
