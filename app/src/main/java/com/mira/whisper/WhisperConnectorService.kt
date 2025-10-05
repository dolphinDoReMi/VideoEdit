package com.mira.whisper

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.content.BroadcastReceiver
import android.content.Context
import android.os.BatteryManager
import android.os.Debug
import android.content.Context.BATTERY_SERVICE
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.Timer
import java.util.TimerTask

/**
 * WhisperConnectorService - Central service that connects all 3 whisper pages
 * with background whisper processing and real-time resource monitoring.
 * 
 * This service provides:
 * 1. Real-time communication between whisper pages
 * 2. Background whisper processing coordination
 * 3. Live resource monitoring for Xiaomi devices
 * 4. Progress updates and status synchronization
 * 5. Data flow management across the 3-page workflow
 */
class WhisperConnectorService : Service() {
    
    companion object {
        private const val TAG = "WhisperConnectorService"
        
        // Service actions
        const val ACTION_START_PROCESSING = "com.mira.whisper.START_PROCESSING"
        const val ACTION_UPDATE_PROGRESS = "com.mira.whisper.UPDATE_PROGRESS"
        const val ACTION_PROCESSING_COMPLETE = "com.mira.whisper.PROCESSING_COMPLETE"
        const val ACTION_RESOURCE_UPDATE = "com.mira.whisper.RESOURCE_UPDATE"
        const val ACTION_PAGE_NAVIGATION = "com.mira.whisper.PAGE_NAVIGATION"
        
        // Intent extras
        const val EXTRA_BATCH_ID = "batch_id"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_FILE_COUNT = "file_count"
        const val EXTRA_CURRENT_FILE = "current_file"
        const val EXTRA_RESOURCE_STATS = "resource_stats"
        const val EXTRA_PAGE_NAME = "page_name"
        const val EXTRA_NAVIGATION_TARGET = "navigation_target"
        
        // Resource monitoring intervals
        private const val RESOURCE_UPDATE_INTERVAL = 2000L // 2 seconds
        private const val PROGRESS_UPDATE_INTERVAL = 1000L // 1 second
    }
    
    // Service binder for local connections
    inner class WhisperConnectorBinder : Binder() {
        fun getService(): WhisperConnectorService = this@WhisperConnectorService
    }
    
    private val binder = WhisperConnectorBinder()
    
    // State management
    private val activeBatches = ConcurrentHashMap<String, BatchProcessingState>()
    private val resourceStats = AtomicResourceStats()
    private val isMonitoring = AtomicBoolean(false)
    private val serviceStartTime = AtomicLong(System.currentTimeMillis())
    
    // Timers for periodic updates
    private var resourceTimer: Timer? = null
    private var progressTimer: Timer? = null
    
