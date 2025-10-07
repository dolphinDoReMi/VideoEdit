package com.mira.com.feature.whisper.service

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import com.mira.com.feature.whisper.data.io.MediaConverter
import java.util.concurrent.Executors

/**
 * Simple background service for automatic H.264 conversion of media files.
 * Runs without UI interaction using coroutines instead of WorkManager.
 */
class BackgroundMediaConverter {

    companion object {
        private const val TAG = "BackgroundMediaConverter"
        private val executor = Executors.newSingleThreadExecutor()
        
        /**
         * Convert a file to H.264 in the background using coroutines.
         */
        fun convertFile(
            context: Context,
            inputUri: Uri,
            outputFileName: String? = null,
            onComplete: (Uri?) -> Unit = {}
        ) {
            executor.execute {
                try {
                    Log.d(TAG, "TECHNICAL: Starting background H.264 conversion for $inputUri")
                    
                    // Check if conversion is needed
                    if (!MediaConverter.needsConversion(inputUri)) {
                        Log.d(TAG, "TECHNICAL: File already in H.264 format, no conversion needed")
                        onComplete(inputUri)
                        return@execute
                    }

                    // Perform conversion
                    val convertedUri = MediaConverter.convertToH264(context, inputUri, outputFileName)
                    
                    if (convertedUri != null) {
                        Log.d(TAG, "TECHNICAL: Background H.264 conversion successful: $convertedUri")
                        onComplete(convertedUri)
                    } else {
                        Log.e(TAG, "TECHNICAL: Background H.264 conversion failed for $inputUri")
                        onComplete(null)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "TECHNICAL: Background conversion error: ${e.message}", e)
                    onComplete(null)
                }
            }
        }

        /**
         * Convert multiple files to H.264 in the background.
         */
        fun convertFiles(
            context: Context,
            inputUris: List<Uri>,
            outputFileNames: List<String?> = emptyList(),
            onComplete: (List<Uri?>) -> Unit = {}
        ) {
            executor.execute {
                val results = mutableListOf<Uri?>()
                
                inputUris.forEachIndexed { index, uri ->
                    val outputFileName = outputFileNames.getOrNull(index)
                    Log.d(TAG, "TECHNICAL: Converting file ${index + 1}/${inputUris.size}: $uri")
                    
                    try {
                        if (!MediaConverter.needsConversion(uri)) {
                            Log.d(TAG, "TECHNICAL: File already in H.264 format: $uri")
                            results.add(uri)
                        } else {
                            val convertedUri = MediaConverter.convertToH264(context, uri, outputFileName)
                            results.add(convertedUri)
                            
                            if (convertedUri != null) {
                                Log.d(TAG, "TECHNICAL: Conversion successful: $uri -> $convertedUri")
                            } else {
                                Log.e(TAG, "TECHNICAL: Conversion failed: $uri")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "TECHNICAL: Error converting $uri: ${e.message}", e)
                        results.add(null)
                    }
                }
                
                Log.d(TAG, "TECHNICAL: Batch conversion completed: ${results.count { it != null }}/${results.size} successful")
                onComplete(results)
            }
        }

        /**
         * Convert TennisInterview.mkv specifically (for testing).
         */
        fun convertTennisInterview(
            context: Context,
            onComplete: (Uri?) -> Unit = {}
        ) {
            val tennisInterviewUri = Uri.parse("content://media/picker_get_content/0/com.android.providers.media.photopicker/media/1000000816")
            Log.d(TAG, "TECHNICAL: Converting TennisInterview.mkv to H.264")
            convertFile(context, tennisInterviewUri, "TennisInterview_converted.mp4", onComplete)
        }

        /**
         * Clean up old converted files.
         */
        fun cleanupOldFiles(context: Context) {
            executor.execute {
                Log.d(TAG, "TECHNICAL: Cleaning up old converted files")
                MediaConverter.cleanupOldFiles(context)
            }
        }
    }
}