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
                callResourceUpdate(resourceData)
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
    
    // Direct callback support instead of broadcasts
    private var resourceCallback: ((JSONObject) -> Unit)? = null
    private var cpuCallback: ((Double) -> Unit)? = null
    private var memoryCallback: ((Double) -> Unit)? = null
    private var batteryCallback: ((Int) -> Unit)? = null
    private var temperatureCallback: ((Double) -> Unit)? = null
    
    /**
     * Register callbacks for direct communication
     */
    fun setResourceCallback(callback: (JSONObject) -> Unit) {
        resourceCallback = callback
    }
    
    fun setCpuCallback(callback: (Double) -> Unit) {
        cpuCallback = callback
    }
    
    fun setMemoryCallback(callback: (Double) -> Unit) {
        memoryCallback = callback
    }
    
    fun setBatteryCallback(callback: (Int) -> Unit) {
        batteryCallback = callback
    }
    
    fun setTemperatureCallback(callback: (Double) -> Unit) {
        temperatureCallback = callback
    }
    
    /**
     * Clear all callbacks
     */
    fun clearCallbacks() {
        resourceCallback = null
        cpuCallback = null
        memoryCallback = null
        batteryCallback = null
        temperatureCallback = null
    }
    
    private fun callResourceUpdate(resourceData: JSONObject) {
        try {
            // Call complete resource data callback
            resourceCallback?.invoke(resourceData)
            
            // Call individual metric callbacks
            if (resourceData.has("cpu")) {
                cpuCallback?.invoke(resourceData.getDouble("cpu"))
            }
            
            if (resourceData.has("memory")) {
                memoryCallback?.invoke(resourceData.getDouble("memory"))
            }
            
            if (resourceData.has("battery")) {
                batteryCallback?.invoke(resourceData.getDouble("battery").toInt())
            }
            
            if (resourceData.has("temperature")) {
                temperatureCallback?.invoke(resourceData.getDouble("temperature"))
            }
            
            Log.d(TAG, "Resource data callbacks called successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calling resource update callbacks: ${e.message}")
        }
    }
    
}