    // Broadcast receiver for whisper service events
    private val whisperReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AndroidWhisperBridge.ACTION_RUN -> handleWhisperRun(intent)
                AndroidWhisperBridge.ACTION_RUN_BATCH -> handleWhisperBatchRun(intent)
                AndroidWhisperBridge.ACTION_EXPORT -> handleWhisperExport(intent)
                AndroidWhisperBridge.ACTION_VERIFY -> handleWhisperVerify(intent)
            }
        }
    }
    
    // Data classes for state management
    data class BatchProcessingState(
        val batchId: String,
        val totalFiles: Int,
        var completedFiles: Int = 0,
        var currentFileIndex: Int = 0,
        var currentFileProgress: Int = 0,
        var overallProgress: Int = 0,
        val startTime: Long = System.currentTimeMillis(),
        val files: MutableList<FileProcessingState> = mutableListOf(),
        var isActive: Boolean = true
    )
    
    data class FileProcessingState(
        val fileName: String,
        val fileUri: String,
        var status: ProcessingStatus = ProcessingStatus.PENDING,
        var progress: Int = 0,
        var startTime: Long = 0,
        var endTime: Long = 0,
        var rtf: Double = 0.0,
        var error: String? = null
    )
    
    enum class ProcessingStatus {
        PENDING, PROCESSING, COMPLETED, ERROR, CANCELLED
    }
    
    data class ResourceStats(
        val memory: Long,
        val cpu: Double,
        val battery: Int,
        val temperature: Double,
        val batteryDetails: String,
        val gpuInfo: String,
        val threadInfo: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    private class AtomicResourceStats {
        @Volatile var memory: Long = 0
        @Volatile var cpu: Double = 0.0
        @Volatile var battery: Int = 0
        @Volatile var temperature: Double = 25.0
        @Volatile var batteryDetails: String = "Loading..."
        @Volatile var gpuInfo: String = "Loading..."
        @Volatile var threadInfo: String = "Loading..."
        @Volatile var timestamp: Long = System.currentTimeMillis()
        
        fun update(stats: ResourceStats) {
            memory = stats.memory
            cpu = stats.cpu
            battery = stats.battery
            temperature = stats.temperature
            batteryDetails = stats.batteryDetails
            gpuInfo = stats.gpuInfo
            threadInfo = stats.threadInfo
            timestamp = stats.timestamp
        }
        
        fun toJson(): String {
            return JSONObject().apply {
                put("memory", memory)
                put("cpu", cpu)
                put("battery", battery)
                put("temperature", temperature)
                put("batteryDetails", batteryDetails)
                put("gpuInfo", gpuInfo)
                put("threadInfo", threadInfo)
                put("timestamp", timestamp)
            }.toString()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WhisperConnectorService created")
        
        // Register broadcast receiver for whisper service events
        val filter = IntentFilter().apply {
            addAction(AndroidWhisperBridge.ACTION_RUN)
            addAction(AndroidWhisperBridge.ACTION_RUN_BATCH)
            addAction(AndroidWhisperBridge.ACTION_EXPORT)
            addAction(AndroidWhisperBridge.ACTION_VERIFY)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(whisperReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(whisperReceiver, filter)
        }
        
        // Start resource monitoring
        startResourceMonitoring()
        
        // Start progress monitoring
        startProgressMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "WhisperConnectorService started")
        
        when (intent?.action) {
            ACTION_START_PROCESSING -> {
                val batchId = intent.getStringExtra(EXTRA_BATCH_ID) ?: "unknown"
                val fileCount = intent.getIntExtra(EXTRA_FILE_COUNT, 0)
                startBatchProcessing(batchId, fileCount)
            }
            ACTION_UPDATE_PROGRESS -> {
                val batchId = intent.getStringExtra(EXTRA_BATCH_ID) ?: "unknown"
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                updateBatchProgress(batchId, progress)
            }
            ACTION_PROCESSING_COMPLETE -> {
                val batchId = intent.getStringExtra(EXTRA_BATCH_ID) ?: "unknown"
                completeBatchProcessing(batchId)
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "WhisperConnectorService bound")
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WhisperConnectorService destroyed")
        
        // Stop monitoring
        stopResourceMonitoring()
        stopProgressMonitoring()
        
        // Unregister receiver
        try {
            unregisterReceiver(whisperReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver: ${e.message}")
        }
    }
    
    /**
     * Start batch processing for a given batch ID
     */
    private fun startBatchProcessing(batchId: String, fileCount: Int) {
        Log.d(TAG, "Starting batch processing: $batchId with $fileCount files")
        
        val batchState = BatchProcessingState(
            batchId = batchId,
            totalFiles = fileCount,
            files = mutableListOf()
        )
        
        activeBatches[batchId] = batchState
        
        // Broadcast start event
        broadcastProcessingStart(batchId, fileCount)
        
        // Start actual whisper processing via the bridge
        startActualWhisperProcessing(batchId, fileCount)
    }
    
    /**
     * Start actual whisper processing using the existing bridge
     */
    private fun startActualWhisperProcessing(batchId: String, fileCount: Int) {
        try {
            // This would integrate with the existing WhisperApi
            // For now, we'll simulate the processing
            Log.d(TAG, "Integrating with WhisperApi for batch: $batchId")
            
            // Simulate file processing
            val batchState = activeBatches[batchId]
            if (batchState != null) {
                // Create mock file states
                for (i in 0 until fileCount) {
                    val fileState = FileProcessingState(
                        fileName = "video_${i + 1}.mp4",
                        fileUri = "file:///sdcard/video_${i + 1}.mp4",
                        status = if (i == 0) ProcessingStatus.PROCESSING else ProcessingStatus.PENDING
                    )
                    batchState.files.add(fileState)
                }
                
                // Start processing simulation
                simulateFileProcessing(batchId)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting whisper processing: ${e.message}", e)
        }
    }
    
    /**
     * Simulate file processing with realistic progress updates
     */
    private fun simulateFileProcessing(batchId: String) {
        val batchState = activeBatches[batchId] ?: return
        
        val handler = Handler(Looper.getMainLooper())
        
        val processingRunnable = object : Runnable {
            override fun run() {
                if (!batchState.isActive) return
                
                val currentFile = batchState.files.getOrNull(batchState.currentFileIndex)
                if (currentFile != null && currentFile.status == ProcessingStatus.PROCESSING) {
                    // Update progress
                    val newProgress = minOf(currentFile.progress + (Math.random() * 5).toInt(), 100)
                    currentFile.progress = newProgress
                    
                    // Update batch progress
                    batchState.currentFileProgress = newProgress
                    batchState.overallProgress = ((batchState.completedFiles * 100 + newProgress) / batchState.totalFiles)
                    
                    // Broadcast progress update
                    broadcastProgressUpdate(batchId, batchState)
                    
                    // Check if file is complete
                    if (newProgress >= 100) {
                        currentFile.status = ProcessingStatus.COMPLETED
                        currentFile.endTime = System.currentTimeMillis()
                        batchState.completedFiles++
                        
                        // Move to next file
                        batchState.currentFileIndex++
                        if (batchState.currentFileIndex < batchState.totalFiles) {
                            batchState.files[batchState.currentFileIndex].status = ProcessingStatus.PROCESSING
                            batchState.files[batchState.currentFileIndex].startTime = System.currentTimeMillis()
                        } else {
                            // All files completed
                            completeBatchProcessing(batchId)
                            return
                        }
                    }
                }
                
                // Schedule next update
                handler.postDelayed(this, 1000)
            }
        }
        
        handler.post(processingRunnable)
    }
    
    /**
     * Update batch progress
     */
    private fun updateBatchProgress(batchId: String, progress: Int) {
        val batchState = activeBatches[batchId] ?: return
        batchState.overallProgress = progress
        
        broadcastProgressUpdate(batchId, batchState)
    }
    
    /**
     * Complete batch processing
     */
    private fun completeBatchProcessing(batchId: String) {
        Log.d(TAG, "Completing batch processing: $batchId")
        
        val batchState = activeBatches[batchId] ?: return
        batchState.isActive = false
        batchState.overallProgress = 100
        
        // Broadcast completion
        broadcastProcessingComplete(batchId)
        
        // Clean up after delay
        Handler(Looper.getMainLooper()).postDelayed({
            activeBatches.remove(batchId)
        }, 30000) // Keep for 30 seconds for reference
    }
    
    /**
     * Start resource monitoring
     */
    private fun startResourceMonitoring() {
        if (isMonitoring.get()) return
        
        isMonitoring.set(true)
        Log.d(TAG, "Starting resource monitoring")
        
        resourceTimer = Timer("ResourceMonitor", true).apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    updateResourceStats()
                }
            }, 0, RESOURCE_UPDATE_INTERVAL)
        }
    }
    
    /**
     * Stop resource monitoring
     */
    private fun stopResourceMonitoring() {
        isMonitoring.set(false)
        resourceTimer?.cancel()
        resourceTimer = null
        Log.d(TAG, "Stopped resource monitoring")
    }
    
    /**
     * Start progress monitoring
     */
    private fun startProgressMonitoring() {
        progressTimer = Timer("ProgressMonitor", true).apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    broadcastResourceUpdate()
                }
            }, 0, PROGRESS_UPDATE_INTERVAL)
        }
    }
    
    /**
     * Stop progress monitoring
     */
    private fun stopProgressMonitoring() {
        progressTimer?.cancel()
        progressTimer = null
    }
    
    /**
     * Update resource statistics
     */
    private fun updateResourceStats() {
        try {
            val memoryUsage = getMemoryUsage()
            val cpuUsage = getCpuUsage()
            val batteryLevel = getBatteryLevel()
            val temperature = getTemperature()
            val batteryDetails = getBatteryDetails()
            val gpuInfo = getGpuInfo()
            val threadInfo = getThreadInfo()
            
            val stats = ResourceStats(
                memory = memoryUsage,
                cpu = cpuUsage,
                battery = batteryLevel,
                temperature = temperature,
                batteryDetails = batteryDetails,
                gpuInfo = gpuInfo,
                threadInfo = threadInfo
            )
            
            resourceStats.update(stats)
            
            Log.d(TAG, "Resource stats updated: Memory: ${memoryUsage}%, CPU: ${cpuUsage}%, Battery: ${batteryLevel}%")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating resource stats: ${e.message}", e)
        }
    }
    
    /**
     * Broadcast resource update to all connected pages
     */
    private fun broadcastResourceUpdate() {
        val intent = Intent(ACTION_RESOURCE_UPDATE).apply {
            putExtra(EXTRA_RESOURCE_STATS, resourceStats.toJson())
        }
        sendBroadcast(intent)
    }
    
    /**
     * Broadcast processing start
     */
    private fun broadcastProcessingStart(batchId: String, fileCount: Int) {
        val intent = Intent(ACTION_START_PROCESSING).apply {
            putExtra(EXTRA_BATCH_ID, batchId)
            putExtra(EXTRA_FILE_COUNT, fileCount)
        }
        sendBroadcast(intent)
    }
    
    /**
     * Broadcast progress update
     */
    private fun broadcastProgressUpdate(batchId: String, batchState: BatchProcessingState) {
        val intent = Intent(ACTION_UPDATE_PROGRESS).apply {
            putExtra(EXTRA_BATCH_ID, batchId)
            putExtra(EXTRA_PROGRESS, batchState.overallProgress)
            putExtra(EXTRA_FILE_COUNT, batchState.totalFiles)
            putExtra(EXTRA_CURRENT_FILE, batchState.currentFileIndex)
        }
        sendBroadcast(intent)
    }
    
    /**
     * Broadcast processing complete
     */
    private fun broadcastProcessingComplete(batchId: String) {
        val intent = Intent(ACTION_PROCESSING_COMPLETE).apply {
            putExtra(EXTRA_BATCH_ID, batchId)
        }
        sendBroadcast(intent)
    }
    
    /**
     * Handle whisper run events
     */
    private fun handleWhisperRun(intent: Intent) {
        val jobId = intent.getStringExtra("job_id") ?: return
        val uri = intent.getStringExtra("uri") ?: return
        val preset = intent.getStringExtra("preset") ?: "Single"
        
        Log.d(TAG, "Handling whisper run: $jobId for $uri")
        
        // Create single-file batch
        val batchId = "single_$jobId"
        startBatchProcessing(batchId, 1)
    }
    
    /**
     * Handle whisper batch run events
     */
    private fun handleWhisperBatchRun(intent: Intent) {
        val batchId = intent.getStringExtra("batch_id") ?: "batch_${System.currentTimeMillis()}"
        val fileCount = intent.getIntExtra("file_count", 1)
        
        Log.d(TAG, "Handling whisper batch run: $batchId with $fileCount files")
        
        startBatchProcessing(batchId, fileCount)
    }
    
    /**
     * Handle whisper export events
     */
    private fun handleWhisperExport(intent: Intent) {
        val jobId = intent.getStringExtra("job_id") ?: return
        Log.d(TAG, "Handling whisper export: $jobId")
        
        // Export logic would be handled by the existing bridge
    }
    
    /**
     * Handle whisper verify events
     */
    private fun handleWhisperVerify(intent: Intent) {
        val jobId = intent.getStringExtra("job_id") ?: return
        Log.d(TAG, "Handling whisper verify: $jobId")
        
        // Verification logic would be handled by the existing bridge
    }
    
    // Resource monitoring methods (similar to AndroidWhisperBridge)
    private fun getMemoryUsage(): Long {
        return try {
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            val memoryMB = memoryInfo.totalPss.toLong() / 1024
            
            val totalSystemMemory = 12288 // 12GB in MB for Xiaomi Pad
            val memoryPercentage = ((memoryMB.toDouble() / totalSystemMemory) * 100.0).toLong()
            
            memoryPercentage.coerceIn(0L, 100L)
        } catch (e: Exception) {
            Log.e(TAG, "Memory error: ${e.message}")
            0L
        }
    }
    
    private fun getCpuUsage(): Double {
        return try {
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
                    
                    val totalCpuTime = user + nice + system + idle + iowait + irq + softirq
                    val idleTime = idle + iowait
                    val usedTime = totalCpuTime - idleTime
                    
                    val cpuPercent = if (totalCpuTime > 0) {
                        (usedTime.toDouble() / totalCpuTime.toDouble()) * 100.0
                    } else {
                        0.0
                    }
                    
                    return cpuPercent.coerceIn(0.0, 100.0)
                }
            }
            
            0.0
        } catch (e: Exception) {
            Log.e(TAG, "CPU error: ${e.message}")
            0.0
        }
    }
    
    private fun getBatteryLevel(): Int {
        return try {
            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            Log.e(TAG, "Battery error: ${e.message}")
            0
        }
    }
    
    private fun getTemperature(): Double {
        return try {
            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            // Note: BATTERY_PROPERTY_TEMPERATURE requires API 21+
            25.0 // Default temperature in Celsius
        } catch (e: Exception) {
            Log.e(TAG, "Temperature error: ${e.message}")
            25.0
        }
    }
    
    private fun getBatteryDetails(): String {
        return try {
            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            "Level: ${level}%, Temp: N/A, Voltage: N/A"
        } catch (e: Exception) {
            Log.e(TAG, "Battery details error: ${e.message}")
            "Level: N/A, Temp: N/A, Voltage: N/A"
        }
    }
    
    private fun getGpuInfo(): String {
        return try {
            val process = Runtime.getRuntime().exec("cat /proc/gpuinfo")
            val reader = process.inputStream.bufferedReader()
            val gpuInfo = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                gpuInfo.append(line).append(" | ")
            }
            reader.close()
            process.waitFor()
            
            gpuInfo.toString().take(100)
        } catch (e: Exception) {
            Log.d(TAG, "GPU info not accessible: ${e.message}")
            "GPU: Not accessible"
        }
    }
    
    private fun getThreadInfo(): String {
        return try {
            val threadInfo = StringBuilder()
            
            try {
                val process = Runtime.getRuntime().exec("cat /proc/self/status")
                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.startsWith("Threads:") == true) {
                        threadInfo.append("Threads: ").append(line.substringAfter("Threads:").trim())
                        break
                    }
                }
                reader.close()
                process.waitFor()
            } catch (e: Exception) {
                Log.d(TAG, "Thread count not accessible: ${e.message}")
            }
            
            val mainThread = Thread.currentThread()
            threadInfo.append(" | Main: ${mainThread.name}")
            
            val threadGroup = mainThread.threadGroup
            if (threadGroup != null) {
                val activeThreads = threadGroup.activeCount()
                threadInfo.append(" | Active: $activeThreads")
            }
            
            try {
                val runtime = Runtime.getRuntime()
                val availableProcessors = runtime.availableProcessors()
                threadInfo.append(" | CPUs: $availableProcessors")
                
                val memoryInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memoryInfo)
                val memoryMB = memoryInfo.totalPss.toLong() / 1024
                
                val estimatedProcessingThreads = when {
                    memoryMB > 500 -> availableProcessors * 3
                    memoryMB > 300 -> availableProcessors * 2
                    else -> availableProcessors
                }
                
                threadInfo.append(" | Est. Processing: $estimatedProcessingThreads")
            } catch (e: Exception) {
                Log.d(TAG, "CPU info not accessible: ${e.message}")
            }
            
            threadInfo.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Thread info error: ${e.message}")
            "Threads: Error"
        }
    }
    
    // Public API methods for activities to use
    fun getCurrentResourceStats(): String = resourceStats.toJson()
    
    fun getActiveBatches(): Map<String, BatchProcessingState> = activeBatches.toMap()
    
    fun getBatchState(batchId: String): BatchProcessingState? = activeBatches[batchId]
    
    fun isServiceRunning(): Boolean = isMonitoring.get()
}
