package com.mira.clip.sampler

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mira.videoeditor.R
import com.mira.clip.config.ConfigProvider
import com.mira.clip.model.SampleResult
import com.mira.clip.util.SamplerIntents
import com.mira.clip.util.SamplerIo
import java.io.File

class SamplerService : Service() {

    private val channelId = "com.mira.clip.sampling"
    private val notifId = 1337

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == SamplerIntents.ACTION_SAMPLE_REQUEST) {
            val pkg = packageName
            val requestId = intent.getStringExtra(SamplerIntents.EXTRA_REQUEST_ID) ?: System.currentTimeMillis().toString()
            val uri = intent.getParcelableExtra<Uri>(SamplerIntents.EXTRA_INPUT_URI)!!
            val overrideN = intent.getIntExtra(SamplerIntents.EXTRA_FRAME_COUNT, -1)

            startForeground(notifId, notification("Sampling…"))

            Thread {
                try {
                    val cfg = ConfigProvider.defaultConfig(this)
                        .copy(frameCount = if (overrideN > 1) overrideN else ConfigProvider.defaultConfig(this).frameCount)

                    val outDir = SamplerIo.appOutDir(this, "${cfg.output.outDirName}/$requestId")
                    // TODO: Fix FrameSampler constructor - using compatibility object for now
                    progress(5, pkg, requestId)

                    // Temporary placeholder - replace with actual sampling logic
                    val sampled = object {
                        val frames = emptyList<File>()
                        val timestampsMs = emptyList<Long>()
                        val durationMs = 0L
                    }
                    progress(80, pkg, requestId)

                    // Verification + sidecar
                    val result = SampleResult(
                        requestId = requestId,
                        inputUri = uri.toString(),
                        frameCountExpected = cfg.frameCount,
                        frameCountObserved = sampled.frames.size,
                        timestampsMs = sampled.timestampsMs.toList(),
                        durationMs = sampled.durationMs,
                        frames = sampled.frames.map { it.absolutePath }
                    )
                    val json = File(outDir, "result.json")
                    SamplerIo.writeJson(json, result)
                    progress(100, pkg, requestId)

                    // Broadcast result (package-scoped)
                    sendBroadcast(
                        Intent(SamplerIntents.ACTION_SAMPLE_RESULT)
                            .putExtra(SamplerIntents.EXTRA_REQUEST_ID, requestId)
                            .putExtra(SamplerIntents.EXTRA_RESULT_JSON, json.absolutePath)
                            .setPackage(pkg)
                    )
                } catch (t: Throwable) {
                    sendBroadcast(
                        Intent(SamplerIntents.ACTION_SAMPLE_ERROR)
                            .putExtra(SamplerIntents.EXTRA_REQUEST_ID, intent?.getStringExtra(SamplerIntents.EXTRA_REQUEST_ID))
                            .putExtra(SamplerIntents.EXTRA_ERROR_MESSAGE, t.message)
                            .setPackage(packageName)
                    )
                } finally {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }.start()
        }
        return START_NOT_STICKY
    }

    private fun progress(p: Int, pkg: String, reqId: String) {
        sendBroadcast(
            Intent(SamplerIntents.ACTION_SAMPLE_PROGRESS)
                .putExtra(SamplerIntents.EXTRA_REQUEST_ID, reqId)
                .putExtra(SamplerIntents.EXTRA_PROGRESS, p)
                .setPackage(pkg)
        )
        val msg = if (p < 100) "Sampling… $p%" else "Sampling complete"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, notification(msg))
    }

    private fun notification(text: String): Notification {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Video Sampler")
            .setContentText(text)
            .setOngoing(true)
        return builder.build()
    }

    private fun createChannel() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(channelId, "Sampler", NotificationManager.IMPORTANCE_LOW)
        mgr.createNotificationChannel(ch)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
