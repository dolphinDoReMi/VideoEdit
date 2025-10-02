package com.mira.whisper.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class AsrFileScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val tag = "AsrFileScanWorker"
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dir = inputData.getString("dir") ?: "/data/user/0/com.mira.whisper/files/asr_in"
            
            // Read config from input data
            val common = Data.Builder()
                .putString("lang", inputData.getString("lang") ?: "auto")
                .putBoolean("translate", inputData.getBoolean("translate", false))
                .putInt("threads", inputData.getInt("threads", maxOf(1, Runtime.getRuntime().availableProcessors() - 2)))
                .putBoolean("useBeam", inputData.getBoolean("useBeam", false))
                .putInt("beamSize", inputData.getInt("beamSize", 5))
                .putFloat("patience", inputData.getFloat("patience", 1.0f))
                .putFloat("temperature", inputData.getFloat("temperature", 0.0f))
                .putBoolean("wordTimestamps", inputData.getBoolean("wordTimestamps", false))
                .build()
            
            Log.i(tag, "Scanning directory: $dir")
            
            val scanDir = File(dir)
            if (!scanDir.exists()) {
                Log.e(tag, "Scan directory does not exist: $dir")
                return@withContext Result.failure()
            }
            
            val audioFiles = scanDir.listFiles { file ->
                file.isFile && (file.extension.lowercase() in listOf("wav", "mp4"))
            } ?: emptyArray()
            
            Log.i(tag, "Found ${audioFiles.size} audio files")
            
            // Enqueue transcription jobs for each file
            for (audioFile in audioFiles) {
                val req = OneTimeWorkRequestBuilder<AsrTranscribeWorker>()
                    .setInputData(
                        Data.Builder()
                            .putAll(common)
                            .putString("path", audioFile.absolutePath)
                            .build()
                    )
                    .addTag("AsrTranscribe")
                    .setBackoffCriteria(androidx.work.BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .build()
                
                WorkManager.getInstance(applicationContext).enqueue(req)
                Log.i(tag, "Enqueued transcription job for: ${audioFile.name}")
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(tag, "Error in AsrFileScanWorker", e)
            Result.failure()
        }
    }
}
