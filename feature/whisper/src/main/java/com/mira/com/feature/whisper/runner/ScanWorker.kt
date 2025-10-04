package com.mira.com.feature.whisper.runner

import android.content.Context
import android.net.Uri
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mira.com.feature.whisper.data.db.AsrDb
import com.mira.com.feature.whisper.data.db.AsrFile
import com.mira.com.feature.whisper.util.Hash

class ScanWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val root = inputData.getString("rootDir") ?: return Result.failure()
        val dao = AsrDb.get(applicationContext).dao()
        val dir = android.webkit.URLUtil.isFileUrl(root).let { java.io.File(Uri.parse(root).path!!) }
        if (!dir.exists()) return Result.failure()
        dir.walkTopDown().filter { it.isFile && (it.name.endsWith(".wav", true) || it.name.endsWith(".mp4", true)) }.forEach { f ->
            val uri = "file://${f.absolutePath}"
            val id = Hash.sha1(uri)
            val file =
                AsrFile(
                    id,
                    uri,
                    if (f.extension == "wav") "audio/wav" else "video/mp4",
                    null,
                    null,
                    null,
                    "NEW",
                    System.currentTimeMillis(),
                )
            dao.upsertFile(file)
            // Enqueue ASR per file with default knobs (you can pass model path via Config or broadcast)
            val w =
                OneTimeWorkRequestBuilder<TranscribeWorker>()
                    .setInputData(
                        workDataOf(
                            "uri" to uri,
                            "model" to "/sdcard/MiraWhisper/models/ggml-base.en.gguf",
                            "threads" to 4,
                            "beam" to 0,
                            "lang" to "auto",
                            "translate" to false,
                        ),
                    ).build()
            WorkManager.getInstance(applicationContext).enqueue(w)
            dao.updateFile(file.copy(state = "QUEUED", updatedAtMs = System.currentTimeMillis()))
        }
        return Result.success()
    }
}
