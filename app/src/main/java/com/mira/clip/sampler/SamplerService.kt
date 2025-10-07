package com.mira.clip.sampler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mira.clip.model.SampleConfig
import com.mira.clip.model.SampleResult
import com.mira.clip.util.SamplerIntents
import com.mira.clip.util.SamplerIo
import java.io.File

class SamplerService : Service() {
    
    // Direct callback support instead of broadcasts
    private var resultCallback: ((String, String) -> Unit)? = null
    private var errorCallback: ((String, String?) -> Unit)? = null
    private var progressCallback: ((String, Int) -> Unit)? = null
    
    /**
     * Register callbacks for direct communication
     */
    fun setResultCallback(callback: (String, String) -> Unit) {
        resultCallback = callback
    }
    
    fun setErrorCallback(callback: (String, String?) -> Unit) {
        errorCallback = callback
    }
    
    fun setProgressCallback(callback: (String, Int) -> Unit) {
        progressCallback = callback
    }
    
    /**
     * Clear all callbacks
     */
    fun clearCallbacks() {
        resultCallback = null
        errorCallback = null
        progressCallback = null
    }

    private val channelId = "com.mira.clip.sampling"
    private val notifId = 1337

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(channelId, "Sampling", NotificationManager.IMPORTANCE_LOW)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.getStringExtra(SamplerIntents.EXTRA_URI)?.let { android.net.Uri.parse(it) }
        val cfg = intent?.getStringExtra(SamplerIntents.EXTRA_CONFIG)?.let { SamplerIo.readConfig(it) }
        val pkg = intent?.getStringExtra(SamplerIntents.EXTRA_PACKAGE) ?: packageName
        val requestId = intent?.getStringExtra(SamplerIntents.EXTRA_REQUEST_ID) ?: "unknown"

        if (uri == null || cfg == null) {
            return START_NOT_STICKY
        }

        startForeground(notifId, notification("Sampling…"))
        Thread {
            try {
                val outDir = File(getExternalFilesDir(null), "sampler/$requestId")
                outDir.mkdirs()
                val sampled = SamplerIo.sample(uri, cfg, outDir) { p ->
                    progress(p, pkg, requestId)
                }

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

                // Call result callback directly
                resultCallback?.invoke(requestId, json.absolutePath)
            } catch (t: Throwable) {
                // Call error callback directly
                errorCallback?.invoke(requestId, t.message)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }.start()
        return START_NOT_STICKY
    }

    private fun progress(p: Int, pkg: String, reqId: String) {
        // Call progress callback directly
        progressCallback?.invoke(reqId, p)
        
        val msg = if (p < 100) "Sampling… $p%" else "Sampling complete"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, notification(msg))
    }

    private fun notification(text: String): Notification {
        val builder = NotificationCompat.Builder(this, channelId)
        builder.setContentTitle("Sampling")
        builder.setContentText(text)
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
        builder.setOngoing(true)
        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}