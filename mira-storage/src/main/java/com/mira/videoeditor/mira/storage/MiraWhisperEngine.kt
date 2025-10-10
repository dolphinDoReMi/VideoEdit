package com.mira.videoeditor.mira.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scoped storage-compliant Whisper engine interface.
 * 
 * This provides the interface for Whisper functionality that will be
 * implemented by the app module to avoid circular dependencies.
 */
interface MiraWhisperEngine {
    
    /**
     * Transcribes audio from URIs using scoped storage and real Whisper processing.
     */
    suspend fun transcribe(
        audioUri: Uri,
        modelUri: Uri,
        language: String = "en",
        translate: Boolean = false,
        threads: Int = 4
    ): String
    
    /**
     * Writes transcript to app-private storage using scoped storage.
     */
    suspend fun writeTranscript(inputKey: String, transcriptText: String): String
    
    /**
     * Writes preview image to app-private storage using scoped storage.
     */
    suspend fun writePreviewImage(inputKey: String, imageBytes: ByteArray): String
    
    /**
     * Gets model information from the loaded Whisper model.
     */
    suspend fun getModelInfo(): String
}