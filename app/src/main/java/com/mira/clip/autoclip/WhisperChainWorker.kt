package com.mira.clip.autoclip

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class WhisperChainWorker(appCtx: Context, params: WorkerParameters) : CoroutineWorker(appCtx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val manifestUri = Uri.parse(inputData.getString(KEY_MANIFEST_URI) ?: return@withContext Result.failure(msg("missing MANIFEST_URI")))
        val model = inputData.getString(KEY_MODEL) ?: "whisper-base.q5_1.bin"
        val threads = inputData.getInt(KEY_THREADS, 4)
        val language = inputData.getString(KEY_LANGUAGE) ?: "auto"
        val translate = inputData.getBoolean(KEY_TRANSLATE, false)

        Log.d("WhisperChainWorker", "Starting Whisper chain processing for manifest: $manifestUri")

        val cr = applicationContext.contentResolver
        val manifestDoc = DocumentFile.fromSingleUri(applicationContext, manifestUri)
            ?: return@withContext Result.failure(msg("cannot open manifest"))

        // Read manifest
        val manifestJson = cr.openInputStream(manifestDoc.uri)?.use { ins ->
            ins.readBytes().decodeToString()
        } ?: return@withContext Result.failure(msg("cannot read manifest"))

        val manifest = JSONObject(manifestJson)
        val clips = manifest.getJSONArray("clips")
        val totalClips = clips.length()

        Log.d("WhisperChainWorker", "Found $totalClips clips to process")

        val workManager = WorkManager.getInstance(applicationContext)
        val whisperJobs = mutableListOf<WorkRequest>()

        // Create Whisper jobs for each clip
        for (i in 0 until totalClips) {
            val clip = clips.getJSONObject(i)
            val clipUri = Uri.parse(clip.getString("uri"))
            val clipName = clip.getString("name")
            val segmentIndex = clip.getInt("segmentIndex")

            Log.d("WhisperChainWorker", "Creating Whisper job for clip $segmentIndex: $clipName")

            // Create Whisper job data
            val whisperData = workDataOf(
                "uri" to clipUri.toString(),
                "model" to model,
                "threads" to threads,
                "lang" to language,
                "translate" to translate,
                "batch_index" to segmentIndex,
                "batch_total" to totalClips,
                "batch_id" to "tennis_interview_autoclip"
            )

            // TODO: Replace with working whisper worker
            // val whisperJob = OneTimeWorkRequestBuilder<com.mira.com.feature.whisper.runner.TranscribeWorker>()
            //     .setInputData(whisperData)
            //     .addTag("WhisperChain")
            //     .addTag("TennisInterview")
            //     .build()

            // TODO: Replace with working whisper worker
            // whisperJobs.add(whisperJob)
        }

        // Enqueue all Whisper jobs
        Log.d("WhisperChainWorker", "Enqueuing ${whisperJobs.size} Whisper jobs")
        // TODO: Replace with working whisper worker
        // workManager.enqueue(whisperJobs)

        Result.success(workDataOf(
            "whisper_jobs_enqueued" to whisperJobs.size,
            "total_clips" to totalClips
        ))
    }

    private fun msg(m: String) = workDataOf("error" to m)

    companion object {
        const val KEY_MANIFEST_URI = "MANIFEST_URI"
        const val KEY_MODEL = "MODEL"
        const val KEY_THREADS = "THREADS"
        const val KEY_LANGUAGE = "LANGUAGE"
        const val KEY_TRANSLATE = "TRANSLATE"

        fun enqueue(
            context: Context,
            manifestUri: Uri,
            model: String = "whisper-base.q5_1.bin",
            threads: Int = 4,
            language: String = "auto",
            translate: Boolean = false
        ): WorkRequest {
            val data = workDataOf(
                KEY_MANIFEST_URI to manifestUri.toString(),
                KEY_MODEL to model,
                KEY_THREADS to threads,
                KEY_LANGUAGE to language,
                KEY_TRANSLATE to translate
            )
            val req = OneTimeWorkRequestBuilder<WhisperChainWorker>()
                .setInputData(data)
                .addTag("WhisperChain")
                .build()
            WorkManager.getInstance(context).enqueue(req)
            return req
        }
    }
}
