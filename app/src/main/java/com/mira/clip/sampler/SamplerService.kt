package com.mira.clip.sampler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
// import com.mira.clip.model.SampleConfig
// import com.mira.clip.model.SampleResult
// import com.mira.clip.util.SamplerIntents
// import com.mira.clip.util.SamplerIo
import android.net.Uri
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
        // Stub implementation - SamplerService functionality disabled for now
        Log.d("SamplerService", "SamplerService called but functionality disabled")
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