package com.mira.com.feature.whisper.runner

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mira.com.core.media.AudioResampler
import com.mira.com.feature.whisper.data.db.AsrDb
import com.mira.com.feature.whisper.data.db.AsrFile
import com.mira.com.feature.whisper.data.db.AsrJob
import com.mira.com.feature.whisper.data.io.AudioIO
import com.mira.com.feature.whisper.engine.WhisperBridge
import com.mira.com.feature.whisper.engine.WhisperParams
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
        val ctx = applicationContext
        val dao = AsrDb.get(ctx).dao()
        val fileId = Hash.sha1(uri)
        val jobId = "wjob_${System.currentTimeMillis()}_${fileId.take(8)}"
        dao.insertJob(
            AsrJob(
                jobId, fileId, model, threads, beam, lang, translate,
                System.currentTimeMillis(), null, null, null, "RUNNING", null,
            ),
        )

        return try {
            // 1) Load & condition
            val pcm = AudioIO.loadPcm16(ctx, Uri.parse(uri))
            val mono = AudioResampler.downmixToMono(pcm.pcm16, pcm.ch)
            val pcm16k = AudioResampler.resampleLinear(mono, pcm.sr, 16_000)

            // 2) Decode (JNI) with timing
            val t0 = SystemClock.elapsedRealtime()
            val json = WhisperBridge.decodeJson(pcm16k, 16_000, model, threads, beam, lang, translate)
            val t1 = SystemClock.elapsedRealtime()
            val inferMs = t1 - t0
            val rtf = inferMs.toDouble() / pcm.durationMs.coerceAtLeast(1)

            // 3) Sidecar + DB
            val sidecar =
                Sidecars.build(
                    uri = uri,
                    durationMs = pcm.durationMs,
                    params = WhisperParams(model, threads, beam, lang, translate),
                    inferMs = inferMs,
                    rtf = rtf,
                    segmentsJson = JSONObject(json).optJSONArray("segments"),
                )
            val outDir = File("/sdcard/Mira/out").apply { mkdirs() }
            val sidecarPath = File(outDir, "${fileId}_$jobId.json").absolutePath
            File(sidecarPath).writeText(sidecar.toString())

            // 4) Persist file state & segments
            dao.updateFile(AsrFile(fileId, uri, null, pcm.durationMs, 16_000, 1, "DONE", System.currentTimeMillis()))
            val segs = Sidecars.segmentsFrom(sidecar, jobId)
            dao.insertSegments(segs)
            dao.finishJob(jobId, inferMs, rtf, "DONE", sidecarPath, null)
            Result.success()
        } catch (t: Throwable) {
            dao.finishJob(jobId, null, null, "ERROR", null, t.message)
            dao.updateFile(AsrFile(fileId, uri, null, null, null, null, "ERROR", System.currentTimeMillis()))
            Result.failure()
        }
    }
}
