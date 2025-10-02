package com.mira.whisper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WhisperEngine(private val context: Context) {
    private val tag = "WhisperEngine"
    private var isInitialized = false
    
    suspend fun start(
        modelPath: String = "/data/user/0/com.mira.whisper/files/whisper.bin",
        language: String = "auto",
        translate: Boolean = false,
        threads: Int = 2
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(tag, "Starting Whisper engine with model: $modelPath")
            
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                Log.e(tag, "Model file not found: $modelPath")
                return@withContext false
            }
            
            val success = WhisperBridge.init(modelPath, language, translate, threads)
            isInitialized = success
            
            if (success) {
                Log.i(tag, "Whisper engine started successfully")
            } else {
                Log.e(tag, "Failed to start Whisper engine")
            }
            
            success
        } catch (e: Exception) {
            Log.e(tag, "Error starting Whisper engine", e)
            false
        }
    }
    
    suspend fun transcribe16k(audioFile: File): String = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.e(tag, "Engine not initialized")
            return@withContext "{\"error\": \"Engine not initialized\"}"
        }
        
        try {
            Log.i(tag, "Transcribing audio file: ${audioFile.name}")
            
            // Read audio file and convert to 16kHz mono PCM
            val pcmData = readAudioFile(audioFile)
            if (pcmData.isEmpty()) {
                Log.e(tag, "Failed to read audio file")
                return@withContext "{\"error\": \"Failed to read audio file\"}"
            }
            
            val result = WhisperBridge.transcribe(pcmData, 16000)
            Log.i(tag, "Transcription completed")
            
            result
        } catch (e: Exception) {
            Log.e(tag, "Error during transcription", e)
            "{\"error\": \"Transcription failed: ${e.message}\"}"
        }
    }
    
    private fun readAudioFile(audioFile: File): ShortArray {
        return try {
            val inputStream = FileInputStream(audioFile)
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            // Convert bytes to shorts (assuming 16-bit PCM)
            val shorts = ShortArray(bytes.size / 2)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            buffer.asShortBuffer().get(shorts)
            
            shorts
        } catch (e: Exception) {
            Log.e(tag, "Error reading audio file", e)
            ShortArray(0)
        }
    }
    
    fun close() {
        if (isInitialized) {
            WhisperBridge.close()
            isInitialized = false
            Log.i(tag, "Whisper engine closed")
        }
    }
}
