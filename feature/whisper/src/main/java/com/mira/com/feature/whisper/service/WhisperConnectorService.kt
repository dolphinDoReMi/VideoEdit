package com.mira.com.feature.whisper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.mira.com.core.ml.WhisperBridge as CoreWhisperBridge
import com.mira.com.feature.whisper.engine.WhisperBridge as FeatureWhisperBridge
import com.mira.com.feature.whisper.data.io.AudioIO

/**
 * Foreground service for headless Whisper processing on Xiaomi/HyperOS.
 * 
 * Provides deterministic lifecycle management and survives OEM background restrictions
 * by running as a proper foreground service with persistent notification.
 */
class WhisperConnectorService : Service() {

    companion object {
        private const val TAG = "WhisperConnectorService"
        const val ACTION_START_PROCESSING = "com.mira.whisper.action.START_PROCESSING"
        const val ACTION_STOP_ALL = "com.mira.whisper.action.STOP_ALL"
        const val ACTION_CANCEL_PROCESSING = "com.mira.whisper.action.CANCEL_PROCESSING"
        const val ACTION_RESET_NATIVE = "com.mira.whisper.action.RESET_NATIVE"

        const val EXTRA_BATCH_ID = "extra_batch_id"
        const val EXTRA_URIS = "extra_uris"
        const val EXTRA_FILE_COUNT = "extra_file_count"

        private const val CHANNEL_ID = "whisper_fg"
        private const val NOTIF_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WhisperConnectorService created")
        
        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Whisper Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background transcription processing"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create and show foreground notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Transcribing in background")
            .setContentText("Processing audio files...")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        startForeground(NOTIF_ID, notification)
        Log.d(TAG, "Started as foreground service")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_CANCEL_PROCESSING -> {
                val batchId = intent.getStringExtra(EXTRA_BATCH_ID)
                if (!batchId.isNullOrBlank()) {
                    WorkManager.getInstance(applicationContext).cancelAllWorkByTag(batchId)
                    Log.d(TAG, "Cancelled WorkManager jobs for batch $batchId")
                }
                cleanupFor(batchId)
            }
            
            ACTION_STOP_ALL -> {
                WorkManager.getInstance(applicationContext).cancelAllWorkByTag("whisper")
                Log.d(TAG, "Requested stop-all; cancelling tag 'whisper'")
                cleanupAll()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            
            ACTION_RESET_NATIVE -> {
                try {
                    FeatureWhisperBridge.resetContext()
                    Log.d(TAG, "Native Whisper context reset via service action")
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to reset native context", t)
                }
            }
            
            ACTION_START_PROCESSING -> {
                val batchId = intent.getStringExtra(EXTRA_BATCH_ID)
                val uris = intent.getStringArrayListExtra(EXTRA_URIS)
                val fileCount = intent.getIntExtra(EXTRA_FILE_COUNT, 0)
                
                Log.d(TAG, "Starting processing for batch $batchId with $fileCount files")
                
                // Update notification with batch info
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle("Transcribing in background")
                    .setContentText("Processing batch $batchId ($fileCount files)")
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .build()
                
                startForeground(NOTIF_ID, notification)
                
                // TODO: Enqueue actual WorkManager jobs here
                // For now, just log the action
                Log.d(TAG, "Would enqueue WorkManager jobs for batch $batchId")
            }
            
            else -> {
                Log.d(TAG, "Unknown action: ${intent?.action}")
            }
        }
        
        // Return START_STICKY to ensure service restarts if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This is a started service, not bound
        return null
    }

    override fun onDestroy() {
        Log.d(TAG, "WhisperConnectorService destroyed")
        cleanupAll()
        super.onDestroy()
    }

    /**
     * Clean up resources for a specific batch
     */
    private fun cleanupFor(batchId: String?) {
        try {
            FeatureWhisperBridge.resetContext()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to reset WhisperBridge context", t)
        }
        
        try {
            AudioIO.clearCodecPool()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to clear AudioIO codec pool", t)
        }
        
        Log.d(TAG, "Cleanup completed for batch ${batchId ?: "unknown"}")
    }

    /**
     * Clean up all resources
     */
    private fun cleanupAll() {
        try {
            FeatureWhisperBridge.resetContext()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to reset WhisperBridge context", t)
        }
        
        try {
            AudioIO.clearCodecPool()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to clear AudioIO codec pool", t)
        }
        
        Log.d(TAG, "Global cleanup completed")
    }
}
