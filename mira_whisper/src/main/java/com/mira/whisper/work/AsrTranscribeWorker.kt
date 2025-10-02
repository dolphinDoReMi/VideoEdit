package com.mira.whisper.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.mira.whisper.WhisperBridge
import com.mira.whisper.WhisperEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AsrTranscribeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val tag = "AsrTranscribeWorker"
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val path = inputData.getString("path") ?: return@withContext Result.failure()
            
            // Read config from input data
            val lang = inputData.getString("lang") ?: "auto"
            val translate = inputData.getBoolean("translate", false)
            val threads = inputData.getInt("threads", maxOf(1, Runtime.getRuntime().availableProcessors() - 2))
            val useBeam = inputData.getBoolean("useBeam", false)
            val beamSize = inputData.getInt("beamSize", 5)
            val patience = inputData.getFloat("patience", 1.0f)
            val temperature = inputData.getFloat("temperature", 0.0f)
            val wordTs = inputData.getBoolean("wordTimestamps", false)
            
            Log.i(tag, "Processing file: $path with config: lang=$lang, translate=$translate, threads=$threads")
            
            val engine = WhisperEngine(applicationContext)
            
            // Initialize engine
            val modelPath = "/data/user/0/com.mira.whisper/files/whisper.bin"
            val initSuccess = engine.start(
                modelPath = modelPath,
                language = lang,
                translate = translate,
                threads = threads
            )
            
            if (!initSuccess) {
                Log.e(tag, "Failed to initialize Whisper engine")
                return@withContext Result.failure()
            }
            
            // Set decoding parameters
            WhisperBridge.setDecodingParams(useBeam, beamSize, patience, temperature, wordTs)
            
            // Transcribe the file
            val audioFile = File(path)
            val result = engine.transcribe16k(audioFile)
            
            // Save result to JSON file
            val outputFile = File(audioFile.parent, "${audioFile.nameWithoutExtension}.json")
            outputFile.writeText(result)
            
            Log.i(tag, "Transcription completed: ${outputFile.name}")
            
            engine.close()
            Result.success()
            
        } catch (e: Exception) {
            Log.e(tag, "Error in AsrTranscribeWorker", e)
            Result.failure()
        }
    }
}
