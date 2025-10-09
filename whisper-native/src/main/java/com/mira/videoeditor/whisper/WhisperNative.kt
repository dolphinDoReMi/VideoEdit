package com.mira.videoeditor.whisper

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileDescriptor

/**
 * Native Whisper implementation with zero-copy operations and real processing.
 * 
 * This provides direct access to Whisper functionality with minimal overhead
 * and no mock data. All operations use real Whisper processing.
 */
object WhisperNative {
    
    companion object {
        private const val TAG = "WhisperNative"
        
        init {
            try {
                System.loadLibrary("whisper_native")
                Log.d(TAG, "✅ Whisper native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "❌ Failed to load Whisper native library", e)
                throw e
            }
        }
    }
    
    /**
     * Initialize Whisper model from file path.
     * 
     * @param modelPath Path to the Whisper model file
     * @return True if initialization successful
     */
    suspend fun initModel(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Whisper model from: $modelPath")
            val contextPtr = initModelFromPath(modelPath)
            val success = contextPtr != 0L
            if (success) {
                Log.d(TAG, "✅ Whisper model initialized successfully")
            } else {
                Log.e(TAG, "❌ Failed to initialize Whisper model")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Exception during model initialization", e)
            false
        }
    }
    
    /**
     * Transcribe audio from file descriptor with zero-copy operations.
     * 
     * @param fd File descriptor to audio data
     * @param sampleRate Sample rate of the audio
     * @param channels Number of audio channels
     * @return JSON string with transcription results
     */
    suspend fun transcribeFromFd(
        fd: FileDescriptor,
        sampleRate: Int = 16000,
        channels: Int = 1
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting transcription from FD with sampleRate: $sampleRate, channels: $channels")
            
            // Convert FileDescriptor to int for JNI
            val fdInt = try {
                val fdField = fd.javaClass.getDeclaredField("descriptor")
                fdField.isAccessible = true
                fdField.getInt(fd)
            } catch (e: Exception) {
                Log.w(TAG, "Could not get FD descriptor, using default", e)
                0
            }
            
            val result = transcribeFromFdNative(fdInt, sampleRate, channels)
            Log.d(TAG, "✅ Transcription completed successfully")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception during transcription", e)
            """{"error":"${e.message}"}"""
        }
    }
    
    /**
     * Get model information.
     * 
     * @return JSON string with model information
     */
    suspend fun getModelInfo(): String = withContext(Dispatchers.IO) {
        try {
            getModelInfoNative()
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting model info", e)
            """{"error":"${e.message}"}"""
        }
    }
    
    /**
     * Free Whisper context.
     */
    suspend fun freeContext() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Freeing Whisper context")
            freeContextNative()
            Log.d(TAG, "✅ Whisper context freed")
        } catch (e: Exception) {
            Log.e(TAG, "Exception freeing context", e)
        }
    }
    
    // Native function declarations
    private external fun initModelFromPath(modelPath: String): Long
    private external fun transcribeFromFdNative(fd: Int, sampleRate: Int, channels: Int): String
    private external fun getModelInfoNative(): String
    private external fun freeContextNative()
}
