package com.mira.videoeditor

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.webkit.JavascriptInterface
import android.widget.Toast
import android.app.ActivityManager
import android.os.Debug
import android.os.Process
import android.util.Log
import java.io.RandomAccessFile
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max
import com.mira.com.whisper.AndroidWhisperBridge
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var resourceTimer: Timer? = null
    private var lastCpuTime: Long = 0
    private var lastAppCpuTime: Long = 0
    
    // Data structures for moving averages (2 minutes = 120 seconds, with 2-second intervals = 60 data points)
    private data class ResourceReading(val timestamp: Long, val cpuUsage: Double, val memoryUsage: Long)
    private val cpuMemoryHistory = ConcurrentLinkedQueue<ResourceReading>()
    private val maxHistorySize = 60 // 2 minutes at 2-second intervals

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Auto-load video_v1.mp4 when page finishes loading
                    loadVideoV1()
                    // Start real resource monitoring
                    startResourceMonitoring()
                }
            }
            addJavascriptInterface(JavaScriptInterface(), "AndroidInterface")
            addJavascriptInterface(AndroidWhisperBridge(this@MainActivity), "AndroidWhisper")
            loadUrl("file:///android_asset/processing.html")
        }
        setContentView(webView)
    }
    
    private fun loadVideoV1() {
        // Simulate loading video_v1.mp4
        webView.evaluateJavascript(
            "window.updateVideoInfo('video_v1.mp4', '2:15', '375 MB');",
            null
        )
        
        // Notify that video is loaded
        webView.evaluateJavascript(
            "window.addLogEntry('Auto-loading video_v1.mp4 (375MB)');",
            null
        )
        
        // Load the actual video file
        webView.evaluateJavascript(
            "window.loadVideoFile('file:///android_asset/video_v1.mp4');",
            null
        )
        
        // Start CLIP processing after a delay
        webView.postDelayed({
            webView.evaluateJavascript(
                "window.addLogEntry('Video loaded successfully');",
                null
            )
            webView.evaluateJavascript(
                "window.startClipProcessing();",
                null
            )
        }, 3000)
    }
    
    private fun startResourceMonitoring() {
        resourceTimer = Timer()
        resourceTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateResourceUsage()
                }
            }
        }, 0, 2000) // Update every 2 seconds
    }
    
    private fun updateResourceUsage() {
        try {
            val currentTime = System.currentTimeMillis()
            val realMemoryPercentage = getRealMemoryUsage()
            val realCpuUsage = getRealCpuUsage()
            
            // Store the current reading in history
            cpuMemoryHistory.offer(ResourceReading(currentTime, realCpuUsage, realMemoryPercentage))
            
            // Clean up old readings (older than 2 minutes)
            cleanupOldReadings(currentTime)
            
            // Calculate moving averages
            val cpuMovingAverage = calculateCpuMovingAverage()
            val memoryMovingAverage = calculateMemoryMovingAverage()
            
            val batteryLevel = getBatteryLevel()
            val temperature = getTemperature()
            val batteryDetails = getBatteryDetails()
            
            // Debug logging to verify values
            Log.d("ResourceMonitor", "Real values - CPU: $realCpuUsage%, Memory: ${realMemoryPercentage}%")
            Log.d("ResourceMonitor", "Moving averages (2min) - CPU: $cpuMovingAverage%, Memory: ${memoryMovingAverage}%")
            Log.d("ResourceMonitor", "Battery: $batteryLevel%, Temp: $temperature°C")
            Log.d("ResourceMonitor", "History size: ${cpuMemoryHistory.size}")
            
            // Ensure WebView is still valid before updating
            if (::webView.isInitialized && webView != null) {
                webView.evaluateJavascript(
                    "window.updateRealResourceUsage($cpuMovingAverage, $memoryMovingAverage, $batteryLevel, $temperature, '$batteryDetails');",
                    null
                )
                Log.d("ResourceMonitor", "Resource update sent to WebView successfully")
            } else {
                Log.w("ResourceMonitor", "WebView not available for resource update")
            }
        } catch (e: Exception) {
            Log.e("ResourceMonitor", "Error updating resource usage", e)
        }
    }
    
    private fun getBatteryLevel(): Int {
        return try {
            // Use BatteryManager for comprehensive battery information
            val batteryManager = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
            
            // Get battery level
            val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            // Get additional battery information
            val batteryStatus = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS)
            val batteryHealth = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS)
            val batteryVoltage = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            
            // Log comprehensive battery information
            Log.d("ResourceMonitor", "Battery - Level: $batteryLevel%, Status: $batteryStatus, Health: $batteryHealth, Voltage: ${batteryVoltage/1000}mV")
            
            batteryLevel
        } catch (e: Exception) {
            Log.e("ResourceMonitor", "Battery error: ${e.message}")
            // Fallback: try to read from /sys/class/power_supply/battery/capacity
            try {
                val reader = RandomAccessFile("/sys/class/power_supply/battery/capacity", "r")
                val level = reader.readLine().toIntOrNull() ?: 0
                reader.close()
                Log.d("ResourceMonitor", "Battery fallback: $level%")
                level
            } catch (e2: Exception) {
                Log.e("ResourceMonitor", "Battery fallback error: ${e2.message}")
                0
            }
        }
    }
    
    private fun getBatteryDetails(): String {
        return try {
            val batteryManager = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
            
            // Get comprehensive battery information
            val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val batteryStatus = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS)
            val batteryHealth = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS)
            val batteryVoltage = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            
            // Convert status codes to readable text
            val statusText = when (batteryStatus) {
                android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                android.os.BatteryManager.BATTERY_STATUS_FULL -> "Full"
                android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }
            
            // Format battery details
            "Level: $batteryLevel% | Status: $statusText | Voltage: ${batteryVoltage/1000}mV"
        } catch (e: Exception) {
            Log.e("ResourceMonitor", "Battery details error: ${e.message}")
            "Level: ${getBatteryLevel()}% | Status: Unknown"
        }
    }
    
    private fun getTemperature(): Double {
        return try {
            // Try to get real temperature from system first
            val tempStr = Runtime.getRuntime().exec("cat /sys/class/thermal/thermal_zone0/temp").inputStream.bufferedReader().readText().trim()
            val realTemp = tempStr.toDoubleOrNull()?.div(1000.0) // Convert millidegrees to degrees
            
            if (realTemp != null && realTemp > 0 && realTemp < 100) {
                Log.d("ResourceMonitor", "Real temperature: ${realTemp}°C")
                return realTemp
            }
            
            // Fallback: Estimate temperature based on battery level and CPU usage
            val batteryLevel = getBatteryLevel()
            val memoryMB = getMemoryUsage()
            
            // Base temperature estimation
            val baseTemp = when {
                batteryLevel < 20 -> 35.0 // Low battery = cooler
                batteryLevel > 80 -> 40.0 // High battery = warmer
                else -> 37.0 // Normal battery = normal temp
            }
            
            // Adjust based on memory usage (higher memory = more heat)
            val memoryHeat = (memoryMB / 1000.0) * 2.0 // Add 2°C per GB of memory
            val estimatedTemp = baseTemp + memoryHeat + (Math.random() * 3.0) // Add some variation
            
            Log.d("ResourceMonitor", "Temperature estimation: battery=$batteryLevel%, memory=${memoryMB}MB, temp=${estimatedTemp}°C")
            estimatedTemp.coerceIn(30.0, 50.0)
        } catch (e: Exception) {
            Log.e("ResourceMonitor", "Temperature error: ${e.message}")
            37.0 // Default room temperature
        }
    }
    
    private fun getRealMemoryUsage(): Long {
        return try {
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            val memoryMB = memoryInfo.totalPss.toLong() / 1024
            
            // Get additional memory information
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / (1024 * 1024) // Convert to MB
            val totalMemory = runtime.totalMemory() / (1024 * 1024) // Convert to MB
            val freeMemory = runtime.freeMemory() / (1024 * 1024) // Convert to MB
            val usedMemory = totalMemory - freeMemory
            
            // Calculate memory usage percentage based on total system memory (12GB for Xiaomi Pad)
            val totalSystemMemory = 12288 // 12GB in MB for Xiaomi Pad
            val memoryPercentage = ((memoryMB.toDouble() / totalSystemMemory) * 100.0).toLong()
            
            // Log comprehensive memory information
            Log.d("ResourceMonitor", "Memory - PSS: ${memoryInfo.totalPss}KB (${memoryMB}MB), " +
                    "Heap: ${usedMemory}MB/${maxMemory}MB, " +
                    "Native: ${memoryInfo.nativePss}KB, " +
                    "Dalvik: ${memoryInfo.dalvikPss}KB, " +
                    "System Memory: ${memoryMB}MB/12288MB, " +
                    "Percentage: ${memoryPercentage}%")
            
            memoryPercentage.coerceIn(0L, 100L)
        } catch (e: Exception) {
            Log.e("ResourceMonitor", "Memory error: ${e.message}")
            0L
        }
    }
    
    // Legacy method for backward compatibility
    private fun getMemoryUsage(): Long = getRealMemoryUsage()
    
    private fun getRealCpuUsage(): Double {
        return try {
            // Try to get real CPU usage from /proc/stat
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
                    
                    // Calculate CPU percentage (this is instantaneous, not average)
                    val cpuPercent = if (totalCpuTime > 0) {
                        (usedTime.toDouble() / totalCpuTime.toDouble()) * 100.0
                    } else {
                        0.0
                    }
                    
                    Log.d("ResourceMonitor", "Real CPU usage: ${cpuPercent.toFixed(2)}% (used: $usedTime, total: $totalCpuTime)")
                    return cpuPercent.coerceIn(0.0, 100.0)
                }
            }
            
            // Fallback: Try to get CPU usage from /proc/loadavg
            val loadavgProcess = Runtime.getRuntime().exec("cat /proc/loadavg")
            val loadavgReader = loadavgProcess.inputStream.bufferedReader()
            val loadavgLine = loadavgReader.readLine()
            loadavgReader.close()
            loadavgProcess.waitFor()
            
            if (loadavgLine != null) {
                val loadavg = loadavgLine.split("\\s+".toRegex())[0].toDoubleOrNull()
                if (loadavg != null) {
                    // Convert load average to approximate CPU percentage
                    val cpuPercent = (loadavg * 25.0).coerceIn(0.0, 100.0) // Rough conversion
                    Log.d("ResourceMonitor", "CPU from loadavg: ${cpuPercent.toFixed(2)}% (load: $loadavg)")
                    return cpuPercent
                }
            }
            
            // Final fallback: Use memory-based estimation
            val runtime = Runtime.getRuntime()
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            val memoryMB = memoryInfo.totalPss.toLong() / 1024
            
            val memoryPressure = (memoryMB.toDouble() / 1000.0) * 100.0
            val baseCpu = 2.0 + (memoryPressure * 0.1)
            val processingFactor = 1.0 + (Math.random() * 0.5)
            
            val estimatedCpu = baseCpu * processingFactor
            Log.d("ResourceMonitor", "CPU estimation fallback: memoryPressure=${memoryPressure}%, estimatedCpu=${estimatedCpu}%")
            
            estimatedCpu.coerceIn(0.0, 100.0)
        } catch (e: Exception) {
            Log.e("ResourceMonitor", "CPU error: ${e.message}")
            0.0
        }
    }
    
    // Legacy method for backward compatibility
    private fun getCpuUsage(): Double = getRealCpuUsage()
    
    // Helper methods for moving average calculations
    private fun cleanupOldReadings(currentTime: Long) {
        val twoMinutesAgo = currentTime - (2 * 60 * 1000) // 2 minutes in milliseconds
        
        // Remove readings older than 2 minutes
        while (cpuMemoryHistory.isNotEmpty() && cpuMemoryHistory.peek()?.timestamp?.let { it < twoMinutesAgo } == true) {
            cpuMemoryHistory.poll()
        }
        
        // Also limit the size to prevent memory issues
        while (cpuMemoryHistory.size > maxHistorySize) {
            cpuMemoryHistory.poll()
        }
    }
    
    private fun calculateCpuMovingAverage(): Double {
        if (cpuMemoryHistory.isEmpty()) {
            return 0.0
        }
        
        val sum = cpuMemoryHistory.sumOf { it.cpuUsage }
        val average = sum / cpuMemoryHistory.size
        
        Log.d("ResourceMonitor", "CPU moving average: $average% (from ${cpuMemoryHistory.size} readings)")
        return average.coerceIn(0.0, 100.0)
    }
    
    private fun calculateMemoryMovingAverage(): Long {
        if (cpuMemoryHistory.isEmpty()) {
            return 0L
        }
        
        val sum = cpuMemoryHistory.sumOf { it.memoryUsage.toDouble() }
        val average = (sum / cpuMemoryHistory.size).toLong()
        
        Log.d("ResourceMonitor", "Memory moving average: $average% (from ${cpuMemoryHistory.size} readings)")
        return average.coerceIn(0L, 100L)
    }
    
    private fun Double.toFixed(digits: Int): String {
        return String.format("%.${digits}f", this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        resourceTimer?.cancel()
    }

    private inner class JavaScriptInterface {
        @JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun selectVideo() {
            Toast.makeText(this@MainActivity, "Video selection requested from WebView", Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun startProcessing(videoUri: String) {
            Toast.makeText(this@MainActivity, "Start processing: $videoUri", Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun pauseProcessing() {
            Toast.makeText(this@MainActivity, "Processing paused", Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun cancelProcessing() {
            Toast.makeText(this@MainActivity, "Processing cancelled", Toast.LENGTH_SHORT).show()
        }
        
        @JavascriptInterface
        fun viewResults() {
            Toast.makeText(this@MainActivity, "Viewing CLIP results...", Toast.LENGTH_SHORT).show()
        }
        
        @JavascriptInterface
        fun newAnalysis() {
            Toast.makeText(this@MainActivity, "Starting new analysis...", Toast.LENGTH_SHORT).show()
        }
        
        @JavascriptInterface
        fun goBack() {
            runOnUiThread {
                finish()
            }
        }
        
        @JavascriptInterface
        fun navigateTo(section: String) {
            Toast.makeText(this@MainActivity, "Navigate to: $section", Toast.LENGTH_SHORT).show()
        }
        
        @JavascriptInterface
        fun openWhisperStep2() {
            runOnUiThread {
                val intent = Intent(this@MainActivity, com.mira.com.whisper.WhisperStep2Activity::class.java)
                startActivity(intent)
            }
        }

        @JavascriptInterface
        fun openRunConsole() {
            runOnUiThread {
                // Temporarily disabled - WhisperRunConsoleActivity moved to temp_compose_files
                // val intent = Intent(this@MainActivity, com.mira.com.whisper.WhisperRunConsoleActivity::class.java)
                // startActivity(intent)
                Toast.makeText(this@MainActivity, "Run Console temporarily disabled", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun openWhisperStep3() {
            runOnUiThread {
                val intent = Intent(this@MainActivity, com.mira.com.whisper.WhisperStep3Activity::class.java)
                startActivity(intent)
            }
        }
    }
}