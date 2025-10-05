package com.mira.com.feature.whisper.runner

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mira.com.core.media.AudioResampler
import com.mira.com.feature.whisper.data.db.AsrDb
import com.mira.com.feature.whisper.data.db.AsrFile
import com.mira.com.feature.whisper.data.db.AsrJob
import com.mira.com.feature.whisper.data.io.AudioIO
import com.mira.com.feature.whisper.engine.WhisperBridge
import com.mira.com.feature.whisper.engine.WhisperParams
import com.mira.com.feature.whisper.engine.LanguageDetectionService
import com.mira.com.feature.whisper.util.Hash
import com.mira.com.feature.whisper.util.Sidecars
import org.json.JSONObject
import java.io.File

class TranscribeWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val uri = inputData.getString("uri") ?: return Result.failure()
        val model = inputData.getString("model") ?: return Result.failure()
        val threads = inputData.getInt("threads", 4)
        val beam = inputData.getInt("beam", 0)
        val lang = inputData.getString("lang") ?: "auto"
        val translate = inputData.getBoolean("translate", false)
        val batchIndex = inputData.getInt("batch_index", -1)
        val batchTotal = inputData.getInt("batch_total", -1)
        val ctx = applicationContext
        val dao = AsrDb.get(ctx).dao()
        val fileId = Hash.sha1(uri)
        val jobId = if (batchIndex >= 0) {
            "batch_${batchIndex}_${System.currentTimeMillis()}_${fileId.take(8)}"
        } else {
            "wjob_${System.currentTimeMillis()}_${fileId.take(8)}"
        }
        
        Log.d("TranscribeWorker", "Starting job $jobId for $uri (batch: $batchIndex/$batchTotal)")
        
        dao.insertJob(
            AsrJob(
                jobId, fileId, model, threads, beam, lang, translate,
                System.currentTimeMillis(), null, null, null, "RUNNING", null,
            ),
        )

        return try {
            // 1) Load & condition audio
            val pcm = AudioIO.loadPcm16(ctx, Uri.parse(uri))
            val mono = AudioResampler.downmixToMono(pcm.pcm16, pcm.ch)
            val pcm16k = AudioResampler.resampleLinear(mono, pcm.sr, 16_000)

            Log.d("TranscribeWorker", "Audio loaded: ${pcm16k.size} samples, ${pcm.durationMs}ms")

            // 2) Robust LID Pipeline
            val lidResult = if (lang == "auto") {
                Log.d("TranscribeWorker", "Running robust LID pipeline...")
                val lidService = LanguageDetectionService()
                lidService.detectLanguage(pcm16k, 16_000, model, threads)
            } else {
                Log.d("TranscribeWorker", "Using forced language: $lang")
                LanguageDetectionService.LanguageDetectionResult(
                    topK = listOf(LanguageDetectionService.LanguageConfidence(lang, 1.0)),
                    chosen = lang,
                    method = "forced",
                    confidence = 1.0
                )
            }

            Log.d("TranscribeWorker", "LID Result: ${lidResult.chosen} (${lidResult.confidence}) via ${lidResult.method}")

            // 3) Decode with detected/forced language
            val t0 = SystemClock.elapsedRealtime()
            val json = WhisperBridge.decodeJson(
                pcm16 = pcm16k,
                sampleRate = 16_000,
                modelPath = model,
                threads = threads,
                beam = beam,
                lang = lidResult.chosen,
                translate = translate,
                temperature = 0.0f,
                enableWordTimestamps = false,
                detectLanguage = false, // Already done above
                noContext = true
            )
            val t1 = SystemClock.elapsedRealtime()
            val inferMs = t1 - t0
            val rtf = inferMs.toDouble() / pcm.durationMs.coerceAtLeast(1)

            Log.d("TranscribeWorker", "Transcription complete: ${inferMs}ms, RTF: $rtf")

            // 4) Enhanced sidecar with LID data
            val sidecar =
                Sidecars.build(
                    uri = uri,
                    durationMs = pcm.durationMs,
                    params = WhisperParams(model, threads, beam, lidResult.chosen, translate),
                    inferMs = inferMs,
                    rtf = rtf,
                    segmentsJson = JSONObject(json).optJSONArray("segments"),
                )

            // Add LID data to sidecar
            val lidService = LanguageDetectionService()
            val lidSidecar = lidService.generateLidSidecar(lidResult)
            sidecar.put("lid", lidSidecar)

            Log.d("TranscribeWorker", "Enhanced sidecar with LID data")
            val outDir = File("/sdcard/MiraWhisper/out").apply { mkdirs() }
            val sidecarDir = File("/sdcard/MiraWhisper/sidecars").apply { mkdirs() }
            
            // Generate batch-specific sidecar filename
            val sidecarFilename = if (batchIndex >= 0) {
                "batch_${batchIndex}_${fileId}_$jobId.json"
            } else {
                "${fileId}_$jobId.json"
            }
            
            val sidecarPath = File(sidecarDir, sidecarFilename).absolutePath
            File(sidecarPath).writeText(sidecar.toString())
            
            Log.d("TranscribeWorker", "Generated sidecar: $sidecarPath")

            // 4) Persist file state & segments
            dao.updateFile(AsrFile(fileId, uri, null, pcm.durationMs, 16_000, 1, "DONE", System.currentTimeMillis()))
            val segs = Sidecars.segmentsFrom(sidecar, jobId)
            dao.insertSegments(segs)
            dao.finishJob(jobId, inferMs, rtf, "DONE", sidecarPath, null)
            
            if (batchIndex >= 0) {
                Log.d("TranscribeWorker", "Completed robust LID batch job $jobId ($batchIndex/$batchTotal) - Language: ${lidResult.chosen}, RTF: $rtf")
            } else {
                Log.d("TranscribeWorker", "Completed robust LID job $jobId - Language: ${lidResult.chosen}, RTF: $rtf")
            }
            
            Result.success()
        } catch (t: Throwable) {
            dao.finishJob(jobId, null, null, "ERROR", null, t.message)
            dao.updateFile(AsrFile(fileId, uri, null, null, null, null, "ERROR", System.currentTimeMillis()))
            Result.failure()
        }
    }
}
