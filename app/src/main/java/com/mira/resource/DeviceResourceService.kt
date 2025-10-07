package com.mira.resource

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background service for stable device resource monitoring.
 * Uses StableResourceMonitor for reliable, smoothed resource data.
 */
class DeviceResourceService : Service() {
    
    companion object {
        private const val TAG = "DeviceResourceService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "device_resource_monitor"
        
        // Broadcast actions
        const val ACTION_RESOURCE_UPDATE = "com.mira.whisper.RESOURCE_UPDATE"
        const val ACTION_CPU_UPDATE = "com.mira.whisper.CPU_UPDATE"
        const val ACTION_MEMORY_UPDATE = "com.mira.whisper.MEMORY_UPDATE"
        const val ACTION_BATTERY_UPDATE = "com.mira.whisper.BATTERY_UPDATE"
        const val ACTION_TEMPERATURE_UPDATE = "com.mira.whisper.TEMPERATURE_UPDATE"
        
        // Intent extras
        const val EXTRA_RESOURCE_DATA = "resource_data"
        const val EXTRA_CPU_USAGE = "cpu_usage"
        const val EXTRA_MEMORY_USAGE = "memory_usage"
        const val EXTRA_BATTERY_LEVEL = "battery_level"
        const val EXTRA_TEMPERATURE = "temperature"
        const val EXTRA_TIMESTAMP = "timestamp"
    }
    
    private val isRunning = AtomicBoolean(false)
    private lateinit var stableResourceMonitor: StableResourceMonitor
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DeviceResourceService created")
        createNotificationChannel()
        stableResourceMonitor = StableResourceMonitor(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "DeviceResourceService started")
        
        if (!isRunning.get()) {
            isRunning.set(true)
            startForeground(NOTIFICATION_ID, createNotification())
            stableResourceMonitor.startMonitoring { resourceData ->
                broadcastResourceUpdate(resourceData)
            }
            Log.d(TAG, "Stable resource monitoring started")
        }
        
        return START_STICKY // Restart if killed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DeviceResourceService destroyed")
        isRunning.set(false)
        stableResourceMonitor.stopMonitoring()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Resource Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors device resources in background"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, com.mira.whisper.WhisperMainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Device Resource Monitor")
            .setContentText("Monitoring CPU, Memory, Battery, Temperature")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun broadcastResourceUpdate(resourceData: JSONObject) {
        try {
            // Broadcast complete resource data
            val intent = Intent(ACTION_RESOURCE_UPDATE).apply {
                putExtra(EXTRA_RESOURCE_DATA, resourceData.toString())
                putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
            }
            intent.setPackage(packageName)
            sendBroadcast(intent)
            
            // Broadcast individual metrics for specific listeners
            if (resourceData.has("cpu")) {
                val cpuIntent = Intent(ACTION_CPU_UPDATE).apply {
                    putExtra(EXTRA_CPU_USAGE, resourceData.getDouble("cpu"))
                    putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                }
                cpuIntent.setPackage(packageName)
                sendBroadcast(cpuIntent)
            }
            
            if (resourceData.has("memory")) {
                val memoryIntent = Intent(ACTION_MEMORY_UPDATE).apply {
                    putExtra(EXTRA_MEMORY_USAGE, resourceData.getDouble("memory"))
                    putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                }
                memoryIntent.setPackage(packageName)
                sendBroadcast(memoryIntent)
            }
            
            if (resourceData.has("battery")) {
                val batteryIntent = Intent(ACTION_BATTERY_UPDATE).apply {
                    putExtra(EXTRA_BATTERY_LEVEL, resourceData.getDouble("battery").toInt())
                    putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                }
                batteryIntent.setPackage(packageName)
                sendBroadcast(batteryIntent)
            }
            
            if (resourceData.has("temperature")) {
                val tempIntent = Intent(ACTION_TEMPERATURE_UPDATE).apply {
                    putExtra(EXTRA_TEMPERATURE, resourceData.getDouble("temperature"))
                    putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                }
                tempIntent.setPackage(packageName)
                sendBroadcast(tempIntent)
            }
            
            Log.d(TAG, "Resource data broadcasted successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Broadcast error: ${e.message}")
        }
    }
    
}
