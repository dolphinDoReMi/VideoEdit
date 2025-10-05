package com.mira.resource

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileReader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background service for real-time device resource monitoring.
 * Runs independently of UI and broadcasts resource data to connected clients.
 */
class DeviceResourceService : Service() {
    
    companion object {
        private const val TAG = "DeviceResourceService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "device_resource_monitor"
        private const val UPDATE_INTERVAL_MS = 2000L // 2 seconds
        
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
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning.get()) {
                collectAndBroadcastResources()
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }
    
    // Resource monitoring components
    private var lastCpuTime: Long = 0
    private var lastCpuIdle: Long = 0
    private var lastCpuTotal: Long = 0
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DeviceResourceService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "DeviceResourceService started")
        
        if (!isRunning.get()) {
            isRunning.set(true)
            startForeground(NOTIFICATION_ID, createNotification())
            handler.post(updateRunnable)
            Log.d(TAG, "Resource monitoring started")
        }
        
        return START_STICKY // Restart if killed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DeviceResourceService destroyed")
        isRunning.set(false)
        handler.removeCallbacks(updateRunnable)
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
        val intent = Intent(this, WhisperMainActivity::class.java)
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
    
    private fun collectAndBroadcastResources() {
        try {
            val resourceData = collectResourceData()
            broadcastResourceUpdate(resourceData)
            Log.d(TAG, "Resource data collected and broadcasted")
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting resources: ${e.message}")
        }
    }
    
    private fun collectResourceData(): JSONObject {
        val resourceData = JSONObject()
        
        // Collect all resource metrics
        resourceData.put("cpu", getCpuUsage())
        resourceData.put("memory", getMemoryUsage())
        resourceData.put("battery", getBatteryData())
        resourceData.put("temperature", getTemperature())
        resourceData.put("threads", getThreadInfo())
        resourceData.put("gpu", getGpuInfo())
        resourceData.put("timestamp", System.currentTimeMillis())
        
        return resourceData
    }
    
    private fun getCpuUsage(): Double {
        return try {
            // Method 1: /proc/stat with proper calculation
            val process = Runtime.getRuntime().exec("cat /proc/stat")
            val reader = process.inputStream.bufferedReader()
            val firstLine = reader.readLine()
            reader.close()
            process.waitFor()
            
            if (firstLine != null && firstLine.startsWith("cpu ")) {
                val parts = firstLine.split("\\s+".toRegex())
                if (parts.size >= 8) {
                    val user = parts[1].toLong()
                    val nice = parts[2].toLong()
                    val system = parts[3].toLong()
                    val idle = parts[4].toLong()
                    val iowait = parts[5].toLong()
                    val irq = parts[6].toLong()
                    val softirq = parts[7].toLong()
                    
                    val currentTotal = user + nice + system + idle + iowait + irq + softirq
                    val currentIdle = idle + iowait
                    
                    if (lastCpuTotal > 0) {
                        val totalDiff = currentTotal - lastCpuTotal
                        val idleDiff = currentIdle - lastCpuIdle
                        val usedDiff = totalDiff - idleDiff
                        
                        val cpuPercent = if (totalDiff > 0) {
                            (usedDiff.toDouble() / totalDiff.toDouble()) * 100.0
                        } else {
                            0.0
                        }
                        
                        lastCpuTotal = currentTotal
                        lastCpuIdle = currentIdle
                        
                        Log.d(TAG, "CPU usage: ${cpuPercent.toFixed(2)}%")
                        return cpuPercent.coerceIn(0.0, 100.0)
                    } else {
                        // First run, store values
                        lastCpuTotal = currentTotal
                        lastCpuIdle = currentIdle
                        return 0.0
                    }
                }
            }
            
            // Method 2: /proc/loadavg fallback
            try {
                val loadavgProcess = Runtime.getRuntime().exec("cat /proc/loadavg")
                val loadavgReader = loadavgProcess.inputStream.bufferedReader()
                val loadavgLine = loadavgReader.readLine()
                loadavgReader.close()
                loadavgProcess.waitFor()
                
                if (loadavgLine != null) {
                    val loadavg = loadavgLine.split("\\s+".toRegex())[0].toDoubleOrNull() ?: 0.0
                    val cpuCores = Runtime.getRuntime().availableProcessors()
                    val cpuPercent = (loadavg / cpuCores) * 100.0
                    Log.d(TAG, "CPU usage (loadavg): ${cpuPercent.toFixed(2)}%")
                    return cpuPercent.coerceIn(0.0, 100.0)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Loadavg method failed: ${e.message}")
            }
            
            0.0
        } catch (e: Exception) {
            Log.e(TAG, "CPU monitoring error: ${e.message}")
            0.0
        }
    }
    
    private fun getMemoryUsage(): Double {
        return try {
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            
            val pssKB = memoryInfo.totalPss.toLong()
            val pssMB = pssKB / 1024
            
            // Get system memory info
            val memInfoFile = FileReader("/proc/meminfo")
            val memInfoReader = BufferedReader(memInfoFile)
            var totalMemKB = 0L
            var availableMemKB = 0L
            
            var line: String?
            while (memInfoReader.readLine().also { line = it } != null) {
                when {
                    line?.startsWith("MemTotal:") == true -> {
                        totalMemKB = line.substringAfter("MemTotal:").trim()
                            .replace("kB", "").toLongOrNull() ?: 0L
                    }
                    line?.startsWith("MemAvailable:") == true -> {
                        availableMemKB = line.substringAfter("MemAvailable:").trim()
                            .replace("kB", "").toLongOrNull() ?: 0L
                    }
                }
            }
            memInfoReader.close()
            
            val usedMemKB = totalMemKB - availableMemKB
            val memoryPercent = if (totalMemKB > 0) {
                (usedMemKB.toDouble() / totalMemKB.toDouble()) * 100.0
            } else {
                0.0
            }
            
            Log.d(TAG, "Memory usage: ${memoryPercent.toFixed(2)}% (${pssMB}MB used)")
            memoryPercent.coerceIn(0.0, 100.0)
        } catch (e: Exception) {
            Log.e(TAG, "Memory monitoring error: ${e.message}")
            0.0
        }
    }
    
    private fun getBatteryData(): JSONObject {
        val batteryData = JSONObject()
        
        try {
            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                
                batteryData.put("level", level)
                batteryData.put("temperature", if (temperature > 0) temperature / 10.0 else 0.0)
                batteryData.put("voltage", if (voltage > 0) voltage / 1000.0 else 0.0)
                batteryData.put("status", when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                    else -> "Unknown"
                })
                
                Log.d(TAG, "Battery: ${level}%, ${temperature/10.0}°C, ${voltage/1000.0}V, $status")
            } else {
                batteryData.put("level", level)
                batteryData.put("temperature", 0.0)
                batteryData.put("voltage", 0.0)
                batteryData.put("status", "Unknown")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Battery monitoring error: ${e.message}")
            batteryData.put("level", 0)
            batteryData.put("temperature", 0.0)
            batteryData.put("voltage", 0.0)
            batteryData.put("status", "Error")
        }
        
        return batteryData
    }
    
    private fun getTemperature(): Double {
        return try {
            // Method 1: Thermal zones
            val thermalZones = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/class/thermal/thermal_zone2/temp",
                "/sys/class/thermal/thermal_zone3/temp",
                "/sys/class/thermal/thermal_zone4/temp"
            )
            
            var totalTemp = 0.0
            var validReadings = 0
            
            for (zone in thermalZones) {
                try {
                    val process = Runtime.getRuntime().exec("cat $zone")
                    val reader = process.inputStream.bufferedReader()
                    val tempStr = reader.readLine()
                    reader.close()
                    process.waitFor()
                    
                    if (tempStr != null && tempStr.isNotEmpty()) {
                        val temp = tempStr.trim().toDoubleOrNull()
                        if (temp != null && temp > 0) {
                            val tempCelsius = if (temp > 1000) temp / 1000.0 else temp
                            if (tempCelsius > 0 && tempCelsius < 100) {
                                totalTemp += tempCelsius
                                validReadings++
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Zone not accessible
                }
            }
            
            if (validReadings > 0) {
                val avgTemp = totalTemp / validReadings
                Log.d(TAG, "Temperature (thermal): ${avgTemp.toFixed(1)}°C")
                return avgTemp
            }
            
            // Method 2: Battery temperature
            try {
                val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (intent != null) {
                    val batteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                    if (batteryTemp > 0) {
                        val tempCelsius = batteryTemp / 10.0
                        Log.d(TAG, "Temperature (battery): ${tempCelsius.toFixed(1)}°C")
                        return tempCelsius
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Battery temperature failed: ${e.message}")
            }
            
            0.0
        } catch (e: Exception) {
            Log.e(TAG, "Temperature monitoring error: ${e.message}")
            0.0
        }
    }
    
    private fun getThreadInfo(): JSONObject {
        val threadData = JSONObject()
        
        try {
            val process = Runtime.getRuntime().exec("cat /proc/self/status")
            val reader = process.inputStream.bufferedReader()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("Threads:") == true) {
                    val threadCount = line.substringAfter("Threads:").trim().toIntOrNull() ?: 0
                    threadData.put("count", threadCount)
                    break
                }
            }
            reader.close()
            process.waitFor()
            
            val mainThread = Thread.currentThread()
            threadData.put("main_thread", mainThread.name)
            
            val threadGroup = mainThread.threadGroup
            if (threadGroup != null) {
                threadData.put("active_threads", threadGroup.activeCount())
            }
            
            val availableProcessors = Runtime.getRuntime().availableProcessors()
            threadData.put("cpu_cores", availableProcessors)
            
            Log.d(TAG, "Thread info collected")
        } catch (e: Exception) {
            Log.e(TAG, "Thread monitoring error: ${e.message}")
            threadData.put("count", 0)
            threadData.put("main_thread", "Unknown")
            threadData.put("active_threads", 0)
            threadData.put("cpu_cores", 0)
        }
        
        return threadData
    }
    
    private fun getGpuInfo(): String {
        return try {
            val process = Runtime.getRuntime().exec("cat /proc/gpuinfo")
            val reader = process.inputStream.bufferedReader()
            val gpuInfo = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line != null && line.isNotEmpty()) {
                    gpuInfo.append(line).append(" | ")
                }
            }
            reader.close()
            process.waitFor()
            
            val result = gpuInfo.toString().take(100)
            Log.d(TAG, "GPU info: $result")
            result
        } catch (e: Exception) {
            Log.d(TAG, "GPU info not accessible: ${e.message}")
            "GPU: Not accessible"
        }
    }
    
    private fun broadcastResourceUpdate(resourceData: JSONObject) {
        try {
            // Broadcast complete resource data
            val intent = Intent(ACTION_RESOURCE_UPDATE).apply {
                putExtra(EXTRA_RESOURCE_DATA, resourceData.toString())
                putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
            }
            sendBroadcast(intent)
            
            // Broadcast individual metrics for specific listeners
            if (resourceData.has("cpu")) {
                val cpuIntent = Intent(ACTION_CPU_UPDATE).apply {
                    putExtra(EXTRA_CPU_USAGE, resourceData.getDouble("cpu"))
                    putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                }
                sendBroadcast(cpuIntent)
            }
            
            if (resourceData.has("memory")) {
                val memoryIntent = Intent(ACTION_MEMORY_UPDATE).apply {
                    putExtra(EXTRA_MEMORY_USAGE, resourceData.getDouble("memory"))
                    putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                }
                sendBroadcast(memoryIntent)
            }
            
            if (resourceData.has("battery")) {
                val batteryIntent = Intent(ACTION_BATTERY_UPDATE).apply {
                    putExtra(EXTRA_BATTERY_LEVEL, resourceData.getJSONObject("battery").getInt("level"))
                    putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                }
                sendBroadcast(batteryIntent)
            }
            
            if (resourceData.has("temperature")) {
                val tempIntent = Intent(ACTION_TEMPERATURE_UPDATE).apply {
                    putExtra(EXTRA_TEMPERATURE, resourceData.getDouble("temperature"))
                    putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                }
                sendBroadcast(tempIntent)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Broadcast error: ${e.message}")
        }
    }
    
    private fun Double.toFixed(digits: Int): String {
        return String.format("%.${digits}f", this)
    }
}
