package com.mira.com.feature.whisper.service

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mira.com.feature.whisper.data.io.MediaConverter

/**
 * Simple media conversion manager that coordinates H.264 conversions.
 * Provides a unified interface for scheduling and managing conversions.
 */
class MediaConversionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MediaConversionManager"
        
        @Volatile
        private var INSTANCE: MediaConversionManager? = null

        fun getInstance(context: Context): MediaConversionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaConversionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Convert a single file to H.264 in the background.
     */
    fun convertFile(
        inputUri: Uri,
        outputFileName: String? = null,
        onComplete: (Uri?) -> Unit = {}
    ) {
        Log.d(TAG, "TECHNICAL: Scheduling H.264 conversion for $inputUri")
        BackgroundMediaConverter.convertFile(context, inputUri, outputFileName, onComplete)
    }

    /**
     * Convert multiple files to H.264 in the background.
     */
    fun convertFiles(
        inputUris: List<Uri>,
        outputFileNames: List<String?> = emptyList(),
        onComplete: (List<Uri?>) -> Unit = {}
    ) {
        Log.d(TAG, "TECHNICAL: Scheduling batch H.264 conversion for ${inputUris.size} files")
        BackgroundMediaConverter.convertFiles(context, inputUris, outputFileNames, onComplete)
    }

    /**
     * Check if a file needs H.264 conversion.
     */
    fun needsConversion(uri: Uri): Boolean {
        return MediaConverter.needsConversion(uri)
    }

    /**
     * Clean up old converted files.
     */
    fun cleanupOldFiles() {
        Log.d(TAG, "TECHNICAL: Cleaning up old converted files")
        BackgroundMediaConverter.cleanupOldFiles(context)
    }

    /**
     * Convert TennisInterview.mkv specifically (for testing).
     */
    fun convertTennisInterview(onComplete: (Uri?) -> Unit = {}) {
        Log.d(TAG, "TECHNICAL: Converting TennisInterview.mkv to H.264")
        BackgroundMediaConverter.convertTennisInterview(context, onComplete)
    }
}