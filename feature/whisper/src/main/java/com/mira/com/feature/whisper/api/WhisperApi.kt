package com.mira.com.feature.whisper.api

import android.content.Context
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
        val data =
            workDataOf(
                "uri" to uri,
                "model" to model,
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
}
