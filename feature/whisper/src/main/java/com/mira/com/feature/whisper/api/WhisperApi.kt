package com.mira.com.feature.whisper.api

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mira.com.feature.whisper.runner.ScanWorker
import com.mira.com.feature.whisper.runner.TranscribeWorker
import java.util.concurrent.TimeUnit

object WhisperApi {
    fun enqueueScan(
        ctx: Context,
        rootDir: String,
    ) {
        val work =
            OneTimeWorkRequestBuilder<ScanWorker>()
                .setInputData(workDataOf("rootDir" to rootDir))
                .build()
        WorkManager.getInstance(ctx).enqueue(work)
    }

    fun enqueueTranscribe(
        ctx: Context,
        uri: String,
        model: String,
        threads: Int,
        beam: Int,
        lang: String?,
        translate: Boolean,
    ) {
        // Use multilingual model by default for robust LID
        val multilingualModel = if (model.contains(".en")) {
            model.replace(".en", "").replace("tiny", "base")
        } else {
            model
        }
        
        val data =
            workDataOf(
                "uri" to uri,
                "model" to multilingualModel,
                "threads" to threads,
                "beam" to beam,
                "lang" to (lang ?: "auto"),
                "translate" to translate,
            )
        val work =
            OneTimeWorkRequestBuilder<TranscribeWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(data).build()
        WorkManager.getInstance(ctx).enqueue(work)
    }

    fun enqueueBatchTranscribe(
        ctx: Context,
        uris: List<String>,
        model: String,
        threads: Int = 4,
        beam: Int = 0,
        lang: String? = null,
        translate: Boolean = false,
    ) {
        // Use multilingual model by default for robust LID
        val multilingualModel = if (model.contains(".en")) {
            model.replace(".en", "").replace("tiny", "base")
        } else {
            model
        }
        Log.d("WhisperApi", "Enqueuing batch transcription for ${uris.size} files")
        
        uris.forEachIndexed { index, uri ->
            val data = workDataOf(
                "uri" to uri,
                "model" to multilingualModel,
                "threads" to threads,
                "beam" to beam,
                "lang" to (lang ?: "auto"),
                "translate" to translate,
                "batch_index" to index,
                "batch_total" to uris.size
            )
            
            val work = OneTimeWorkRequestBuilder<TranscribeWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(data)
                .addTag("batch_transcribe")
                .build()
                
            WorkManager.getInstance(ctx).enqueue(work)
            
            Log.d("WhisperApi", "Enqueued batch job ${index + 1}/${uris.size} for $uri")
        }
        
        Log.d("WhisperApi", "All ${uris.size} batch jobs enqueued")
    }
}
