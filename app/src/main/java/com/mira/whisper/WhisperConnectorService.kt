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
import com.mira.com.feature.whisper.data.db.AsrDb

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
        const val EXTRA_URIS = "uris"
        
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
    }
    
    @Volatile private var lastStartUris: List<String>? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "WhisperConnectorService started")
        
        when (intent?.action) {
            ACTION_START_PROCESSING -> {
                val batchId = intent.getStringExtra(EXTRA_BATCH_ID) ?: "unknown"
                val fileCount = intent.getIntExtra(EXTRA_FILE_COUNT, 0)
                lastStartUris = intent.getStringArrayListExtra(EXTRA_URIS)?.toList() ?: emptyList()
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
        // Immediately request UI navigation to processing page (async queue -> navigate)
        broadcastPageNavigation("processing")
        
        // Start actual whisper processing via the bridge
        startActualWhisperProcessing(batchId, fileCount)
    }
    
    /**
     * Start actual whisper processing using WorkManager monitoring
     */
    private fun startActualWhisperProcessing(batchId: String, fileCount: Int) {
        try {
            Log.d(TAG, "Starting actual whisper processing for batch: $batchId")

            val batchState = activeBatches[batchId]
            if (batchState != null) {
                // Initialize file states for tracking using provided URIs if available
                val provided = lastStartUris ?: emptyList()
                if (provided.isNotEmpty()) {
                    provided.forEachIndexed { i, u ->
                        val name = android.net.Uri.parse(u).lastPathSegment ?: "file_${i + 1}"
                        batchState.files.add(
                            FileProcessingState(
                                fileName = name,
                                fileUri = u,
                                status = ProcessingStatus.PENDING
                            )
                        )
                    }
                } else {
                    for (i in 0 until fileCount) {
                        val fileState = FileProcessingState(
                            fileName = "video_${i + 1}.mp4",
                            fileUri = "file:///sdcard/video_${i + 1}.mp4",
                            status = ProcessingStatus.PENDING
                        )
                        batchState.files.add(fileState)
                    }
                }

                // Start monitoring WorkManager jobs
                startWorkManagerMonitoring(batchId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting whisper processing: ${e.message}", e)
        }
    }

    /**
     * Monitor WorkManager jobs for this batch
     */
    private fun startWorkManagerMonitoring(batchId: String) {
        Log.d(TAG, "Starting WorkManager monitoring for batch: $batchId")

        // Start a timer to periodically check WorkManager status
        progressTimer = Timer()
        progressTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                try {
                    checkWorkManagerProgress(batchId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking WorkManager progress: ${e.message}", e)
                }
            }
        }, 1000, 2000) // Check every 2 seconds
    }

    /**
     * Check WorkManager job progress for a batch
     */
    private fun checkWorkManagerProgress(batchId: String) {
        Log.d(TAG, "checkWorkManagerProgress called for batch: $batchId")
        val batchState = activeBatches[batchId] ?: return

        try {
            Log.d(TAG, "Querying database for jobs...")
            // Query the database for job status
            val dao = AsrDb.get(this).dao()
            val jobs = dao.getAllJobs()
            Log.d(TAG, "Found ${jobs.size} jobs in database")

            var completedJobs = 0
            var currentJobIndex = 0

            // Count completed jobs and find current processing job
            for (i in 0 until batchState.totalFiles) {
                val jobIdPrefix = "batch_${i}_" // Match the job ID pattern from TranscribeWorker
                val matchingJobs = jobs.filter { it.jobId.startsWith(jobIdPrefix) }
                Log.d(TAG, "Looking for jobs with prefix: $jobIdPrefix, found: ${matchingJobs.size}")

                if (matchingJobs.isNotEmpty()) {
                    val job = matchingJobs.first()
                    Log.d(TAG, "Job ${job.jobId} status: ${job.status}")
                    when (job.status) {
                        "DONE" -> {
                            completedJobs++
                            batchState.files[i].status = ProcessingStatus.COMPLETED
                            batchState.files[i].progress = 100
                            batchState.files[i].rtf = job.rtf ?: 0.0
                        }
                        "RUNNING" -> {
                            batchState.files[i].status = ProcessingStatus.PROCESSING
                            batchState.files[i].progress = 50 // Estimate progress
                            currentJobIndex = i
                        }
                        "ERROR" -> {
                            batchState.files[i].status = ProcessingStatus.ERROR
                            batchState.files[i].error = job.error ?: "Unknown error"
                            // Count ERROR jobs as completed to avoid infinite waiting
                            completedJobs++
                        }
                        else -> {
                            batchState.files[i].status = ProcessingStatus.PENDING
                            batchState.files[i].progress = 0
                        }
                    }
                }
            }

            // Update batch state
            batchState.completedFiles = completedJobs
            batchState.currentFileIndex = currentJobIndex
            batchState.overallProgress = Math.floor((completedJobs.toDouble() / batchState.totalFiles) * 100).toInt()

            Log.d(TAG, "Batch $batchId: completedJobs=$completedJobs, totalFiles=${batchState.totalFiles}, overallProgress=${batchState.overallProgress}%")

            // Broadcast progress update
            broadcastProgressUpdate(batchId, batchState)

            // Check if all jobs are complete
            if (completedJobs >= batchState.totalFiles) {
                Log.d(TAG, "All jobs completed for batch: $batchId")
                progressTimer?.cancel()
                broadcastProcessingComplete(batchId)
            } else {
                Log.d(TAG, "Batch $batchId not yet complete: $completedJobs/${batchState.totalFiles}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking WorkManager progress: ${e.message}", e)
        }
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
                    try {
                        val stats = getResourceStats()
                        resourceStats.update(stats)
                        broadcastResourceUpdate(resourceStats)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in resource monitoring task: ${e.message}", e)
                    }
                }
            }, 0, RESOURCE_UPDATE_INTERVAL)
        }
    }

    /**
     * Stop resource monitoring
     */
    private fun stopResourceMonitoring() {
        if (!isMonitoring.get()) return

        isMonitoring.set(false)
        resourceTimer?.cancel()
        resourceTimer = null
        Log.d(TAG, "Stopped resource monitoring")
    }

    /**
     * Stop progress monitoring
     */
    private fun stopProgressMonitoring() {
        progressTimer?.cancel()
        progressTimer = null
        Log.d(TAG, "Stopped progress monitoring")
    }

    /**
     * Get current resource stats
     */
    private fun getResourceStats(): ResourceStats {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalMemory = memoryInfo.totalMem
        val availableMemory = memoryInfo.availMem

        // CPU usage (simplified, for real usage needs /proc/stat parsing)
        val cpuUsage = try {
            val reader = java.io.RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()
            val toks = load.split(" +".toRegex()).drop(1).map { it.toLong() }
            val idle = toks[3]
            val total = toks.sum()
            val diffIdle = idle - lastIdleCpu
            val diffTotal = total - lastTotalCpu
            lastIdleCpu = idle
            lastTotalCpu = total
            if (diffTotal > 0) (100.0 * (diffTotal - diffIdle) / diffTotal) else 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting CPU stats: ${e.message}", e)
            0.0
        }

        // Battery info
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val batteryTemp = getBatteryTemperature()
        val batteryStatus = getBatteryStatus(batteryManager)

        // GPU info (placeholder, actual GPU monitoring is complex and device-specific)
        val gpuInfo = "N/A"

        // Thread info (placeholder)
        val threadInfo = "Threads: ${Debug.getThreadAllocCount()}"

        return ResourceStats(
            memory = totalMemory - availableMemory, // Used memory
            cpu = cpuUsage,
            battery = batteryLevel,
            temperature = batteryTemp,
            batteryDetails = batteryStatus,
            gpuInfo = gpuInfo,
            threadInfo = threadInfo
        )
    }

    private var lastIdleCpu: Long = 0
    private var lastTotalCpu: Long = 0

    private fun getBatteryTemperature(): Double {
        val intent: Intent? = applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)?.div(10.0) ?: 0.0
        return temp
    }

    private fun getBatteryStatus(bm: BatteryManager): String {
        val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
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
            putExtra("completed_files", batchState.completedFiles)
            putExtra("current_file_progress", batchState.currentFileProgress)
            // Add file details as JSON array
            val filesJson = JSONArray()
            batchState.files.forEach { fileState ->
                filesJson.put(JSONObject().apply {
                    put("fileName", fileState.fileName)
                    put("fileUri", fileState.fileUri)
                    put("status", fileState.status.name)
                    put("progress", fileState.progress)
                    put("rtf", fileState.rtf)
                    put("error", fileState.error)
                })
            }
            putExtra("file_states", filesJson.toString())
        }
        sendBroadcast(intent)
    }

    /**
     * Broadcast processing complete
     */
    private fun broadcastProcessingComplete(batchId: String) {
        Log.d(TAG, "Broadcasting processing complete for batch: $batchId")
        val intent = Intent(ACTION_PROCESSING_COMPLETE).apply {
            putExtra(EXTRA_BATCH_ID, batchId)
        }
        sendBroadcast(intent)
        Log.d(TAG, "Broadcast sent for batch: $batchId")
    }

    /**
     * Broadcast page navigation request to all connected pages
     */
    private fun broadcastPageNavigation(targetPage: String) {
        val intent = Intent(ACTION_PAGE_NAVIGATION).apply {
            putExtra(EXTRA_NAVIGATION_TARGET, targetPage)
        }
        sendBroadcast(intent)
    }

    /**
     * Broadcast resource update
     */
    private fun broadcastResourceUpdate(resourceStats: AtomicResourceStats) {
        val intent = Intent(ACTION_RESOURCE_UPDATE).apply {
            putExtra(EXTRA_RESOURCE_STATS, resourceStats.toJson())
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
        // Implementation for export
        Log.d(TAG, "Handling whisper export event")
    }

    /**
     * Handle whisper verify events
     */
    private fun handleWhisperVerify(intent: Intent) {
        // Implementation for verify
        Log.d(TAG, "Handling whisper verify event")
    }

    /**
     * Update batch progress (legacy method for compatibility)
     */
    private fun updateBatchProgress(batchId: String, progress: Int) {
        val batchState = activeBatches[batchId] ?: return
        batchState.overallProgress = progress
        broadcastProgressUpdate(batchId, batchState)
    }
}